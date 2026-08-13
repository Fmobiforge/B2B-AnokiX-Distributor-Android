package com.anokix.distributorapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.dto.ReportsData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generated-report cards for the Reports screen — name, category, period, when it was
 * built, and the format / size / rows facts the web table kept in separate columns.
 *
 * <p>Each card carries a Download button and an overflow that opens Generate again /
 * Delete (New-Design/Report/2.Item-Context-Menu.png). A report that is not "ready" has
 * no file behind it, so its Download button is disabled and its error is shown instead.
 */
public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    /** Row actions: the Download button and the overflow menu. */
    public interface OnRowAction {
        void onDownload(ReportsData.Report report);

        void onMore(ReportsData.Report report, View anchor);
    }

    /** Fills the KPI header that scrolls above the report cards. */
    public interface HeaderBinder {
        void bindHeader(View header);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ROW = 1;
    private static final int TYPE_EMPTY = 2;

    private final List<ReportsData.Report> items = new ArrayList<>();
    private OnRowAction listener;
    private HeaderBinder headerBinder;
    private String emptyMessage = "";

    public void setOnRowActionListener(OnRowAction listener) {
        this.listener = listener;
    }

    /**
     * Adds the KPI summary as the first row. Keeping it inside the list means the report
     * cards always own the leftover height — a fixed header left nothing to scroll on
     * short screens.
     */
    public void setHeaderBinder(HeaderBinder binder) {
        this.headerBinder = binder;
        notifyDataSetChanged();
    }

    /** Shown in place of the rows when nothing matches; keeps the header visible. */
    public void setItems(List<ReportsData.Report> data, String emptyMessage) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        this.emptyMessage = emptyMessage == null ? "" : emptyMessage;
        notifyDataSetChanged();
    }

    private boolean hasHeader() {
        return headerBinder != null;
    }

    private boolean showsEmptyState() {
        return items.isEmpty() && hasHeader();
    }

    @Override
    public int getItemViewType(int position) {
        if (hasHeader() && position == 0) {
            return TYPE_HEADER;
        }
        return showsEmptyState() ? TYPE_EMPTY : TYPE_ROW;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        if (viewType == TYPE_HEADER) {
            layout = R.layout.item_reports_summary;
        } else if (viewType == TYPE_EMPTY) {
            layout = R.layout.item_reports_empty;
        } else {
            layout = R.layout.item_report;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == TYPE_HEADER) {
            headerBinder.bindHeader(holder.itemView);
            return;
        }
        if (type == TYPE_EMPTY) {
            ((TextView) holder.itemView).setText(emptyMessage);
            return;
        }

        ReportsData.Report report = items.get(hasHeader() ? position - 1 : position);
        View item = holder.itemView;

        holder.name.setText(report.name == null ? "" : report.name);
        applyBadge(holder.categoryBadge, capitalise(report.category),
                R.color.purple_primary, R.drawable.bg_badge_purple);
        applyBadge(holder.statusBadge, capitalise(report.status),
                statusColor(report.status), statusBackground(report.status));

        holder.period.setText(item.getContext()
                .getString(R.string.report_period_value, report.periodLabel()));

        String time = report.generatedTimeLabel();
        holder.generated.setText(item.getContext().getString(R.string.report_generated_value,
                time.isEmpty() ? report.generatedDateLabel()
                        : report.generatedDateLabel() + ", " + time));

        holder.meta.setText(metaLine(item, report));

        boolean failed = !report.isReady();
        String error = report.error == null ? "" : report.error.trim();
        holder.error.setVisibility(failed && !error.isEmpty() ? View.VISIBLE : View.GONE);
        holder.error.setText(error);

        // Nothing to fetch until the server says the file is ready.
        holder.download.setEnabled(!failed);
        holder.download.setAlpha(failed ? 0.4f : 1f);
        holder.download.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownload(report);
            }
        });

        holder.moreButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMore(report, v);
            }
        });
    }

    /** "CSV · 933 B · 10 rows · 1 download" — the table's trailing columns as one line. */
    private String metaLine(View item, ReportsData.Report report) {
        StringBuilder line = new StringBuilder(report.formatLabel())
                .append(" · ").append(report.sizeLabel())
                .append(" · ").append(item.getContext().getResources()
                        .getQuantityString(R.plurals.report_rows, report.row_count, report.row_count));
        if (report.downloads > 0) {
            line.append(" · ").append(item.getContext().getResources()
                    .getQuantityString(R.plurals.report_downloads, report.downloads, report.downloads));
        }
        return line.toString();
    }

    private void applyBadge(TextView badge, String label, int colorRes, int backgroundRes) {
        if (label == null || label.trim().isEmpty()) {
            badge.setVisibility(View.GONE);
            return;
        }
        badge.setVisibility(View.VISIBLE);
        badge.setText(label);
        badge.setTextColor(ContextCompat.getColor(badge.getContext(), colorRes));
        badge.setBackgroundResource(backgroundRes);
    }

    private static int statusColor(String status) {
        if (equalsAny(status, "ready")) {
            return R.color.success;
        }
        if (equalsAny(status, "failed", "error")) {
            return R.color.danger;
        }
        return R.color.warning;
    }

    private static int statusBackground(String status) {
        if (equalsAny(status, "ready")) {
            return R.drawable.bg_badge_success;
        }
        if (equalsAny(status, "failed", "error")) {
            return R.drawable.bg_badge_danger;
        }
        return R.drawable.bg_badge_warning;
    }

    /** The API sends categories and statuses lower-cased ("inventory", "ready"). */
    private static String capitalise(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.getDefault()) + trimmed.substring(1);
    }

    private static boolean equalsAny(String value, String... candidates) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getItemCount() {
        int rows = showsEmptyState() ? 1 : items.size();
        return rows + (hasHeader() ? 1 : 0);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, categoryBadge, statusBadge, period, generated, meta, error, download;
        final ImageButton moreButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.reportName);
            categoryBadge = itemView.findViewById(R.id.reportCategoryBadge);
            statusBadge = itemView.findViewById(R.id.reportStatusBadge);
            period = itemView.findViewById(R.id.reportPeriod);
            generated = itemView.findViewById(R.id.reportGenerated);
            meta = itemView.findViewById(R.id.reportMeta);
            error = itemView.findViewById(R.id.reportError);
            download = itemView.findViewById(R.id.reportDownloadButton);
            moreButton = itemView.findViewById(R.id.reportMoreButton);
        }
    }
}
