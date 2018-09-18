package com.microsoft.appcenter.sasquatch.util;

import android.content.Context;

import com.microsoft.appcenter.persistence.DatabasePersistence;

import java.text.NumberFormat;

public class StorageUtil {

    private static final long FACTOR = 1024;
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();

    public static long getStorageFileSize(Context context) {
        return context.getDatabasePath("com.microsoft.appcenter.persistence").length();
    }

    public static String getFormattedSize(long size, SizeUnit minSizeUnit) {
        if (size < 0) {
            return "Unknown";
        }
        SizeUnit determinedUnit = SizeUnit.B;
        for (SizeUnit unit : SizeUnit.values()) {
            if (unit.getBase() >= minSizeUnit.getBase()) {
                determinedUnit = unit;
                if (size / unit.getBase() < FACTOR) {
                    break;
                }
            }
        }
        return String.format("%s %s", NUMBER_FORMAT.format(size / determinedUnit.getBase()), determinedUnit.getUnit());
    }

    public enum SizeUnit {

        B("B", 1),
        KB("KB", FACTOR),
        MB("MB", FACTOR * FACTOR),
        GB("GB", FACTOR * FACTOR * FACTOR);

        private String mUnit;
        private double mBase;

        SizeUnit(String unit, double base) {
            mUnit = unit;
            mBase = base;
        }

        public String getUnit() {
            return mUnit;
        }

        public double getBase() {
            return mBase;
        }
    }
}
