package com.microsoft.sonoma.crashes;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({
        ErrorReporting.SEND,
        ErrorReporting.DONT_SEND,
        ErrorReporting.ALWAYS_SEND
})
@interface UserConfirmationDef {
}
