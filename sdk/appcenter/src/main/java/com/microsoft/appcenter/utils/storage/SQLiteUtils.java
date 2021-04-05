/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import android.database.sqlite.SQLiteQueryBuilder;
import androidx.annotation.NonNull;

public class SQLiteUtils {

    @NonNull
    public static SQLiteQueryBuilder newSQLiteQueryBuilder() {
        return new SQLiteQueryBuilder();
    }
}
