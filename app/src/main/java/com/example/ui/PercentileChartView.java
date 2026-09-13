package com.example.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import com.example.model.MeasurementRecord;
import com.example.util.DoseCalculator;
import com.example.util.PercentileCalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PercentileChartView extends View {

    private boolean isMale = true;
    private boolean isHeightMode = true; // true = Height (cm), false = Weight (kg)
    private float currentAgeYears = 7.0f;
    private String birthDateKey = "2019-05-15";
    private List<MeasurementRecord> records = new ArrayList<>();

    // Internal points for child
    private final List<ChildPoint> childPoints = new ArrayList<>();

    // Drawing paints
    private Paint gridPaint;
    private Paint textPaint;
    private Paint curvePaint;
    private Paint p50Paint;
    private Paint bandPaint;
    private Paint childLinePaint;
    private Paint childPointPaint;
    private Paint childPointInnerPaint;
    private Paint tooltipBgPaint;
    private Paint tooltipTextPaint;
    private Paint highlightRingPaint;

    // Dimensions
    private float paddingLeft = 100f;
    private float paddingRight = 80f;
    private float paddingTop = 40f;
    private float paddingBottom = 60f;

    // Min & Max ranges for axes
    private float minAge = 2f;
    private float maxAge = 18f;
    private float minValue = 75f; // cm
    private float maxValue = 190f; // cm

    // Selected point for tooltip
    private int selectedPointIndex = -1;
    private boolean isTouching = false;

    public static class ChildPoint {
        public float age;
        public float value;
        public String dateKey;
        public int percentile;
        public PointF screenPos = new PointF();

        public ChildPoint(float age, float value, String dateKey, int percentile) {
            this.age = age;
            this.value = value;
            this.dateKey = dateKey;
            this.percentile = percentile;
        }
    }

    public PercentileChartView(Context context) {
        super(context);
        init();
    }

    public PercentileChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PercentileChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#E2E8F0"));
        gridPaint.setStrokeWidth(2f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#64748B"));
        textPaint.setTextSize(spToPx(10));

        curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curvePaint.setColor(Color.parseColor("#94A3B8"));
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeWidth(2.5f);

        p50Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        p50Paint.setColor(Color.parseColor("#0D9488")); // Teal
        p50Paint.setStyle(Paint.Style.STROKE);
        p50Paint.setStrokeWidth(4.5f);

        bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bandPaint.setStyle(Paint.Style.FILL);
        bandPaint.setColor(Color.parseColor("#120D9488")); // 7% translucent teal

        childLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        childLinePaint.setColor(Color.parseColor("#2563EB")); // Vibrant Blue
        childLinePaint.setStyle(Paint.Style.STROKE);
        childLinePaint.setStrokeWidth(5f);

        childPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        childPointPaint.setColor(Color.parseColor("#2563EB"));
        childPointPaint.setStyle(Paint.Style.FILL);

        childPointInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        childPointInnerPaint.setColor(Color.WHITE);
        childPointInnerPaint.setStyle(Paint.Style.FILL);

        highlightRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightRingPaint.setColor(Color.parseColor("#60A5FA"));
        highlightRingPaint.setStyle(Paint.Style.STROKE);
        highlightRingPaint.setStrokeWidth(4f);

        tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipBgPaint.setColor(Color.parseColor("#0F172A"));
        tooltipBgPaint.setStyle(Paint.Style.FILL);

        tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipTextPaint.setColor(Color.WHITE);
        tooltipTextPaint.setTextSize(spToPx(11));
    }

    public void setData(boolean isMale, boolean isHeightMode, float currentAgeYears, String birthDateKey, List<MeasurementRecord> records) {
        this.isMale = isMale;
        this.isHeightMode = isHeightMode;
        this.currentAgeYears = currentAgeYears;
        this.birthDateKey = birthDateKey;
        this.records = records != null ? new ArrayList<>(records) : new ArrayList<>();

        updateAxesRange();
        computeChildPoints();
        invalidate();
    }

    public void setMeasurementType(boolean isHeight) {
        if (this.isHeightMode != isHeight) {
            this.isHeightMode = isHeight;
            updateAxesRange();
            computeChildPoints();
            invalidate();
        }
    }

    private void updateAxesRange() {
        // If height: 70cm to 190cm. If weight: 8kg to 95kg.
        if (isHeightMode) {
            minValue = 75f;
            maxValue = 190f;
        } else {
            minValue = 8f;
            maxValue = 95f;
        }
        minAge = 2f;
        maxAge = 18f;
    }

    private void computeChildPoints() {
        childPoints.clear();
        if (records.isEmpty()) return;

        // Sort by date ascending
        List<MeasurementRecord> sorted = new ArrayList<>(records);
        Collections.sort(sorted, new Comparator<MeasurementRecord>() {
            @Override
            public int compare(MeasurementRecord o1, MeasurementRecord o2) {
                return o1.getDateKey().compareTo(o2.getDateKey());
            }
        });

        for (MeasurementRecord rec : sorted) {
            float val = isHeightMode ? rec.getHeightCm() : rec.getWeightKg();
            if (val <= 0) continue;

            float age = DoseCalculator.calculateDecimalAge(birthDateKey, rec.getDateKey());
            int p = PercentileCalculator.calculatePercentile(val, age, isMale, isHeightMode);
            childPoints.add(new ChildPoint(age, val, rec.getDateKey(), p));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        paddingLeft = spToPx(38);
        paddingRight = spToPx(36);
        paddingTop = spToPx(24);
        paddingBottom = spToPx(32);
    }

    private float getXForAge(float age, float width) {
        float chartWidth = width - paddingLeft - paddingRight;
        return paddingLeft + ((age - minAge) / (maxAge - minAge)) * chartWidth;
    }

    private float getYForValue(float val, float height) {
        float chartHeight = height - paddingTop - paddingBottom;
        return (height - paddingBottom) - ((val - minValue) / (maxValue - minValue)) * chartHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. Draw Grid Lines & X/Y Labels
        drawGridAndLabels(canvas, w, h);

        // 2. Draw Shaded Percentile Band (P10 to P90)
        drawPercentileBand(canvas, w, h);

        // 3. Draw Percentile Curves (P3, P10, P50, P90, P97)
        drawPercentileCurves(canvas, w, h);

        // 4. Draw Child's measurements
        drawChildData(canvas, w, h);

        // 5. Draw Tooltip if touching or selected
        if (selectedPointIndex >= 0 && selectedPointIndex < childPoints.size()) {
            drawTooltip(canvas, childPoints.get(selectedPointIndex), w, h);
        }
    }

    private void drawGridAndLabels(Canvas canvas, float w, float h) {
        // Horizontal grid lines & Y labels
        int steps = 5;
        float valStep = (maxValue - minValue) / steps;
        for (int i = 0; i <= steps; i++) {
            float val = minValue + (i * valStep);
            float y = getYForValue(val, h);

            canvas.drawLine(paddingLeft, y, w - paddingRight, y, gridPaint);

            String label = String.format(Locale.US, "%.0f %s", val, isHeightMode ? "cm" : "kg");
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(label, paddingLeft - 10f, y + 8f, textPaint);
        }

        // Vertical grid lines & Age labels (Ages 2, 4, 6, 8, 10, 12, 14, 16, 18)
        for (int age = 2; age <= 18; age += 2) {
            float x = getXForAge(age, w);
            canvas.drawLine(x, paddingTop, x, h - paddingBottom, gridPaint);

            String ageLabel = age + "y";
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(ageLabel, x, h - paddingBottom + 30f, textPaint);
        }
    }

    private void drawPercentileBand(Canvas canvas, float w, float h) {
        List<PointF> p10Points = PercentileCalculator.getCurvePoints(isMale, isHeightMode, PercentileCalculator.P10);
        List<PointF> p90Points = PercentileCalculator.getCurvePoints(isMale, isHeightMode, PercentileCalculator.P90);

        if (p10Points.isEmpty() || p90Points.isEmpty()) return;

        Path bandPath = new Path();
        // Forward along P90
        for (int i = 0; i < p90Points.size(); i++) {
            PointF pt = p90Points.get(i);
            float x = getXForAge(pt.x, w);
            float y = getYForValue(pt.y, h);
            if (i == 0) {
                bandPath.moveTo(x, y);
            } else {
                bandPath.lineTo(x, y);
            }
        }
        // Backward along P10
        for (int i = p10Points.size() - 1; i >= 0; i--) {
            PointF pt = p10Points.get(i);
            float x = getXForAge(pt.x, w);
            float y = getYForValue(pt.y, h);
            bandPath.lineTo(x, y);
        }
        bandPath.close();
        canvas.drawPath(bandPath, bandPaint);
    }

    private void drawPercentileCurves(Canvas canvas, float w, float h) {
        int[] curves = {
                PercentileCalculator.P3,
                PercentileCalculator.P10,
                PercentileCalculator.P50,
                PercentileCalculator.P90,
                PercentileCalculator.P97
        };

        for (int pIdx : curves) {
            List<PointF> pts = PercentileCalculator.getCurvePoints(isMale, isHeightMode, pIdx);
            if (pts.isEmpty()) continue;

            Path path = new Path();
            for (int i = 0; i < pts.size(); i++) {
                PointF pt = pts.get(i);
                float x = getXForAge(pt.x, w);
                float y = getYForValue(pt.y, h);
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }

            Paint p = (pIdx == PercentileCalculator.P50) ? p50Paint : curvePaint;
            canvas.drawPath(path, p);

            // Right label (e.g. "P50", "P97", "P3")
            PointF lastPt = pts.get(pts.size() - 1);
            float endX = getXForAge(lastPt.x, w);
            float endY = getYForValue(lastPt.y, h);
            textPaint.setTextAlign(Paint.Align.LEFT);

            String pLabel = "P" + PercentileCalculator.PERCENTILE_LEVELS[pIdx];
            if (pIdx == PercentileCalculator.P50) {
                textPaint.setColor(Color.parseColor("#0D9488"));
                textPaint.setFakeBoldText(true);
            } else {
                textPaint.setColor(Color.parseColor("#94A3B8"));
                textPaint.setFakeBoldText(false);
            }
            canvas.drawText(pLabel, endX + 8f, endY + 6f, textPaint);
        }
        textPaint.setFakeBoldText(false);
    }

    private void drawChildData(Canvas canvas, float w, float h) {
        if (childPoints.isEmpty()) {
            // Draw empty state message
            textPaint.setColor(Color.parseColor("#94A3B8"));
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Henüz ölçüm kaydı eklenmedi", w / 2f, h / 2f, textPaint);
            return;
        }

        // Calculate screen positions
        for (ChildPoint cp : childPoints) {
            cp.screenPos.x = getXForAge(cp.age, w);
            cp.screenPos.y = getYForValue(cp.value, h);
        }

        // Draw connecting path
        Path linePath = new Path();
        for (int i = 0; i < childPoints.size(); i++) {
            ChildPoint cp = childPoints.get(i);
            if (i == 0) {
                linePath.moveTo(cp.screenPos.x, cp.screenPos.y);
            } else {
                linePath.lineTo(cp.screenPos.x, cp.screenPos.y);
            }
        }
        canvas.drawPath(linePath, childLinePaint);

        // Draw points
        for (int i = 0; i < childPoints.size(); i++) {
            ChildPoint cp = childPoints.get(i);
            boolean isLast = (i == childPoints.size() - 1);

            if (isLast) {
                // Highlight ring
                canvas.drawCircle(cp.screenPos.x, cp.screenPos.y, 16f, highlightRingPaint);
            }

            // Outer dot
            canvas.drawCircle(cp.screenPos.x, cp.screenPos.y, 10f, childPointPaint);
            // Inner white dot
            canvas.drawCircle(cp.screenPos.x, cp.screenPos.y, 5f, childPointInnerPaint);
        }
    }

    private void drawTooltip(Canvas canvas, ChildPoint cp, float w, float h) {
        String line1 = String.format(Locale.getDefault(), "%.1f %s (%s)",
                cp.value,
                isHeightMode ? "cm" : "kg",
                cp.dateKey);
        String line2 = String.format(Locale.getDefault(), "Yaş: %.1fy • %% %d Persentil",
                cp.age,
                cp.percentile);

        float textWidth = Math.max(tooltipTextPaint.measureText(line1), tooltipTextPaint.measureText(line2));
        float boxWidth = textWidth + 36f;
        float boxHeight = 70f;

        float boxX = cp.screenPos.x - (boxWidth / 2f);
        float boxY = cp.screenPos.y - boxHeight - 20f;

        // Keep inside view bounds
        if (boxX < 10f) boxX = 10f;
        if (boxX + boxWidth > w - 10f) boxX = w - 10f - boxWidth;
        if (boxY < 10f) boxY = cp.screenPos.y + 24f;

        RectF rect = new RectF(boxX, boxY, boxX + boxWidth, boxY + boxHeight);
        canvas.drawRoundRect(rect, 16f, 16f, tooltipBgPaint);

        tooltipTextPaint.setFakeBoldText(true);
        canvas.drawText(line1, boxX + 18f, boxY + 28f, tooltipTextPaint);
        tooltipTextPaint.setFakeBoldText(false);
        canvas.drawText(line2, boxX + 18f, boxY + 54f, tooltipTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (childPoints.isEmpty()) return super.onTouchEvent(event);

        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                isTouching = true;
                // Find closest point
                int closestIdx = -1;
                float minDistance = Float.MAX_VALUE;
                for (int i = 0; i < childPoints.size(); i++) {
                    ChildPoint cp = childPoints.get(i);
                    float dx = touchX - cp.screenPos.x;
                    float dy = touchY - cp.screenPos.y;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist < minDistance) {
                        minDistance = dist;
                        closestIdx = i;
                    }
                }
                if (minDistance < spToPx(48) || event.getAction() == MotionEvent.ACTION_MOVE) {
                    selectedPointIndex = closestIdx;
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Keep selected for inspection or toggle
                break;
        }
        return super.onTouchEvent(event);
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
