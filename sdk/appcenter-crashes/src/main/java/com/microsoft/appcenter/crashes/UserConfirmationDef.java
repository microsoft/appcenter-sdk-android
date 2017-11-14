package com.microsoft.appcenter.crashes;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({
        Crashes.SEND,
        Crashes.DONT_SEND,
        Crashes.ALWAYS_SEND
})
@interface UserConfirmationDef {
}
