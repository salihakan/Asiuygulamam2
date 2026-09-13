package com.example.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.R;
import com.example.model.Limb;

/**
 * A modern, responsive, animated medical body map view.
 * Displays an anatomical human silhouette with interactive limb targets (Right Arm, Right Leg, Left Arm, Left Leg),
 * continuous pulsing radar ripples around the active injection site, glowing limb highlights,
 * and 4-step rotation indicators.
 */
public class InteractiveBodyMapView extends View {

    public interface OnLimbSelectedListener {
        void onLimbSelected(Limb limb, boolean isTodayScheduledLimb);
    }

    // Normalized coordinate space: 600 x 900
    private static final float V_WIDTH = 600f;
    private static final float V_HEIGHT = 900f;

    // Hotspot points in normalized coordinate space
    // Anatomical Right = Screen Left (patient facing viewer)
    private static final PointF HOTSPOT_RIGHT_ARM = new PointF(160f, 260f);
    private static final PointF HOTSPOT_RIGHT_LEG = new PointF(245f, 520f);
    private static final PointF HOTSPOT_LEFT_ARM = new PointF(440f, 260f);
    private static final PointF HOTSPOT_LEFT_LEG = new PointF(355f, 520f);

    private Limb todayLimb = Limb.RIGHT_ARM;
    private Limb selectedLimb = Limb.RIGHT_ARM;
    private boolean isCompleted = false;
    private OnLimbSelectedListener listener;

    // Paints
    private Paint bodyFillPaint;
    private Paint bodyStrokePaint;
    private Paint jointPaint;
    private Paint limbHighlightFillPaint;
    private Paint limbHighlightStrokePaint;
    private Paint radarRingPaint;
    private Paint targetCenterPaint;
    private Paint targetGlowPaint;
    private Paint badgeBgPaint;
    private Paint badgeTextPaint;
    private Paint labelTextPaint;
    private Paint calloutLinePaint;
    private Paint calloutBgPaint;
    private Paint calloutTextPaint;

    // Dynamic paths
    private Path headPath;
    private Path torsoPath;
    private Path rightArmPath;
    private Path rightForearmPath;
    private Path leftArmPath;
    private Path leftForearmPath;
    private Path rightThighPath;
    private Path rightLowerLegPath;
    private Path leftThighPath;
    private Path leftLowerLegPath;

    // Animation values
    private ValueAnimator pulseAnimator;
    private float pulseFraction = 0f;
    private ValueAnimator selectionAnimator;
    private float selectionFraction = 1f;

    // Scale and transform parameters
    private float scale = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;

    // Touch tracking
    private float touchDownX = 0f;
    private float touchDownY = 0f;
    private long touchDownTime = 0;

    public InteractiveBodyMapView(Context context) {
        super(context);
        init();
    }

