package com.anokix.distributorapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.StoreItem;

import java.util.ArrayList;
import java.util.List;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {

    /** Tag chips beyond this are collapsed into a "+N" chip so cards stay one line. */
    private static final int MAX_VISIBLE_TAGS = 3;

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
        holder.address.setVisibility(item.address == null || item.address.trim().isEmpty()
                ? View.GONE : View.VISIBLE);
        holder.followerCount.setText(String.valueOf(item.followerCount));
        holder.reviewCount.setText(String.valueOf(item.reviewCount));
        bindTags(holder, item);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(item);
            }
        });
    }

    private void bindTags(ViewHolder holder, StoreItem item) {
        holder.tags.removeAllViews();
        if (item.tags.isEmpty()) {
            holder.tags.setVisibility(View.GONE);
            return;
        }
        holder.tags.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        int shown = Math.min(item.tags.size(), MAX_VISIBLE_TAGS);
        for (int i = 0; i < shown; i++) {
            holder.tags.addView(tagChip(inflater, holder.tags, item.tags.get(i)));
        }
        int overflow = item.tags.size() - shown;
        if (overflow > 0) {
            holder.tags.addView(tagChip(inflater, holder.tags,
                    holder.itemView.getContext().getString(R.string.more_tags, overflow)));
        }
    }

    private TextView tagChip(LayoutInflater inflater, ViewGroup parent, String text) {
        TextView chip = (TextView) inflater.inflate(R.layout.item_store_tag, parent, false);
        chip.setText(text);
        return chip;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, address, followerCount, reviewCount;
        final LinearLayout tags;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.storeName);
            address = itemView.findViewById(R.id.storeAddress);
            followerCount = itemView.findViewById(R.id.followerCount);
            reviewCount = itemView.findViewById(R.id.reviewCount);
            tags = itemView.findViewById(R.id.storeTags);
        }
    }
}
