package com.anokix.distributorapp.ui.util;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.ReportsData;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * "Generate a report" (New-Design/Report/3.Generate-Report.png).
 *
 * <p>A bottom sheet with two pickers: Report and Period, matching the reference dialog.
 * Every entry in {@code available[]} already carries its own category and the generate
 * call only takes a {@code report_key}, so there is nothing a category field could add
 * here. (The list screen keeps its Category filter — that one narrows generated reports,
 * which is a different job.)
 *
 * <p>"Generate again" on a report card opens the same sheet with that report and its
 * original period already selected, so the user only confirms.
 */
public final class GenerateReportSheet {

    /** Called after a successful generate, with the server's confirmation message. */
    public interface OnGenerated {
        void onGenerated(@Nullable String message);
    }

    /** The API takes and returns plain calendar dates. */
    private static final String API_DATE = "yyyy-MM-dd";

    private GenerateReportSheet() {
    }

    public static void show(@NonNull FragmentActivity activity,
                            @NonNull ReportsData data,
                            @NonNull OnGenerated onGenerated) {
        show(activity, data, null, onGenerated);
    }

    /**
     * @param repeat the report being regenerated, or {@code null} for a fresh Generate.
     *               Its name resolves back to a catalogue key and its period is reused.
     */
    public static void show(@NonNull FragmentActivity activity,
                            @NonNull ReportsData data,
                            @Nullable ReportsData.Report repeat,
                            @NonNull OnGenerated onGenerated) {
        List<ReportsData.Available> catalogue = data.available == null
                ? new ArrayList<>() : new ArrayList<>(data.available);
        if (catalogue.isEmpty()) {
            Toast.makeText(activity, R.string.err_choose_report, Toast.LENGTH_LONG).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View content = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_generate_report, null, false);
        dialog.setContentView(content);

        TextView reportValue = content.findViewById(R.id.reportValue);
        TextView reportAbout = content.findViewById(R.id.reportAbout);
        TextView periodValue = content.findViewById(R.id.periodValue);
        View submit = content.findViewById(R.id.sheetSubmit);

        final ReportsData.Available[] selected = {null};
        final long[] period = defaultPeriodUtc();

        // Seed from the report being regenerated, so the sheet opens ready to confirm.
        if (repeat != null) {
            selected[0] = data.findAvailable(repeat.name, repeat.category);
            Long from = parseApiDate(repeat.period_from);
            Long to = parseApiDate(repeat.period_to);
            if (from != null && to != null) {
                period[0] = from;
                period[1] = to;
            }
        }

        Runnable render = () -> {
            boolean chosen = selected[0] != null;
            reportValue.setText(chosen ? selected[0].name
                    : activity.getString(R.string.choose_a_report));
            reportValue.setTextColor(ContextCompat.getColor(activity,
                    chosen ? R.color.text_primary : R.color.text_secondary));
            String about = chosen && selected[0].about != null ? selected[0].about.trim() : "";
            reportAbout.setText(about);
            reportAbout.setVisibility(about.isEmpty() ? View.GONE : View.VISIBLE);
            periodValue.setText(FilterUiHelper.formatDateRange(activity, period[0], period[1]));
        };
        render.run();

        content.findViewById(R.id.reportField).setOnClickListener(v -> {
            String[] labels = new String[catalogue.size()];
            for (int i = 0; i < catalogue.size(); i++) {
                labels[i] = catalogue.get(i).name;
            }
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.report_label)
                    .setItems(labels, (d, which) -> {
                        selected[0] = catalogue.get(which);
                        render.run();
                    })
                    .show();
        });

        content.findViewById(R.id.periodField).setOnClickListener(v ->
                FilterUiHelper.showDateRangePicker(activity.getSupportFragmentManager(),
                        period[0], period[1], periodValue, (start, end, formatted) -> {
                            period[0] = start;
                            period[1] = end;
                        }));

        content.findViewById(R.id.sheetClose).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.sheetCancel).setOnClickListener(v -> dialog.dismiss());

        submit.setOnClickListener(v -> {
            if (selected[0] == null) {
                Toast.makeText(activity, R.string.err_choose_report, Toast.LENGTH_LONG).show();
                return;
            }
            submit.setEnabled(false);
            ((TextView) submit).setText(R.string.generating_report);
            ApiClient.get(activity).generateReport(selected[0].key,
                    formatApiDate(period[0]), formatApiDate(period[1]),
                    new ApiCallback<String>() {
                        @Override
                        public void onSuccess(String message) {
                            dialog.dismiss();
                            onGenerated.onGenerated(message);
                        }

                        @Override
                        public void onError(String message) {
                            submit.setEnabled(true);
                            ((TextView) submit).setText(R.string.generate_btn);
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }

    // ---- Period ----------------------------------------------------------

    /** Defaults to the last 30 days, matching the range the web dialog opens with. */
    private static long[] defaultPeriodUtc() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long end = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        return new long[]{cal.getTimeInMillis(), end};
    }

    private static String formatApiDate(long utcMillis) {
        SimpleDateFormat format = new SimpleDateFormat(API_DATE, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new java.util.Date(utcMillis));
    }

    private static Long parseApiDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(API_DATE, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return format.parse(raw.trim()).getTime();
        } catch (Exception e) {
            return null;
        }
    }
}
