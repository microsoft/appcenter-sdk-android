package com.microsoft.appcenter.channel;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.CancellationException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.AppCenterIngestion;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.ingestion.models.one.PartAUtils;
import com.microsoft.appcenter.persistence.DatabasePersistence;
import com.microsoft.appcenter.persistence.Persistence;
import com.microsoft.appcenter.utils.AbstractTokenContextListener;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.IdHelper;
import com.microsoft.appcenter.utils.IdentityTokenContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;

public class DefaultChannel implements Channel {

    /**
     * Persistence batch size for {@link Persistence#getLogs(String, Collection, int, List)} when clearing.
     */
    @VisibleForTesting
    static final int CLEAR_BATCH_SIZE = 100;

    /**
     * Application context.
     */
    private final Context mContext;

    /**
     * The application secret for the ingestion service.
     */
    private String mAppSecret;

    /**
     * The installId that's required for forwarding to ingestion.
     */
    private final UUID mInstallId;

    /**
     * Channel state per log group.
     */
    private final Map<String, GroupState> mGroupStates;

    /**
     * Global listeners.
     */
    private final Collection<Listener> mListeners;

    /**
     * The Persistence instance used to store events in the local storage.
     */
    private final Persistence mPersistence;

    /**
     * The ingestion object used to send batches to the server.
     */
    private final Ingestion mIngestion;

    /**
     * A set of ingestion objects used to send batches to the server.
     */
    private final Set<Ingestion> mIngestions;

    /**
     * App Center core handler.
     */
    private final Handler mAppCenterHandler;

    /**
     * Is channel enabled?
     */
    private boolean mEnabled;

    /**
     * Is channel disabled due to connectivity issues or was the problem fatal?
     * In that case we stop accepting new logs in database.
     */
    private boolean mDiscardLogs;

    /**
     * Device properties.
     */
    private Device mDevice;

    /**
     * State checker. If this counter changes during an async call, we have to ignore the result in the callback.
     * Cancelling a database call would be unreliable, and if it's too fast you could still have the callback being called.
     */
    private int mCurrentState;

    /**
     * The Identity context object to retrieve identity token value.
     */
    private IdentityTokenContext mIdentityTokenContext;

    /**
     * Creates and initializes a new instance.
     *
     * @param context          The context.
     * @param appSecret        The application secret.
     * @param logSerializer    The log serializer.
     * @param appCenterHandler App Center looper thread handler.
     */
    public DefaultChannel(@NonNull Context context, String appSecret, @NonNull LogSerializer logSerializer, @NonNull Handler appCenterHandler, IdentityTokenContext identityTokenContext) {
        this(context, appSecret, buildDefaultPersistence(context, logSerializer), new AppCenterIngestion(context, logSerializer), appCenterHandler, identityTokenContext);
    }

    /**
     * Overloaded constructor with limited visibility that allows for dependency injection.
     *
     * @param context          The context.
     * @param appSecret        The application secret.
     * @param persistence      Persistence object for dependency injection.
     * @param ingestion        Ingestion object for dependency injection.
     * @param appCenterHandler App Center looper thread handler.
     */
    @VisibleForTesting
    DefaultChannel(@NonNull Context context, String appSecret, @NonNull Persistence persistence, @NonNull Ingestion ingestion, @NonNull Handler appCenterHandler, IdentityTokenContext identityTokenContext) {
        mContext = context;
        mAppSecret = appSecret;
        mInstallId = IdHelper.getInstallId();
        mGroupStates = new HashMap<>();
        mListeners = new LinkedHashSet<>();
        mPersistence = persistence;
        mIngestion = ingestion;
        mIngestions = new HashSet<>();
        mIngestions.add(mIngestion);
        mAppCenterHandler = appCenterHandler;
        mEnabled = true;
        identityTokenContext.addListener(new AbstractTokenContextListener() {

            @Override
            public void onNewToken() {
                newTokenCallback();
            }
        });
    }

    private void newTokenCallback() {
        // TODO [Identity56021 Add implementation here]
    }

    /**
     * Init Persistence for default constructor.
     */
    private static Persistence buildDefaultPersistence(@NonNull Context context, @NonNull LogSerializer logSerializer) {
        Persistence persistence = new DatabasePersistence(context);
        persistence.setLogSerializer(logSerializer);
        return persistence;
    }

    @Override
    public synchronized boolean setMaxStorageSize(long maxStorageSizeInBytes) {
        return mPersistence.setMaxStorageSize(maxStorageSizeInBytes);
    }

