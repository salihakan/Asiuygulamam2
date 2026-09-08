package com.example.util;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.example.MainActivity;
import com.example.R;
import com.example.data.DataManager;
import com.example.model.InjectionPlan;
import com.example.model.UserSettings;

import java.util.Calendar;
import java.util.Locale;

public class NotificationHelper {

    public static final String CHANNEL_ID = "injection_reminders";
    public static final String CHANNEL_NAME = "Enjeksiyon Hatırlatıcıları";
    public static final int NOTIFICATION_ID = 1001;
    public static final int ALARM_REQUEST_CODE = 2001;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Günlük enjeksiyon dozu ve uygulanacak uzuv hatırlatıcısı");
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void scheduleDailyReminder(Context context) {
        DataManager dm = DataManager.getInstance(context);
        UserSettings settings = dm.getSettings();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (!settings.isReminderEnabled()) {
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
            return;
        }

        Calendar targetCal = Calendar.getInstance();
        targetCal.set(Calendar.HOUR_OF_DAY, settings.getReminderHour());
        targetCal.set(Calendar.MINUTE, settings.getReminderMinute());
        targetCal.set(Calendar.SECOND, 0);
        targetCal.set(Calendar.MILLISECOND, 0);

        // If target time already passed today, schedule for tomorrow
        if (targetCal.getTimeInMillis() <= System.currentTimeMillis()) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                targetCal.getTimeInMillis(),
                                pendingIntent
                        );
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                targetCal.getTimeInMillis(),
                                pendingIntent
                        );
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            targetCal.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            targetCal.getTimeInMillis(),
                            pendingIntent
                    );
                }
            } catch (SecurityException se) {
                try {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            targetCal.getTimeInMillis(),
                            pendingIntent
                    );
                } catch (Exception ignored) {
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void showReminderNotification(Context context) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        try {
            DataManager dm = DataManager.getInstance(context);
            InjectionPlan plan = dm.getTodayPlan();

            // If today's injection is already marked as completed, no need to alert
            if (plan.isCompleted()) {
                return;
            }

            createNotificationChannel(context);

            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            String doseStr = String.format(Locale.getDefault(), "%.1f", plan.getDose());
            String limbStr = plan.getLimb().getLocalizedName(context).toLowerCase(new Locale("tr", "TR"));
            String contentText = String.format("Bugün %s ünite — %s", doseStr, limbStr);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_syringe)
                    .setContentTitle("Enjeksiyon Vakti 💉")
                    .setContentText(contentText)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, builder.build());
            }
        } catch (SecurityException ignored) {
        } catch (Exception ignored) {
        }
    }
}
