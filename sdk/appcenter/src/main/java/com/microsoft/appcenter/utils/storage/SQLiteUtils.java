package com.microsoft.appcenter.utils.storage;

import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.NonNull;

public class SQLiteUtils {

    @NonNull
    public static SQLiteQueryBuilder newSQLiteQueryBuilder() {
        return new SQLiteQueryBuilder();
    }
}