    /**
     * Call this after every async (such as database/ingestion) callback and stop processing if it returns false.
     * That means either the groupState was removed (or removed/added again),
     * or the channel was disabled (or disabled then re-enabled again).
     * If state changed, what we were trying to achieve before the async call is no longer valid.
     *
     * @param groupState    group state as before the async call.
     * @param stateSnapshot state as before the async call.
     * @return true if state did not change and code should proceed, false if state changed.
     */
    private synchronized boolean checkStateDidNotChange(GroupState groupState, int stateSnapshot) {
        return stateSnapshot == mCurrentState && groupState == mGroupStates.get(groupState.mName);
    }

    @Override
    public synchronized void setAppSecret(@NonNull String appSecret) {

        /* Set app secret. */
        mAppSecret = appSecret;

        /* Resume sending logs for groups that use default ingestion once app secret is known. */
        if (mEnabled) {
            for (GroupState groupState : mGroupStates.values()) {
                if (groupState.mIngestion == mIngestion) {
                    checkPendingLogs(groupState);
                }
            }
        }
    }

    @Override
    public synchronized void addGroup(final String groupName, int maxLogsPerBatch, long batchTimeInterval, int maxParallelBatches, Ingestion ingestion, GroupListener groupListener) {

        /* Init group. */
        AppCenterLog.debug(LOG_TAG, "addGroup(" + groupName + ")");
        ingestion = ingestion == null ? mIngestion : ingestion;
        mIngestions.add(ingestion);
        final GroupState groupState = new GroupState(groupName, maxLogsPerBatch, batchTimeInterval, maxParallelBatches, ingestion, groupListener);
        mGroupStates.put(groupName, groupState);

        /* Count pending logs. */
        groupState.mPendingLogCount = mPersistence.countLogs(groupName);

        /*
         * If no app secret, don't resume sending App Center logs from storage.
         * If the ingestion is alternate implementation we assume One Collector
         * and thus we have the keys in database.
         */
        if (mAppSecret != null || mIngestion != ingestion) {

            /* Schedule sending any pending log. */
            checkPendingLogs(groupState);
        }

        /* Call listeners so that they can react on group adding. */
        for (Listener listener : mListeners) {
            listener.onGroupAdded(groupName, groupListener);
        }
    }

    @Override
    public synchronized void removeGroup(String groupName) {
        AppCenterLog.debug(LOG_TAG, "removeGroup(" + groupName + ")");
        GroupState groupState = mGroupStates.remove(groupName);
        if (groupState != null) {
            cancelTimer(groupState);
        }

        /* Call listeners so that they can react on group removed. */
        for (Listener listener : mListeners) {
            listener.onGroupRemoved(groupName);
        }
    }

    @Override
    public synchronized void pauseGroup(String groupName, String targetToken) {
        GroupState groupState = mGroupStates.get(groupName);
        if (groupState != null) {
            if (targetToken != null) {
                String targetKey = PartAUtils.getTargetKey(targetToken);
                if (groupState.mPausedTargetKeys.add(targetKey)) {
                    AppCenterLog.debug(LOG_TAG, "pauseGroup(" + groupName + ", " + targetKey + ")");
                }
            } else if (!groupState.mPaused) {
                AppCenterLog.debug(LOG_TAG, "pauseGroup(" + groupName + ")");
                groupState.mPaused = true;
                cancelTimer(groupState);
            }

            /* Call listeners so that they can react on group resuming. */
            for (Listener listener : mListeners) {
                listener.onPaused(groupName, targetToken);
            }
        }
    }

    @Override
    public synchronized void resumeGroup(String groupName, String targetToken) {
        GroupState groupState = mGroupStates.get(groupName);
        if (groupState != null) {
            if (targetToken != null) {
                String targetKey = PartAUtils.getTargetKey(targetToken);
                if (groupState.mPausedTargetKeys.remove(targetKey)) {

                    /*
                     * Log count can be 0 in memory because of the partial pause, but we might have
                     * logs in storage for this key, a simple fix is to reevaluate log count and check
                     * for logs again. This might create a batch with fewer logs than expected as
                     * the log count does not exclude logs with paused keys, this would be an optimization
                     * that does not seem necessary for now.
                     */
                    AppCenterLog.debug(LOG_TAG, "resumeGroup(" + groupName + ", " + targetKey + ")");
                    groupState.mPendingLogCount = mPersistence.countLogs(groupName);
                    checkPendingLogs(groupState);
                }
            } else if (groupState.mPaused) {
                AppCenterLog.debug(LOG_TAG, "resumeGroup(" + groupName + ")");
                groupState.mPaused = false;
                checkPendingLogs(groupState);
            }

            /* Call listeners so that they can react on group resuming. */
            for (Listener listener : mListeners) {
                listener.onResumed(groupName, targetToken);
            }
        }
    }

