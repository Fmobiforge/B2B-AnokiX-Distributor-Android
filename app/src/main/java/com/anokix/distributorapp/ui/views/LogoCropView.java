package com.anokix.distributorapp.ui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Lightweight square logo cropper for the Create Account flow.
 *
 * Shows a bitmap inside the view and lets the caller adjust scale, rotation,
 * and position (drag to pan). {@link #getCroppedBitmap()} renders the centred
 * square viewport — the area the user sees — to a new bitmap, which is what
 * gets uploaded as the company logo.
 */
public class LogoCropView extends View {

    private Bitmap source;
    private float baseScale = 1f;   // scale that covers the square viewport
    private float userScale = 1f;   // extra zoom from the Scale slider
    private float rotationDeg = 0f; // from the Rotation slider
    private float offsetX = 0f;     // pan offset from drag (screen space)
    private float offsetY = 0f;

    private float lastTouchX;
    private float lastTouchY;
    private boolean dragging;

    private final Matrix matrix = new Matrix();
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

    public LogoCropView(Context context) {
        super(context);
        init();
    }

    public LogoCropView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LogoCropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
    }

    public void setBitmap(Bitmap bitmap) {
        this.source = bitmap;
        this.userScale = 1f;
        this.rotationDeg = 0f;
        this.offsetX = 0f;
        this.offsetY = 0f;
        recomputeBaseScale();
        invalidate();
    }

    public void setUserScale(float scale) {
        this.userScale = scale;
        clampOffsets();
        invalidate();
    }

    public void setRotationDegrees(float deg) {
        this.rotationDeg = deg;
        clampOffsets();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recomputeBaseScale();
        clampOffsets();
    }

    /** Side of the centred square crop viewport. */
    private int viewport() {
        return Math.min(getWidth(), getHeight());
    }

    private void recomputeBaseScale() {
        if (source == null || viewport() == 0) {
            return;
        }
        float side = viewport();
        // Fill the square viewport (cover), so there are no empty edges by default.
        baseScale = Math.max(side / (float) source.getWidth(), side / (float) source.getHeight());
        clampOffsets();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (source == null) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                dragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    offsetX += event.getX() - lastTouchX;
                    offsetY += event.getY() - lastTouchY;
                    clampOffsets();
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    /**
     * Keeps the crop square filled with image content — prevents dragging so far
     * that empty margins appear inside the viewport.
     */
    private void clampOffsets() {
        if (source == null || viewport() == 0) {
            offsetX = 0f;
            offsetY = 0f;
            return;
        }
        float scale = baseScale * userScale;
        float halfSide = viewport() / 2f;
        float hw = source.getWidth() * scale / 2f;
        float hh = source.getHeight() * scale / 2f;

        // Axis-aligned bounds of the rotated, scaled image.
        float rad = (float) Math.toRadians(rotationDeg);
        float cos = Math.abs((float) Math.cos(rad));
        float sin = Math.abs((float) Math.sin(rad));
        float boundW = hw * cos + hh * sin;
        float boundH = hw * sin + hh * cos;

        float maxOffsetX = Math.max(0f, boundW - halfSide);
        float maxOffsetY = Math.max(0f, boundH - halfSide);
        offsetX = Math.max(-maxOffsetX, Math.min(maxOffsetX, offsetX));
        offsetY = Math.max(-maxOffsetY, Math.min(maxOffsetY, offsetY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (source == null) {
            return;
        }
        buildMatrix(getWidth() / 2f, getHeight() / 2f, 1f);
        canvas.drawBitmap(source, matrix, paint);
    }

    private void buildMatrix(float centerX, float centerY, float outputRatio) {
        float scale = baseScale * userScale * outputRatio;
        matrix.reset();
        matrix.postTranslate(-source.getWidth() / 2f, -source.getHeight() / 2f);
        matrix.postScale(scale, scale);
        matrix.postRotate(rotationDeg);
        matrix.postTranslate(centerX + offsetX * outputRatio, centerY + offsetY * outputRatio);
    }

    /** Renders the centred square viewport to a new bitmap, or null if no image set. */
    @Nullable
    public Bitmap getCroppedBitmap() {
        if (source == null) {
            return null;
        }
        int side = viewport();
        if (side <= 0) {
            return null;
        }
        // Cap the output so very large source images don't produce huge uploads.
        int out = Math.min(side, 1024);
        Bitmap result = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        float ratio = out / (float) side;
        buildMatrix(out / 2f, out / 2f, ratio);
        canvas.drawBitmap(source, matrix, paint);
        return result;
    }
}
