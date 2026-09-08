package com.example.model;

public class UserSettings {
    private String startDateKey; // yyyy-MM-dd
    private int startLimbIndex; // 0 = Right Arm
    private boolean reminderEnabled;
    private int reminderHour;
    private int reminderMinute;
    private String themeMode; // "SYSTEM", "LIGHT", "DARK"

    public UserSettings() {
        this.startDateKey = "2026-09-07";
        this.startLimbIndex = 0;
        this.reminderEnabled = true;
        this.reminderHour = 22;
        this.reminderMinute = 0;
        this.themeMode = "SYSTEM";
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

    public String getFormattedTime() {
        return String.format("%02d:%02d", reminderHour, reminderMinute);
    }
}
