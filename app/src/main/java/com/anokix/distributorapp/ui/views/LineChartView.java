package com.anokix.distributorapp.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Lightweight smooth area/line chart used by the Dashboard Revenue Overview card.
 * No external chart library; renders a gradient-filled curve with point markers plus
 * a labelled Y axis (compact currency gridlines from 0) and X axis (date labels).
 */
public class LineChartView extends View {

    private float[] values = {32, 48, 40, 64, 56, 80, 72};
    private String[] labels = null;
    private String currencySymbol = "R";

    // Portal revenue series: chart_violet line with a fading violet fill; muted axis + hairline grid.
    private final int lineColor = Color.parseColor("#8B5CF6");      // @color/chart_violet
    private final int fillTop = Color.parseColor("#558B5CF6");
    private final int fillBottom = Color.parseColor("#008B5CF6");
    private final int axisTextColor = Color.parseColor("#8A96A3");  // @color/text_tertiary
    private final int gridColor = Color.parseColor("#E8ECF1");      // @color/divider

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1));
        gridPaint.setColor(gridColor);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{dp(3), dp(3)}, 0));

        textPaint.setColor(axisTextColor);
        textPaint.setTextSize(sp(9));
    }

    public void setValues(float[] values) {
        if (values != null && values.length > 1) {
            this.values = values;
            invalidate();
        }
    }

    /** X-axis labels (e.g. "13 Jul"). Should align 1:1 with the values. */
    public void setLabels(String[] labels) {
        this.labels = labels;
        invalidate();
    }

    public void setCurrencySymbol(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {
            this.currencySymbol = symbol;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // Reserve gutters for the axis labels.
        float leftPad = hasAxes() ? dp(38) : dp(6);
        float bottomPad = (labels != null && labels.length > 0) ? dp(18) : dp(10);
        float topPad = dp(10);

        float chartLeft = leftPad;
        float chartRight = w - dp(6);
        float chartTop = topPad;
        float chartBottom = h - bottomPad;
        float chartW = chartRight - chartLeft;
        float chartH = chartBottom - chartTop;

        // Y axis is anchored at 0 so heights read against the labelled gridlines.
        float dataMax = 0f;
        for (float v : values) {
            dataMax = Math.max(dataMax, v);
        }
        float step = niceStep(dataMax);
        float niceMax = (float) (Math.ceil(dataMax / step) * step);
        if (niceMax <= 0) {
            niceMax = 1f;
            step = 1f;
        }

        // Horizontal gridlines + Y labels (top-down).
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (float tick = 0; tick <= niceMax + 0.5f; tick += step) {
            float norm = niceMax == 0 ? 0 : tick / niceMax;
            float y = chartBottom - norm * chartH;
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);
            canvas.drawText(compact(tick), chartLeft - dp(6), y + sp(3), textPaint);
        }

        int n = values.length;
        float stepX = n > 1 ? chartW / (n - 1) : 0;

        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = chartLeft + stepX * i;
            float norm = niceMax == 0 ? 0 : values[i] / niceMax;
            ys[i] = chartBottom - norm * chartH;
        }

        linePath.reset();
        linePath.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            float midX = (xs[i - 1] + xs[i]) / 2;
            linePath.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i]);
        }

        fillPath.reset();
        fillPath.addPath(linePath);
        fillPath.lineTo(xs[n - 1], chartBottom);
        fillPath.lineTo(xs[0], chartBottom);
        fillPath.close();

        fillPaint.setShader(new LinearGradient(0, chartTop, 0, chartBottom, fillTop, fillBottom, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        for (int i = 0; i < n; i++) {
            canvas.drawCircle(xs[i], ys[i], dp(4), dotPaint);
            canvas.drawCircle(xs[i], ys[i], dp(2f), dotCorePaint);
        }

        // X labels under each point.
        if (labels != null && labels.length > 0) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            int labelCount = Math.min(labels.length, n);
            // Thin labels out if there is not enough room for them all.
            int skip = Math.max(1, (int) Math.ceil(labelCount / (chartW / dp(34))));
            for (int i = 0; i < labelCount; i++) {
                if (i % skip != 0 && i != labelCount - 1) {
                    continue;
                }
                float x = xs[i];
                x = Math.max(chartLeft + dp(10), Math.min(chartRight - dp(10), x));
                canvas.drawText(labels[i], x, h - dp(4), textPaint);
            }
        }
    }

    private boolean hasAxes() {
        return true;
    }

    /** A gridline step (1/2/5 × 10ⁿ) sized so the axis shows ~5 rounded lines. */
    private float niceStep(float dataMax) {
        if (dataMax <= 0) {
            return 1f;
        }
        return (float) niceNum(dataMax / 5.0, true);
    }

    /** Nearest "nice" number (1/2/5 × 10ⁿ) to {@code value}; rounds up when not rounding. */
    private double niceNum(double value, boolean round) {
        double exp = Math.floor(Math.log10(value));
        double base = Math.pow(10, exp);
        double frac = value / base;
        double niceFrac;
        if (round) {
            if (frac < 1.5) {
                niceFrac = 1;
            } else if (frac < 3) {
                niceFrac = 2;
            } else if (frac < 7) {
                niceFrac = 5;
            } else {
                niceFrac = 10;
            }
        } else {
            if (frac <= 1) {
                niceFrac = 1;
            } else if (frac <= 2) {
                niceFrac = 2;
            } else if (frac <= 5) {
                niceFrac = 5;
            } else {
                niceFrac = 10;
            }
        }
        return niceFrac * base;
    }

    private String compact(float value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000) {
            return trim(currencySymbol, value / 1_000_000f, "M");
        }
        if (abs >= 1_000) {
            return trim(currencySymbol, value / 1_000f, "K");
        }
        return currencySymbol + (int) value;
    }

    private String trim(String symbol, float v, String suffix) {
        if (v == Math.rint(v)) {
            return symbol + (int) v + suffix;
        }
        return String.format(java.util.Locale.US, "%s%.1f%s", symbol, v, suffix);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float sp(float v) {
        return v * getResources().getDisplayMetrics().scaledDensity;
    }
}