    @Override
    public synchronized boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Set the enabled flag. If false, the channel will continue to persist data but not forward any item to ingestion.
     * The most common use-case would be to set it to false and enable sending again after the channel has disabled itself after receiving
     * a recoverable error (most likely related to a server issue).
     *
     * @param enabled flag to enable or disable the channel.
     */
    @Override
    public synchronized void setEnabled(boolean enabled) {
        if (mEnabled == enabled) {
            return;
        }
        if (enabled) {
            mEnabled = true;
            mDiscardLogs = false;
            mCurrentState++;
            for (Ingestion ingestion : mIngestions) {
                ingestion.reopen();
            }
            for (GroupState groupState : mGroupStates.values()) {
                checkPendingLogs(groupState);
            }
        } else {
            suspend(true, new CancellationException());
        }

        /* Notify listeners that channel state has changed. */
        for (Listener listener : mListeners) {
            listener.onGloballyEnabled(enabled);
        }
    }

    @Override
    public synchronized void setLogUrl(String logUrl) {
        mIngestion.setLogUrl(logUrl);
    }

    /**
     * Delete all persisted logs for the given group.
     *
     * @param groupName the group name.
     */
    @Override
    public synchronized void clear(String groupName) {
        if (!mGroupStates.containsKey(groupName)) {
            return;
        }
        AppCenterLog.debug(LOG_TAG, "clear(" + groupName + ")");
        mPersistence.deleteLogs(groupName);

        /* Call listeners so that they can react on group clearing. */
        for (Listener listener : mListeners) {
            listener.onClear(groupName);
        }
    }

    @Override
    public synchronized void invalidateDeviceCache() {
        mDevice = null;
    }

    /**
     * Stop sending logs until app is restarted or the channel is enabled again.
     *
     * @param deleteLogs in addition to suspending, if this is true, delete all logs from Persistence.
     * @param exception  the exception that caused suspension.
     */
    private void suspend(boolean deleteLogs, Exception exception) {
        mEnabled = false;
        mDiscardLogs = deleteLogs;
        mCurrentState++;
        for (GroupState groupState : mGroupStates.values()) {
            cancelTimer(groupState);

            /* Delete all other batches and call callback method that are currently in progress. */
            for (Iterator<Map.Entry<String, List<Log>>> iterator = groupState.mSendingBatches.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, List<Log>> entry = iterator.next();
                iterator.remove();
                if (deleteLogs) {
                    GroupListener groupListener = groupState.mListener;
                    if (groupListener != null) {
                        for (Log log : entry.getValue()) {
                            groupListener.onFailure(log, exception);
                        }
                    }
                }
            }
        }
        for (Ingestion ingestion : mIngestions) {
            try {
                ingestion.close();
            } catch (IOException e) {
                AppCenterLog.error(LOG_TAG, "Failed to close ingestion: " + ingestion, e);
            }
        }
        if (deleteLogs) {
            for (GroupState groupState : mGroupStates.values()) {
                deleteLogsOnSuspended(groupState);
            }
        } else {
            mPersistence.clearPendingLogState();
        }
    }

    private void deleteLogsOnSuspended(final GroupState groupState) {
        final List<Log> logs = new ArrayList<>();
        mPersistence.getLogs(groupState.mName, Collections.<String>emptyList(), CLEAR_BATCH_SIZE, logs);
        if (logs.size() > 0 && groupState.mListener != null) {
            for (Log log : logs) {
                groupState.mListener.onBeforeSending(log);
                groupState.mListener.onFailure(log, new CancellationException());
            }
        }
        if (logs.size() >= CLEAR_BATCH_SIZE && groupState.mListener != null) {
            deleteLogsOnSuspended(groupState);
        } else {
            mPersistence.deleteLogs(groupState.mName);
        }
    }

    @VisibleForTesting
    void cancelTimer(GroupState groupState) {
        if (groupState.mScheduled) {
            groupState.mScheduled = false;
            mAppCenterHandler.removeCallbacks(groupState.mRunnable);
        }
    }

