package com.example.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import com.example.model.MeasurementRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WeightChartView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private final List<MeasurementRecord> records = new ArrayList<>();

    private int selectedIndex = -1;
    private final float density;

    public WeightChartView(Context context) {
        this(context, null);
    }

    public WeightChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeightChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        init();
    }

    private void init() {
        linePaint.setColor(Color.parseColor("#006876"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3.5f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint.setColor(Color.parseColor("#006876"));
        dotPaint.setStyle(Paint.Style.FILL);

        dotInnerPaint.setColor(Color.WHITE);
        dotInnerPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.parseColor("#64748B"));
        textPaint.setTextSize(11f * density);
        textPaint.setTextAlign(Paint.Align.CENTER);

        gridPaint.setColor(Color.parseColor("#E2E8F0"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f * density);

        highlightPaint.setColor(Color.parseColor("#F43F5E"));
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(2f * density);
    }

    public void setData(List<MeasurementRecord> newRecords) {
        records.clear();
        if (newRecords != null) {
            // Sort ascending by date for chronological chart display
            List<MeasurementRecord> copy = new ArrayList<>(newRecords);
            Collections.sort(copy, (o1, o2) -> o1.getDateKey().compareTo(o2.getDateKey()));
            records.addAll(copy);
        }
        selectedIndex = records.isEmpty() ? -1 : records.size() - 1;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (records.size() < 2) return super.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float paddingLeft = 40f * density;
            float paddingRight = 40f * density;
            float width = getWidth() - paddingLeft - paddingRight;
            float stepX = width / (records.size() - 1);

            int closest = 0;
            float minDistance = Float.MAX_VALUE;
            for (int i = 0; i < records.size(); i++) {
                float pointX = paddingLeft + i * stepX;
                float dist = Math.abs(pointX - x);
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = i;
                }
            }
            if (selectedIndex != closest) {
                selectedIndex = closest;
                invalidate();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (records.isEmpty()) {
            textPaint.setTextSize(13f * density);
            canvas.drawText("Henüz kilo kaydı yok", w / 2f, h / 2f, textPaint);
            return;
        }

        float paddingLeft = 40f * density;
        float paddingRight = 40f * density;
        float paddingTop = 45f * density;
        float paddingBottom = 35f * density;

        float chartW = w - paddingLeft - paddingRight;
        float chartH = h - paddingTop - paddingBottom;

        if (records.size() == 1) {
            // Single point
            MeasurementRecord single = records.get(0);
            float cx = w / 2f;
            float cy = h / 2f;
            canvas.drawCircle(cx, cy, 7f * density, dotPaint);
            canvas.drawCircle(cx, cy, 4f * density, dotInnerPaint);
            textPaint.setTextSize(13f * density);
            canvas.drawText(String.format(Locale.US, "%.1f kg", single.getWeightKg()), cx, cy - 14f * density, textPaint);
            canvas.drawText(single.getDateKey(), cx, cy + 22f * density, textPaint);
            return;
        }

        float minWeight = Float.MAX_VALUE;
        float maxWeight = Float.MIN_VALUE;
        for (MeasurementRecord r : records) {
            if (r.getWeightKg() < minWeight) minWeight = r.getWeightKg();
            if (r.getWeightKg() > maxWeight) maxWeight = r.getWeightKg();
        }

        // Add 1kg buffer around range
        minWeight = Math.max(0, minWeight - 1.5f);
        maxWeight = maxWeight + 1.5f;
        float range = Math.max(1f, maxWeight - minWeight);

        // Draw horizontal grid lines
        for (int i = 0; i <= 3; i++) {
            float gridY = paddingTop + (chartH / 3f) * i;
            canvas.drawLine(paddingLeft - 10f * density, gridY, w - paddingRight + 10f * density, gridY, gridPaint);
            float weightLabel = maxWeight - (range / 3f) * i;
            textPaint.setTextSize(9f * density);
            canvas.drawText(String.format(Locale.US, "%.0f", weightLabel), 20f * density, gridY + 3f * density, textPaint);
        }

        // Calculate point coordinates
        float stepX = chartW / (records.size() - 1);
        float[] pointsX = new float[records.size()];
        float[] pointsY = new float[records.size()];

        linePath.reset();
        fillPath.reset();

        for (int i = 0; i < records.size(); i++) {
            pointsX[i] = paddingLeft + i * stepX;
            float normalizedY = (records.get(i).getWeightKg() - minWeight) / range;
            pointsY[i] = paddingTop + chartH * (1f - normalizedY);

            if (i == 0) {
                linePath.moveTo(pointsX[i], pointsY[i]);
                fillPath.moveTo(pointsX[i], paddingTop + chartH);
                fillPath.lineTo(pointsX[i], pointsY[i]);
            } else {
                // Smooth bezier cubic connection
                float prevX = pointsX[i - 1];
                float prevY = pointsY[i - 1];
                float midX = (prevX + pointsX[i]) / 2f;
                linePath.cubicTo(midX, prevY, midX, pointsY[i], pointsX[i], pointsY[i]);
                fillPath.cubicTo(midX, prevY, midX, pointsY[i], pointsX[i], pointsY[i]);
            }
        }

        // Close fill path
        fillPath.lineTo(pointsX[records.size() - 1], paddingTop + chartH);
        fillPath.close();

        // Fill gradient
        fillPaint.setShader(new LinearGradient(
                0, paddingTop, 0, paddingTop + chartH,
                Color.parseColor("#44006876"), Color.parseColor("#05006876"),
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        // Draw points and labels
        for (int i = 0; i < records.size(); i++) {
            boolean isSelected = (i == selectedIndex);
            float radius = isSelected ? 6.5f * density : 4.5f * density;

            if (isSelected) {
                // Draw vertical indicator line
                canvas.drawLine(pointsX[i], paddingTop, pointsX[i], paddingTop + chartH, highlightPaint);
            }

            canvas.drawCircle(pointsX[i], pointsY[i], radius, dotPaint);
            canvas.drawCircle(pointsX[i], pointsY[i], radius - 2f * density, dotInnerPaint);

            // Draw date label for first, middle, last, or selected
            if (isSelected || i == 0 || i == records.size() - 1 || records.size() <= 5) {
                textPaint.setTextSize(10f * density);
                String shortDate = records.get(i).getDateKey().substring(5); // MM-DD
                canvas.drawText(shortDate, pointsX[i], paddingTop + chartH + 18f * density, textPaint);
            }

            // Draw weight value badge for selected or endpoints
            if (isSelected || (records.size() <= 4)) {
                String valText = String.format(Locale.US, "%.1f kg", records.get(i).getWeightKg());
                textPaint.setTextSize(11f * density);
                canvas.drawText(valText, pointsX[i], pointsY[i] - 10f * density, textPaint);
            }
        }
    }
}
