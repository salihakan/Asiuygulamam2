package com.example.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class InjectionPlan {
    private final Calendar calendar;
    private final String dateKey; // YYYY-MM-DD
    private final String formattedDate; // e.g., "7 Eylül 2026, Pazartesi"
    private final Limb limb;
    private final double dose;
    private final boolean isSunday;
    private boolean completed;
    private long completedTimestamp;

    public InjectionPlan(Calendar calendar, Limb limb, double dose, boolean isSunday, boolean completed) {
        this.calendar = (Calendar) calendar.clone();
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        this.dateKey = keyFormat.format(calendar.getTime());

        SimpleDateFormat displayFormat = new SimpleDateFormat("d MMMM yyyy, EEEE", new Locale("tr", "TR"));
        this.formattedDate = displayFormat.format(calendar.getTime());

        this.limb = limb;
        this.dose = dose;
        this.isSunday = isSunday;
        this.completed = completed;
        this.completedTimestamp = 0;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public String getDateKey() {
        return dateKey;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public Limb getLimb() {
        return limb;
    }

    public double getDose() {
        return dose;
    }

    public String getFormattedDose() {
        return String.format(Locale.getDefault(), "%.1f ünite", dose);
    }

    public boolean isSunday() {
        return isSunday;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCompletedTimestamp() {
        return completedTimestamp;
    }

    public void setCompletedTimestamp(long completedTimestamp) {
        this.completedTimestamp = completedTimestamp;
    }
}
