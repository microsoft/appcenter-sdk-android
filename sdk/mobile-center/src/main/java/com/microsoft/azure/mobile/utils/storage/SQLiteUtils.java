/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.utils.storage;

import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.NonNull;

class SQLiteUtils {

    @NonNull
    static SQLiteQueryBuilder newSQLiteQueryBuilder() {
        return new SQLiteQueryBuilder();
    }
}