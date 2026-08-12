package com.anokix.distributorapp.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
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
 * Inventory stock cards — product swatch, name, SKU · category, on-hand units against
 * capacity, stock value, plus the view / more actions from the web layout.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    /** Row actions: the eye opens the details screen, the overflow opens the context menu. */
    public interface OnRowAction {
        void onView(InventoryPageData.Row row);

        void onMore(InventoryPageData.Row row, View anchor);
    }

    private final List<InventoryPageData.Row> items = new ArrayList<>();
    private OnRowAction listener;

    public void setOnRowActionListener(OnRowAction listener) {
        this.listener = listener;
    }

    public void setItems(List<InventoryPageData.Row> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryPageData.Row row = items.get(position);
        Context context = holder.itemView.getContext();

        holder.name.setText(row.name);
        holder.meta.setText(meta(row));
        holder.units.setText(context.getString(R.string.inventory_units_of_capacity,
                format(row.units), format(row.capacity)));
        holder.value.setText(formatValue(row.stockValue));

        boolean hasWarehouse = row.warehouse != null && !row.warehouse.isEmpty()
                && !"—".equals(row.warehouse);
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

    private String meta(InventoryPageData.Row row) {
        StringBuilder meta = new StringBuilder();
        if (row.sku != null && !row.sku.isEmpty()) {
            meta.append(row.sku);
        }
        if (row.category != null && !row.category.isEmpty()) {
            if (meta.length() > 0) meta.append("  ·  ");
            meta.append(row.category);
        }
        return meta.toString();
    }

    /** Tints the product swatch with the colour the API picked for the row. */
    private void applySwatch(ViewHolder holder, String hex) {
        int color = parseColor(hex, ContextCompat.getColor(
                holder.itemView.getContext(), R.color.purple_primary));
        View swatch = holder.swatch;
        if (swatch.getBackground() instanceof GradientDrawable) {
            GradientDrawable shape = (GradientDrawable) swatch.getBackground().mutate();
            shape.setColor(color);
            swatch.setBackground(shape);
        } else {
            swatch.setBackgroundColor(color);
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

        holder.progress.setProgress(fillPercent(row));
        holder.progress.getProgressDrawable().mutate()
                .setColorFilter(color, PorterDuff.Mode.SRC_IN);
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
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, meta, units, warehouse, value, statusBadge;
        final View swatch;
        final ProgressBar progress;
        final ImageButton viewButton, moreButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            swatch = itemView.findViewById(R.id.itemSwatch);
            name = itemView.findViewById(R.id.itemName);
            meta = itemView.findViewById(R.id.itemMeta);
            units = itemView.findViewById(R.id.itemUnits);
            warehouse = itemView.findViewById(R.id.itemWarehouse);
            value = itemView.findViewById(R.id.itemValue);
            statusBadge = itemView.findViewById(R.id.itemStatusBadge);
            progress = itemView.findViewById(R.id.itemStockProgress);
            viewButton = itemView.findViewById(R.id.itemViewButton);
            moreButton = itemView.findViewById(R.id.itemMoreButton);
        }
    }
}
