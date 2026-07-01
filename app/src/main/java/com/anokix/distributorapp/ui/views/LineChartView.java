package com.anokix.distributorapp.ui.views;

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
 * Lightweight smooth area/line chart used by the Dashboard Revenue Overview card.
 * No external chart library; renders a gradient-filled curve with point markers.
 */
public class LineChartView extends View {

    private float[] values = {32, 48, 40, 64, 56, 80, 72};
    private int lineColor = Color.parseColor("#7C3AED");
    private int fillTop = Color.parseColor("#557C3AED");
    private int fillBottom = Color.parseColor("#007C3AED");

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    public LineChartView(Context context) {
        super(context);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2.5f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(lineColor);

        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(lineColor);
        dotCorePaint.setStyle(Paint.Style.FILL);
        dotCorePaint.setColor(Color.WHITE);
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
        float padV = dp(10);
        float padH = dp(6);

        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (float v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        float range = Math.max(1f, max - min);

        int n = values.length;
        float stepX = (w - padH * 2) / (n - 1);

        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = padH + stepX * i;
            float norm = (values[i] - min) / range;
            ys[i] = h - padV - norm * (h - padV * 2);
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

        fillPaint.setShader(new LinearGradient(0, padV, 0, h, fillTop, fillBottom, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        for (int i = 0; i < n; i++) {
            canvas.drawCircle(xs[i], ys[i], dp(4), dotPaint);
            canvas.drawCircle(xs[i], ys[i], dp(2f), dotCorePaint);
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
