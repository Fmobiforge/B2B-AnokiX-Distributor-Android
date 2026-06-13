package com.anokix.distributor.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Simple donut/ring chart used by the Inventory Summary card.
 * Segments are supplied as parallel value + color arrays.
 */
public class DonutChartView extends View {

    private float[] values = {186, 42, 8};
    private int[] colors = {
            Color.parseColor("#16A34A"),
            Color.parseColor("#EA580C"),
            Color.parseColor("#DC2626")
    };

    private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();
    private float strokeWidthDp = 16f;

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DonutChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        segmentPaint.setStyle(Paint.Style.STROKE);
        segmentPaint.setStrokeWidth(dp(strokeWidthDp));
        segmentPaint.setStrokeCap(Paint.Cap.BUTT);
    }

    public void setData(float[] values, int[] colors) {
        if (values != null && colors != null && values.length == colors.length) {
            this.values = values;
            this.colors = colors;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float stroke = dp(strokeWidthDp);
        float pad = stroke / 2 + dp(2);
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2 + pad;
        float top = (getHeight() - size) / 2 + pad;
        arcRect.set(left, top, left + size - pad * 2, top + size - pad * 2);

        float total = 0;
        for (float v : values) total += v;
        if (total <= 0) return;

        float startAngle = -90f;
        float gap = 3f;
        for (int i = 0; i < values.length; i++) {
            float sweep = (values[i] / total) * 360f;
            segmentPaint.setColor(colors[i]);
            float drawSweep = Math.max(0, sweep - gap);
            canvas.drawArc(arcRect, startAngle + gap / 2, drawSweep, false, segmentPaint);
            startAngle += sweep;
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
