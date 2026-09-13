package com.example.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.example.data.DataManager;
import com.example.model.InjectionPlan;
import com.example.model.UserSettings;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AlarmScheduler {

    public static final int ALARM_INTENT_REQUEST_CODE = 3001;
    public static final String ACTION_INJECTION_ALARM = "com.example.ACTION_INJECTION_ALARM";

    /**
     * Schedules the next injection alarm based on user settings and today's completion status.
     */
    public static void scheduleNextAlarm(Context context) {
        DataManager dm = DataManager.getInstance(context);
        UserSettings settings = dm.getSettings();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_INJECTION_ALARM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_INTENT_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (!settings.isAlarmEnabled()) {
            alarmManager.cancel(pendingIntent);
            return;
        }

        Calendar targetCal = Calendar.getInstance();
        targetCal.set(Calendar.HOUR_OF_DAY, settings.getAlarmHour());
        targetCal.set(Calendar.MINUTE, settings.getAlarmMinute());
        targetCal.set(Calendar.SECOND, 0);
        targetCal.set(Calendar.MILLISECOND, 0);

        InjectionPlan todayPlan = dm.getTodayPlan();
        long now = System.currentTimeMillis();

        // If today's injection is already done or the time for today has passed, schedule for tomorrow
        if (todayPlan.isCompleted() || targetCal.getTimeInMillis() <= now) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        long triggerAtMillis = targetCal.getTimeInMillis();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                    );
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                    );
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            }
        } catch (SecurityException se) {
            try {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Snoozes the alarm for a given number of minutes.
     */
    public static void snoozeAlarm(Context context, int minutes) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_INJECTION_ALARM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_INTENT_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAt = System.currentTimeMillis() + (minutes * 60L * 1000L);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Cancels the active injection alarm.
     */
    public static void cancelAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_INJECTION_ALARM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_INTENT_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    /**
     * Returns a human readable description of the next alarm date/time.
     */
    public static String getNextAlarmDescription(Context context) {
        DataManager dm = DataManager.getInstance(context);
        UserSettings settings = dm.getSettings();

        if (!settings.isAlarmEnabled()) {
            return "Alarm Kapalı";
        }

        Calendar targetCal = Calendar.getInstance();
        targetCal.set(Calendar.HOUR_OF_DAY, settings.getAlarmHour());
        targetCal.set(Calendar.MINUTE, settings.getAlarmMinute());
        targetCal.set(Calendar.SECOND, 0);

        InjectionPlan todayPlan = dm.getTodayPlan();
        long now = System.currentTimeMillis();

        boolean isToday = true;
        if (todayPlan.isCompleted() || targetCal.getTimeInMillis() <= now) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1);
            isToday = false;
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = timeFormat.format(targetCal.getTime());

        if (isToday) {
            return "Bugün, " + timeStr;
        } else {
            return "Yarın, " + timeStr;
        }
    }
}
