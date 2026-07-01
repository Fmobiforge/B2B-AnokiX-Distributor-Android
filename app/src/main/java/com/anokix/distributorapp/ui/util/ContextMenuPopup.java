package com.anokix.distributorapp.ui.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;

import java.util.Arrays;
import java.util.List;

/**
 * Compact floating action menu — white card, 13sp labels, destructive item in red.
 * Replaces the system {@link androidx.appcompat.widget.PopupMenu} for row overflow actions.
 */
public final class ContextMenuPopup {

    public interface OnItemClick {
        void onClick();
    }

    public static final class MenuItem {
        private final CharSequence title;
        private final boolean destructive;
        private final OnItemClick action;

        private MenuItem(CharSequence title, boolean destructive, OnItemClick action) {
            this.title = title;
            this.destructive = destructive;
            this.action = action;
        }

        public static MenuItem of(CharSequence title, OnItemClick action) {
            return new MenuItem(title, false, action);
        }

        public static MenuItem destructive(CharSequence title, OnItemClick action) {
            return new MenuItem(title, true, action);
        }
    }

    private ContextMenuPopup() {}

    public static void show(Activity activity, View anchor, MenuItem... items) {
        show(activity, anchor, Arrays.asList(items));
    }

    public static void show(Activity activity, View anchor, List<MenuItem> items) {
        if (activity == null || anchor == null || items == null || items.isEmpty()) {
            return;
        }

        Context context = activity;
        View content = LayoutInflater.from(context).inflate(R.layout.popup_context_menu, null, false);
        LinearLayout container = content.findViewById(R.id.menuItemsContainer);
        LayoutInflater inflater = LayoutInflater.from(context);

        PopupWindow popup = new PopupWindow(content,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setElevation(context.getResources().getDimension(R.dimen.context_menu_elevation));

        for (MenuItem item : items) {
            TextView row = (TextView) inflater.inflate(R.layout.item_context_menu, container, false);
            row.setText(item.title);
            if (item.destructive) {
                row.setTextColor(ContextCompat.getColor(context, R.color.danger));
            }
            row.setOnClickListener(v -> {
                popup.dismiss();
                if (item.action != null) {
                    item.action.onClick();
                }
            });
            container.addView(row);
        }

        content.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupWidth = content.getMeasuredWidth();
        int offsetY = context.getResources().getDimensionPixelSize(R.dimen.context_menu_offset_y);
        int xOff = anchor.getWidth() - popupWidth;

        popup.showAsDropDown(anchor, xOff, offsetY);
    }
}
