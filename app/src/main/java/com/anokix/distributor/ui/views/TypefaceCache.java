package com.anokix.distributor.ui.views;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.util.HashMap;

public class TypefaceCache {
    private static final HashMap<String, Typeface> cache = new HashMap<>();

    /**
     * Resolves a font by path. This project's fonts live in res/font, so a path like
     * "font/inter_regular.ttf" is mapped to its font resource and loaded with
     * ResourcesCompat. Falls back to assets for genuine asset paths. Previously this
     * only used createFromAsset, which always failed for res/font fonts and caused
     * every custom view to silently render the system default instead of Inter.
     */
    public static Typeface get(Context context, String fontPath) {
        synchronized (cache) {
            if (cache.containsKey(fontPath)) {
                return cache.get(fontPath);
            }
            Typeface typeface = loadFromFontRes(context, fontPath);
            if (typeface == null) {
                try {
                    typeface = Typeface.createFromAsset(context.getAssets(), fontPath);
                } catch (Exception ignored) {
                    typeface = null;
                }
            }
            if (typeface != null) {
                cache.put(fontPath, typeface);
            }
            return typeface;
        }
    }

    private static Typeface loadFromFontRes(Context context, String fontPath) {
        try {
            String name = fontPath;
            int slash = name.lastIndexOf('/');
            if (slash >= 0) {
                name = name.substring(slash + 1);
            }
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                name = name.substring(0, dot);
            }
            int resId = context.getResources().getIdentifier(name, "font", context.getPackageName());
            if (resId != 0) {
                return ResourcesCompat.getFont(context, resId);
            }
        } catch (Exception ignored) {
            // fall through to asset loading
        }
        return null;
    }

    public static void applyFontToNavigationViewMenu(NavigationView navigationView, Context context) {
        Typeface typeface = get(context, "font/inter_regular.ttf");

        for (int i = 0; i < navigationView.getMenu().size(); i++) {
            MenuItem menuItem = navigationView.getMenu().getItem(i);

            SpannableString spannableTitle = new SpannableString(menuItem.getTitle());
            spannableTitle.setSpan(
                    new CustomTypefaceSpan("Inter Regular", typeface),
                    0,
                    spannableTitle.length(),
                    SpannableString.SPAN_INCLUSIVE_INCLUSIVE
            );
            menuItem.setTitle(spannableTitle);

            // also check for sub-menus
            SubMenu subMenu = menuItem.getSubMenu();
            if (subMenu != null) {
                for (int j = 0; j < subMenu.size(); j++) {
                    MenuItem subMenuItem = subMenu.getItem(j);
                    SpannableString subSpannableTitle = new SpannableString(subMenuItem.getTitle());
                    subSpannableTitle.setSpan(
                            new CustomTypefaceSpan("", typeface),
                            0,
                            subSpannableTitle.length(),
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE
                    );
                    subMenuItem.setTitle(subSpannableTitle);
                }
            }
        }
    }

    public static void applyFontToBottomNavigationViewMenu(BottomNavigationView navigationView, Context context) {
        applyFontToBottomNavigationViewMenu(navigationView, context, 12f);
    }

    public static void applyFontToBottomNavigationViewMenu(
            BottomNavigationView navigationView, Context context, float textSizeSp) {
        Typeface typeface = get(context, "font/inter_regular.ttf");
        if (typeface == null) {
            return;
        }

        for (int i = 0; i < navigationView.getMenu().size(); i++) {
            MenuItem menuItem = navigationView.getMenu().getItem(i);
            CharSequence title = menuItem.getTitle();
            if (title == null) {
                continue;
            }

            SpannableString spannableTitle = new SpannableString(title);
            spannableTitle.setSpan(
                    new CustomTypefaceSpan("Inter", typeface),
                    0,
                    spannableTitle.length(),
                    SpannableString.SPAN_INCLUSIVE_INCLUSIVE
            );
            spannableTitle.setSpan(
                    new AbsoluteSizeSpan(Math.round(textSizeSp), true),
                    0,
                    spannableTitle.length(),
                    SpannableString.SPAN_INCLUSIVE_INCLUSIVE
            );
            menuItem.setTitle(spannableTitle);

            SubMenu subMenu = menuItem.getSubMenu();
            if (subMenu != null) {
                for (int j = 0; j < subMenu.size(); j++) {
                    MenuItem subMenuItem = subMenu.getItem(j);
                    CharSequence subTitle = subMenuItem.getTitle();
                    if (subTitle == null) {
                        continue;
                    }
                    SpannableString subSpannableTitle = new SpannableString(subTitle);
                    subSpannableTitle.setSpan(
                            new CustomTypefaceSpan("Inter", typeface),
                            0,
                            subSpannableTitle.length(),
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE
                    );
                    subSpannableTitle.setSpan(
                            new AbsoluteSizeSpan(Math.round(textSizeSp), true),
                            0,
                            subSpannableTitle.length(),
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE
                    );
                    subMenuItem.setTitle(subSpannableTitle);
                }
            }
        }

        navigationView.post(() -> applyRobotoToChildTextViews(navigationView, typeface, textSizeSp));
    }

    private static void applyRobotoToChildTextViews(View view, Typeface typeface, float textSizeSp) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setTypeface(typeface);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyRobotoToChildTextViews(group.getChildAt(i), typeface, textSizeSp);
            }
        }
    }
}
