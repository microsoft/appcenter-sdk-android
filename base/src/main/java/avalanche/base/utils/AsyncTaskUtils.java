package avalanche.base.utils;

import android.os.AsyncTask;

import java.util.concurrent.Executor;

/**
 * <h3>Description</h3>
 *
 * Either calls execute or executeOnExecutor on an AsyncTask depending on the
 * API level.
 */
public class AsyncTaskUtils {

    private static Executor sCustomExecutor;

    public static void execute(AsyncTask<Void, ?, ?> asyncTask) {
            asyncTask.executeOnExecutor(sCustomExecutor != null ? sCustomExecutor : AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static Executor getCustomExecutor() {
        return sCustomExecutor;
    }

    public static void setCustomExecutor(Executor customExecutor) {
        sCustomExecutor = customExecutor;
    }

}
