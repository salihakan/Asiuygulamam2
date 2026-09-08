package com.example.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MeasurementRecord {
    private String id;
    private String dateKey; // YYYY-MM-DD
    private float weightKg;
    private float heightCm;
    private float bmi;
    private long timestamp;

    public MeasurementRecord() {
    }

    public MeasurementRecord(String id, String dateKey, float weightKg, float heightCm, long timestamp) {
        this.id = id;
        this.dateKey = dateKey;
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        this.timestamp = timestamp;
        this.bmi = calculateBmi(weightKg, heightCm);
    }

    public static float calculateBmi(float weightKg, float heightCm) {
        if (heightCm <= 0 || weightKg <= 0) return 0f;
        float heightMeters = heightCm / 100f;
        return weightKg / (heightMeters * heightMeters);
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

    public float getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(float weightKg) {
        this.weightKg = weightKg;
        this.bmi = calculateBmi(this.weightKg, this.heightCm);
    }

    public float getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(float heightCm) {
        this.heightCm = heightCm;
        this.bmi = calculateBmi(this.weightKg, this.heightCm);
    }

    public float getBmi() {
        return bmi;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedDate() {
        try {
            SimpleDateFormat srcFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = srcFormat.parse(dateKey);
            if (date != null) {
                SimpleDateFormat outFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("tr", "TR"));
                return outFormat.format(date);
            }
        } catch (Exception ignored) {
        }
        return dateKey;
    }

    public String getBmiCategoryLabel() {
        if (bmi < 18.5f) {
            return "Zayıf";
        } else if (bmi < 25.0f) {
            return "Normal";
        } else if (bmi < 30.0f) {
            return "Fazla Kilolu";
        } else {
            return "Obezite";
        }
    }

    public int getBmiCategoryColorHex() {
        if (bmi < 18.5f) {
            return 0xFFF59E0B; // Warning Amber
        } else if (bmi < 25.0f) {
            return 0xFF10B981; // Success Green
        } else if (bmi < 30.0f) {
            return 0xFFF97316; // Orange
        } else {
            return 0xFFEF4444; // Red
        }
    }
}
