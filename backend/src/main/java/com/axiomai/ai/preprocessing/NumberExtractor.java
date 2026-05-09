package com.axiomai.ai.preprocessing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberExtractor {

    public static double extractEuro(String text) {
        Matcher m = Pattern.compile("(\\d+\\.?\\d*)\\s*euro").matcher(text.toLowerCase());
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return -1;
    }

    public static double extractPercent(String text) {
        Matcher m = Pattern.compile("(\\d+\\.?\\d*)\\s*%").matcher(text.toLowerCase());
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return -1;
    }

    public static double extractYears(String text) {
        Matcher m = Pattern.compile("(\\d+\\.?\\d*)\\s*years?").matcher(text.toLowerCase());
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return -1;
    }

    public static double[] extractAllPercents(String text) {
        Matcher m = Pattern.compile("(\\d+\\.?\\d*)\\s*%").matcher(text.toLowerCase());
        double[] vals = new double[20];
        int i = 0;
        while (m.find() && i < vals.length) {
            vals[i++] = Double.parseDouble(m.group(1));
        }
        double[] out = new double[i];
        System.arraycopy(vals, 0, out, 0, i);
        return out;
    }

    public static double[] extractAllYears(String text) {
        Matcher m = Pattern.compile("(\\d+\\.?\\d*)\\s*years?").matcher(text.toLowerCase());
        double[] vals = new double[20];
        int i = 0;
        while (m.find() && i < vals.length) {
            vals[i++] = Double.parseDouble(m.group(1));
        }
        double[] out = new double[i];
        System.arraycopy(vals, 0, out, 0, i);
        return out;
    }

    // ⭐ Missing method — now added
    public static double[] extractTwoEuroValues(String text) {
        Matcher m = Pattern.compile("(\\d+\\.?\\d*)\\s*euro").matcher(text.toLowerCase());
        double[] vals = new double[2];
        int i = 0;

        while (m.find() && i < 2) {
            vals[i++] = Double.parseDouble(m.group(1));
        }

        if (i == 2) return vals;
        return null;
    }
}
