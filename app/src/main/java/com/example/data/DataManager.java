package com.example.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.model.InjectionPlan;
import com.example.model.InjectionRecord;
import com.example.model.Limb;
import com.example.model.MeasurementRecord;
import com.example.model.UserSettings;
import com.example.util.DoseCalculator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private static final String PREFS_NAME = "injection_tracker_prefs";
    private static final String KEY_SETTINGS = "user_settings";
    private static final String KEY_RECORDS = "injection_records";
    private static final String KEY_MEASUREMENTS = "measurements";

    private static DataManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Context appContext;

    private DataManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        initDefaultSettingsIfNeeded();
    }

    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context);
        }
        return instance;
    }

    private void initDefaultSettingsIfNeeded() {
        if (!prefs.contains(KEY_SETTINGS)) {
            UserSettings settings = new UserSettings();
            Calendar today = DoseCalculator.normalizeToMidnight(Calendar.getInstance());
            settings.setStartDateKey(DoseCalculator.formatDateKey(today));
            settings.setStartLimbIndex(0); // Right arm
            saveSettings(settings);
        }
    }

    public UserSettings getSettings() {
        String json = prefs.getString(KEY_SETTINGS, null);
        if (json != null) {
            try {
                UserSettings settings = gson.fromJson(json, UserSettings.class);
                if (settings != null) return settings;
            } catch (Exception ignored) {
            }
        }
        UserSettings def = new UserSettings();
        def.setStartDateKey(DoseCalculator.formatDateKey(Calendar.getInstance()));
        return def;
    }

    public void saveSettings(UserSettings settings) {
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply();
    }

    public List<InjectionRecord> getInjectionRecords() {
        String json = prefs.getString(KEY_RECORDS, null);
        if (json != null) {
            try {
                Type listType = new TypeToken<ArrayList<InjectionRecord>>() {}.getType();
                List<InjectionRecord> list = gson.fromJson(json, listType);
                if (list != null) {
                    Collections.sort(list, (o1, o2) -> o2.getDateKey().compareTo(o1.getDateKey()));
                    return list;
                }
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>();
    }

    public void saveInjectionRecords(List<InjectionRecord> records) {
        prefs.edit().putString(KEY_RECORDS, gson.toJson(records)).apply();
    }

    public InjectionRecord getRecordForDate(String dateKey) {
        List<InjectionRecord> list = getInjectionRecords();
        for (InjectionRecord record : list) {
            if (record.getDateKey().equals(dateKey)) {
                return record;
            }
        }
        return null;
    }

    public boolean isInjectionCompleted(String dateKey) {
        InjectionRecord record = getRecordForDate(dateKey);
        return record != null && record.isCompleted();
    }

    public boolean isInjectionCompletedToday() {
        String todayKey = DoseCalculator.formatDateKey(Calendar.getInstance());
        return isInjectionCompleted(todayKey);
    }

    public void setInjectionCompleted(Calendar date, boolean completed) {
        String dateKey = DoseCalculator.formatDateKey(date);
        List<InjectionRecord> list = getInjectionRecords();
        InjectionRecord found = null;
        for (InjectionRecord r : list) {
            if (r.getDateKey().equals(dateKey)) {
                found = r;
                break;
            }
        }

        UserSettings settings = getSettings();
        Calendar startCal = DoseCalculator.parseDateKey(settings.getStartDateKey());
        InjectionPlan plan = DoseCalculator.calculatePlan(date, startCal, settings.getStartLimbIndex(), completed);

        if (found != null) {
            found.setCompleted(completed);
            found.setCompletedTimestamp(completed ? System.currentTimeMillis() : 0);
        } else {
            found = new InjectionRecord(
                    UUID.randomUUID().toString(),
                    dateKey,
                    plan.getLimb().getIndex(),
                    plan.getLimb().getDefaultName(),
                    plan.getDose(),
                    completed,
                    completed ? System.currentTimeMillis() : 0
            );
            list.add(found);
        }

        saveInjectionRecords(list);
    }

    public InjectionPlan getPlanForDate(Calendar date) {
        UserSettings settings = getSettings();
        Calendar startCal = DoseCalculator.parseDateKey(settings.getStartDateKey());
        boolean completed = isInjectionCompleted(DoseCalculator.formatDateKey(date));
        return DoseCalculator.calculatePlan(date, startCal, settings.getStartLimbIndex(), completed);
    }

    public InjectionPlan getTodayPlan() {
        return getPlanForDate(Calendar.getInstance());
    }

    public InjectionPlan getTomorrowPlan() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        return getPlanForDate(cal);
    }

    public void setTodayLimbOverride(Limb newLimb) {
        if (newLimb == null) return;
        UserSettings settings = getSettings();
        Calendar today = DoseCalculator.normalizeToMidnight(Calendar.getInstance());
        Calendar startCal = DoseCalculator.parseDateKey(settings.getStartDateKey());
        long diffMillis = today.getTimeInMillis() - startCal.getTimeInMillis();
        long diffDays = Math.round((double) diffMillis / (24.0 * 60 * 60 * 1000));
        int newStartIndex = (int) (((newLimb.getIndex() - diffDays) % 4 + 4) % 4);
        settings.setStartLimbIndex(newStartIndex);
        saveSettings(settings);

        String todayKey = DoseCalculator.formatDateKey(today);
        List<InjectionRecord> list = getInjectionRecords();
        for (InjectionRecord r : list) {
            if (r.getDateKey().equals(todayKey)) {
                r.setLimbIndex(newLimb.getIndex());
                r.setLimbName(newLimb.getDefaultName());
                saveInjectionRecords(list);
                break;
            }
        }
    }

    public int calculateStreak() {
        List<InjectionRecord> records = getInjectionRecords();
        Map<String, Boolean> statusMap = new HashMap<>();
        for (InjectionRecord r : records) {
            if (r.isCompleted()) {
                statusMap.put(r.getDateKey(), true);
            }
        }

        Calendar checkCal = Calendar.getInstance();
        String todayKey = DoseCalculator.formatDateKey(checkCal);

        // If today is completed, streak starts today. If not completed yet, check from yesterday!
        if (!Boolean.TRUE.equals(statusMap.get(todayKey))) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1);
        }

        int streak = 0;
        while (true) {
            String key = DoseCalculator.formatDateKey(checkCal);
            if (Boolean.TRUE.equals(statusMap.get(key))) {
                streak++;
                checkCal.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        return streak;
    }

    // Measurements
    public List<MeasurementRecord> getMeasurements() {
        String json = prefs.getString(KEY_MEASUREMENTS, null);
        if (json != null) {
            try {
                Type listType = new TypeToken<ArrayList<MeasurementRecord>>() {}.getType();
                List<MeasurementRecord> list = gson.fromJson(json, listType);
                if (list != null) {
                    Collections.sort(list, (o1, o2) -> o2.getDateKey().compareTo(o1.getDateKey()));
                    return list;
                }
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>();
    }

    public void saveMeasurements(List<MeasurementRecord> list) {
        prefs.edit().putString(KEY_MEASUREMENTS, gson.toJson(list)).apply();
    }

    public void addOrUpdateMeasurement(MeasurementRecord record) {
        List<MeasurementRecord> list = getMeasurements();
        boolean updated = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(record.getId())) {
                list.set(i, record);
                updated = true;
                break;
            }
        }
        if (!updated) {
            list.add(0, record);
        }
        saveMeasurements(list);
    }

    public void deleteMeasurement(String id) {
        List<MeasurementRecord> list = getMeasurements();
        List<MeasurementRecord> remaining = new ArrayList<>();
        for (MeasurementRecord r : list) {
            if (!r.getId().equals(id)) {
                remaining.add(r);
            }
        }
        saveMeasurements(remaining);
    }

    public MeasurementRecord getLatestMeasurement() {
        List<MeasurementRecord> list = getMeasurements();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public void loadSampleData() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -10);

        UserSettings settings = getSettings();
        settings.setChildGender("MALE");
        settings.setChildBirthDateKey("2019-05-15");
        saveSettings(settings);

        Calendar startCal = DoseCalculator.parseDateKey(settings.getStartDateKey());

        List<InjectionRecord> records = new ArrayList<>();
        for (int i = 10; i >= 1; i--) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -i);
            String key = DoseCalculator.formatDateKey(c);
            InjectionPlan p = DoseCalculator.calculatePlan(c, startCal, settings.getStartLimbIndex(), true);
            // make 1 day missed for realistic history
            boolean completed = (i != 4);
            records.add(new InjectionRecord(
                    UUID.randomUUID().toString(),
                    key,
                    p.getLimb().getIndex(),
                    p.getLimb().getDefaultName(),
                    p.getDose(),
                    completed,
                    completed ? c.getTimeInMillis() : 0
            ));
        }
        saveInjectionRecords(records);

        List<MeasurementRecord> measurements = new ArrayList<>();
        // Realistic progression for a ~7-year old child over the past year (ideal growth curve)
        float[] sampleWeights = {21.0f, 21.8f, 22.5f, 23.4f, 24.2f};
        float[] sampleHeights = {118.0f, 119.5f, 121.0f, 122.5f, 124.0f};
        for (int i = 0; i < sampleWeights.length; i++) {
            Calendar mc = Calendar.getInstance();
            mc.add(Calendar.DAY_OF_YEAR, -((sampleWeights.length - 1 - i) * 60)); // every 2 months
            measurements.add(new MeasurementRecord(
                    UUID.randomUUID().toString(),
                    DoseCalculator.formatDateKey(mc),
                    sampleWeights[i],
                    sampleHeights[i],
                    mc.getTimeInMillis()
            ));
        }
        saveMeasurements(measurements);
    }

    public void clearAllData() {
        prefs.edit().remove(KEY_RECORDS).remove(KEY_MEASUREMENTS).apply();
    }
}
