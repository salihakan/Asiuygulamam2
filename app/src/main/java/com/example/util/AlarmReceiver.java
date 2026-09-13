package com.example.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.AlarmActivity;
import com.example.R;
import com.example.data.DataManager;
import com.example.model.InjectionPlan;

import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String ALARM_CHANNEL_ID = "injection_alarm_channel";
    public static final int ALARM_NOTIFICATION_ID = 5001;

    @Override
    public void onReceive(Context context, Intent intent) {
        DataManager dm = DataManager.getInstance(context);
        InjectionPlan plan = dm.getTodayPlan();

        // If today is already completed, schedule next day and return
        if (plan.isCompleted()) {
            AlarmScheduler.scheduleNextAlarm(context);
            return;
        }

        // 1. Launch Alarm Fullscreen Activity
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        alarmIntent.putExtra(AlarmActivity.EXTRA_DOSE, plan.getDose());
        alarmIntent.putExtra(AlarmActivity.EXTRA_LIMB_INDEX, plan.getLimb().getIndex());

        // 2. Also prepare Full Screen Intent Notification for system lockscreen wake up
        createAlarmChannel(context);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                ALARM_NOTIFICATION_ID,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String doseStr = String.format(Locale.getDefault(), "%.1f", plan.getDose());
        String limbStr = plan.getLimb().getLocalizedName(context);
        String alertText = "Doz: " + doseStr + " ünite • " + limbStr;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_syringe)
                .setContentTitle("⏰ ENJEKSİYON ALARMI!")
                .setContentText(alertText)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(ALARM_NOTIFICATION_ID, builder.build());
        }

        try {
            context.startActivity(alarmIntent);
        } catch (Exception ignored) {
        }

        // Reschedule for next day
        AlarmScheduler.scheduleNextAlarm(context);
    }

    private void createAlarmChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ALARM_CHANNEL_ID,
                    "Enjeksiyon Alarmları",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Yüksek öncelikli sesli enjeksiyon alarmı");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 600, 300, 600, 300, 800});
            channel.setBypassDnd(true);

            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
