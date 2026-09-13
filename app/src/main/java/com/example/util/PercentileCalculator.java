package com.example.util;

import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;

/**
 * Pediatric Growth Percentile Calculator (WHO & Turkish Neyzi Growth Reference Standards).
 * Provides 3rd, 10th, 25th, 50th, 75th, 90th, and 97th percentiles for Height (cm) and Weight (kg)
 * for ages 2 to 18 years for Boys and Girls.
 */
public class PercentileCalculator {

    public static final int P3 = 0;
    public static final int P10 = 1;
    public static final int P25 = 2;
    public static final int P50 = 3;
    public static final int P75 = 4;
    public static final int P90 = 5;
    public static final int P97 = 6;

    public static final int[] PERCENTILE_LEVELS = {3, 10, 25, 50, 75, 90, 97};

    // Ages: 2 to 18
    public static final float[] AGES = {2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f};

    // BOYS HEIGHT (cm) [AgeIndex][Percentile: P3, P10, P25, P50, P75, P90, P97]
    private static final float[][] BOYS_HEIGHT = {
            {81.5f, 83.5f, 85.5f, 87.5f, 89.5f, 91.5f, 93.5f}, // 2
            {89.0f, 91.5f, 93.8f, 96.0f, 98.5f, 100.5f, 103.0f}, // 3
            {95.5f, 98.5f, 101.0f, 103.5f, 106.0f, 108.5f, 111.5f}, // 4
            {101.5f, 105.0f, 107.5f, 110.0f, 112.5f, 115.5f, 118.5f}, // 5
            {107.5f, 111.0f, 113.5f, 116.0f, 119.0f, 122.0f, 125.5f}, // 6
            {113.0f, 116.5f, 119.0f, 122.0f, 125.0f, 128.0f, 132.0f}, // 7
            {118.0f, 122.0f, 125.0f, 128.0f, 131.0f, 134.0f, 138.0f}, // 8
            {123.0f, 127.0f, 130.0f, 133.5f, 137.0f, 140.0f, 144.0f}, // 9
            {128.0f, 132.0f, 135.0f, 138.5f, 142.0f, 145.5f, 150.0f}, // 10
            {132.5f, 137.0f, 140.5f, 144.0f, 148.0f, 151.5f, 156.5f}, // 11
            {137.5f, 142.5f, 146.0f, 150.0f, 154.5f, 158.5f, 163.5f}, // 12
            {143.5f, 149.0f, 153.0f, 157.0f, 162.0f, 166.0f, 171.5f}, // 13
            {150.5f, 156.0f, 160.0f, 164.0f, 169.0f, 173.0f, 178.5f}, // 14
            {156.5f, 162.0f, 166.0f, 170.0f, 174.5f, 178.0f, 183.0f}, // 15
            {160.5f, 165.5f, 169.5f, 173.5f, 177.5f, 181.5f, 186.0f}, // 16
            {162.5f, 167.5f, 171.5f, 175.5f, 179.5f, 183.0f, 187.5f}, // 17
            {163.5f, 168.5f, 172.5f, 176.5f, 180.5f, 184.0f, 188.5f}  // 18
    };

    // BOYS WEIGHT (kg) [AgeIndex][Percentile: P3, P10, P25, P50, P75, P90, P97]
    private static final float[][] BOYS_WEIGHT = {
            {10.0f, 10.8f, 11.5f, 12.2f, 13.0f, 13.9f, 15.0f}, // 2
            {11.8f, 12.8f, 13.5f, 14.3f, 15.3f, 16.5f, 18.0f}, // 3
            {13.5f, 14.6f, 15.4f, 16.3f, 17.6f, 19.3f, 21.2f}, // 4
            {15.0f, 16.3f, 17.2f, 18.3f, 20.0f, 22.0f, 24.5f}, // 5
            {16.8f, 18.2f, 19.3f, 20.6f, 22.8f, 25.4f, 28.5f}, // 6
            {18.8f, 20.5f, 21.8f, 23.3f, 26.0f, 29.2f, 33.2f}, // 7
            {21.0f, 23.0f, 24.5f, 26.3f, 29.6f, 33.5f, 38.5f}, // 8
            {23.5f, 25.8f, 27.6f, 29.7f, 33.8f, 38.5f, 44.5f}, // 9
            {26.2f, 28.8f, 30.9f, 33.5f, 38.4f, 44.0f, 51.5f}, // 10
            {29.2f, 32.2f, 34.7f, 37.8f, 43.5f, 50.0f, 59.0f}, // 11
            {32.5f, 36.0f, 38.9f, 42.5f, 49.0f, 56.5f, 66.5f}, // 12
            {36.5f, 40.5f, 44.0f, 48.0f, 55.0f, 63.5f, 74.0f}, // 13
            {41.0f, 45.5f, 49.2f, 53.5f, 61.2f, 70.0f, 80.5f}, // 14
            {45.5f, 50.2f, 54.0f, 58.5f, 66.5f, 75.0f, 85.5f}, // 15
            {49.0f, 54.0f, 58.0f, 62.5f, 70.5f, 79.0f, 89.0f}, // 16
            {51.5f, 56.5f, 60.5f, 65.2f, 73.0f, 81.5f, 91.5f}, // 17
            {53.0f, 58.0f, 62.0f, 67.0f, 74.8f, 83.5f, 93.0f}  // 18
    };

