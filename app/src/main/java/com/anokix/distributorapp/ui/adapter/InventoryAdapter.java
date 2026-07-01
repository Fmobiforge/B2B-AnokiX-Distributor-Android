package com.anokix.distributorapp.ui.adapter;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.ListItem;

import java.util.ArrayList;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private final List<ListItem> items = new ArrayList<>();

    public InventoryAdapter(List<ListItem> data) {
        setItems(data);
    }

    public void setItems(List<ListItem> data) {
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
        ListItem item = items.get(position);
        holder.title.setText(item.title);
        holder.sku.setText(item.sku);
        holder.stock.setText(String.valueOf(item.stockCount));
        holder.progress.setProgress(item.stockPercent);
        holder.outLabel.setText(holder.itemView.getContext().getString(R.string.out_label));
        applyStockStyle(holder, item.badge);
        holder.divider.setVisibility(position < items.size() - 1 ? View.VISIBLE : View.GONE);
    }

    private void applyStockStyle(ViewHolder holder, String status) {
        int color;
        boolean outOfStock = "Out of Stock".equals(status);
        boolean expired = "Expired".equals(status);
        if ("In Stock".equals(status)) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.success);
        } else if ("Low Stock".equals(status)) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.warning);
        } else if (expired) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary);
        } else {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.danger);
        }
        holder.progress.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        holder.progress.setVisibility(outOfStock || expired ? View.INVISIBLE : View.VISIBLE);
        holder.outLabel.setVisibility(outOfStock ? View.VISIBLE : View.GONE);
        if (expired) {
            holder.outLabel.setText(holder.itemView.getContext().getString(R.string.expired));
            holder.outLabel.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, sku, stock, outLabel;
        final ProgressBar progress;
        final View divider;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.rowTitle);
            sku = itemView.findViewById(R.id.rowSku);
            stock = itemView.findViewById(R.id.rowStock);
            progress = itemView.findViewById(R.id.stockProgress);
            outLabel = itemView.findViewById(R.id.outLabel);
            divider = itemView.findViewById(R.id.rowDivider);
        }
    }
}
