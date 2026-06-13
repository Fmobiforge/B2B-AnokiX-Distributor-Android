package com.anokix.distributor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.DeliveryItem;

import java.util.ArrayList;
import java.util.List;

public class DeliveryAdapter extends RecyclerView.Adapter<DeliveryAdapter.ViewHolder> {

    private final List<DeliveryItem> items = new ArrayList<>();

    public void setItems(List<DeliveryItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_delivery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeliveryItem item = items.get(position);
        holder.deliveryId.setText("DEL-ID: " + item.id);
        holder.traderName.setText(item.traderName);
        holder.driverName.setText(item.driverName);
        holder.eta.setText(item.eta);

        String label;
        int colorRes;
        int bgRes;
        switch (item.status.toLowerCase()) {
            case "delivered":
                label = "Delivered";
                colorRes = R.color.success;
                bgRes = R.drawable.bg_badge_success;
                break;
            case "delayed":
                label = "Delayed";
                colorRes = R.color.danger;
                bgRes = R.drawable.bg_badge_danger;
                break;
            default:
                label = "In Transit";
                colorRes = R.color.warning;
                bgRes = R.drawable.bg_badge_warning;
                break;
        }
        holder.statusBadge.setText(label);
        holder.statusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), colorRes));
        holder.statusBadge.setBackgroundResource(bgRes);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView deliveryId, traderName, driverName, eta, statusBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            deliveryId = itemView.findViewById(R.id.deliveryId);
            traderName = itemView.findViewById(R.id.traderName);
            driverName = itemView.findViewById(R.id.driverName);
            eta = itemView.findViewById(R.id.eta);
            statusBadge = itemView.findViewById(R.id.statusBadge);
        }
    }
}
