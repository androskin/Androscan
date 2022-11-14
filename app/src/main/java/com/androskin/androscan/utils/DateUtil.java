package com.androskin.androscan.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtil {
    private static final String format = "dd.MM.yyyy";

    public static Date getCurrentDate() {
        return new Date();
    }

    public static String getString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.UK);
        return dateFormat.format(date);

    }
}
