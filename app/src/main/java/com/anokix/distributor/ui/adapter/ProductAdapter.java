package com.anokix.distributor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.ProductItem;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    /** Tap callback for a product row (e.g. open the edit wizard). */
    public interface OnItemClick {
        void onClick(ProductItem item);
    }

    // Full data set from the API, plus the (possibly filtered) view shown to the user.
    private final List<ProductItem> allItems = new ArrayList<>();
    private final List<ProductItem> items = new ArrayList<>();
    private String query = "";
    private OnItemClick onItemClick;

    public ProductAdapter() {
    }

    public void setOnItemClickListener(OnItemClick listener) {
        this.onItemClick = listener;
    }

    public ProductAdapter(List<ProductItem> data) {
        setItems(data);
    }

    public void setItems(List<ProductItem> data) {
        allItems.clear();
        if (data != null) allItems.addAll(data);
        applyFilter();
    }

    /** Local, case-insensitive keyword search over name / SKU / category. */
    public void filter(String text) {
        query = text == null ? "" : text.trim().toLowerCase(Locale.getDefault());
        applyFilter();
    }

    private void applyFilter() {
        items.clear();
        if (query.isEmpty()) {
            items.addAll(allItems);
        } else {
            for (ProductItem item : allItems) {
                if (matches(item, query)) items.add(item);
            }
        }
        notifyDataSetChanged();
    }

    private boolean matches(ProductItem item, String q) {
        return contains(item.name, q) || contains(item.sku, q) || contains(item.category, q);
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(q);
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
        holder.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(item);
        });
        holder.name.setText(item.name);
        holder.sku.setText(holder.itemView.getContext().getString(R.string.sku_label, item.sku));
        holder.category.setText(holder.itemView.getContext().getString(R.string.category_label, item.category));
        holder.price.setText(item.price);
        holder.stockBadge.setText(item.stockStatus);
        applyStockBadgeStyle(holder, item.stockStatus);

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
        int textColor;
        int bgRes;
        if ("In Stock".equalsIgnoreCase(status)) {
            textColor = holder.itemView.getContext().getColor(R.color.success);
            bgRes = R.drawable.bg_badge_success;
        } else if ("Low Stock".equalsIgnoreCase(status)) {
            textColor = holder.itemView.getContext().getColor(R.color.warning);
            bgRes = R.drawable.bg_badge_warning;
        } else {
            textColor = holder.itemView.getContext().getColor(R.color.danger);
            bgRes = R.drawable.bg_badge_danger;
        }
        holder.stockBadge.setTextColor(textColor);
        holder.stockBadge.setBackgroundResource(bgRes);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, sku, category, price, stockBadge;
        final ImageView image;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            sku = itemView.findViewById(R.id.productSku);
            category = itemView.findViewById(R.id.productCategory);
            price = itemView.findViewById(R.id.productPrice);
            stockBadge = itemView.findViewById(R.id.productStockBadge);
            image = itemView.findViewById(R.id.productImage);
        }
    }
}
