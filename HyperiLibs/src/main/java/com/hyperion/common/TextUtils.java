package com.hyperion.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TextUtils {

    public static String join(String separator, Object[] elements) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            joined.append(elements[i].toString());
            if (i < elements.length - 1) {
                joined.append(separator);
            }
        }
        return joined.toString();
    }

    public static String getFormattedDate() {
        String pattern = "MM/dd/yyyy h:mm:ss:S a";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        return simpleDateFormat.format(new Date());
    }

    public static void printSocketLog(String from, String to, String message) {
        System.out.println("[" + TextUtils.getFormattedDate() + "] " + from.toUpperCase() + " -> " + to.toUpperCase() + ": " + message);
    }

    public static String condensed(String str) {
        return str.replaceAll("(\\s|\n)", "").trim();
    }

    public static boolean condensedEquals(String s1, String s2) {
        return condensed(s1).equals(condensed(s2));
    }
}
