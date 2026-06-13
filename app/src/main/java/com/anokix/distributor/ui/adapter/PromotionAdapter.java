package com.anokix.distributor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.PromotionItem;

import java.util.ArrayList;
import java.util.List;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.ViewHolder> {

    private final List<PromotionItem> items = new ArrayList<>();

    public void setItems(List<PromotionItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promotion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PromotionItem item = items.get(position);
        holder.title.setText(item.title);
        holder.impressions.setText(item.impressions);
        holder.ctr.setText(item.ctr);
        holder.bannerLabel.setText(item.bannerLabel);
        holder.banner.setBackgroundResource(item.bannerBgRes);
        holder.statusBadge.setText(item.status);

        int statusColor;
        int bgRes;
        if ("Paused".equalsIgnoreCase(item.status)) {
            statusColor = R.color.warning;
            bgRes = R.drawable.bg_badge_warning;
        } else {
            statusColor = R.color.success;
            bgRes = R.drawable.bg_badge_success;
        }
        holder.statusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), statusColor));
        holder.statusBadge.setBackgroundResource(bgRes);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, impressions, ctr, statusBadge, bannerLabel;
        final FrameLayout banner;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            impressions = itemView.findViewById(R.id.impressions);
            ctr = itemView.findViewById(R.id.ctr);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            bannerLabel = itemView.findViewById(R.id.bannerLabel);
            banner = itemView.findViewById(R.id.banner);
        }
    }
}
