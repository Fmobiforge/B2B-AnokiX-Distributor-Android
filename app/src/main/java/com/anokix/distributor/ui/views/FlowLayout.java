package com.anokix.distributor.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * A minimal flow layout: lays children left-to-right and wraps to the next line
 * when the current row runs out of horizontal space. Used for chip groups
 * (categories, distributors, delivery days) in the Create Trader wizard.
 */
public class FlowLayout extends ViewGroup {

    private int horizontalSpacing = dp(8);
    private int verticalSpacing = dp(8);

    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSpacing(int horizontalDp, int verticalDp) {
        this.horizontalSpacing = dp(horizontalDp);
        this.verticalSpacing = dp(verticalDp);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int available = width - getPaddingLeft() - getPaddingRight();

        int childState = 0;
        int rowWidth = 0;
        int rowHeight = 0;
        int totalHeight = 0;

        int childWidthSpec = MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            measureChild(child, childWidthSpec, childHeightSpec);
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();

            if (rowWidth > 0 && rowWidth + horizontalSpacing + cw > available) {
                totalHeight += rowHeight + verticalSpacing;
                rowWidth = cw;
                rowHeight = ch;
            } else {
                rowWidth += (rowWidth > 0 ? horizontalSpacing : 0) + cw;
                rowHeight = Math.max(rowHeight, ch);
            }
        }
        totalHeight += rowHeight;
        totalHeight += getPaddingTop() + getPaddingBottom();

        int measuredHeight = resolveSizeAndState(totalHeight, heightMeasureSpec, childState);
        setMeasuredDimension(width, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int available = getWidth() - getPaddingLeft() - getPaddingRight();
        int x = getPaddingLeft();
        int y = getPaddingTop();
        int rowHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();

            if (x > getPaddingLeft() && x - getPaddingLeft() + cw > available) {
                x = getPaddingLeft();
                y += rowHeight + verticalSpacing;
                rowHeight = 0;
            }
            child.layout(x, y, x + cw, y + ch);
            x += cw + horizontalSpacing;
            rowHeight = Math.max(rowHeight, ch);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
