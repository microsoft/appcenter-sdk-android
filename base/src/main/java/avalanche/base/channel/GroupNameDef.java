package avalanche.base.channel;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static avalanche.base.channel.DefaultAvalancheChannel.GROUP_ERROR;
import static avalanche.base.channel.DefaultAvalancheChannel.GROUP_ANALYTICS;

@Retention(RetentionPolicy.SOURCE)
@StringDef({
        GROUP_ERROR,
        GROUP_ANALYTICS,
})
public @interface GroupNameDef { }
