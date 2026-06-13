package com.anokix.distributor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.NotificationItem;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final List<NotificationItem> items = new ArrayList<>();

    public void setItems(List<NotificationItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        holder.title.setText(item.title);
        holder.message.setText(item.message);
        holder.time.setText(item.relativeTime);
        holder.icon.setImageResource(item.iconRes);
        if (item.iconRes == R.drawable.ic_check_green) {
            holder.icon.clearColorFilter();
        } else {
            holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), item.iconTintRes));
        }
        holder.iconContainer.setBackgroundResource(item.iconBgRes);
        holder.unreadDot.setVisibility(item.unread ? View.VISIBLE : View.GONE);

        boolean hasAction = item.actionLabel != null && !item.actionLabel.isEmpty();
        holder.actionButton.setVisibility(hasAction ? View.VISIBLE : View.GONE);
        if (hasAction) {
            holder.actionButton.setText(item.actionLabel);
            holder.actionButton.setOnClickListener(v ->
                    Toast.makeText(v.getContext(), item.actionLabel, Toast.LENGTH_SHORT).show());
        } else {
            holder.actionButton.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, message, time, actionButton;
        final ImageView icon;
        final FrameLayout iconContainer;
        final View unreadDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            message = itemView.findViewById(R.id.message);
            time = itemView.findViewById(R.id.time);
            actionButton = itemView.findViewById(R.id.actionButton);
            icon = itemView.findViewById(R.id.icon);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            unreadDot = itemView.findViewById(R.id.unreadDot);
        }
    }
}
