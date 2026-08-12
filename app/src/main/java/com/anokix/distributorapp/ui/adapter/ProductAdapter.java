package com.anokix.distributorapp.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.ProductItem;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    /** Tap callback for a product row (e.g. open the edit wizard). */
    public interface OnItemClick {
        void onClick(ProductItem item);
    }

    private final List<ProductItem> items = new ArrayList<>();
    private OnItemClick onItemClick;

    public ProductAdapter() {
    }

    public void setOnItemClickListener(OnItemClick listener) {
        this.onItemClick = listener;
    }

    /** Replaces the rows on screen. Tab / search / filter selection lives in ProductListSection. */
    public void setItems(List<ProductItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductItem item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(item);
        });
        holder.name.setText(item.name);
        holder.sku.setText(context.getString(R.string.sku_label, item.sku));

        holder.category.setText(item.category);
        holder.category.setVisibility(item.category == null || item.category.isEmpty()
                ? View.GONE : View.VISIBLE);
        holder.brand.setText(item.brand);
        holder.brand.setVisibility(item.brand == null || item.brand.isEmpty()
                ? View.GONE : View.VISIBLE);

        holder.price.setText(item.price);
        holder.stock.setText(String.format(Locale.getDefault(), "%,d", item.stockQty));
        holder.stockBadge.setText(item.stockStatus);
        applyStockBadgeStyle(holder, item.stockStatus);
        applyStatusBadgeStyle(holder, item);

        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            holder.image.setVisibility(View.VISIBLE);
            Glide.with(holder.image.getContext())
                    .load(item.imageUrl)
                    .centerCrop()
                    .into(holder.image);
        } else {
            holder.image.setVisibility(View.GONE);
            Glide.with(holder.image.getContext()).clear(holder.image);
        }
    }

    private void applyStockBadgeStyle(ViewHolder holder, String status) {
        Context context = holder.itemView.getContext();
        int textColor;
        int bgRes;
        if ("In Stock".equalsIgnoreCase(status)) {
            textColor = context.getColor(R.color.success);
            bgRes = R.drawable.bg_badge_success;
        } else if ("Low Stock".equalsIgnoreCase(status)) {
            textColor = context.getColor(R.color.warning);
            bgRes = R.drawable.bg_badge_warning;
        } else {
            textColor = context.getColor(R.color.danger);
            bgRes = R.drawable.bg_badge_danger;
        }
        holder.stockBadge.setTextColor(textColor);
        holder.stockBadge.setBackgroundResource(bgRes);
    }

    private void applyStatusBadgeStyle(ViewHolder holder, ProductItem item) {
        Context context = holder.itemView.getContext();
        String label = item.status == null || item.status.isEmpty()
                ? context.getString(R.string.active) : item.status;
        holder.statusBadge.setText(label);
        boolean active = item.isActive();
        holder.statusBadge.setTextColor(context.getColor(
                active ? R.color.success : R.color.text_secondary));
        holder.statusBadge.setBackgroundResource(
                active ? R.drawable.bg_badge_success : R.drawable.bg_badge_muted);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, sku, category, brand, price, stock, stockBadge, statusBadge;
        final ImageView image;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            sku = itemView.findViewById(R.id.productSku);
            category = itemView.findViewById(R.id.productCategory);
            brand = itemView.findViewById(R.id.productBrand);
            price = itemView.findViewById(R.id.productPrice);
            stock = itemView.findViewById(R.id.productStock);
            stockBadge = itemView.findViewById(R.id.productStockBadge);
            statusBadge = itemView.findViewById(R.id.productStatusBadge);
            image = itemView.findViewById(R.id.productImage);
        }
    }
}
