package com.example.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import com.example.MainActivity;
import com.example.R;
import com.example.data.DataManager;
import com.example.model.InjectionPlan;

import java.util.Calendar;
import java.util.Locale;

public class InjectionAppWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_MARK_DONE = "com.example.widget.ACTION_MARK_DONE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_MARK_DONE.equals(intent.getAction())) {
            DataManager dm = DataManager.getInstance(context);
            boolean current = dm.isInjectionCompletedToday();
            dm.setInjectionCompleted(Calendar.getInstance(), !current);
            updateAllWidgets(context);
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, InjectionAppWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        for (int id : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id);
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        DataManager dm = DataManager.getInstance(context);
        InjectionPlan plan = dm.getTodayPlan();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_injection);

        String doseStr = String.format(Locale.getDefault(), "%.1f ünite", plan.getDose());
        views.setTextViewText(R.id.widget_tv_dose, doseStr);
        views.setTextViewText(R.id.widget_tv_limb, plan.getLimb().getLocalizedName(context));
        views.setTextViewText(R.id.widget_tv_date, plan.getFormattedDate());

        if (plan.isCompleted()) {
            views.setTextViewText(R.id.widget_tv_status, "Tamamlandı ✓");
            views.setTextViewText(R.id.widget_btn_action, "Geri Al");
        } else {
            views.setTextViewText(R.id.widget_tv_status, "Bekliyor");
            views.setTextViewText(R.id.widget_btn_action, "Yapıldı İşaretle");
        }

        // Open app intent
        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openAppPending = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, openAppPending);

        // Mark done action intent
        Intent markDoneIntent = new Intent(context, InjectionAppWidgetProvider.class);
        markDoneIntent.setAction(ACTION_MARK_DONE);
        PendingIntent markDonePending = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                markDoneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_btn_action, markDonePending);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
