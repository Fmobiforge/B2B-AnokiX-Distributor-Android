package com.anokix.distributorapp.ui.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class RobotoButton extends androidx.appcompat.widget.AppCompatButton {

    public RobotoButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public RobotoButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RobotoButton(Context context) {
        super(context);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            //Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/lato-bold.ttf");
            //setTypeface(tf);

            // Portal buttons are Inter 600.
            setTypeface(TypefaceCache.get(getContext(), "font/inter_semibold.ttf"));
        }
    }
}