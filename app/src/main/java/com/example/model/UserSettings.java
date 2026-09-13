package com.example.model;

import com.example.util.DoseCalculator;
import java.util.Calendar;

public class UserSettings {
    private String startDateKey; // yyyy-MM-dd
    private int startLimbIndex; // 0 = Right Arm
    private boolean reminderEnabled;
    private int reminderHour;
    private int reminderMinute;
    private String themeMode; // "SYSTEM", "LIGHT", "DARK"

    // Alarm Settings (Bildirim değil, Alarm mekanizması)
    private boolean alarmEnabled;
    private int alarmHour;
    private int alarmMinute;
    private boolean alarmVibrate;

    // Child Profile (Cinsiyet, Doğum Tarihi, Yaş)
    private String childGender; // "MALE" (Erkek) or "FEMALE" (Kız)
    private String childBirthDateKey; // yyyy-MM-dd
    private String childName;

    public UserSettings() {
        this.startDateKey = "2026-09-07";
        this.startLimbIndex = 0;
        this.reminderEnabled = true;
        this.reminderHour = 22;
        this.reminderMinute = 0;
        this.themeMode = "SYSTEM";

        this.alarmEnabled = true;
        this.alarmHour = 22;
        this.alarmMinute = 0;
        this.alarmVibrate = true;

        this.childGender = "MALE";
        this.childBirthDateKey = "2019-05-15"; // Default 7 years old
        this.childName = "Çocuğum";
    }

    public String getStartDateKey() {
        if (startDateKey == null || startDateKey.trim().isEmpty()) {
            startDateKey = "2026-09-07";
        }
        return startDateKey;
    }

    public void setStartDateKey(String startDateKey) {
        this.startDateKey = startDateKey;
    }

    public int getStartLimbIndex() {
        return startLimbIndex;
    }

    public void setStartLimbIndex(int startLimbIndex) {
        this.startLimbIndex = startLimbIndex;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public int getReminderHour() {
        return reminderHour;
    }

    public void setReminderHour(int reminderHour) {
        this.reminderHour = reminderHour;
    }

    public int getReminderMinute() {
        return reminderMinute;
    }

    public void setReminderMinute(int reminderMinute) {
        this.reminderMinute = reminderMinute;
    }

    public String getThemeMode() {
        return themeMode != null ? themeMode : "SYSTEM";
    }

    public void setThemeMode(String themeMode) {
        this.themeMode = themeMode;
    }

    public boolean isAlarmEnabled() {
        return alarmEnabled;
    }

    public void setAlarmEnabled(boolean alarmEnabled) {
        this.alarmEnabled = alarmEnabled;
    }

    public int getAlarmHour() {
        return alarmHour;
    }

    public void setAlarmHour(int alarmHour) {
        this.alarmHour = alarmHour;
    }

    public int getAlarmMinute() {
        return alarmMinute;
    }

    public void setAlarmMinute(int alarmMinute) {
        this.alarmMinute = alarmMinute;
    }

    public boolean isAlarmVibrate() {
        return alarmVibrate;
    }

    public void setAlarmVibrate(boolean alarmVibrate) {
        this.alarmVibrate = alarmVibrate;
    }

    public String getChildGender() {
        return (childGender != null && childGender.equalsIgnoreCase("FEMALE")) ? "FEMALE" : "MALE";
    }

    public void setChildGender(String childGender) {
        this.childGender = childGender;
    }

    public boolean isMale() {
        return "MALE".equalsIgnoreCase(getChildGender());
    }

    public String getChildBirthDateKey() {
        if (childBirthDateKey == null || childBirthDateKey.trim().isEmpty()) {
            childBirthDateKey = "2019-05-15";
        }
        return childBirthDateKey;
    }

    public void setChildBirthDateKey(String childBirthDateKey) {
        this.childBirthDateKey = childBirthDateKey;
    }

    public String getChildName() {
        return (childName != null && !childName.trim().isEmpty()) ? childName : "Çocuğum";
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }

    public String getFormattedTime() {
        return String.format("%02d:%02d", reminderHour, reminderMinute);
    }

    public String getFormattedAlarmTime() {
        return String.format("%02d:%02d", alarmHour, alarmMinute);
    }

    /**
     * Calculates child age in years and remaining months from childBirthDateKey.
     * @return int[]{ years, months }
     */
    public int[] getChildAgeYearsMonths() {
        try {
            Calendar birth = DoseCalculator.parseDateKey(getChildBirthDateKey());
            Calendar now = Calendar.getInstance();

            int years = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
            int months = now.get(Calendar.MONTH) - birth.get(Calendar.MONTH);
            if (now.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)) {
                months--;
            }
            if (months < 0) {
                years--;
                months += 12;
            }
            if (years < 0) {
                years = 0;
                months = 0;
            }
            return new int[]{years, months};
        } catch (Exception e) {
            return new int[]{7, 0};
        }
    }

    public float getChildAgeDecimal() {
        int[] ym = getChildAgeYearsMonths();
        return ym[0] + (ym[1] / 12.0f);
    }

    public String getFormattedChildAge() {
        int[] ym = getChildAgeYearsMonths();
        if (ym[0] <= 0) {
            return ym[1] + " Aylık";
        } else if (ym[1] == 0) {
            return ym[0] + " Yaş";
        } else {
            return ym[0] + " Yaş " + ym[1] + " Ay";
        }
    }
}
