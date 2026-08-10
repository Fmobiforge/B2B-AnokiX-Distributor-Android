package com.anokix.distributorapp.ui.vas;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;

import java.util.Calendar;
import java.util.Locale;

/**
 * Small building blocks shared by the Airtime &amp; VAS sections: tap-to-select dropdowns,
 * a date field, the "Something went wrong" dialog that surfaces Limes' own wording, and a
 * one-shot text watcher.
 */
public final class VasUi {

    /** Called with the index the operator picked in a dropdown. */
    public interface OnPick {
        void onPick(int index);
    }

    private VasUi() {
    }

    /**
     * Turns a {@code ProductDropdown} block into a tap-to-select field. {@code labels} is
     * what the operator reads; the caller keeps the matching API code by index.
     */
    public static void bindDropdown(@NonNull Activity activity, @NonNull View field,
                                    @NonNull TextView label, @NonNull String title,
                                    @NonNull String[] labels, int selectedIndex,
                                    @NonNull OnPick onPick) {
        setDropdownValue(activity, label, labels, selectedIndex);
        field.setOnClickListener(v -> new AlertDialog.Builder(activity)
                .setTitle(title)
                .setItems(labels, (d, which) -> {
                    setDropdownValue(activity, label, labels, which);
                    onPick.onPick(which);
                })
                .show());
    }

    /** Writes the selected label into a dropdown, switching it from hint grey to body colour. */
    public static void setDropdownValue(@NonNull Activity activity, @NonNull TextView label,
                                        @NonNull String[] labels, int index) {
        if (index < 0 || index >= labels.length) return;
        setDropdownValue(activity, label, labels[index]);
    }

    /** Same, for dropdowns whose options come from the API rather than a fixed list. */
    public static void setDropdownValue(@NonNull Activity activity, @NonNull TextView label,
                                        @NonNull String value) {
        label.setText(value);
        label.setTextColor(ContextCompat.getColor(activity, R.color.text_primary));
    }

    /**
     * Date field that returns the API form ({@code yyyy-MM-dd}) and shows the same string —
     * the Limes {@code expiryDate} is a plain date, so there is nothing to localise.
     */
    public static void bindDateField(@NonNull Activity activity, @NonNull View field,
                                     @NonNull TextView label, @NonNull OnDatePicked onPicked) {
        field.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(activity, (view, year, month, day) -> {
                String api = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                label.setText(api);
                label.setTextColor(ContextCompat.getColor(activity, R.color.text_primary));
                onPicked.onPicked(api);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            // A bundle that expired yesterday cannot be provisioned, so the past is not offered.
            dialog.getDatePicker().setMinDate(c.getTimeInMillis());
            dialog.show();
        });
    }

    /** Called with the picked date in {@code yyyy-MM-dd}. */
    public interface OnDatePicked {
        void onPicked(String isoDate);
    }

    /**
     * The failure dialog. Limes' replies are long, multi-line and worth reading in full
     * (they name the reason, e.g. "not an active subscriber"), so the body goes into a
     * scrollable, selectable text view rather than the default single-line message.
     */
    public static void showError(@NonNull Activity activity, @Nullable String message) {
        showLongMessage(activity, activity.getString(R.string.vas_error_title), message);
    }

    /** Same presentation as {@link #showError}, for informational payloads (request preview). */
    public static void showLongMessage(@NonNull Activity activity, @NonNull String title,
                                       @Nullable String message) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        TextView body = new TextView(activity);
        body.setText(message == null ? "" : message);
        body.setTextIsSelectable(true);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        body.setTextColor(ContextCompat.getColor(activity, R.color.text_primary));
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20,
                activity.getResources().getDisplayMetrics());
        body.setPadding(pad, pad / 2, pad, 0);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(body);

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(scroll)
                .setPositiveButton(R.string.vas_close, null)
                .show();
    }

    /** Validation prompt: flags the offending field and explains what is missing. */
    public static void showValidation(@NonNull Activity activity, int messageRes,
                                      @Nullable android.widget.EditText field) {
        if (field != null) {
            field.setError(activity.getString(messageRes));
            field.requestFocus();
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.vas_validation_title)
                .setMessage(messageRes)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /** Minimal TextWatcher that runs a callback on every change. */
    public static class SimpleWatcher implements TextWatcher {
        private final Runnable onChange;

        public SimpleWatcher(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            onChange.run();
        }
    }
}
