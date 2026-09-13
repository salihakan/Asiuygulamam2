package com.example;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.data.DataManager;
import com.example.model.InjectionPlan;
import com.example.model.Limb;
import com.example.model.UserSettings;
import com.example.util.AlarmScheduler;
import com.example.widget.InjectionAppWidgetProvider;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity {

    public static final String EXTRA_DOSE = "extra_dose";
    public static final String EXTRA_LIMB_INDEX = "extra_limb_index";

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Turn screen on and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        setContentView(R.layout.activity_alarm);

        DataManager dm = DataManager.getInstance(this);
        UserSettings settings = dm.getSettings();
        InjectionPlan plan = dm.getTodayPlan();

        TextView tvTime = findViewById(R.id.alarm_tv_time);
        TextView tvDose = findViewById(R.id.alarm_tv_dose);
        TextView tvLimb = findViewById(R.id.alarm_tv_limb);
        ImageView ivLimb = findViewById(R.id.alarm_iv_limb);

        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvTime.setText(timeFmt.format(Calendar.getInstance().getTime()));

        tvDose.setText(String.format(Locale.getDefault(), "%.1f ünite", plan.getDose()));
        tvLimb.setText(plan.getLimb().getLocalizedName(this));
        ivLimb.setImageResource(plan.getLimb().getIconResId());

        MaterialButton btnComplete = findViewById(R.id.alarm_btn_complete);
        MaterialButton btnSnooze = findViewById(R.id.alarm_btn_snooze);
        MaterialButton btnDismiss = findViewById(R.id.alarm_btn_dismiss);

        btnComplete.setOnClickListener(v -> {
            stopAlarm();
            dm.setInjectionCompleted(Calendar.getInstance(), true);
            InjectionAppWidgetProvider.updateAllWidgets(this);
            Toast.makeText(this, "Tebrikler! Enjeksiyon tamamlandı.", Toast.LENGTH_LONG).show();
            finish();
        });

        btnSnooze.setOnClickListener(v -> {
            stopAlarm();
            AlarmScheduler.snoozeAlarm(this, 5);
            Toast.makeText(this, "Alarm 5 dakika ertelendi.", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnDismiss.setOnClickListener(v -> {
            stopAlarm();
            finish();
        });

        startAlarmSoundAndVibration(settings.isAlarmVibrate());
    }

    private void startAlarmSoundAndVibration(boolean vibrate) {
        // 1. Play Sound
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception ignored) {
        }

        // 2. Vibrate
        if (vibrate) {
            try {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    long[] pattern = {0, 600, 300, 600, 300, 800};
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                    } else {
                        vibrator.vibrate(pattern, 0);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }

        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception ignored) {
            }
            vibrator = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarm();
    }
}
