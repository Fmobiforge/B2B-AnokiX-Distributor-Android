package com.anokix.distributor.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Compact sparkline used by the dashboard summary KPI cards. Renders a smooth
 * line with a soft gradient fill and no axes/markers — a slimmed {@link LineChartView}.
 * The colour is configurable so the four cards (purple/blue/green/orange) match the
 * web portal.
 */
public class SparklineView extends View {

    private float[] values = {4, 6, 5, 8, 7, 9, 8};
    private int lineColor = Color.parseColor("#7C3AED");

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    public SparklineView(Context context) {
        super(context);
        init();
    }

    public SparklineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SparklineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(lineColor);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    public void setLineColor(int color) {
        this.lineColor = color;
        linePaint.setColor(color);
        invalidate();
    }

    public void setValues(float[] values) {
        if (values != null && values.length > 1) {
            this.values = values;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float pad = dp(3);
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (float v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        float range = Math.max(0.0001f, max - min);

        int n = values.length;
        float stepX = (float) w / (n - 1);
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = stepX * i;
            float norm = (values[i] - min) / range;
            ys[i] = h - pad - norm * (h - pad * 2);
        }

        linePath.reset();
        linePath.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            float midX = (xs[i - 1] + xs[i]) / 2;
            linePath.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i]);
        }

        fillPath.reset();
        fillPath.addPath(linePath);
        fillPath.lineTo(xs[n - 1], h);
        fillPath.lineTo(xs[0], h);
        fillPath.close();

        int fillTop = (lineColor & 0x00FFFFFF) | 0x40000000;
        int fillBottom = (lineColor & 0x00FFFFFF);
        fillPaint.setShader(new LinearGradient(0, 0, 0, h, fillTop, fillBottom, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
