package avalanche.base.channel;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import java.util.UUID;

import avalanche.base.ingestion.models.Device;
import avalanche.base.ingestion.models.Log;
import avalanche.base.utils.AvalancheLog;
import avalanche.base.utils.DeviceInfoHelper;

public class AvalancheChannelSessionDecorator implements AvalancheChannel, Application.ActivityLifecycleCallbacks {

    private static final int ACTIVITY_TIMEOUT = 20000;

    private final Context mContext;

    private final AvalancheChannel mChannel;

    private UUID mSid;

    private Device mDevice;

    private long mLastQueuedLogTime;

    private long mLastActivityPausedTime;

    public AvalancheChannelSessionDecorator(Context context, AvalancheChannel channel) {
        mContext = context;
        mChannel = channel;
    }

    @Override
    public void enqueue(@NonNull Log log, @NonNull @GroupNameDef String queueName) {
        long now = SystemClock.elapsedRealtime();
        if (mSid == null || (now - mLastQueuedLogTime >= ACTIVITY_TIMEOUT && now - mLastActivityPausedTime >= ACTIVITY_TIMEOUT)) {
            mSid = UUID.randomUUID();
            try {
                mDevice = DeviceInfoHelper.getDeviceInfo(mContext);
            } catch (DeviceInfoHelper.DeviceInfoException e) {
                AvalancheLog.error("Device log cannot be generated", e);
                return;
            }
        }
        log.setSid(mSid);
        log.setDevice(mDevice);
        // TODO log.setToffset(now); // use absolute time for persistence, will be converted before sending
        mChannel.enqueue(log, queueName);
        mLastQueuedLogTime = SystemClock.elapsedRealtime();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
        mLastActivityPausedTime = SystemClock.elapsedRealtime();
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
