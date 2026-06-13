package com.anokix.distributor.ui.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class RobotoLightTextView extends androidx.appcompat.widget.AppCompatTextView {

    public RobotoLightTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public RobotoLightTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RobotoLightTextView(Context context) {
        super(context);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            //Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/lato-light.ttf");
            //setTypeface(tf);

            setTypeface(TypefaceCache.get(getContext(), "font/inter_light.ttf"));
        }
    }
}