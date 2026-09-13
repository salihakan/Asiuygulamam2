package com.example;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.data.DataManager;
import com.example.model.InjectionPlan;
import com.example.model.Limb;
import com.example.model.MeasurementRecord;
import com.example.model.UserSettings;
import com.example.ui.InteractiveBodyMapView;
import com.example.ui.MeasurementAdapter;
import com.example.ui.PercentileChartView;
import com.example.util.AlarmScheduler;
import com.example.util.DoseCalculator;
import com.example.util.NotificationHelper;
import com.example.util.PercentileCalculator;
import com.example.widget.InjectionAppWidgetProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private DataManager dataManager;
    private BottomNavigationView bottomNav;

    // Tab Views
    private View tabViewToday;
    private View tabViewMeasurements;
    private View tabViewSettings;

    // Header elements
    private TextView toolbarSubtitle;
    private TextView toolbarTvStreak;

    // Today Tab elements
    private MaterialCardView cardStatusBanner;
    private ImageView ivBannerIcon;
    private TextView tvBannerText;
    private TextView tvTodayDate;
    private TextView tvHeroPillStatus;
    private TextView tvTodayDose;
    private TextView tvTodayDoseRule;
    private ImageView ivTodayLimbIcon;
    private TextView tvTodayLimb;
    private MaterialButton btnMarkCompleted;
    private MaterialButton btnUndoCompleted;
    private TextView tvActiveLimbBadge;
    private TextView tvSpotlightLabel;
    private InteractiveBodyMapView bodyMapView;
    private View layoutLimbOverrideAction;
    private TextView tvLimbInspectHint;
    private MaterialButton btnSetAsTodayLimb;
    private MaterialButton btnResetToTodayLimb;
    private LinearLayout limbPillRightArm;
    private LinearLayout limbPillRightLeg;
    private LinearLayout limbPillLeftArm;
    private LinearLayout limbPillLeftLeg;
    private ImageView ivTomorrowIcon;
    private TextView tvTomorrowInfo;

    // Next Injection Alarm Card Elements (Today Tab)
    private MaterialSwitch switchTodayAlarm;
    private TextView tvNextAlarmStatusDesc;
    private TextView tvTodayAlarmTimeDisplay;
    private TextView tvTodayAlarmTargetInfo;
    private MaterialButton btnChangeTodayAlarmTime;
    private MaterialButton btnTestAlarmDirect;

    // Child Profile & Percentile Tab Elements
    private MaterialButtonToggleGroup toggleGroupGender;
    private MaterialButton btnGenderMale;
    private MaterialButton btnGenderFemale;
    private TextView tvChildBirthdateVal;
    private TextView tvChildAgeCalculated;
    private MaterialButton btnChangeBirthdate;

    private MaterialButtonToggleGroup toggleChartType;
    private MaterialButton btnChartHeight;
    private MaterialButton btnChartWeight;
    private TextView tvChartReferenceSubtitle;
    private PercentileChartView chartPercentile;
    private TextView tvPercentileStatusText;

    private TextView tvCurrentBmi;
    private TextView tvBmiCategoryPill;
    private TextView tvSummaryWeight;
    private TextView tvSummaryHeight;
    private RecyclerView rvMeasurements;
    private TextView tvNoMeasurements;
    private TextView tvMeasurementCount;
    private ExtendedFloatingActionButton fabAddMeasurement;
    private MeasurementAdapter measurementAdapter;
    private boolean isPercentileHeightMode = true;

    // Settings Tab elements
    private RadioGroup rgTheme;
    private MaterialSwitch switchAlarm;
    private TextView tvAlarmTimeVal;
    private MaterialButton btnChangeAlarmTime;
    private MaterialSwitch switchAlarmVibrate;
    private MaterialButton btnTestAlarm;
    private MaterialButton btnChangeStartDate;
    private MaterialButton btnLoadSample;
    private MaterialButton btnClearData;

    // Permission launcher for Android 13+
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    AlarmScheduler.scheduleNextAlarm(this);
                    NotificationHelper.scheduleDailyReminder(this);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dataManager = DataManager.getInstance(this);

        // Apply saved theme preference before view inflation
        applyTheme(dataManager.getSettings().getThemeMode());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupBottomNav();
        setupTodayTab();
        setupMeasurementsTab();
        setupSettingsTab();

        // Notification channel and permission check
        NotificationHelper.createNotificationChannel(this);
        checkPermissionsAndSchedule();

        // Initial schedule of the injection alarm
        AlarmScheduler.scheduleNextAlarm(this);

        // Load today's tab data
        refreshAllData();
    }

    private void applyTheme(String themeMode) {
        int targetMode;
        if ("LIGHT".equals(themeMode)) {
            targetMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if ("DARK".equals(themeMode)) {
            targetMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            targetMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
        }
    }

    private void checkPermissionsAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        AlarmScheduler.scheduleNextAlarm(this);
        NotificationHelper.scheduleDailyReminder(this);
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
        toolbarSubtitle = findViewById(R.id.toolbar_subtitle);
        toolbarTvStreak = findViewById(R.id.toolbar_tv_streak);

        tabViewToday = findViewById(R.id.tab_view_today);
        tabViewMeasurements = findViewById(R.id.tab_view_measurements);
        tabViewSettings = findViewById(R.id.tab_view_settings);

        // Today views
        cardStatusBanner = findViewById(R.id.card_status_banner);
        ivBannerIcon = findViewById(R.id.iv_banner_icon);
        tvBannerText = findViewById(R.id.tv_banner_text);
        tvTodayDate = findViewById(R.id.tv_today_date);
        tvHeroPillStatus = findViewById(R.id.tv_hero_pill_status);
        tvTodayDose = findViewById(R.id.tv_today_dose);
        tvTodayDoseRule = findViewById(R.id.tv_today_dose_rule);
        ivTodayLimbIcon = findViewById(R.id.iv_today_limb_icon);
        tvTodayLimb = findViewById(R.id.tv_today_limb);
        btnMarkCompleted = findViewById(R.id.btn_mark_completed);
        btnUndoCompleted = findViewById(R.id.btn_undo_completed);
        tvActiveLimbBadge = findViewById(R.id.tv_active_limb_badge);
        tvSpotlightLabel = findViewById(R.id.tv_spotlight_label);
        bodyMapView = findViewById(R.id.body_map_view);
        layoutLimbOverrideAction = findViewById(R.id.layout_limb_override_action);
        tvLimbInspectHint = findViewById(R.id.tv_limb_inspect_hint);
        btnSetAsTodayLimb = findViewById(R.id.btn_set_as_today_limb);
        btnResetToTodayLimb = findViewById(R.id.btn_reset_to_today_limb);
        limbPillRightArm = findViewById(R.id.limb_pill_right_arm);
        limbPillRightLeg = findViewById(R.id.limb_pill_right_leg);
        limbPillLeftArm = findViewById(R.id.limb_pill_left_arm);
        limbPillLeftLeg = findViewById(R.id.limb_pill_left_leg);
        ivTomorrowIcon = findViewById(R.id.iv_tomorrow_icon);
        tvTomorrowInfo = findViewById(R.id.tv_tomorrow_info);

        // Next Injection Alarm Card views (Today Tab)
        switchTodayAlarm = findViewById(R.id.switch_today_alarm);
        tvNextAlarmStatusDesc = findViewById(R.id.tv_next_alarm_status_desc);
        tvTodayAlarmTimeDisplay = findViewById(R.id.tv_today_alarm_time_display);
        tvTodayAlarmTargetInfo = findViewById(R.id.tv_today_alarm_target_info);
        btnChangeTodayAlarmTime = findViewById(R.id.btn_change_today_alarm_time);
        btnTestAlarmDirect = findViewById(R.id.btn_test_alarm_direct);

        // Child Profile & Percentile views
        toggleGroupGender = findViewById(R.id.toggle_group_gender);
        btnGenderMale = findViewById(R.id.btn_gender_male);
        btnGenderFemale = findViewById(R.id.btn_gender_female);
        tvChildBirthdateVal = findViewById(R.id.tv_child_birthdate_val);
        tvChildAgeCalculated = findViewById(R.id.tv_child_age_calculated);
        btnChangeBirthdate = findViewById(R.id.btn_change_birthdate);

        toggleChartType = findViewById(R.id.toggle_chart_type);
        btnChartHeight = findViewById(R.id.btn_chart_height);
        btnChartWeight = findViewById(R.id.btn_chart_weight);
        tvChartReferenceSubtitle = findViewById(R.id.tv_chart_reference_subtitle);
        chartPercentile = findViewById(R.id.chart_percentile);
        tvPercentileStatusText = findViewById(R.id.tv_percentile_status_text);

        tvCurrentBmi = findViewById(R.id.tv_current_bmi);
        tvBmiCategoryPill = findViewById(R.id.tv_bmi_category_pill);
        tvSummaryWeight = findViewById(R.id.tv_summary_weight);
        tvSummaryHeight = findViewById(R.id.tv_summary_height);
        rvMeasurements = findViewById(R.id.rv_measurements);
        tvNoMeasurements = findViewById(R.id.tv_no_measurements);
        tvMeasurementCount = findViewById(R.id.tv_measurement_count);
        fabAddMeasurement = findViewById(R.id.fab_add_measurement);

        // Settings views
        rgTheme = findViewById(R.id.rg_theme);
        switchAlarm = findViewById(R.id.switch_alarm);
        tvAlarmTimeVal = findViewById(R.id.tv_alarm_time_val);
        btnChangeAlarmTime = findViewById(R.id.btn_change_alarm_time);
        switchAlarmVibrate = findViewById(R.id.switch_alarm_vibrate);
        btnTestAlarm = findViewById(R.id.btn_test_alarm);
        btnChangeStartDate = findViewById(R.id.btn_change_start_date);
        btnLoadSample = findViewById(R.id.btn_load_sample);
        btnClearData = findViewById(R.id.btn_clear_data);
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            tabViewToday.setVisibility(View.GONE);
            tabViewMeasurements.setVisibility(View.GONE);
            tabViewSettings.setVisibility(View.GONE);

            if (itemId == R.id.nav_today) {
                tabViewToday.setVisibility(View.VISIBLE);
                toolbarSubtitle.setText(R.string.tab_today);
                refreshTodayTab();
                return true;
            } else if (itemId == R.id.nav_measurements) {
                tabViewMeasurements.setVisibility(View.VISIBLE);
                toolbarSubtitle.setText(R.string.tab_measurements);
                refreshMeasurementsTab();
                return true;
            } else if (itemId == R.id.nav_settings) {
                tabViewSettings.setVisibility(View.VISIBLE);
                toolbarSubtitle.setText(R.string.tab_settings);
                refreshSettingsTab();
                return true;
            }
            return false;
        });
    }

    private void setupTodayTab() {
        btnMarkCompleted.setOnClickListener(v -> {
            dataManager.setInjectionCompleted(Calendar.getInstance(), true);
            InjectionAppWidgetProvider.updateAllWidgets(this);
            AlarmScheduler.scheduleNextAlarm(this);
            refreshAllData();
            Snackbar.make(btnMarkCompleted, "Bugünkü enjeksiyon tamamlandı olarak işaretlendi ✓", Snackbar.LENGTH_SHORT).show();
        });

        btnUndoCompleted.setOnClickListener(v -> {
            dataManager.setInjectionCompleted(Calendar.getInstance(), false);
            InjectionAppWidgetProvider.updateAllWidgets(this);
            AlarmScheduler.scheduleNextAlarm(this);
            refreshAllData();
            Snackbar.make(btnUndoCompleted, "Bugünkü işaretleme geri alındı.", Snackbar.LENGTH_SHORT).show();
        });

        // Interactive Body Map Tap Listener
        if (bodyMapView != null) {
            bodyMapView.setOnLimbSelectedListener((limb, isTodayScheduled) -> {
                onLimbInspected(limb);
            });
        }

        // Interactive Limb 4-Pills Click Listeners
        if (limbPillRightArm != null) {
            limbPillRightArm.setOnClickListener(v -> {
                animatePillClick(limbPillRightArm);
                if (bodyMapView != null) bodyMapView.setSelectedLimb(Limb.RIGHT_ARM, true);
                onLimbInspected(Limb.RIGHT_ARM);
            });
        }
        if (limbPillRightLeg != null) {
            limbPillRightLeg.setOnClickListener(v -> {
                animatePillClick(limbPillRightLeg);
                if (bodyMapView != null) bodyMapView.setSelectedLimb(Limb.RIGHT_LEG, true);
                onLimbInspected(Limb.RIGHT_LEG);
            });
        }
        if (limbPillLeftArm != null) {
            limbPillLeftArm.setOnClickListener(v -> {
                animatePillClick(limbPillLeftArm);
                if (bodyMapView != null) bodyMapView.setSelectedLimb(Limb.LEFT_ARM, true);
                onLimbInspected(Limb.LEFT_ARM);
            });
        }
        if (limbPillLeftLeg != null) {
            limbPillLeftLeg.setOnClickListener(v -> {
                animatePillClick(limbPillLeftLeg);
                if (bodyMapView != null) bodyMapView.setSelectedLimb(Limb.LEFT_LEG, true);
                onLimbInspected(Limb.LEFT_LEG);
            });
        }

        // Action to switch today's limb to the inspected one
        if (btnSetAsTodayLimb != null) {
            btnSetAsTodayLimb.setOnClickListener(v -> {
                if (bodyMapView == null) return;
                Limb targetLimb = bodyMapView.getSelectedLimb();
                dataManager.setTodayLimbOverride(targetLimb);
                InjectionAppWidgetProvider.updateAllWidgets(this);
                refreshAllData();
                Snackbar.make(btnSetAsTodayLimb, "Bugünkü enjeksiyon uzvu " + targetLimb.getLocalizedName(this) + " olarak ayarlandı.", Snackbar.LENGTH_SHORT).show();
            });
        }

        // Action to revert back to today's scheduled limb
        if (btnResetToTodayLimb != null) {
            btnResetToTodayLimb.setOnClickListener(v -> {
                InjectionPlan today = dataManager.getTodayPlan();
                if (bodyMapView != null) {
                    bodyMapView.setSelectedLimb(today.getLimb(), true);
                }
                onLimbInspected(today.getLimb());
            });
        }

        // Today Next Injection Alarm controls
        if (switchTodayAlarm != null) {
            UserSettings settings = dataManager.getSettings();
            switchTodayAlarm.setChecked(settings.isAlarmEnabled());
            switchTodayAlarm.setOnCheckedChangeListener((bv, isChecked) -> {
                UserSettings s = dataManager.getSettings();
                s.setAlarmEnabled(isChecked);
                dataManager.saveSettings(s);
                AlarmScheduler.scheduleNextAlarm(this);
                refreshTodayAlarmCard();
                if (switchAlarm != null) switchAlarm.setChecked(isChecked);
                String msg = isChecked ? "Enjeksiyon alarmı açıldı." : "Enjeksiyon alarmı kapatıldı.";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            });
        }

        if (btnChangeTodayAlarmTime != null) {
            btnChangeTodayAlarmTime.setOnClickListener(v -> {
                UserSettings s = dataManager.getSettings();
                TimePickerDialog tpd = new TimePickerDialog(
                        this,
                        (view, hourOfDay, minute) -> {
                            s.setAlarmHour(hourOfDay);
                            s.setAlarmMinute(minute);
                            dataManager.saveSettings(s);
                            AlarmScheduler.scheduleNextAlarm(this);
                            refreshTodayAlarmCard();
                            if (tvAlarmTimeVal != null) tvAlarmTimeVal.setText(s.getFormattedAlarmTime());
                            Toast.makeText(this, "Alarm saati " + s.getFormattedAlarmTime() + " olarak ayarlandı.", Toast.LENGTH_SHORT).show();
                        },
                        s.getAlarmHour(),
                        s.getAlarmMinute(),
                        true
                );
                tpd.show();
            });
        }

        if (btnTestAlarmDirect != null) {
            btnTestAlarmDirect.setOnClickListener(v -> {
                Intent alarmIntent = new Intent(this, AlarmActivity.class);
                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(alarmIntent);
            });
        }
    }

    private void animatePillClick(View pill) {
        if (pill == null) return;
        pill.animate()
                .scaleX(1.12f)
                .scaleY(1.12f)
                .setDuration(120)
                .withEndAction(() -> pill.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                .start();
    }

    private void onLimbInspected(Limb limb) {
        if (limb == null) return;
        InjectionPlan today = dataManager.getTodayPlan();
        boolean isTodayLimb = (limb == today.getLimb());

        updateLimbPills(limb);

        if (isTodayLimb) {
            if (layoutLimbOverrideAction != null) layoutLimbOverrideAction.setVisibility(View.GONE);
            if (tvSpotlightLabel != null) {
                tvSpotlightLabel.setText(today.isCompleted() ? "Tamamlandı: " + limb.getLocalizedName(this) : "Bugün: " + limb.getLocalizedName(this));
            }
        } else {
            if (layoutLimbOverrideAction != null) layoutLimbOverrideAction.setVisibility(View.VISIBLE);
            if (tvLimbInspectHint != null) {
                tvLimbInspectHint.setText("Seçilen: " + limb.getLocalizedName(this) + " (" + (limb.getIndex() + 1) + ". Sıra)");
            }
            if (tvSpotlightLabel != null) {
                tvSpotlightLabel.setText("İncelenen: " + limb.getLocalizedName(this));
            }
        }
    }

    private void refreshTodayTab() {
        InjectionPlan today = dataManager.getTodayPlan();
        InjectionPlan tomorrow = dataManager.getTomorrowPlan();

        tvTodayDate.setText(today.getFormattedDate());
        tvTodayDose.setText(today.getFormattedDose());

        if (today.isSunday()) {
            tvTodayDoseRule.setText("Pazar kuralı (0,4 ünite)");
        } else {
            tvTodayDoseRule.setText("Pazartesi–Cumartesi kuralı (0,7 ünite)");
        }

        Limb limb = today.getLimb();
        String limbName = limb.getLocalizedName(this);
        tvTodayLimb.setText(limbName);
        ivTodayLimbIcon.setImageResource(limb.getIconResId());
        tvActiveLimbBadge.setText(limbName);
        tvSpotlightLabel.setText(today.isCompleted() ? "Tamamlandı: " + limbName : "Bugün: " + limbName);

        // Update animated body map
        if (bodyMapView != null) {
            bodyMapView.setCompleted(today.isCompleted());
            bodyMapView.setTodayLimb(limb);
        }
        if (layoutLimbOverrideAction != null) {
            layoutLimbOverrideAction.setVisibility(View.GONE);
        }

        // Update Limb 4-Pills Highlight
        updateLimbPills(limb);

        // Completion Status
        if (today.isCompleted()) {
            cardStatusBanner.setCardBackgroundColor(ContextCompat.getColor(this, R.color.success_light));
            cardStatusBanner.setStrokeColor(ContextCompat.getColor(this, R.color.success));
            ivBannerIcon.setImageResource(R.drawable.ic_check);
            ivBannerIcon.setColorFilter(ContextCompat.getColor(this, R.color.success));
            tvBannerText.setText(R.string.status_completed);
            tvBannerText.setTextColor(ContextCompat.getColor(this, R.color.on_success));

            tvHeroPillStatus.setText("TAMAMLANDI ✓");
            tvHeroPillStatus.setBackgroundResource(R.drawable.bg_badge_done);
            tvHeroPillStatus.setTextColor(ContextCompat.getColor(this, R.color.on_success));

            btnMarkCompleted.setText("Bugün Tamamlandı ✓");
            btnMarkCompleted.setEnabled(false);
            btnMarkCompleted.setAlpha(0.85f);
            btnUndoCompleted.setVisibility(View.VISIBLE);
        } else {
            cardStatusBanner.setCardBackgroundColor(ContextCompat.getColor(this, R.color.warning_light));
            cardStatusBanner.setStrokeColor(ContextCompat.getColor(this, R.color.warning));
            ivBannerIcon.setImageResource(R.drawable.ic_bell);
            ivBannerIcon.setColorFilter(ContextCompat.getColor(this, R.color.warning));
            tvBannerText.setText(R.string.status_pending);
            tvBannerText.setTextColor(Color.parseColor("#78350F"));

            tvHeroPillStatus.setText("BEKLİYOR");
            tvHeroPillStatus.setBackgroundResource(R.drawable.bg_badge_pending);
            tvHeroPillStatus.setTextColor(Color.parseColor("#92400E"));

            btnMarkCompleted.setText(R.string.mark_completed);
            btnMarkCompleted.setEnabled(true);
            btnMarkCompleted.setAlpha(1.0f);
            btnUndoCompleted.setVisibility(View.GONE);
        }

        // Refresh Next Alarm Card
        refreshTodayAlarmCard();

        // Tomorrow preview
        String tomorrowDose = tomorrow.getFormattedDose();
        String tomorrowLimb = tomorrow.getLimb().getLocalizedName(this);
        tvTomorrowInfo.setText(tomorrowDose + " • " + tomorrowLimb);
        ivTomorrowIcon.setImageResource(tomorrow.getLimb().getIconResId());

        // Header streak
        int streak = dataManager.calculateStreak();
        toolbarTvStreak.setText(streak + " Gün");
    }

    private void refreshTodayAlarmCard() {
        UserSettings settings = dataManager.getSettings();
        InjectionPlan today = dataManager.getTodayPlan();
        InjectionPlan tomorrow = dataManager.getTomorrowPlan();

        if (switchTodayAlarm != null) {
            switchTodayAlarm.setChecked(settings.isAlarmEnabled());
        }

        if (tvTodayAlarmTimeDisplay != null) {
            tvTodayAlarmTimeDisplay.setText(AlarmScheduler.getNextAlarmDescription(this));
        }

        if (tvTodayAlarmTargetInfo != null) {
            boolean isScheduledForToday = !today.isCompleted();
            InjectionPlan targetPlan = isScheduledForToday ? today : tomorrow;
            String dayLabel = isScheduledForToday ? "Bugün" : "Yarın";
            tvTodayAlarmTargetInfo.setText(targetPlan.getFormattedDose() + " • " + targetPlan.getLimb().getLocalizedName(this) + " (" + dayLabel + ")");
        }

        if (tvNextAlarmStatusDesc != null) {
            if (settings.isAlarmEnabled()) {
                tvNextAlarmStatusDesc.setText("Belirlenen saatte tam ekran sesli alarm çalacaktır");
            } else {
                tvNextAlarmStatusDesc.setText("Alarm kapalı — bildirim veya ses çalmaz");
            }
        }
    }

    private void updateLimbPills(Limb activeLimb) {
        setPillState(limbPillRightArm, activeLimb == Limb.RIGHT_ARM);
        setPillState(limbPillRightLeg, activeLimb == Limb.RIGHT_LEG);
        setPillState(limbPillLeftArm, activeLimb == Limb.LEFT_ARM);
        setPillState(limbPillLeftLeg, activeLimb == Limb.LEFT_LEG);
    }

    private void setPillState(LinearLayout pill, boolean isActive) {
        if (pill == null || pill.getChildCount() < 2) return;
        try {
            if (isActive) {
                pill.setBackgroundResource(R.drawable.bg_limb_active);
                View v0 = pill.getChildAt(0);
                View v1 = pill.getChildAt(1);
                if (v0 instanceof ImageView) {
                    ((ImageView) v0).setColorFilter(ContextCompat.getColor(this, R.color.primary));
                }
                if (v1 instanceof TextView) {
                    TextView tv = (TextView) v1;
                    tv.setTextColor(ContextCompat.getColor(this, R.color.primary));
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                }
            } else {
                pill.setBackgroundResource(R.drawable.bg_limb_inactive);
                View v0 = pill.getChildAt(0);
                View v1 = pill.getChildAt(1);
                if (v0 instanceof ImageView) {
                    ((ImageView) v0).setColorFilter(ContextCompat.getColor(this, R.color.text_secondary_light));
                }
                if (v1 instanceof TextView) {
                    TextView tv = (TextView) v1;
                    tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_light));
                    tv.setTypeface(null, android.graphics.Typeface.NORMAL);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void setupMeasurementsTab() {
        // Child Gender Toggle
        if (toggleGroupGender != null) {
            UserSettings settings = dataManager.getSettings();
            if ("FEMALE".equalsIgnoreCase(settings.getChildGender())) {
                toggleGroupGender.check(R.id.btn_gender_female);
            } else {
                toggleGroupGender.check(R.id.btn_gender_male);
            }

            toggleGroupGender.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    UserSettings s = dataManager.getSettings();
                    String gender = (checkedId == R.id.btn_gender_female) ? "FEMALE" : "MALE";
                    s.setChildGender(gender);
                    dataManager.saveSettings(s);
                    refreshMeasurementsTab();
                }
            });
        }

        // Child Birth Date Change
        if (btnChangeBirthdate != null) {
            btnChangeBirthdate.setOnClickListener(v -> {
                UserSettings s = dataManager.getSettings();
                Calendar birthCal = DoseCalculator.parseDateKey(s.getChildBirthDateKey());
                DatePickerDialog dpd = new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {
                            Calendar chosen = Calendar.getInstance();
                            chosen.set(year, month, dayOfMonth);
                            s.setChildBirthDateKey(DoseCalculator.formatDateKey(chosen));
                            dataManager.saveSettings(s);
                            refreshMeasurementsTab();
                        },
                        birthCal.get(Calendar.YEAR),
                        birthCal.get(Calendar.MONTH),
                        birthCal.get(Calendar.DAY_OF_MONTH)
                );
                dpd.show();
            });
        }

        // Chart Type Toggle (Boy / Kilo)
        if (toggleChartType != null) {
            toggleChartType.check(R.id.btn_chart_height);
            toggleChartType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    isPercentileHeightMode = (checkedId == R.id.btn_chart_height);
                    refreshMeasurementsTab();
                }
            });
        }

        rvMeasurements.setLayoutManager(new LinearLayoutManager(this));
        measurementAdapter = new MeasurementAdapter(new MeasurementAdapter.OnMeasurementListener() {
            @Override
            public void onEdit(MeasurementRecord record) {
                showAddOrEditMeasurementDialog(record);
            }

            @Override
            public void onDelete(MeasurementRecord record) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Kaydı Sil")
                        .setMessage(record.getFormattedDate() + " tarihli kilo kaydını silmek istediğinize emin misiniz?")
                        .setPositiveButton("Sil", (dialog, which) -> {
                            dataManager.deleteMeasurement(record.getId());
                            refreshMeasurementsTab();
                        })
                        .setNegativeButton("İptal", null)
                        .show();
            }
        });
        rvMeasurements.setAdapter(measurementAdapter);

        fabAddMeasurement.setOnClickListener(v -> showAddOrEditMeasurementDialog(null));
    }

    private void refreshMeasurementsTab() {
        UserSettings settings = dataManager.getSettings();
        boolean isMale = !"FEMALE".equalsIgnoreCase(settings.getChildGender());
        String birthDateKey = settings.getChildBirthDateKey();

        // 1. Birth Date & Age
        Calendar birthCal = DoseCalculator.parseDateKey(birthDateKey);
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", new Locale("tr", "TR"));
        if (tvChildBirthdateVal != null) {
            tvChildBirthdateVal.setText(sdf.format(birthCal.getTime()));
        }

        float currentAge = DoseCalculator.calculateDecimalAge(birthDateKey, DoseCalculator.formatDateKey(Calendar.getInstance()));
        int ageYears = (int) currentAge;
        int ageMonths = (int) ((currentAge - ageYears) * 12);
        if (tvChildAgeCalculated != null) {
            tvChildAgeCalculated.setText(String.format(Locale.getDefault(), "%d Yaş %d Ay (%.1f Yaşında)", ageYears, ageMonths, currentAge));
        }

        // 2. Chart Subtitle
        if (tvChartReferenceSubtitle != null) {
            String genderStr = isMale ? "Erkek Çocuk" : "Kız Çocuk";
            tvChartReferenceSubtitle.setText(genderStr + " • DSÖ & Neyzi Standardı (" + (isPercentileHeightMode ? "Boy Eğrisi" : "Kilo Eğrisi") + ")");
        }

        // 3. Percentile Chart Data
        List<MeasurementRecord> records = dataManager.getMeasurements();
        measurementAdapter.submitList(records);

        if (chartPercentile != null) {
            chartPercentile.setData(isMale, isPercentileHeightMode, currentAge, birthDateKey, records);
        }

        tvMeasurementCount.setText(records.size() + " kayıt");
        tvNoMeasurements.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);

        // 4. Latest Measurement & Percentile Evaluation
        MeasurementRecord latest = dataManager.getLatestMeasurement();
        if (latest != null) {
            tvCurrentBmi.setText(String.format(Locale.US, "%.1f", latest.getBmi()));
            tvBmiCategoryPill.setText(latest.getBmiCategoryLabel());
            tvBmiCategoryPill.setTextColor(latest.getBmiCategoryColorHex());
            tvSummaryWeight.setText(String.format(Locale.US, "%.1f kg", latest.getWeightKg()));
            tvSummaryHeight.setText(String.format(Locale.US, "%.0f cm", latest.getHeightCm()));

            float latestAge = DoseCalculator.calculateDecimalAge(birthDateKey, latest.getDateKey());
            float targetVal = isPercentileHeightMode ? latest.getHeightCm() : latest.getWeightKg();
            int p = PercentileCalculator.calculatePercentile(targetVal, latestAge, isMale, isPercentileHeightMode);

            if (tvPercentileStatusText != null) {
                String unit = isPercentileHeightMode ? "cm" : "kg";
                String evalText;
                if (p < 3) evalText = "Düşük Persentil";
                else if (p < 10) evalText = "Sınırda Düşük";
                else if (p <= 90) evalText = "İdeal / Normal Gelişim";
                else if (p <= 97) evalText = "Sınırda Yüksek";
                else evalText = "Yüksek Persentil";

                tvPercentileStatusText.setText(String.format(Locale.getDefault(),
                        "Son Ölçüm: %.1f %s • %% %d Persentil (%s)",
                        targetVal, unit, p, evalText));
            }
        } else {
            tvCurrentBmi.setText("--");
            tvBmiCategoryPill.setText("Kayıt Yok");
            tvBmiCategoryPill.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_light));
            tvSummaryWeight.setText("-- kg");
            tvSummaryHeight.setText("-- cm");
            if (tvPercentileStatusText != null) {
                tvPercentileStatusText.setText("Persentil hesaplaması için lütfen bir boy/kilo kaydı ekleyin.");
            }
        }
    }

    private void showAddOrEditMeasurementDialog(MeasurementRecord existing) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_measurement, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.dialog_tv_title);
        MaterialButton btnDate = dialogView.findViewById(R.id.dialog_btn_date);
        EditText etWeight = dialogView.findViewById(R.id.dialog_et_weight);
        EditText etHeight = dialogView.findViewById(R.id.dialog_et_height);
        TextView tvBmiResult = dialogView.findViewById(R.id.dialog_tv_bmi_result);

        Calendar selectedDate = Calendar.getInstance();

        if (existing != null) {
            tvDialogTitle.setText("Ölçümü Düzenle");
            selectedDate = DoseCalculator.parseDateKey(existing.getDateKey());
            etWeight.setText(String.format(Locale.US, "%.1f", existing.getWeightKg()));
            etHeight.setText(String.format(Locale.US, "%.0f", existing.getHeightCm()));
        } else {
            tvDialogTitle.setText("Yeni Ölçüm Ekle");
            MeasurementRecord latest = dataManager.getLatestMeasurement();
            if (latest != null && latest.getHeightCm() > 0) {
                etHeight.setText(String.format(Locale.US, "%.0f", latest.getHeightCm()));
            }
        }

        updateDialogDateButton(btnDate, selectedDate);

        Calendar finalSelectedDate = selectedDate;
        btnDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        finalSelectedDate.set(year, month, dayOfMonth);
                        updateDialogDateButton(btnDate, finalSelectedDate);
                    },
                    finalSelectedDate.get(Calendar.YEAR),
                    finalSelectedDate.get(Calendar.MONTH),
                    finalSelectedDate.get(Calendar.DAY_OF_MONTH)
            );
            dpd.show();
        });

        // Real-time BMI and Percentile calculation watcher
        UserSettings settings = dataManager.getSettings();
        boolean isMale = !"FEMALE".equalsIgnoreCase(settings.getChildGender());

        TextWatcher liveWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    float w = Float.parseFloat(etWeight.getText().toString().replace(',', '.'));
                    float h = Float.parseFloat(etHeight.getText().toString().replace(',', '.'));
                    float bmi = MeasurementRecord.calculateBmi(w, h);
                    float mAge = DoseCalculator.calculateDecimalAge(settings.getChildBirthDateKey(), DoseCalculator.formatDateKey(finalSelectedDate));
                    int pHeight = PercentileCalculator.calculatePercentile(h, mAge, isMale, true);
                    int pWeight = PercentileCalculator.calculatePercentile(w, mAge, isMale, false);

                    if (bmi > 0) {
                        tvBmiResult.setText(String.format(Locale.getDefault(),
                                "VKİ: %.1f • Boy: %%%dP • Kilo: %%%dP",
                                bmi, pHeight, pWeight));
                    } else {
                        tvBmiResult.setText("--");
                    }
                } catch (Exception e) {
                    tvBmiResult.setText("--");
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        etWeight.addTextChangedListener(liveWatcher);
        etHeight.addTextChangedListener(liveWatcher);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Kaydet", (dialog, which) -> {
                    String wStr = etWeight.getText().toString().replace(',', '.').trim();
                    String hStr = etHeight.getText().toString().replace(',', '.').trim();

                    if (wStr.isEmpty() || hStr.isEmpty()) {
                        Toast.makeText(this, "Lütfen geçerli kilo ve boy girin.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        float weight = Float.parseFloat(wStr);
                        float height = Float.parseFloat(hStr);
                        String id = (existing != null) ? existing.getId() : UUID.randomUUID().toString();
                        String dateKey = DoseCalculator.formatDateKey(finalSelectedDate);

                        MeasurementRecord record = new MeasurementRecord(id, dateKey, weight, height, finalSelectedDate.getTimeInMillis());
                        dataManager.addOrUpdateMeasurement(record);
                        refreshMeasurementsTab();
                        Toast.makeText(this, "Ölçüm kaydedildi.", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Değerler sayısal olmalıdır.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void updateDialogDateButton(MaterialButton btn, Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", new Locale("tr", "TR"));
        btn.setText("Tarih: " + sdf.format(cal.getTime()));
    }

    private void setupSettingsTab() {
        UserSettings settings = dataManager.getSettings();

        // Theme radio setup
        String currentTheme = settings.getThemeMode();
        if ("LIGHT".equals(currentTheme)) {
            rgTheme.check(R.id.rb_theme_light);
        } else if ("DARK".equals(currentTheme)) {
            rgTheme.check(R.id.rb_theme_dark);
        } else {
            rgTheme.check(R.id.rb_theme_system);
        }

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            String newTheme = "SYSTEM";
            if (checkedId == R.id.rb_theme_light) {
                newTheme = "LIGHT";
            } else if (checkedId == R.id.rb_theme_dark) {
                newTheme = "DARK";
            }
            UserSettings s = dataManager.getSettings();
            if (newTheme.equals(s.getThemeMode())) {
                return;
            }
            s.setThemeMode(newTheme);
            dataManager.saveSettings(s);
            applyTheme(newTheme);
        });

        // Alarm Master Switch setup
        if (switchAlarm != null) {
            switchAlarm.setChecked(settings.isAlarmEnabled());
            switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                UserSettings s = dataManager.getSettings();
                s.setAlarmEnabled(isChecked);
                dataManager.saveSettings(s);
                AlarmScheduler.scheduleNextAlarm(this);
                if (switchTodayAlarm != null) switchTodayAlarm.setChecked(isChecked);
                refreshTodayAlarmCard();
                String msg = isChecked ? "Enjeksiyon alarmı açıldı." : "Enjeksiyon alarmı kapatıldı.";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            });
        }

        // Alarm Time Picker setup
        if (tvAlarmTimeVal != null) {
            tvAlarmTimeVal.setText(settings.getFormattedAlarmTime());
        }

        if (btnChangeAlarmTime != null) {
            btnChangeAlarmTime.setOnClickListener(v -> {
                UserSettings s = dataManager.getSettings();
                TimePickerDialog tpd = new TimePickerDialog(
                        this,
                        (view, hourOfDay, minute) -> {
                            s.setAlarmHour(hourOfDay);
                            s.setAlarmMinute(minute);
                            dataManager.saveSettings(s);
                            if (tvAlarmTimeVal != null) tvAlarmTimeVal.setText(s.getFormattedAlarmTime());
                            AlarmScheduler.scheduleNextAlarm(this);
                            refreshTodayAlarmCard();
                            Toast.makeText(this, "Alarm saati " + s.getFormattedAlarmTime() + " olarak ayarlandı.", Toast.LENGTH_SHORT).show();
                        },
                        s.getAlarmHour(),
                        s.getAlarmMinute(),
                        true
                );
                tpd.show();
            });
        }

        // Vibration Switch setup
        if (switchAlarmVibrate != null) {
            switchAlarmVibrate.setChecked(settings.isAlarmVibrate());
            switchAlarmVibrate.setOnCheckedChangeListener((buttonView, isChecked) -> {
                UserSettings s = dataManager.getSettings();
                s.setAlarmVibrate(isChecked);
                dataManager.saveSettings(s);
            });
        }

        // Test Alarm
        if (btnTestAlarm != null) {
            btnTestAlarm.setOnClickListener(v -> {
                Intent alarmIntent = new Intent(this, AlarmActivity.class);
                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(alarmIntent);
            });
        }

        // Start Date config
        btnChangeStartDate.setOnClickListener(v -> {
            Calendar currentStart = DoseCalculator.parseDateKey(dataManager.getSettings().getStartDateKey());
            DatePickerDialog dpd = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(year, month, dayOfMonth);
                        UserSettings s = dataManager.getSettings();
                        s.setStartDateKey(DoseCalculator.formatDateKey(chosen));
                        dataManager.saveSettings(s);
                        refreshAllData();
                        Toast.makeText(this, "Başlangıç tarihi güncellendi.", Toast.LENGTH_SHORT).show();
                    },
                    currentStart.get(Calendar.YEAR),
                    currentStart.get(Calendar.MONTH),
                    currentStart.get(Calendar.DAY_OF_MONTH)
            );
            dpd.show();
        });

        // Load Sample Data
        btnLoadSample.setOnClickListener(v -> {
            dataManager.loadSampleData();
            InjectionAppWidgetProvider.updateAllWidgets(this);
            AlarmScheduler.scheduleNextAlarm(this);
            refreshAllData();
            Toast.makeText(this, "Örnek çocuk gelişim ve enjeksiyon verileri yüklendi!", Toast.LENGTH_LONG).show();
        });

        // Clear Data
        btnClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Verileri Sıfırla")
                    .setMessage("Tüm enjeksiyon ve ölçüm kayıtları silinecektir. Devam etmek istiyor musunuz?")
                    .setPositiveButton("Evet, Sil", (dialog, which) -> {
                        dataManager.clearAllData();
                        InjectionAppWidgetProvider.updateAllWidgets(this);
                        AlarmScheduler.scheduleNextAlarm(this);
                        refreshAllData();
                        Toast.makeText(this, "Tüm kayıtlar temizlendi.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("İptal", null)
                    .show();
        });
    }

    private void refreshSettingsTab() {
        UserSettings settings = dataManager.getSettings();
        if (tvAlarmTimeVal != null) tvAlarmTimeVal.setText(settings.getFormattedAlarmTime());
        if (switchAlarm != null) switchAlarm.setChecked(settings.isAlarmEnabled());
        if (switchAlarmVibrate != null) switchAlarmVibrate.setChecked(settings.isAlarmVibrate());

        Calendar start = DoseCalculator.parseDateKey(settings.getStartDateKey());
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", new Locale("tr", "TR"));
        btnChangeStartDate.setText("Başlangıç: " + sdf.format(start.getTime()));
    }

    private void refreshAllData() {
        refreshTodayTab();
        refreshMeasurementsTab();
        refreshSettingsTab();
    }
}
