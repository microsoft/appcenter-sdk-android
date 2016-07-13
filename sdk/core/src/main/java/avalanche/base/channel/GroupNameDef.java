package avalanche.base.channel;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static avalanche.base.channel.DefaultAvalancheChannel.ERROR_GROUP;
import static avalanche.base.channel.DefaultAvalancheChannel.ANALYTICS_GROUP;

@Retention(RetentionPolicy.SOURCE)
@StringDef({
        ERROR_GROUP,
        ANALYTICS_GROUP,
})
public @interface GroupNameDef { }
