package com.example.util;

import com.example.model.InjectionPlan;
import com.example.model.Limb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DoseCalculator {

    public static double getDoseForDay(Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SUNDAY) {
            return 0.4;
        } else {
            return 0.7;
        }
    }

    public static Limb getLimbForDate(Calendar targetDate, Calendar startDate, int startLimbIndex) {
        Calendar cleanTarget = normalizeToMidnight((Calendar) targetDate.clone());
        Calendar cleanStart = normalizeToMidnight((Calendar) startDate.clone());

        long diffMillis = cleanTarget.getTimeInMillis() - cleanStart.getTimeInMillis();
        long diffDays = Math.round((double) diffMillis / (24.0 * 60 * 60 * 1000));

        int limbIndex = (int) (((startLimbIndex + diffDays) % 4 + 4) % 4);
        return Limb.fromIndex(limbIndex);
    }

    public static InjectionPlan calculatePlan(Calendar targetDate, Calendar startDate, int startLimbIndex, boolean completed) {
        double dose = getDoseForDay(targetDate);
        boolean isSunday = targetDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
        Limb limb = getLimbForDate(targetDate, startDate, startLimbIndex);
        return new InjectionPlan(targetDate, limb, dose, isSunday, completed);
    }

    public static Calendar normalizeToMidnight(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    public static Calendar parseDateKey(String dateKey) {
        Calendar cal = Calendar.getInstance();
        if (dateKey != null && !dateKey.trim().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date date = sdf.parse(dateKey);
                if (date != null) {
                    cal.setTime(date);
                }
            } catch (Exception ignored) {
            }
        }
        return normalizeToMidnight(cal);
    }

    public static String formatDateKey(Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(cal.getTime());
    }

    public static float calculateDecimalAge(String birthDateKey, String targetDateKey) {
        try {
            Calendar birth = parseDateKey(birthDateKey);
            Calendar target = parseDateKey(targetDateKey);
            long diffMillis = target.getTimeInMillis() - birth.getTimeInMillis();
            float diffYears = (float) diffMillis / (365.25f * 24f * 60f * 60f * 1000f);
            return Math.max(0.1f, diffYears);
        } catch (Exception e) {
            return 7.0f;
        }
    }
}
