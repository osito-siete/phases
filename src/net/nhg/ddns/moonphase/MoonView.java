package akk.astro.droid.moonphase;

import net.nhg.ddns.moonphase.R;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class MoonView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Astronomical constants
    private static final double SYNODIC_MONTH = 29.530588853;
    private static final double KNOWN_NEW_MOON_JD = 2451550.26;
    private static final double UNIX_EPOCH_JD = 2440587.5;
    private static final double MILLIS_PER_DAY = 86400000.0;

    private Bitmap[] moonBitmaps = new Bitmap[29];
    private Bitmap currentBitmap;

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
        loadMoonImages();
    }

    private void loadMoonImages() {
        for (int i = 0; i < 29; i++) {
            int resId = getResources().getIdentifier(
                    "moon_" + i,
                    "drawable",
                    getContext().getPackageName());

            if (resId != 0) {
                moonBitmaps[i] = BitmapFactory.decodeResource(getResources(), resId);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        canvas.drawColor(Color.BLACK);

        MoonResult result = calculateMoonPhase();

        currentBitmap = moonBitmaps[result.phaseIndex];

        if (currentBitmap != null) {
            drawMoon(canvas, currentBitmap, viewWidth, viewHeight);
        }

        drawPhaseText(canvas, viewWidth, viewHeight, result);
    }

    private void drawMoon(Canvas canvas, Bitmap bitmap, int viewWidth, int viewHeight) {

        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();

        float scale = Math.min((float) viewWidth / bmpWidth,
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

        String text = getPhaseName(result.phaseIndex);

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

        int phaseIndex = (int) Math.floor((lunarAge / SYNODIC_MONTH) * 29.0);

        if (phaseIndex < 0) phaseIndex = 0;
        if (phaseIndex > 28) phaseIndex = 28;

        double phaseAngle = (lunarAge / SYNODIC_MONTH) * 2.0 * Math.PI;
        double illumination = (1 - Math.cos(phaseAngle)) / 2.0 * 100.0;

        boolean waxing = lunarAge <= SYNODIC_MONTH / 2.0;

        return new MoonResult(phaseIndex, illumination, waxing);
    }

    private String getPhaseName(int index) {

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
