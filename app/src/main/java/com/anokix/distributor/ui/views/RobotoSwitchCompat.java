package com.anokix.distributor.ui.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class RobotoSwitchCompat extends androidx.appcompat.widget.SwitchCompat {

    public RobotoSwitchCompat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public RobotoSwitchCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RobotoSwitchCompat(Context context) {
        super(context);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            //Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/lato.ttf");
            //setTypeface(tf);

            setTypeface(TypefaceCache.get(getContext(), "font/inter_regular.ttf"));
        }
    }
}