package com.fifers.leathercalculator;

import android.os.Build;
import android.view.View;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Shared helpers used by both {@link MainActivity} and {@link MonthlyResultsActivity}.
 * Keeping these in one place avoids the two screens drifting apart over time.
 */
final class Utils {

    static final String[] MONTHS = {
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    };

    private static final int YEARS_BEFORE_CURRENT = 2;
    private static final int YEARS_AFTER_CURRENT = 10;

    private Utils() {
    }

    static double parseNumber(String value) {
        if (value == null) return Double.NaN;
        String normalized = toEnglishDigits(value.trim())
                .replace('٫', '.')
                .replace('،', '.')
                .replace(',', '.');
        normalized = normalized.replaceAll("[^0-9.\\-]", "");
        if (normalized.isEmpty() || normalized.equals(".") || normalized.equals("-")) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    static String formatNumber(double value) {
        DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
        decimalFormat.applyPattern("#,##0.##");
        return toPersianDigits(decimalFormat.format(value));
    }

    static String formatSignedNumber(double value) {
        String sign = value > 0 ? "+" : "";
        return sign + formatNumber(value);
    }

    static String toEnglishDigits(String value) {
        return value
                .replace('۰', '0')
                .replace('۱', '1')
                .replace('۲', '2')
                .replace('۳', '3')
                .replace('۴', '4')
                .replace('۵', '5')
                .replace('۶', '6')
                .replace('۷', '7')
                .replace('۸', '8')
                .replace('۹', '9')
                .replace('٠', '0')
                .replace('١', '1')
                .replace('٢', '2')
                .replace('٣', '3')
                .replace('٤', '4')
                .replace('٥', '5')
                .replace('٦', '6')
                .replace('٧', '7')
                .replace('٨', '8')
                .replace('٩', '9');
    }

    static String toPersianDigits(String value) {
        return value
                .replace('0', '۰')
                .replace('1', '۱')
                .replace('2', '۲')
                .replace('3', '۳')
                .replace('4', '۴')
                .replace('5', '۵')
                .replace('6', '۶')
                .replace('7', '۷')
                .replace('8', '۸')
                .replace('9', '۹');
    }

    /** Adds bottom padding equal to the system navigation-bar inset so buttons never sit under it. */
    static void applyBottomSystemInset(View rootView) {
        if (rootView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        final int initialLeft = rootView.getPaddingLeft();
        final int initialTop = rootView.getPaddingTop();
        final int initialRight = rootView.getPaddingRight();
        final int initialBottom = rootView.getPaddingBottom();
        rootView.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(initialLeft, initialTop, initialRight,
                    initialBottom + insets.getSystemWindowInsetBottom());
            return insets;
        });
        rootView.requestApplyInsets();
    }

    /**
     * Approximates the current Jalali (Persian) year from the device's Gregorian date.
     * Nowruz (Persian new year) always falls on Gregorian March 20 or 21, so this is
     * precise enough to pick a sensible default in the year selector without needing
     * a full Gregorian-to-Jalali date conversion.
     */
    static int currentJalaliYearApprox() {
        Calendar calendar = Calendar.getInstance();
        int gYear = calendar.get(Calendar.YEAR);
        int gMonth = calendar.get(Calendar.MONTH) + 1;
        int gDay = calendar.get(Calendar.DAY_OF_MONTH);
        if (gMonth > 3 || (gMonth == 3 && gDay >= 21)) {
            return gYear - 621;
        }
        return gYear - 622;
    }

    /** Persian-digit year labels centered around the current Jalali year. */
    static List<String> jalaliYearOptions() {
        int current = currentJalaliYearApprox();
        List<String> years = new ArrayList<>();
        for (int y = current - YEARS_BEFORE_CURRENT; y <= current + YEARS_AFTER_CURRENT; y++) {
            years.add(toPersianDigits(String.valueOf(y)));
        }
        return years;
    }

    /** Index of the current Jalali year within a list produced by {@link #jalaliYearOptions()}. */
    static int currentJalaliYearIndexIn(List<String> years) {
        String currentLabel = toPersianDigits(String.valueOf(currentJalaliYearApprox()));
        int index = years.indexOf(currentLabel);
        return index >= 0 ? index : 0;
    }
}
