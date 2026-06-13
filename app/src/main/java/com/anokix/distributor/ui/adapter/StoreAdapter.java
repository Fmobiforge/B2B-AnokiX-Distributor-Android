package com.anokix.distributor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.StoreItem;

import java.util.ArrayList;
import java.util.List;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {

    public interface OnStoreClickListener {
        void onClick(StoreItem item);
    }

    private final List<StoreItem> items = new ArrayList<>();
    private OnStoreClickListener listener;

    public void setOnItemClickListener(OnStoreClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<StoreItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_store, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StoreItem item = items.get(position);
        holder.name.setText(item.name);
        holder.address.setText(item.address);
        holder.followerCount.setText(String.valueOf(item.followerCount));
        holder.reviewCount.setText(String.valueOf(item.reviewCount));
        holder.statusBadge.setText(item.status);

        boolean active = "Active".equalsIgnoreCase(item.status);
        int color = ContextCompat.getColor(holder.itemView.getContext(),
                active ? R.color.success : R.color.text_secondary);
        int bg = active ? R.drawable.bg_badge_success : R.drawable.bg_search_bar;
        holder.statusBadge.setTextColor(color);
        holder.statusBadge.setBackgroundResource(bg);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, address, followerCount, reviewCount, statusBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.storeName);
            address = itemView.findViewById(R.id.storeAddress);
            followerCount = itemView.findViewById(R.id.followerCount);
            reviewCount = itemView.findViewById(R.id.reviewCount);
            statusBadge = itemView.findViewById(R.id.statusBadge);
        }
    }
}