    /**
     * This will, if we're not using the limit for pending batches, trigger sending of a new request.
     * It will also reset the counters for sending out items for both the number of items enqueued and
     * the handlers. It will do this even if we don't have reached the limit
     * of pending batches or the time interval.
     *
     * @param groupState the group state.
     */
    private synchronized void triggerIngestion(final @NonNull GroupState groupState) {
        if (!mEnabled) {
            return;
        }
        int pendingLogCount = groupState.mPendingLogCount;
        int maxFetch = Math.min(pendingLogCount, groupState.mMaxLogsPerBatch);
        AppCenterLog.debug(LOG_TAG, "triggerIngestion(" + groupState.mName + ") pendingLogCount=" + pendingLogCount);
        cancelTimer(groupState);

        /* Check if we have reached the maximum number of pending batches, log to LogCat and don't trigger another sending. */
        if (groupState.mSendingBatches.size() == groupState.mMaxParallelBatches) {
            AppCenterLog.debug(LOG_TAG, "Already sending " + groupState.mMaxParallelBatches + " batches of analytics data to the server.");
            return;
        }

        /* Get a batch from Persistence. */
        final List<Log> batch = new ArrayList<>(maxFetch);
        final int stateSnapshot = mCurrentState;
        final String batchId = mPersistence.getLogs(groupState.mName, groupState.mPausedTargetKeys, maxFetch, batch);

        /* Decrement counter. */
        groupState.mPendingLogCount -= maxFetch;

        /* Nothing more to do if no logs. */
        if (batchId == null) {
            return;
        }
        AppCenterLog.debug(LOG_TAG, "ingestLogs(" + groupState.mName + "," + batchId + ") pendingLogCount=" + groupState.mPendingLogCount);

        /* Call group listener before sending logs to ingestion service. */
        if (groupState.mListener != null) {
            for (Log log : batch) {
                groupState.mListener.onBeforeSending(log);
            }
        }

        /* Remember this batch. */
        groupState.mSendingBatches.put(batchId, batch);

        /*
         * Due to bug on old Android versions (verified on 4.0.4),
         * if we start an async task from here, i.e. the async handler thread,
         * we end up with AsyncTask configured with the wrong Handler to use for onPostExecute
         * instead of using main thread as advertised in Javadoc (and its a static field there).
         *
         * Our SDK guards against an application that would make a first async task in non UI
         * thread before SDK is initialized, but we should also avoid corrupting AsyncTask
         * with our wrong handler to avoid creating bugs in the application code since we are
         * a library.
         *
         * So make sure we execute the async task from UI thread to avoid any issue.
         */
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                sendLogs(groupState, stateSnapshot, batch, batchId);
            }
        });
    }

    /**
     * Send logs.
     *
     * @param groupState   The group state.
     * @param currentState The current state.
     * @param batch        The log batch.
     * @param batchId      The batch ID.
     */
    @MainThread
    private synchronized void sendLogs(final GroupState groupState, final int currentState, List<Log> batch, final String batchId) {
        if (checkStateDidNotChange(groupState, currentState)) {

            /* Send logs. */
            LogContainer logContainer = new LogContainer();
            logContainer.setLogs(batch);
            groupState.mIngestion.sendAsync(mIdentityTokenContext.getIdentityToken(), mAppSecret, mInstallId, logContainer, new ServiceCallback() {

                @Override
                public void onCallSucceeded(String payload, Map<String, String> headers) {
                    mAppCenterHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            handleSendingSuccess(groupState, batchId);
                        }
                    });
                }

                @Override
                public void onCallFailed(final Exception e) {
                    mAppCenterHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            handleSendingFailure(groupState, batchId, e);
                        }
                    });
                }
            });

            /* Check for more pending logs. */
            mAppCenterHandler.post(new Runnable() {

                @Override
                public void run() {
                    checkPendingLogsAfterPost(groupState, currentState);
                }
            });
        }
    }

    private void checkPendingLogsAfterPost(@NonNull final GroupState groupState, int currentState) {
        if (checkStateDidNotChange(groupState, currentState)) {
            checkPendingLogs(groupState);
        }
    }

    /**
     * The actual implementation to react to sending a batch to the server successfully.
     *
     * @param groupState The group state.
     * @param batchId    The batch ID.
     */
    private synchronized void handleSendingSuccess(@NonNull GroupState groupState, @NonNull String batchId) {
        List<Log> removedLogsForBatchId = groupState.mSendingBatches.remove(batchId);
        if (removedLogsForBatchId != null) {
            mPersistence.deleteLogs(groupState.mName, batchId);
            GroupListener groupListener = groupState.mListener;
            if (groupListener != null) {
                for (Log log : removedLogsForBatchId) {
                    groupListener.onSuccess(log);
                }
            }
            checkPendingLogs(groupState);
        }
    }

    /**
     * The actual implementation to react to not being able to send a batch to the server.
     * Will disable the sender in case of a recoverable error.
     * Will delete batch of data in case of a non-recoverable error.
     *
     * @param groupState the group state
     * @param batchId    the batch ID
     * @param e          the exception
     */
    private synchronized void handleSendingFailure(@NonNull GroupState groupState, @NonNull String batchId, @NonNull Exception e) {
        String groupName = groupState.mName;
        List<Log> removedLogsForBatchId = groupState.mSendingBatches.remove(batchId);
        if (removedLogsForBatchId != null) {
            AppCenterLog.error(LOG_TAG, "Sending logs groupName=" + groupName + " id=" + batchId + " failed", e);
            boolean recoverableError = HttpUtils.isRecoverableError(e);
            if (recoverableError) {
                groupState.mPendingLogCount += removedLogsForBatchId.size();
            } else {
                GroupListener groupListener = groupState.mListener;
                if (groupListener != null) {
                    for (Log log : removedLogsForBatchId) {
                        groupListener.onFailure(log, e);
                    }
                }
            }
            suspend(!recoverableError, e);
        }
    }

    @Override
    public synchronized void enqueue(@NonNull Log log, @NonNull final String groupName, int flags) {

        /* Check group name is registered. */
        GroupState groupState = mGroupStates.get(groupName);
        if (groupState == null) {
            AppCenterLog.error(LOG_TAG, "Invalid group name:" + groupName);
            return;
        }

        /* Check if disabled with discarding logs. */
        if (mDiscardLogs) {
            AppCenterLog.warn(LOG_TAG, "Channel is disabled, log are discarded.");
            if (groupState.mListener != null) {
                groupState.mListener.onBeforeSending(log);
                groupState.mListener.onFailure(log, new CancellationException());
            }
            return;
        }

        /* Call listeners so that they can decorate the log. */
        for (Listener listener : mListeners) {
            listener.onPreparingLog(log, groupName);
        }

        /* Attach device properties to every log if its not already attached by a service. */
        if (log.getDevice() == null) {

            /* Generate device properties only once per process life time. */
            if (mDevice == null) {
                try {
                    mDevice = DeviceInfoHelper.getDeviceInfo(mContext);
                } catch (DeviceInfoHelper.DeviceInfoException e) {
                    AppCenterLog.error(LOG_TAG, "Device log cannot be generated", e);
                    return;
                }
            }

            /* Attach device properties. */
            log.setDevice(mDevice);
        }

        /* Set date to current if not explicitly set in the past by a module (such as a crash). */
        if (log.getTimestamp() == null) {
            log.setTimestamp(new Date());
        }

        /* Notify listeners that log is prepared and is in a final state. */
        for (Listener listener : mListeners) {
            listener.onPreparedLog(log, groupName, flags);
        }

        /* Call listeners so that they can filter the log. */
        boolean filteredOut = false;
        for (Listener listener : mListeners) {
            filteredOut = filteredOut || listener.shouldFilter(log);
        }

        /* If filtered out, nothing more to do. */
        if (filteredOut) {
            AppCenterLog.debug(LOG_TAG, "Log of type '" + log.getType() + "' was filtered out by listener(s)");
        } else {
            if (mAppSecret == null && groupState.mIngestion == mIngestion) {

                /* Log was not filtered out but no app secret has been provided. Do nothing in this case. */
                AppCenterLog.debug(LOG_TAG, "Log of type '" + log.getType() + "' was not filtered out by listener(s) but no app secret was provided. Not persisting/sending the log.");
                return;
            }
            try {

                /* Persist log. */
                mPersistence.putLog(log, groupName, flags);
            } catch (Persistence.PersistenceException e) {
                AppCenterLog.error(LOG_TAG, "Error persisting log", e);
                if (groupState.mListener != null) {
                    groupState.mListener.onBeforeSending(log);
                    groupState.mListener.onFailure(log, e);
                }
                return;
            }

            /* Nothing more to do if the log is from a paused transmission target. */
            Iterator<String> targetKeys = log.getTransmissionTargetTokens().iterator();
            String targetKey = targetKeys.hasNext() ? PartAUtils.getTargetKey(targetKeys.next()) : null;
            if (groupState.mPausedTargetKeys.contains(targetKey)) {
                AppCenterLog.debug(LOG_TAG, "Transmission target ikey=" + targetKey + " is paused.");
                return;
            }

            /* Increment counters and schedule ingestion if we are enabled. */
            groupState.mPendingLogCount++;
            AppCenterLog.debug(LOG_TAG, "enqueue(" + groupState.mName + ") pendingLogCount=" + groupState.mPendingLogCount);
            if (mEnabled) {
                checkPendingLogs(groupState);
            } else {
                AppCenterLog.debug(LOG_TAG, "Channel is temporarily disabled, log was saved to disk.");
            }
        }
    }

    /**
     * Check for logs to trigger immediately or schedule with a timer or does nothing if no logs.
     *
     * @param groupState the group state.
     */
    @VisibleForTesting
    synchronized void checkPendingLogs(@NonNull GroupState groupState) {
        if (groupState.mPaused) {
            AppCenterLog.debug(LOG_TAG, groupState.mName + " is paused. Skip checking pending logs.");
            return;
        }
        long pendingLogCount = groupState.mPendingLogCount;
        AppCenterLog.debug(LOG_TAG, "checkPendingLogs(" + groupState.mName + ") pendingLogCount=" + pendingLogCount);
        if (pendingLogCount >= groupState.mMaxLogsPerBatch) {
            triggerIngestion(groupState);
        } else if (pendingLogCount > 0 && !groupState.mScheduled) {
            groupState.mScheduled = true;
            mAppCenterHandler.postDelayed(groupState.mRunnable, groupState.mBatchTimeInterval);
        }
    }

    @VisibleForTesting
    GroupState getGroupState(@SuppressWarnings("SameParameterValue") String groupName) {
        return mGroupStates.get(groupName);
    }

    @Override
    public synchronized void addListener(Listener listener) {
        mListeners.add(listener);
    }

    @Override
    public synchronized void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    @Override
    public synchronized void shutdown() {
        suspend(false, new CancellationException());
    }

    /**
     * State for a specific log group.
     */
    @VisibleForTesting
    class GroupState {

        /**
         * Group name.
         */
        final String mName;

        /**
         * Maximum log count per batch.
         */
        final int mMaxLogsPerBatch;

        /**
         * Time to wait before 2 batches, in ms.
         */
        final long mBatchTimeInterval;

        /**
         * Maximum number of batches in parallel.
         */
        final int mMaxParallelBatches;

        /**
         * Batches being currently sent to ingestion.
         */
        final Map<String, List<Log>> mSendingBatches = new HashMap<>();

        /**
         * Ingestion for the group state.
         */
        final Ingestion mIngestion;

        /**
         * A listener for a service.
         */
        final GroupListener mListener;

        /**
         * Pending log count not part of a batch yet.
         */
        int mPendingLogCount;

        /**
         * Is timer scheduled.
         */
        boolean mScheduled;

        /**
         * Indicates if the group is paused.
         */
        boolean mPaused;

        /**
         * List of paused target keys.
         */
        final Collection<String> mPausedTargetKeys = new HashSet<>();

        /**
         * Runnable that triggers ingestion of this group data
         * and triggers itself in {@link #mBatchTimeInterval} ms.
         */
        final Runnable mRunnable = new Runnable() {

            @Override
            public void run() {
                mScheduled = false;
                triggerIngestion(GroupState.this);
            }
        };

        /**
         * Init.
         *
         * @param name               group name.
         * @param maxLogsPerBatch    max batch size.
         * @param batchTimeInterval  batch interval in ms.
         * @param maxParallelBatches max number of parallel batches.
         * @param ingestion          ingestion for the group state.
         * @param listener           listener for a service.
         */
        GroupState(String name, int maxLogsPerBatch, long batchTimeInterval, int maxParallelBatches, Ingestion ingestion, GroupListener listener) {
            mName = name;
            mMaxLogsPerBatch = maxLogsPerBatch;
            mBatchTimeInterval = batchTimeInterval;
            mMaxParallelBatches = maxParallelBatches;
            mIngestion = ingestion;
            mListener = listener;
        }
    }
}
