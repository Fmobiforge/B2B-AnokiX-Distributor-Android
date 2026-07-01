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
import com.anokix.distributorapp.model.PromotionItem;

import java.util.ArrayList;
import java.util.List;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.ViewHolder> {

    /** Fired when the row's overflow (3-dot) button is tapped. */
    public interface OnItemMenu {
        void onMenu(View anchor, PromotionItem item);
    }

    private final List<PromotionItem> items = new ArrayList<>();
    private OnItemMenu onItemMenu;

    public void setOnItemMenu(OnItemMenu listener) {
        this.onItemMenu = listener;
    }

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
        holder.title.setText(item.name);
        holder.subtitle.setText(subtitle(item));
        holder.budget.setText(item.budget);
        holder.reach.setText(item.reach);
        holder.clicks.setText(item.clicks);
        holder.ctr.setText(item.ctr);

        holder.statusBadge.setText(item.statusLabel);
        int statusColor;
        int bgRes;
        switch (item.status == null ? "" : item.status.toLowerCase()) {
            case "active":
                statusColor = R.color.success;
                bgRes = R.drawable.bg_badge_success;
                break;
            case "rejected":
            case "inactive":
            case "expired":
                statusColor = R.color.danger;
                bgRes = R.drawable.bg_badge_danger;
                break;
            case "pending_approval":
            case "paused":
            case "draft":
                statusColor = R.color.warning;
                bgRes = R.drawable.bg_badge_warning;
                break;
            default:
                statusColor = R.color.info;
                bgRes = R.drawable.bg_badge_info;
                break;
        }
        holder.statusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), statusColor));
        holder.statusBadge.setBackgroundResource(bgRes);

        holder.menuButton.setOnClickListener(v -> {
            if (onItemMenu != null) onItemMenu.onMenu(v, item);
        });
    }

    /** "Best Deal • 15 Jun – 30 Jun 2026" (drops the bullet when a part is missing). */
    private String subtitle(PromotionItem item) {
        boolean hasType = item.typeLabel != null && !item.typeLabel.isEmpty();
        boolean hasDates = item.dateRange != null && !item.dateRange.isEmpty();
        if (hasType && hasDates) {
            return item.typeLabel + " • " + item.dateRange;
        }
        return hasType ? item.typeLabel : (hasDates ? item.dateRange : "");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, subtitle, statusBadge, budget, reach, clicks, ctr;
        final ImageButton menuButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            budget = itemView.findViewById(R.id.budget);
            reach = itemView.findViewById(R.id.reach);
            clicks = itemView.findViewById(R.id.clicks);
            ctr = itemView.findViewById(R.id.ctr);
            menuButton = itemView.findViewById(R.id.menuButton);
        }
    }
}
