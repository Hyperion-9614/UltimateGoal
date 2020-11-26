package com.hyperion.common;



import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.Toolkit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javafx.scene.control.TextField;

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

    public static String condensed(String str) {
        return str.replaceAll("(\\s|\n)", "").trim();
    }

    public static boolean condensedEquals(String s1, String s2) {
        return condensed(s1).equals(condensed(s2));
    }

    public static double stringWidth(FontMetrics metrics, String string) {
        return metrics.computeStringWidth(string);
    }

    public static double stringWidth(TextField field) {
        return stringWidth(Toolkit.getToolkit().getFontLoader().getFontMetrics(field.getFont()), field.getText());
    }

}
