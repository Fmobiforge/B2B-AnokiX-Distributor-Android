package com.anokix.distributorapp.ui.util;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class FilterUiHelper {

    public interface OnOptionSelected {
        void onSelected(int index, @NonNull String value);
    }

    public interface OnDateRangeSelected {
        void onSelected(long startUtcMillis, long endUtcMillis, @NonNull String formattedRange);
    }

    private FilterUiHelper() {
    }

    public static void bindDropdown(
            @NonNull View trigger,
            @NonNull TextView labelView,
            @NonNull String[] options,
            int selectedIndex,
            @NonNull OnOptionSelected callback) {
        if (options.length == 0) {
            return;
        }
        int safeIndex = Math.max(0, Math.min(selectedIndex, options.length - 1));
        labelView.setText(options[safeIndex]);

        trigger.setClickable(true);
        trigger.setFocusable(true);
        trigger.setOnClickListener(v -> new AlertDialog.Builder(v.getContext())
                .setTitle(labelView.getContentDescription() != null
                        ? labelView.getContentDescription()
                        : null)
                .setItems(options, (dialog, which) -> {
                    labelView.setText(options[which]);
                    callback.onSelected(which, options[which]);
                })
                .show());
    }

    public static void bindDateRangePicker(
            @NonNull FragmentActivity activity,
            @NonNull View trigger,
            @NonNull TextView labelView,
            long startUtcMillis,
            long endUtcMillis,
            @NonNull OnDateRangeSelected callback) {
        labelView.setText(formatDateRange(activity, startUtcMillis, endUtcMillis));

        trigger.setClickable(true);
        trigger.setFocusable(true);
        trigger.setOnClickListener(v -> showDateRangePicker(
                activity.getSupportFragmentManager(),
                startUtcMillis,
                endUtcMillis,
                labelView,
                callback));
    }

    public static void showDateRangePicker(
            @NonNull FragmentManager fragmentManager,
            long startUtcMillis,
            long endUtcMillis,
            @NonNull TextView labelView,
            @NonNull OnDateRangeSelected callback) {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker();
        builder.setSelection(new Pair<>(startUtcMillis, endUtcMillis));
        builder.setCalendarConstraints(new CalendarConstraints.Builder().build());

        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first == null || selection.second == null) {
                return;
            }
            long start = selection.first;
            long end = selection.second;
            Context context = labelView.getContext();
            String formatted = formatDateRange(context, start, end);
            labelView.setText(formatted);
            callback.onSelected(start, end, formatted);
        });
        picker.show(fragmentManager, "date_range_picker");
    }

    @NonNull
    public static String formatDateRange(@NonNull Context context, long startUtcMillis, long endUtcMillis) {
        SimpleDateFormat dayMonth = new SimpleDateFormat("d MMM", Locale.getDefault());
        SimpleDateFormat dayMonthYear = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        dayMonth.setTimeZone(TimeZone.getTimeZone("UTC"));
        dayMonthYear.setTimeZone(TimeZone.getTimeZone("UTC"));

        Calendar startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        startCal.setTimeInMillis(startUtcMillis);
        Calendar endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        endCal.setTimeInMillis(endUtcMillis);

        boolean sameYear = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR);
        if (sameYear) {
            return dayMonth.format(new Date(startUtcMillis)) + " – "
                    + dayMonthYear.format(new Date(endUtcMillis));
        }
        return dayMonthYear.format(new Date(startUtcMillis)) + " – "
                + dayMonthYear.format(new Date(endUtcMillis));
    }

    @NonNull
    public static Pair<Long, Long> defaultWeekRangeUtc() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2026, Calendar.JUNE, 4, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.set(2026, Calendar.JUNE, 10, 0, 0, 0);
        long end = cal.getTimeInMillis();
        return new Pair<>(start, end);
    }

    public static void setDropdownContentDescription(
            @NonNull TextView labelView, @Nullable CharSequence description) {
        labelView.setContentDescription(description);
    }
}
