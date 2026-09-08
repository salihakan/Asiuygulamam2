package com.example.model;

public class InjectionRecord {
    private String id;
    private String dateKey; // YYYY-MM-DD
    private int limbIndex;
    private String limbName;
    private double dose;
    private boolean completed;
    private long completedTimestamp;

    public InjectionRecord() {
    }

    public InjectionRecord(String id, String dateKey, int limbIndex, String limbName, double dose, boolean completed, long completedTimestamp) {
        this.id = id;
        this.dateKey = dateKey;
        this.limbIndex = limbIndex;
        this.limbName = limbName;
        this.dose = dose;
        this.completed = completed;
        this.completedTimestamp = completedTimestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDateKey() {
        return dateKey;
    }

    public void setDateKey(String dateKey) {
        this.dateKey = dateKey;
    }

    public int getLimbIndex() {
        return limbIndex;
    }

    public void setLimbIndex(int limbIndex) {
        this.limbIndex = limbIndex;
    }

    public String getLimbName() {
        return limbName;
    }

    public void setLimbName(String limbName) {
        this.limbName = limbName;
    }

    public double getDose() {
        return dose;
    }

    public void setDose(double dose) {
        this.dose = dose;
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
