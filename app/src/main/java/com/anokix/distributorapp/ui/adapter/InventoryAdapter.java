package com.anokix.distributorapp.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.dto.InventoryPageData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Inventory stock cards — product swatch, name, SKU / category, on-hand units against
 * capacity, stock value, plus the view / more actions from the web layout.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    /** Row actions: the eye opens the details screen, the overflow opens the context menu. */
    public interface OnRowAction {
        void onView(InventoryPageData.Row row);

        void onMore(InventoryPageData.Row row, View anchor);
    }

    /** Fills the KPI / donut header that scrolls above the stock cards. */
    public interface HeaderBinder {
        void bindHeader(View header);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ROW = 1;
    private static final int TYPE_EMPTY = 2;

    private final List<InventoryPageData.Row> items = new ArrayList<>();
    private OnRowAction listener;
    private HeaderBinder headerBinder;
    private String emptyMessage = "";

    public void setOnRowActionListener(OnRowAction listener) {
        this.listener = listener;
    }

    /**
     * Adds a summary header as the first row. Keeping it inside the list means the stock
     * cards always own the leftover height — a fixed header left nothing to scroll on
     * short screens.
     */
    public void setHeaderBinder(HeaderBinder binder) {
        this.headerBinder = binder;
        notifyDataSetChanged();
    }

    /** Re-renders the header in place (after the page reloads). */
    public void refreshHeader() {
        if (headerBinder != null) {
            notifyItemChanged(0);
        }
    }

    /** Shown in place of the rows when nothing matches; keeps the header visible. */
    public void setItems(List<InventoryPageData.Row> data, String emptyMessage) {
        items.clear();
        if (data != null) items.addAll(data);
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
            layout = R.layout.item_inventory_summary;
        } else if (viewType == TYPE_EMPTY) {
            layout = R.layout.item_inventory_empty;
        } else {
            layout = R.layout.item_inventory;
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

        InventoryPageData.Row row = items.get(hasHeader() ? position - 1 : position);
        Context context = holder.itemView.getContext();

        holder.name.setText(row.name);
        holder.sku.setText(row.sku);
        holder.sku.setVisibility(isEmpty(row.sku) ? View.GONE : View.VISIBLE);
        holder.category.setText(row.category);
        holder.category.setVisibility(isEmpty(row.category) ? View.GONE : View.VISIBLE);

        holder.units.setText(context.getString(R.string.inventory_units_of_capacity,
                format(row.units), format(row.capacity)));
        holder.value.setText(formatValue(row.stockValue));

        boolean hasWarehouse = !isEmpty(row.warehouse) && !"—".equals(row.warehouse);
        holder.warehouse.setText(hasWarehouse ? row.warehouse : "");
        holder.warehouse.setVisibility(hasWarehouse ? View.VISIBLE : View.GONE);

        applySwatch(holder, row.imageColor);
        applyStatus(holder, row);

        holder.viewButton.setOnClickListener(v -> {
            if (listener != null) listener.onView(row);
        });
        holder.moreButton.setOnClickListener(v -> {
            if (listener != null) listener.onMore(row, v);
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onView(row);
        });
    }

    /** Tints the product swatch with the colour the API picked for the row. */
    private void applySwatch(ViewHolder holder, String hex) {
        int color = parseColor(hex, ContextCompat.getColor(
                holder.itemView.getContext(), R.color.purple_primary));
        Drawable background = holder.swatch.getBackground();
        if (background instanceof GradientDrawable) {
            GradientDrawable shape = (GradientDrawable) background.mutate();
            shape.setColor(color);
            holder.swatch.setBackground(shape);
        } else {
            holder.swatch.setBackgroundColor(color);
        }
    }

    private void applyStatus(ViewHolder holder, InventoryPageData.Row row) {
        Context context = holder.itemView.getContext();
        int color;
        int badgeBg;
        if (row.isOutOfStock()) {
            color = ContextCompat.getColor(context, R.color.danger);
            badgeBg = R.drawable.bg_badge_danger;
        } else if (row.isLowStock()) {
            color = ContextCompat.getColor(context, R.color.warning);
            badgeBg = R.drawable.bg_badge_warning;
        } else {
            color = ContextCompat.getColor(context, R.color.success);
            badgeBg = R.drawable.bg_badge_success;
        }
        holder.statusBadge.setText(row.statusLabel());
        holder.statusBadge.setTextColor(color);
        holder.statusBadge.setBackgroundResource(badgeBg);

        // Tint first, then set the level, so swapping the drawable can't drop the progress.
        int percent = fillPercent(row);
        tintProgress(holder.progress, color);
        holder.progress.setProgress(percent);

        boolean showPercent = row.capacity > 0 && row.units > 0;
        holder.capacityPercent.setText(showPercent
                ? context.getString(R.string.inventory_capacity_percent, percent) : "");
        holder.capacityPercent.setTextColor(color);
        holder.capacityPercent.setVisibility(showPercent ? View.VISIBLE : View.GONE);
    }

    /**
     * Colours only the progress layer. Tinting the whole drawable would paint the track
     * too, which made every bar look full regardless of the stock level.
     */
    private void tintProgress(ProgressBar bar, int color) {
        Drawable drawable = bar.getProgressDrawable();
        if (drawable == null) {
            return;
        }
        Drawable mutated = drawable.mutate();
        if (mutated instanceof LayerDrawable) {
            Drawable fill = ((LayerDrawable) mutated).findDrawableByLayerId(android.R.id.progress);
            if (fill != null) {
                fill.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
                bar.setProgressDrawable(mutated);
                return;
            }
        }
        mutated.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
    }

    /** How full the shelf is; without a capacity figure a stocked row reads as full. */
    private int fillPercent(InventoryPageData.Row row) {
        if (row.units <= 0) {
            return 0;
        }
        if (row.capacity <= 0) {
            return 100;
        }
        return (int) Math.max(2, Math.min(100, Math.round(row.units * 100d / row.capacity)));
    }

    private String format(long value) {
        return String.format(Locale.getDefault(), "%,d", value);
    }

    private String formatValue(double value) {
        return String.format(Locale.US, "R%,.0f", value);
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Parses a "#rrggbb" from the API, falling back when it is missing or malformed. */
    public static int parseColor(String hex, int fallback) {
        if (hex == null || hex.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Color.parseColor(hex.trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    @Override
    public int getItemCount() {
        int rows = showsEmptyState() ? 1 : items.size();
        return rows + (hasHeader() ? 1 : 0);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, sku, category, units, warehouse, value, statusBadge, capacityPercent;
        final View swatch;
        final ProgressBar progress;
        final ImageButton viewButton, moreButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            swatch = itemView.findViewById(R.id.itemSwatch);
            name = itemView.findViewById(R.id.itemName);
            sku = itemView.findViewById(R.id.itemSku);
            category = itemView.findViewById(R.id.itemCategory);
            units = itemView.findViewById(R.id.itemUnits);
            warehouse = itemView.findViewById(R.id.itemWarehouse);
            value = itemView.findViewById(R.id.itemValue);
            statusBadge = itemView.findViewById(R.id.itemStatusBadge);
            capacityPercent = itemView.findViewById(R.id.itemCapacityPercent);
            progress = itemView.findViewById(R.id.itemStockProgress);
            viewButton = itemView.findViewById(R.id.itemViewButton);
            moreButton = itemView.findViewById(R.id.itemMoreButton);
        }
    }
}