    // GIRLS HEIGHT (cm) [AgeIndex][Percentile: P3, P10, P25, P50, P75, P90, P97]
    private static final float[][] GIRLS_HEIGHT = {
            {80.0f, 82.5f, 84.5f, 86.5f, 88.5f, 90.5f, 92.5f}, // 2
            {88.0f, 90.5f, 92.8f, 95.0f, 97.2f, 99.5f, 102.0f}, // 3
            {94.5f, 97.5f, 100.0f, 102.5f, 105.0f, 107.5f, 110.5f}, // 4
            {100.5f, 104.0f, 106.5f, 109.0f, 111.8f, 114.5f, 118.0f}, // 5
            {106.0f, 110.0f, 112.5f, 115.0f, 118.0f, 121.0f, 124.5f}, // 6
            {111.5f, 115.5f, 118.0f, 121.0f, 124.0f, 127.0f, 131.0f}, // 7
            {116.5f, 121.0f, 124.0f, 127.0f, 130.0f, 133.0f, 137.5f}, // 8
            {121.5f, 126.0f, 129.0f, 132.5f, 135.8f, 139.0f, 144.0f}, // 9
            {126.5f, 131.5f, 135.0f, 138.5f, 142.0f, 145.5f, 151.0f}, // 10
            {132.0f, 137.5f, 141.0f, 145.0f, 148.8f, 152.5f, 158.0f}, // 11
            {138.0f, 143.5f, 147.2f, 151.0f, 155.0f, 158.5f, 163.5f}, // 12
            {143.0f, 148.5f, 152.0f, 155.5f, 159.0f, 162.5f, 167.0f}, // 13
            {146.5f, 151.5f, 155.0f, 158.5f, 162.0f, 165.0f, 169.0f}, // 14
            {148.5f, 153.0f, 156.5f, 160.0f, 163.5f, 166.5f, 170.0f}, // 15
            {149.5f, 154.0f, 157.5f, 161.0f, 164.2f, 167.0f, 170.5f}, // 16
            {150.0f, 154.5f, 158.0f, 161.5f, 164.8f, 167.5f, 171.0f}, // 17
            {150.0f, 154.5f, 158.0f, 161.5f, 164.8f, 167.5f, 171.0f}  // 18
    };

    // GIRLS WEIGHT (kg) [AgeIndex][Percentile: P3, P10, P25, P50, P75, P90, P97]
    private static final float[][] GIRLS_WEIGHT = {
            {9.5f, 10.2f, 10.8f, 11.5f, 12.4f, 13.3f, 14.5f}, // 2
            {11.2f, 12.2f, 13.0f, 13.9f, 14.9f, 16.0f, 17.5f}, // 3
            {12.8f, 14.0f, 14.9f, 16.0f, 17.3f, 18.8f, 20.8f}, // 4
            {14.5f, 15.8f, 16.8f, 18.0f, 19.8f, 21.8f, 24.2f}, // 5
            {16.2f, 17.6f, 18.8f, 20.2f, 22.4f, 25.0f, 28.2f}, // 6
            {18.0f, 19.8f, 21.1f, 22.8f, 25.5f, 28.8f, 33.0f}, // 7
            {20.0f, 22.2f, 23.8f, 25.8f, 29.2f, 33.0f, 38.5f}, // 8
            {22.5f, 25.0f, 26.9f, 29.2f, 33.3f, 38.0f, 44.5f}, // 9
            {25.2f, 28.2f, 30.5f, 33.2f, 38.2f, 44.0f, 51.5f}, // 10
            {28.5f, 32.0f, 34.7f, 38.0f, 43.5f, 50.0f, 58.5f}, // 11
            {32.2f, 36.2f, 39.3f, 43.0f, 49.0f, 55.5f, 64.5f}, // 12
            {36.0f, 40.5f, 43.8f, 47.5f, 53.5f, 60.0f, 69.5f}, // 13
            {39.5f, 44.0f, 47.4f, 51.0f, 57.0f, 63.5f, 73.0f}, // 14
            {42.0f, 46.5f, 49.8f, 53.5f, 59.5f, 66.0f, 75.5f}, // 15
            {43.5f, 48.0f, 51.2f, 55.0f, 61.0f, 67.5f, 77.0f}, // 16
            {44.5f, 49.0f, 52.1f, 55.8f, 61.8f, 68.5f, 78.0f}, // 17
            {45.0f, 49.5f, 52.6f, 56.2f, 62.2f, 69.0f, 78.5f}  // 18
    };

