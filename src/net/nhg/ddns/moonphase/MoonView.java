//
// MoonPhase.java:
// Calculate the phase of the moon.
//    Copyright 2014 by Audrius Meskauskas
// You may use or distribute this code under the terms of the GPLv3
//
package akk.astro.droid.moonphase;

import net.nhg.ddns.moonphase.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class MoonView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Synodic month length
    private static final double SYNODIC_MONTH = 29.530588853;

    // Known new moon reference (2000-01-06 18:14 UTC)
    private static final double KNOWN_NEW_MOON_JD = 2451550.1;

    public MoonView(Context context) {
        super(context);
    }

    public MoonView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MoonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        canvas.drawColor(Color.BLACK);

        MoonResult result = calculateMoonPhase();

        int imageIndex = result.phaseIndex;
        double illumination = result.illumination;
        boolean waxing = result.waxing;

        // Load correct moon image
        int resId = getResources().getIdentifier(
                "moon_" + imageIndex,
                "drawable",
                getContext().getPackageName());

        Bitmap moonBitmap = null;
        if (resId != 0) {
            moonBitmap = BitmapFactory.decodeResource(getResources(), resId);
        }

        // Draw moon bitmap scaled
        if (moonBitmap != null) {
            int bmpWidth = moonBitmap.getWidth();
            int bmpHeight = moonBitmap.getHeight();
            float scale = Math.min((float) viewWidth / bmpWidth,
                                   (float) viewHeight / bmpHeight);

            int drawWidth = (int) (bmpWidth * scale);
            int drawHeight = (int) (bmpHeight * scale);

            int left = (viewWidth - drawWidth) / 2;
            int top = (viewHeight - drawHeight) / 2;

            Bitmap scaled = Bitmap.createScaledBitmap(
                    moonBitmap, drawWidth, drawHeight, true);

            canvas.drawBitmap(scaled, left, top, paint);
        }

        // Draw phase text
        paint.setColor(Color.DKGRAY);
        paint.setTextSize(Math.max(viewWidth, viewHeight) * 0.05f);
        paint.setTextAlign(Paint.Align.CENTER);

	String text = getPhaseName(imageIndex, illumination, waxing);

        canvas.drawText(text, viewWidth / 2f, viewHeight - 20f, paint);
    }

    // =============================
    // NEW MOON CALCULATION SECTION
    // =============================

private MoonResult calculateMoonPhase() {

    long nowMillis = System.currentTimeMillis();

    // Convert Unix time to Julian Date
    double julianDate = nowMillis / 86400000.0 + 2440587.5;

    // Synodic month length
    final double SYNODIC_MONTH = 29.530588853;

    // Known new moon reference (2000-01-06 18:14 UTC)
    final double KNOWN_NEW_MOON_JD = 2451550.1;

    double daysSinceNew = julianDate - KNOWN_NEW_MOON_JD;

    double lunarAge = daysSinceNew % SYNODIC_MONTH;
    if (lunarAge < 0) {
        lunarAge += SYNODIC_MONTH;
    }

    // Map to 0–28
    int phaseIndex = (int)Math.floor((lunarAge / SYNODIC_MONTH) * 29.0);

    if (phaseIndex < 0) phaseIndex = 0;
    if (phaseIndex > 28) phaseIndex = 28;

    // Illumination %
    double phaseAngle = (lunarAge / SYNODIC_MONTH) * 2.0 * Math.PI;
    double illumination = (1 - Math.cos(phaseAngle)) / 2.0 * 100.0;

    boolean waxing = lunarAge <= SYNODIC_MONTH / 2.0;

    return new MoonResult(phaseIndex, illumination, waxing);
}


private String getPhaseName(int index, double illumination, boolean waxing) {

    if (index == 0) return getContext().getString(R.string.phase_new_moon);
    if (index == 7) return getContext().getString(R.string.phase_first_quarter);
    if (index == 14) return getContext().getString(R.string.phase_full_moon);
    if (index == 21) return getContext().getString(R.string.phase_third_quarter);

    if (index < 7) return getContext().getString(R.string.phase_waxing_crescent);
    if (index < 14) return getContext().getString(R.string.phase_waxing_gibbous);
    if (index < 21) return getContext().getString(R.string.phase_waning_gibbous);

    return getContext().getString(R.string.phase_waning_crescent);
}

    private static class MoonResult {
        final int phaseIndex;
        final double illumination;
        final boolean waxing;

        MoonResult(int phaseIndex, double illumination, boolean waxing) {
            this.phaseIndex = phaseIndex;
            this.illumination = illumination;
            this.waxing = waxing;
        }
    }
}