    public InteractiveBodyMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public InteractiveBodyMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);

        boolean isDark = isDarkTheme();

        // Body Silhouette Fill
        bodyFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyFillPaint.setStyle(Paint.Style.FILL);
        bodyFillPaint.setColor(isDark ? Color.parseColor("#1E293B") : Color.parseColor("#E2E8F0"));

        // Body Silhouette Outline
        bodyStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyStrokePaint.setStyle(Paint.Style.STROKE);
        bodyStrokePaint.setStrokeWidth(2f);
        bodyStrokePaint.setColor(isDark ? Color.parseColor("#334155") : Color.parseColor("#CBD5E1"));

        // Joint / separator lines
        jointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        jointPaint.setStyle(Paint.Style.STROKE);
        jointPaint.setStrokeWidth(1.5f);
        jointPaint.setColor(isDark ? Color.parseColor("#475569") : Color.parseColor("#94A3B8"));

        // Highlight for selected/active limb
        limbHighlightFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        limbHighlightFillPaint.setStyle(Paint.Style.FILL);

        limbHighlightStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        limbHighlightStrokePaint.setStyle(Paint.Style.STROKE);
        limbHighlightStrokePaint.setStrokeWidth(3.5f);
        limbHighlightStrokePaint.setColor(Color.parseColor("#00ACC1"));

        // Animated Radar Rings
        radarRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        radarRingPaint.setStyle(Paint.Style.STROKE);
        radarRingPaint.setStrokeWidth(2.5f);

        // Target Bullseye
        targetCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        targetCenterPaint.setStyle(Paint.Style.FILL);

        targetGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        targetGlowPaint.setStyle(Paint.Style.FILL);

        // Rotation Step Badges (1, 2, 3, 4)
        badgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeBgPaint.setStyle(Paint.Style.FILL);

        badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeTextPaint.setTextAlign(Paint.Align.CENTER);
        badgeTextPaint.setTextSize(12f);
        badgeTextPaint.setFakeBoldText(true);

        // Limb labels (Sağ Kol, Sol Bacak, etc.)
        labelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelTextPaint.setTextAlign(Paint.Align.CENTER);
        labelTextPaint.setTextSize(11f);
        labelTextPaint.setColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));

        // Callout pointer banner
        calloutLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        calloutLinePaint.setStyle(Paint.Style.STROKE);
        calloutLinePaint.setStrokeWidth(2f);
        calloutLinePaint.setColor(Color.parseColor("#00ACC1"));

        calloutBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        calloutBgPaint.setStyle(Paint.Style.FILL);
        calloutBgPaint.setColor(isDark ? Color.parseColor("#0F172A") : Color.WHITE);
        calloutBgPaint.setShadowLayer(8f, 0, 3f, Color.parseColor("#40000000"));

        calloutTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        calloutTextPaint.setTextAlign(Paint.Align.CENTER);
        calloutTextPaint.setTextSize(12f);
        calloutTextPaint.setFakeBoldText(true);

        buildAnatomyPaths();
        startAnimations();
    }

    private boolean isDarkTheme() {
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void buildAnatomyPaths() {
        // Head
        headPath = new Path();
        headPath.addCircle(300f, 100f, 40f, Path.Direction.CW);
        // Neck
        RectF neckRect = new RectF(286f, 135f, 314f, 168f);
        headPath.addRoundRect(neckRect, 8f, 8f, Path.Direction.CW);

        // Torso
        torsoPath = new Path();
        torsoPath.moveTo(230f, 170f); // Right shoulder junction
        torsoPath.lineTo(370f, 170f); // Left shoulder junction
        torsoPath.quadTo(375f, 250f, 360f, 340f); // Left ribcage to waist
        torsoPath.quadTo(370f, 400f, 365f, 450f); // Left hip
        torsoPath.lineTo(300f, 465f); // Pelvic center bottom
        torsoPath.lineTo(235f, 450f); // Right hip
        torsoPath.quadTo(230f, 400f, 240f, 340f); // Right waist
        torsoPath.quadTo(225f, 250f, 230f, 170f); // Right ribcage to shoulder
        torsoPath.close();

        // Right Upper Arm (Anatomical Right = Viewer Left)
        rightArmPath = new Path();
        rightArmPath.moveTo(230f, 170f); // Inner shoulder
        rightArmPath.quadTo(175f, 165f, 150f, 195f); // Deltoid peak
        rightArmPath.quadTo(135f, 250f, 140f, 320f); // Outer upper arm to elbow
        rightArmPath.lineTo(190f, 320f); // Elbow inner crease
        rightArmPath.quadTo(195f, 250f, 230f, 205f); // Inner bicep to armpit
        rightArmPath.close();

        // Right Forearm & Hand
        rightForearmPath = new Path();
        rightForearmPath.moveTo(140f, 320f);
        rightForearmPath.lineTo(130f, 430f); // Wrist outer
        rightForearmPath.quadTo(125f, 480f, 140f, 485f); // Hand
        rightForearmPath.lineTo(165f, 480f);
        rightForearmPath.lineTo(180f, 430f); // Wrist inner
        rightForearmPath.lineTo(190f, 320f);
        rightForearmPath.close();

        // Left Upper Arm (Anatomical Left = Viewer Right)
        leftArmPath = new Path();
        leftArmPath.moveTo(370f, 170f); // Inner shoulder
        leftArmPath.quadTo(425f, 165f, 450f, 195f); // Deltoid peak
        leftArmPath.quadTo(465f, 250f, 460f, 320f); // Outer upper arm to elbow
        leftArmPath.lineTo(410f, 320f); // Elbow inner crease
        leftArmPath.quadTo(405f, 250f, 370f, 205f); // Inner bicep to armpit
        leftArmPath.close();

        // Left Forearm & Hand
        leftForearmPath = new Path();
        leftForearmPath.moveTo(410f, 320f);
        leftForearmPath.lineTo(420f, 430f); // Wrist inner
        leftForearmPath.lineTo(435f, 480f);
        leftForearmPath.quadTo(475f, 485f, 470f, 430f); // Hand to wrist outer
        leftForearmPath.lineTo(460f, 320f);
        leftForearmPath.close();

        // Right Thigh (Anatomical Right = Viewer Left)
        rightThighPath = new Path();
        rightThighPath.moveTo(235f, 450f); // Hip
        rightThighPath.lineTo(300f, 465f); // Groin
        rightThighPath.quadTo(280f, 540f, 275f, 625f); // Inner thigh to knee
        rightThighPath.lineTo(215f, 625f); // Knee outer
        rightThighPath.quadTo(210f, 535f, 235f, 450f); // Outer thigh
        rightThighPath.close();

        // Right Lower Leg & Foot
        rightLowerLegPath = new Path();
        rightLowerLegPath.moveTo(215f, 625f);
        rightLowerLegPath.quadTo(210f, 715f, 215f, 790f); // Calf to ankle
        rightLowerLegPath.quadTo(195f, 835f, 245f, 835f); // Foot
        rightLowerLegPath.lineTo(260f, 790f); // Inner ankle
        rightLowerLegPath.quadTo(265f, 715f, 275f, 625f); // Inner calf
        rightLowerLegPath.close();

        // Left Thigh (Anatomical Left = Viewer Right)
        leftThighPath = new Path();
        leftThighPath.moveTo(300f, 465f); // Groin
        leftThighPath.lineTo(365f, 450f); // Hip
        leftThighPath.quadTo(390f, 535f, 385f, 625f); // Outer thigh to knee
        leftThighPath.lineTo(325f, 625f); // Knee inner
        leftThighPath.quadTo(320f, 540f, 300f, 465f); // Inner thigh
        leftThighPath.close();

        // Left Lower Leg & Foot
        leftLowerLegPath = new Path();
        leftLowerLegPath.moveTo(325f, 625f);
        leftLowerLegPath.quadTo(335f, 715f, 340f, 790f); // Inner calf to ankle
        leftLowerLegPath.lineTo(355f, 835f); // Foot
        leftLowerLegPath.quadTo(405f, 835f, 385f, 790f); // Outer foot to ankle
        leftLowerLegPath.quadTo(390f, 715f, 385f, 625f); // Outer calf
        leftLowerLegPath.close();
    }

    private void startAnimations() {
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1600);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulseFraction = (float) animation.getAnimatedValue();
            postInvalidateOnAnimation();
        });
        pulseAnimator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (pulseAnimator != null && !pulseAnimator.isRunning()) {
            pulseAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        if (selectionAnimator != null) {
            selectionAnimator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (h == 0) {
            h = (int) (w * 1.35f);
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float scaleX = (float) w / V_WIDTH;
        float scaleY = (float) h / V_HEIGHT;
        scale = Math.min(scaleX, scaleY) * 0.95f;
        offsetX = (w - (V_WIDTH * scale)) / 2f;
        offsetY = (h - (V_HEIGHT * scale)) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);

        boolean isDark = isDarkTheme();

        // 1. Draw subtle background anatomical grid lines/circles
        drawAnatomicalBackdrop(canvas, isDark);

        // 2. Draw neutral human silhouette
        canvas.drawPath(headPath, bodyFillPaint);
        canvas.drawPath(headPath, bodyStrokePaint);

        canvas.drawPath(torsoPath, bodyFillPaint);
        canvas.drawPath(torsoPath, bodyStrokePaint);

        // Draw Inactive Limbs
        drawLimbSegment(canvas, Limb.RIGHT_ARM, rightArmPath, rightForearmPath);
        drawLimbSegment(canvas, Limb.LEFT_ARM, leftArmPath, leftForearmPath);
        drawLimbSegment(canvas, Limb.RIGHT_LEG, rightThighPath, rightLowerLegPath);
        drawLimbSegment(canvas, Limb.LEFT_LEG, leftThighPath, leftLowerLegPath);

        // 3. Draw Selected / Active Limb glowing highlight
        drawActiveLimbHighlight(canvas, selectedLimb);

        // 4. Draw continuous radar ripple pulse around the active injection site
        drawRadarPulse(canvas, selectedLimb);

        // 5. Draw rotation step badges (1, 2, 3, 4) & injection target pins
        drawLimbHotspotBadges(canvas, isDark);

        // 6. Draw floating active limb tag / callout
        drawCalloutBanner(canvas, selectedLimb, isDark);

        canvas.restore();
    }

    private void drawAnatomicalBackdrop(Canvas canvas, boolean isDark) {
        Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(isDark ? Color.parseColor("#152238") : Color.parseColor("#F1F5F9"));

        // Subtle guide circles around body center
        canvas.drawCircle(300f, 400f, 180f, gridPaint);
        canvas.drawCircle(300f, 400f, 260f, gridPaint);
    }

    private void drawLimbSegment(Canvas canvas, Limb limb, Path upperPath, Path lowerPath) {
        if (limb == selectedLimb) {
            // Drawn specifically in drawActiveLimbHighlight
            return;
        }

        // Inactive limb appearance
        canvas.drawPath(upperPath, bodyFillPaint);
        canvas.drawPath(upperPath, bodyStrokePaint);

        if (lowerPath != null) {
            canvas.drawPath(lowerPath, bodyFillPaint);
            canvas.drawPath(lowerPath, bodyStrokePaint);
        }
    }

    private void drawActiveLimbHighlight(Canvas canvas, Limb limb) {
        Path targetPath;
        PointF hotspot;
        switch (limb) {
            case RIGHT_ARM:
                targetPath = rightArmPath;
                hotspot = HOTSPOT_RIGHT_ARM;
                break;
            case RIGHT_LEG:
                targetPath = rightThighPath;
                hotspot = HOTSPOT_RIGHT_LEG;
                break;
            case LEFT_ARM:
                targetPath = leftArmPath;
                hotspot = HOTSPOT_LEFT_ARM;
                break;
            case LEFT_LEG:
            default:
                targetPath = leftThighPath;
                hotspot = HOTSPOT_LEFT_LEG;
                break;
        }

        boolean isToday = (limb == todayLimb);
        int primaryColor = isToday
                ? (isCompleted ? Color.parseColor("#10B981") : Color.parseColor("#00ACC1"))
                : Color.parseColor("#0284C7");
        int glowColor = isToday
                ? (isCompleted ? Color.parseColor("#6EE7B7") : Color.parseColor("#80DEEA"))
                : Color.parseColor("#38BDF8");

        // Subtle radial gradient fill over the limb
        RadialGradient fillGradient = new RadialGradient(
                hotspot.x, hotspot.y, 110f,
                Color.argb(160, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
                Color.argb(45, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
                Shader.TileMode.CLAMP
        );
        limbHighlightFillPaint.setShader(fillGradient);
        canvas.drawPath(targetPath, limbHighlightFillPaint);

        // Highlight stroke with glowing alpha
        limbHighlightStrokePaint.setColor(primaryColor);
        limbHighlightStrokePaint.setStrokeWidth(3.5f);
        canvas.drawPath(targetPath, limbHighlightStrokePaint);
    }

    private void drawRadarPulse(Canvas canvas, Limb limb) {
        PointF center = getHotspotForLimb(limb);
        boolean isToday = (limb == todayLimb);
        int ringColor = isToday
                ? (isCompleted ? Color.parseColor("#10B981") : Color.parseColor("#00ACC1"))
                : Color.parseColor("#0284C7");

        float minRadius = 14f;
        float maxRadius = 65f;

        // Wave 1
        float fraction1 = pulseFraction;
        float radius1 = minRadius + fraction1 * (maxRadius - minRadius);
        int alpha1 = (int) ((1f - fraction1) * 210);
        radarRingPaint.setColor(ringColor);
        radarRingPaint.setAlpha(alpha1);
        canvas.drawCircle(center.x, center.y, radius1, radarRingPaint);

        // Wave 2 (offset by 0.5)
        float fraction2 = (pulseFraction + 0.5f) % 1f;
        float radius2 = minRadius + fraction2 * (maxRadius - minRadius);
        int alpha2 = (int) ((1f - fraction2) * 210);
        radarRingPaint.setAlpha(alpha2);
        canvas.drawCircle(center.x, center.y, radius2, radarRingPaint);

        // Center Target Bullseye
        // Outer soft glow halo
        targetGlowPaint.setColor(ringColor);
        targetGlowPaint.setAlpha(120);
        float pulseScale = 1f + 0.15f * (float) Math.sin(pulseFraction * Math.PI * 2);
        canvas.drawCircle(center.x, center.y, 14f * pulseScale, targetGlowPaint);

        // Inner solid dot
        targetCenterPaint.setColor(ringColor);
        canvas.drawCircle(center.x, center.y, 8f, targetCenterPaint);

        // Center white core
        targetCenterPaint.setColor(Color.WHITE);
        canvas.drawCircle(center.x, center.y, 3.5f, targetCenterPaint);
    }

    private void drawLimbHotspotBadges(Canvas canvas, boolean isDark) {
        Limb[] limbs = Limb.values();
        for (Limb limb : limbs) {
            PointF p = getHotspotForLimb(limb);
            boolean isSel = (limb == selectedLimb);
            boolean isTod = (limb == todayLimb);
            int stepNumber = limb.getIndex() + 1; // 1, 2, 3, 4

            // Position badge beside the hotspot
            float badgeX = (limb == Limb.RIGHT_ARM || limb == Limb.RIGHT_LEG) ? p.x - 44f : p.x + 44f;
            float badgeY = p.y;
            float badgeRadius = 14f;

            if (isSel) {
                int activeColor = isTod
                        ? (isCompleted ? Color.parseColor("#10B981") : Color.parseColor("#00ACC1"))
                        : Color.parseColor("#0284C7");
                badgeBgPaint.setColor(activeColor);
                badgeTextPaint.setColor(Color.WHITE);
            } else {
                badgeBgPaint.setColor(isDark ? Color.parseColor("#334155") : Color.parseColor("#E2E8F0"));
                badgeTextPaint.setColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
            }

            canvas.drawCircle(badgeX, badgeY, badgeRadius, badgeBgPaint);

            // Text vertical centering
            Paint.FontMetrics fm = badgeTextPaint.getFontMetrics();
            float textY = badgeY - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(String.valueOf(stepNumber), badgeX, textY, badgeTextPaint);

            // Sub-label below badge: e.g. "Sağ Kol"
            float labelY = badgeY + badgeRadius + 14f;
            labelTextPaint.setColor(isSel
                    ? (isTod ? (isCompleted ? Color.parseColor("#10B981") : Color.parseColor("#00ACC1")) : Color.parseColor("#0284C7"))
                    : (isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
            labelTextPaint.setFakeBoldText(isSel);
            canvas.drawText(limb.getDefaultName(), badgeX, labelY, labelTextPaint);
        }
    }

    private void drawCalloutBanner(Canvas canvas, Limb limb, boolean isDark) {
        PointF hotspot = getHotspotForLimb(limb);
        boolean isTod = (limb == todayLimb);

        String title;
        if (isTod) {
            title = isCompleted ? "✓ " + limb.getDefaultName() + " (Tamamlandı)" : "💉 Bugün: " + limb.getDefaultName();
        } else {
            title = "🎯 " + limb.getDefaultName() + " (" + (limb.getIndex() + 1) + ". Sıra)";
        }

        float bannerWidth = 190f;
        float bannerHeight = 36f;
        float bannerY = hotspot.y - 65f;
        float bannerX = (limb == Limb.RIGHT_ARM || limb == Limb.RIGHT_LEG)
                ? hotspot.x - 30f
                : hotspot.x + 30f;

        // Clamp to canvas borders
        if (bannerX - (bannerWidth / 2f) < 20f) bannerX = 20f + (bannerWidth / 2f);
        if (bannerX + (bannerWidth / 2f) > V_WIDTH - 20f) bannerX = V_WIDTH - 20f - (bannerWidth / 2f);

        // Pointer line connecting hotspot to banner
        calloutLinePaint.setColor(isTod
                ? (isCompleted ? Color.parseColor("#10B981") : Color.parseColor("#00ACC1"))
                : Color.parseColor("#0284C7"));
        canvas.drawLine(hotspot.x, hotspot.y - 12f, bannerX, bannerY + (bannerHeight / 2f), calloutLinePaint);

        // Rounded banner rect
        RectF rect = new RectF(
                bannerX - (bannerWidth / 2f),
                bannerY - (bannerHeight / 2f),
                bannerX + (bannerWidth / 2f),
                bannerY + (bannerHeight / 2f)
        );

        calloutBgPaint.setColor(isDark ? Color.parseColor("#0F172A") : Color.WHITE);
        canvas.drawRoundRect(rect, 18f, 18f, calloutBgPaint);

        // Banner border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setColor(isTod
                ? (isCompleted ? Color.parseColor("#10B981") : Color.parseColor("#00ACC1"))
                : Color.parseColor("#0284C7"));
        canvas.drawRoundRect(rect, 18f, 18f, borderPaint);

        // Banner text
        calloutTextPaint.setColor(isTod
                ? (isCompleted ? Color.parseColor("#10B981") : Color.parseColor("#00838F"))
                : Color.parseColor("#0284C7"));
        Paint.FontMetrics fm = calloutTextPaint.getFontMetrics();
        float textY = bannerY - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(title, bannerX, textY, calloutTextPaint);
    }

    private PointF getHotspotForLimb(Limb limb) {
        switch (limb) {
            case RIGHT_ARM:
                return HOTSPOT_RIGHT_ARM;
            case RIGHT_LEG:
                return HOTSPOT_RIGHT_LEG;
            case LEFT_ARM:
                return HOTSPOT_LEFT_ARM;
            case LEFT_LEG:
            default:
                return HOTSPOT_LEFT_LEG;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getX();
                touchDownY = event.getY();
                touchDownTime = SystemClock.uptimeMillis();
                return true;

            case MotionEvent.ACTION_UP:
                float dx = event.getX() - touchDownX;
                float dy = event.getY() - touchDownY;
                long elapsed = SystemClock.uptimeMillis() - touchDownTime;

                // Tap detection (< 25px move and < 400ms duration)
                if (Math.hypot(dx, dy) < 25f && elapsed < 400) {
                    handleTap(event.getX(), event.getY());
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleTap(float screenX, float screenY) {
        // Convert screen coordinates to normalized V_WIDTH x V_HEIGHT space
        float normX = (screenX - offsetX) / scale;
        float normY = (screenY - offsetY) / scale;

        Limb closestLimb = null;
        float minDistance = Float.MAX_VALUE;

        Limb[] limbs = Limb.values();
        for (Limb limb : limbs) {
            PointF p = getHotspotForLimb(limb);
            float dist = (float) Math.hypot(normX - p.x, normY - p.y);

            // Also check bounding area of limb
            boolean inBounds = isPointInLimbBounds(limb, normX, normY);
            if (inBounds && dist < minDistance) {
                minDistance = dist;
                closestLimb = limb;
            } else if (dist < 80f && dist < minDistance) { // Tap within 80 units
                minDistance = dist;
                closestLimb = limb;
            }
        }

        if (closestLimb != null) {
            setSelectedLimb(closestLimb, true);
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (listener != null) {
                listener.onLimbSelected(closestLimb, closestLimb == todayLimb);
            }
        }
    }

    private boolean isPointInLimbBounds(Limb limb, float x, float y) {
        switch (limb) {
            case RIGHT_ARM:
                return x >= 80f && x <= 230f && y >= 150f && y <= 380f;
            case RIGHT_LEG:
                return x >= 170f && x <= 295f && y >= 430f && y <= 720f;
            case LEFT_ARM:
                return x >= 370f && x <= 520f && y >= 150f && y <= 380f;
            case LEFT_LEG:
                return x >= 305f && x <= 430f && y >= 430f && y <= 720f;
            default:
                return false;
        }
    }

    public void setTodayLimb(Limb limb) {
        this.todayLimb = limb;
        this.selectedLimb = limb;
        postInvalidateOnAnimation();
    }

    public void setSelectedLimb(Limb limb, boolean animate) {
        if (this.selectedLimb == limb) {
            return;
        }
        this.selectedLimb = limb;
        if (animate) {
            if (selectionAnimator != null) {
                selectionAnimator.cancel();
            }
            selectionAnimator = ValueAnimator.ofFloat(0f, 1f);
            selectionAnimator.setDuration(300);
            selectionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            selectionAnimator.addUpdateListener(animation -> {
                selectionFraction = (float) animation.getAnimatedValue();
                postInvalidateOnAnimation();
            });
            selectionAnimator.start();
        } else {
            postInvalidateOnAnimation();
        }
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
        postInvalidateOnAnimation();
    }

    public Limb getSelectedLimb() {
        return selectedLimb;
    }

    public Limb getTodayLimb() {
        return todayLimb;
    }

    public void setOnLimbSelectedListener(OnLimbSelectedListener listener) {
        this.listener = listener;
    }
}