    private static float[][] getTable(boolean isMale, boolean isHeight) {
        if (isMale) {
            return isHeight ? BOYS_HEIGHT : BOYS_WEIGHT;
        } else {
            return isHeight ? GIRLS_HEIGHT : GIRLS_WEIGHT;
        }
    }

    /**
     * Interpolate value at a specific age and percentile index.
     */
    public static float getValueAt(boolean isMale, boolean isHeight, float ageYears, int percentileIdx) {
        if (ageYears < 2f) ageYears = 2f;
        if (ageYears > 18f) ageYears = 18f;

        float[][] table = getTable(isMale, isHeight);

        // Find surrounding age brackets
        for (int i = 0; i < AGES.length - 1; i++) {
            if (ageYears >= AGES[i] && ageYears <= AGES[i + 1]) {
                float fraction = (ageYears - AGES[i]) / (AGES[i + 1] - AGES[i]);
                float val0 = table[i][percentileIdx];
                float val1 = table[i + 1][percentileIdx];
                return val0 + fraction * (val1 - val0);
            }
        }
        return table[table.length - 1][percentileIdx];
    }

    /**
     * Calculate estimated percentile (1 to 99) for a given measurement value at a specific age.
     */
    public static int calculatePercentile(float value, float ageYears, boolean isMale, boolean isHeight) {
        if (value <= 0) return 50;

        float p3 = getValueAt(isMale, isHeight, ageYears, P3);
        float p10 = getValueAt(isMale, isHeight, ageYears, P10);
        float p25 = getValueAt(isMale, isHeight, ageYears, P25);
        float p50 = getValueAt(isMale, isHeight, ageYears, P50);
        float p75 = getValueAt(isMale, isHeight, ageYears, P75);
        float p90 = getValueAt(isMale, isHeight, ageYears, P90);
        float p97 = getValueAt(isMale, isHeight, ageYears, P97);

        if (value < p3) {
            float frac = Math.max(0.1f, value / p3);
            return Math.max(1, Math.round(frac * 3f));
        } else if (value < p10) {
            float frac = (value - p3) / (p10 - p3);
            return Math.round(3f + frac * 7f);
        } else if (value < p25) {
            float frac = (value - p10) / (p25 - p10);
            return Math.round(10f + frac * 15f);
        } else if (value < p50) {
            float frac = (value - p25) / (p50 - p25);
            return Math.round(25f + frac * 25f);
        } else if (value < p75) {
            float frac = (value - p50) / (p75 - p50);
            return Math.round(50f + frac * 25f);
        } else if (value < p90) {
            float frac = (value - p75) / (p90 - p75);
            return Math.round(75f + frac * 15f);
        } else if (value < p97) {
            float frac = (value - p90) / (p97 - p90);
            return Math.round(90f + frac * 7f);
        } else {
            float diff = value - p97;
            return Math.min(99, Math.round(97f + Math.min(2f, diff * 0.5f)));
        }
    }

    /**
     * Medical pediatric evaluation for the percentile.
     */
    public static String getEvaluationText(int percentile) {
        if (percentile < 3) {
            return "3. Persentil Altında • Doktor Takibi Önerilir";
        } else if (percentile <= 10) {
            return "3 - 10. Persentil • Alt Sınırda";
        } else if (percentile <= 25) {
            return "10 - 25. Persentil • Normal (Alt Dilim)";
        } else if (percentile <= 75) {
            return "25 - 75. Persentil • İdeal / Ortalama Büyüme";
        } else if (percentile <= 90) {
            return "75 - 90. Persentil • Normal (Üst Dilim)";
        } else if (percentile <= 97) {
            return "90 - 97. Persentil • Üst Sınırda";
        } else {
            return "97. Persentil Üzerinde";
        }
    }

    /**
     * Get points for rendering a percentile curve (age vs value).
     */
    public static List<PointF> getCurvePoints(boolean isMale, boolean isHeight, int percentileIdx) {
        List<PointF> points = new ArrayList<>();
        float[][] table = getTable(isMale, isHeight);
        for (int i = 0; i < AGES.length; i++) {
            points.add(new PointF(AGES[i], table[i][percentileIdx]));
        }
        return points;
    }
}
