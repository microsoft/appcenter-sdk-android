package avalanche.core.utils;

import android.os.SystemClock;

public class AndroidTimeSource implements TimeSource {

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }
}
