package com.example;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.data.DataManager;
import com.example.model.InjectionPlan;
import com.example.model.InjectionRecord;
import com.example.model.Limb;
import com.example.model.MeasurementRecord;
import com.example.model.UserSettings;
import com.example.ui.HistoryAdapter;
import com.example.ui.MeasurementAdapter;
import com.example.ui.WeightChartView;
import com.example.util.DoseCalculator;
import com.example.util.NotificationHelper;
import com.example.widget.InjectionAppWidgetProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private DataManager dataManager;
    private BottomNavigationView bottomNav;

    // Tab Views
    private View tabViewToday;
    private View tabViewMeasurements;
    private View tabViewHistory;
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
    private LinearLayout limbPillRightArm;
    private LinearLayout limbPillRightLeg;
    private LinearLayout limbPillLeftArm;
    private LinearLayout limbPillLeftLeg;
    private ImageView ivTomorrowIcon;
    private TextView tvTomorrowInfo;

    // Measurements Tab elements
    private TextView tvCurrentBmi;
    private TextView tvBmiCategoryPill;
    private TextView tvSummaryWeight;
    private TextView tvSummaryHeight;
    private WeightChartView chartWeight;
    private RecyclerView rvMeasurements;
    private TextView tvNoMeasurements;
    private TextView tvMeasurementCount;
    private ExtendedFloatingActionButton fabAddMeasurement;
    private MeasurementAdapter measurementAdapter;

    // History Tab elements
    private TextView tvHistoryStreak;
    private TextView tvHistoryTotalDone;
    private TextView tvHistoryRate;
    private ChipGroup chipgroupHistoryFilter;
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    private int historyDaysFilter = 14;

    // Settings Tab elements
    private RadioGroup rgTheme;
    private MaterialSwitch switchReminder;
    private TextView tvReminderTimeVal;
    private MaterialButton btnChangeTime;
    private MaterialButton btnTestNotification;
    private MaterialButton btnChangeStartDate;
    private MaterialButton btnLoadSample;
    private MaterialButton btnClearData;

    // Permission launcher for Android 13+
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
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
        setupHistoryTab();
        setupSettingsTab();

        // Notification channel and permission check
        NotificationHelper.createNotificationChannel(this);
        checkNotificationPermissionAndSchedule();

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

    private void checkNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        NotificationHelper.scheduleDailyReminder(this);
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
        toolbarSubtitle = findViewById(R.id.toolbar_subtitle);
        toolbarTvStreak = findViewById(R.id.toolbar_tv_streak);

        tabViewToday = findViewById(R.id.tab_view_today);
        tabViewMeasurements = findViewById(R.id.tab_view_measurements);
        tabViewHistory = findViewById(R.id.tab_view_history);
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
        limbPillRightArm = findViewById(R.id.limb_pill_right_arm);
        limbPillRightLeg = findViewById(R.id.limb_pill_right_leg);
        limbPillLeftArm = findViewById(R.id.limb_pill_left_arm);
        limbPillLeftLeg = findViewById(R.id.limb_pill_left_leg);
        ivTomorrowIcon = findViewById(R.id.iv_tomorrow_icon);
        tvTomorrowInfo = findViewById(R.id.tv_tomorrow_info);

        // Measurements views
        tvCurrentBmi = findViewById(R.id.tv_current_bmi);
        tvBmiCategoryPill = findViewById(R.id.tv_bmi_category_pill);
        tvSummaryWeight = findViewById(R.id.tv_summary_weight);
        tvSummaryHeight = findViewById(R.id.tv_summary_height);
        chartWeight = findViewById(R.id.chart_weight);
        rvMeasurements = findViewById(R.id.rv_measurements);
        tvNoMeasurements = findViewById(R.id.tv_no_measurements);
        tvMeasurementCount = findViewById(R.id.tv_measurement_count);
        fabAddMeasurement = findViewById(R.id.fab_add_measurement);

        // History views
        tvHistoryStreak = findViewById(R.id.tv_history_streak);
        tvHistoryTotalDone = findViewById(R.id.tv_history_total_done);
        tvHistoryRate = findViewById(R.id.tv_history_rate);
        chipgroupHistoryFilter = findViewById(R.id.chipgroup_history_filter);
        rvHistory = findViewById(R.id.rv_history);

        // Settings views
        rgTheme = findViewById(R.id.rg_theme);
        switchReminder = findViewById(R.id.switch_reminder);
        tvReminderTimeVal = findViewById(R.id.tv_reminder_time_val);
        btnChangeTime = findViewById(R.id.btn_change_time);
        btnTestNotification = findViewById(R.id.btn_test_notification);
        btnChangeStartDate = findViewById(R.id.btn_change_start_date);
        btnLoadSample = findViewById(R.id.btn_load_sample);
        btnClearData = findViewById(R.id.btn_clear_data);
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            tabViewToday.setVisibility(View.GONE);
            tabViewMeasurements.setVisibility(View.GONE);
            tabViewHistory.setVisibility(View.GONE);
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
            } else if (itemId == R.id.nav_history) {
                tabViewHistory.setVisibility(View.VISIBLE);
                toolbarSubtitle.setText(R.string.tab_history);
                refreshHistoryTab();
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
            refreshAllData();
            Snackbar.make(btnMarkCompleted, "Bugünkü enjeksiyon tamamlandı olarak işaretlendi ✓", Snackbar.LENGTH_SHORT).show();
        });

        btnUndoCompleted.setOnClickListener(v -> {
            dataManager.setInjectionCompleted(Calendar.getInstance(), false);
            InjectionAppWidgetProvider.updateAllWidgets(this);
            refreshAllData();
            Snackbar.make(btnUndoCompleted, "Bugünkü işaretleme geri alındı.", Snackbar.LENGTH_SHORT).show();
        });
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
        tvSpotlightLabel.setText("Bugün: " + limbName);

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

        // Tomorrow preview
        String tomorrowDose = tomorrow.getFormattedDose();
        String tomorrowLimb = tomorrow.getLimb().getLocalizedName(this);
        tvTomorrowInfo.setText(tomorrowDose + " • " + tomorrowLimb);
        ivTomorrowIcon.setImageResource(tomorrow.getLimb().getIconResId());

        // Header streak
        int streak = dataManager.calculateStreak();
        toolbarTvStreak.setText(streak + " Gün");
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
        List<MeasurementRecord> records = dataManager.getMeasurements();
        measurementAdapter.submitList(records);
        chartWeight.setData(records);

        tvMeasurementCount.setText(records.size() + " kayıt");
        tvNoMeasurements.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);

        MeasurementRecord latest = dataManager.getLatestMeasurement();
        if (latest != null) {
            tvCurrentBmi.setText(String.format(Locale.US, "%.1f", latest.getBmi()));
            tvBmiCategoryPill.setText(latest.getBmiCategoryLabel());
            tvBmiCategoryPill.setTextColor(latest.getBmiCategoryColorHex());
            tvSummaryWeight.setText(String.format(Locale.US, "%.1f kg", latest.getWeightKg()));
            tvSummaryHeight.setText(String.format(Locale.US, "%.0f cm", latest.getHeightCm()));
        } else {
            tvCurrentBmi.setText("--");
            tvBmiCategoryPill.setText("Kayıt Yok");
            tvBmiCategoryPill.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_light));
            tvSummaryWeight.setText("-- kg");
            tvSummaryHeight.setText("-- cm");
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

        // Real-time BMI calculation watcher
        TextWatcher bmiWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    float w = Float.parseFloat(etWeight.getText().toString().replace(',', '.'));
                    float h = Float.parseFloat(etHeight.getText().toString().replace(',', '.'));
                    float bmi = MeasurementRecord.calculateBmi(w, h);
                    if (bmi > 0) {
                        String cat;
                        if (bmi < 18.5f) cat = "Zayıf";
                        else if (bmi < 25.0f) cat = "Normal";
                        else if (bmi < 30.0f) cat = "Fazla Kilolu";
                        else cat = "Obezite";
                        tvBmiResult.setText(String.format(Locale.US, "%.1f (%s)", bmi, cat));
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
        etWeight.addTextChangedListener(bmiWatcher);
        etHeight.addTextChangedListener(bmiWatcher);

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

    private void setupHistoryTab() {
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(plan -> {
            Calendar target = plan.getCalendar();
            dataManager.setInjectionCompleted(target, !plan.isCompleted());
            InjectionAppWidgetProvider.updateAllWidgets(this);
            refreshAllData();
        });
        rvHistory.setAdapter(historyAdapter);

        chipgroupHistoryFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_filter_14)) {
                historyDaysFilter = 14;
            } else if (checkedIds.contains(R.id.chip_filter_30)) {
                historyDaysFilter = 30;
            } else {
                historyDaysFilter = 90; // All recent
            }
            refreshHistoryTab();
        });
    }

    private void refreshHistoryTab() {
        int streak = dataManager.calculateStreak();
        tvHistoryStreak.setText(streak + " Gün");

        List<InjectionPlan> planList = new ArrayList<>();
        int totalDone = 0;

        for (int i = 0; i < historyDaysFilter; i++) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -i);
            InjectionPlan plan = dataManager.getPlanForDate(c);
            planList.add(plan);
            if (plan.isCompleted()) {
                totalDone++;
            }
        }

        tvHistoryTotalDone.setText(String.valueOf(totalDone));
        int rate = (int) Math.round(((double) totalDone / historyDaysFilter) * 100.0);
        tvHistoryRate.setText("%" + rate);

        historyAdapter.submitList(planList);
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

        // Reminder Switch setup
        switchReminder.setChecked(settings.isReminderEnabled());
        tvReminderTimeVal.setText(settings.getFormattedTime());

        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UserSettings s = dataManager.getSettings();
            if (s.isReminderEnabled() == isChecked) {
                return;
            }
            s.setReminderEnabled(isChecked);
            dataManager.saveSettings(s);
            NotificationHelper.scheduleDailyReminder(this);
            String msg = isChecked ? "Günlük hatırlatıcı açıldı." : "Hatırlatıcı kapatıldı.";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Time Picker setup
        btnChangeTime.setOnClickListener(v -> {
            UserSettings s = dataManager.getSettings();
            TimePickerDialog tpd = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        s.setReminderHour(hourOfDay);
                        s.setReminderMinute(minute);
                        dataManager.saveSettings(s);
                        tvReminderTimeVal.setText(s.getFormattedTime());
                        NotificationHelper.scheduleDailyReminder(this);
                        Toast.makeText(this, "Hatırlatma saati " + s.getFormattedTime() + " olarak ayarlandı.", Toast.LENGTH_SHORT).show();
                    },
                    s.getReminderHour(),
                    s.getReminderMinute(),
                    true
            );
            tpd.show();
        });

        // Test Notification
        btnTestNotification.setOnClickListener(v -> {
            NotificationHelper.showReminderNotification(this);
            Toast.makeText(this, "Test bildirimi gönderildi.", Toast.LENGTH_SHORT).show();
        });

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
            refreshAllData();
            Toast.makeText(this, "Örnek enjeksiyon ve ölçüm verileri yüklendi!", Toast.LENGTH_LONG).show();
        });

        // Clear Data
        btnClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Verileri Sıfırla")
                    .setMessage("Tüm geçmiş enjeksiyon ve kilo kayıtları silinecektir. Devam etmek istiyor musunuz?")
                    .setPositiveButton("Evet, Sil", (dialog, which) -> {
                        dataManager.clearAllData();
                        InjectionAppWidgetProvider.updateAllWidgets(this);
                        refreshAllData();
                        Toast.makeText(this, "Tüm kayıtlar temizlendi.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("İptal", null)
                    .show();
        });
    }

    private void refreshSettingsTab() {
        UserSettings settings = dataManager.getSettings();
        tvReminderTimeVal.setText(settings.getFormattedTime());
        switchReminder.setChecked(settings.isReminderEnabled());

        Calendar start = DoseCalculator.parseDateKey(settings.getStartDateKey());
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", new Locale("tr", "TR"));
        btnChangeStartDate.setText("Başlangıç: " + sdf.format(start.getTime()));
    }

    private void refreshAllData() {
        refreshTodayTab();
        refreshMeasurementsTab();
        refreshHistoryTab();
        refreshSettingsTab();
    }
}
