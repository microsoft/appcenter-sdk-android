package avalanche.core.channel;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static avalanche.core.channel.DefaultAvalancheChannel.ANALYTICS_GROUP;
import static avalanche.core.channel.DefaultAvalancheChannel.ERROR_GROUP;

@Retention(RetentionPolicy.SOURCE)
@StringDef({
        ERROR_GROUP,
        ANALYTICS_GROUP,
})
public @interface GroupNameDef { }
