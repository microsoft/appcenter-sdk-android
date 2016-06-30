package avalanche.base.channel;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static avalanche.base.channel.DefaultAvalancheChannel.HIGH_PRIORITY;
import static avalanche.base.channel.DefaultAvalancheChannel.REGULAR_PRIORITY;

@Retention(RetentionPolicy.SOURCE)
@IntDef({
        HIGH_PRIORITY,
        REGULAR_PRIORITY,
})
public @interface ChannelPriorityDef { }
