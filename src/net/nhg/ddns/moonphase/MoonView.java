package akk.astro.droid.moonphase;

import net.nhg.ddns.moonphase.R;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class MoonView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Image sequence limits
    private static final int FIRST_IMAGE = 1133;
    private static final int LAST_IMAGE  = 1840;
    private static final int NEW_MOON_IMAGE = 1141;

    // Lunar cycle
    private static final double SYNODIC_MONTH_DAYS = 29.530588853;
    private static final double HOURS_PER_CYCLE = SYNODIC_MONTH_DAYS * 24.0;

    // Astronomical constants
    private static final double SYNODIC_MONTH = 29.530588853;
    private static final double KNOWN_NEW_MOON_JD = 2451550.26;
    private static final double UNIX_EPOCH_JD = 2440587.5;
    private static final double MILLIS_PER_DAY = 86400000.0;

    private Bitmap currentBitmap;

    // small bitmap cache
    private final Map<Integer, Bitmap> bitmapCache = new HashMap<>();

    public MoonView(Context context) {
        super(context);
        init();
    }

    public MoonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MoonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint.setTextAlign(Paint.Align.CENTER);
    }

    private Bitmap loadMoonImage(int imageNumber) {

        Bitmap cached = bitmapCache.get(imageNumber);
        if (cached != null) return cached;

        String name = "moon_" + imageNumber;

        int resId = getResources().getIdentifier(
                name,
                "drawable",
                getContext().getPackageName());

        if (resId != 0) {

            Bitmap bmp = BitmapFactory.decodeResource(getResources(), resId);

            bitmapCache.put(imageNumber, bmp);

            return bmp;
        }

        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        canvas.drawColor(Color.BLACK);

        MoonResult result = calculateMoonPhase();

        currentBitmap = loadMoonImage(result.imageNumber);

        if (currentBitmap != null) {
            drawMoon(canvas, currentBitmap, viewWidth, viewHeight);
        }

        drawPhaseText(canvas, viewWidth, viewHeight, result);
    }

    private void drawMoon(Canvas canvas, Bitmap bitmap, int viewWidth, int viewHeight) {

        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();

        float scale = Math.min(
                (float) viewWidth / bmpWidth,
                (float) viewHeight / bmpHeight);

        int drawWidth = (int) (bmpWidth * scale);
        int drawHeight = (int) (bmpHeight * scale);

        Rect src = new Rect(0, 0, bmpWidth, bmpHeight);

        Rect dst = new Rect(
                (viewWidth - drawWidth) / 2,
                (viewHeight - drawHeight) / 2,
                (viewWidth + drawWidth) / 2,
                (viewHeight + drawHeight) / 2
        );

        canvas.drawBitmap(bitmap, src, dst, paint);
    }

    private void drawPhaseText(Canvas canvas, int viewWidth, int viewHeight, MoonResult result) {

        paint.setColor(Color.DKGRAY);

        paint.setTextSize(Math.max(viewWidth, viewHeight) * 0.05f);

        String text = getPhaseName(result.imageNumber);

        canvas.drawText(text, viewWidth / 2f, viewHeight - 20f, paint);
    }

    private MoonResult calculateMoonPhase() {

        long nowMillis = System.currentTimeMillis();

        double julianDate = nowMillis / MILLIS_PER_DAY + UNIX_EPOCH_JD;

        double daysSinceNew = julianDate - KNOWN_NEW_MOON_JD;

        double lunarAge = daysSinceNew % SYNODIC_MONTH;

        if (lunarAge < 0) {
            lunarAge += SYNODIC_MONTH;
        }

        double cycleFraction = lunarAge / SYNODIC_MONTH_DAYS;

        double hoursIntoCycle = cycleFraction * HOURS_PER_CYCLE;

        int imageNumber = NEW_MOON_IMAGE + (int) Math.round(hoursIntoCycle);

        int cycleFrames = (int) Math.round(HOURS_PER_CYCLE);

        while (imageNumber > LAST_IMAGE)
            imageNumber -= cycleFrames;

        while (imageNumber < FIRST_IMAGE)
            imageNumber += cycleFrames;

        double phaseAngle = cycleFraction * 2.0 * Math.PI;

        double illumination = (1 - Math.cos(phaseAngle)) / 2.0 * 100.0;

        boolean waxing = lunarAge <= SYNODIC_MONTH / 2.0;

        return new MoonResult(imageNumber, illumination, waxing);
    }

    private String getPhaseName(int imageNumber) {

        if (Math.abs(imageNumber - 1133) <= 16)
            return getContext().getString(R.string.phase_new_moon);

        if (Math.abs(imageNumber - 1310) <= 16)
            return getContext().getString(R.string.phase_first_quarter);

        if (Math.abs(imageNumber - 1487) <= 16)
            return getContext().getString(R.string.phase_full_moon);

        if (Math.abs(imageNumber - 1665) <= 16)
            return getContext().getString(R.string.phase_third_quarter);

        if (imageNumber < 1318)
            return getContext().getString(R.string.phase_waxing_crescent);

        if (imageNumber < 1495)
            return getContext().getString(R.string.phase_waxing_gibbous);

        if (imageNumber < 1673)
            return getContext().getString(R.string.phase_waning_gibbous);

        return getContext().getString(R.string.phase_waning_crescent);
    }

    private static class MoonResult {

        final int imageNumber;
        final double illumination;
        final boolean waxing;

        MoonResult(int imageNumber, double illumination, boolean waxing) {
            this.imageNumber = imageNumber;
            this.illumination = illumination;
            this.waxing = waxing;
        }
    }
}
