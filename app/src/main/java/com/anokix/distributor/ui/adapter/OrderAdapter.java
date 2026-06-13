package com.anokix.distributor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.OrderItem;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
    private final List<OrderItem> items;
    private final boolean showActions;

    public OrderAdapter(List<OrderItem> items, boolean showActions) {
        this.items = items;
        this.showActions = showActions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = items.get(position);
        holder.traderName.setText(item.traderName);
        holder.orderId.setText(item.orderId);
        holder.location.setText(item.location);
        holder.amount.setText(item.amount);
        holder.payment.setText(item.payment);
        holder.initials.setText(item.initials);

        String timeLabel = item.relativeTime != null && !item.relativeTime.isEmpty()
                ? item.relativeTime : item.time;
        holder.time.setText(timeLabel);

        if (item.productSummary != null && !item.productSummary.isEmpty()) {
            holder.productSummary.setText(item.productSummary);
            holder.productSummary.setVisibility(View.VISIBLE);
        } else {
            holder.productSummary.setVisibility(View.GONE);
        }

        holder.paidBadge.setVisibility(item.isPaid ? View.VISIBLE : View.GONE);

        if ("new".equals(item.status)) {
            holder.statusRibbon.setText("NEW ORDER");
            holder.statusRibbon.setVisibility(View.VISIBLE);
        } else if ("processing".equals(item.status)) {
            holder.statusRibbon.setText("PROCESSING");
            holder.statusRibbon.setVisibility(View.VISIBLE);
        } else {
            holder.statusRibbon.setVisibility(View.GONE);
        }

        if (showActions) {
            holder.actions.setVisibility(View.VISIBLE);
            holder.acceptButton.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Order accepted", Toast.LENGTH_SHORT).show());
            holder.rejectButton.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Order rejected", Toast.LENGTH_SHORT).show());
            holder.viewButton.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "View order " + item.orderId, Toast.LENGTH_SHORT).show());
            holder.copyOrderId.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Copied " + item.orderId, Toast.LENGTH_SHORT).show());
        } else {
            holder.actions.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView traderName;
        final TextView orderId;
        final TextView location;
        final TextView amount;
        final TextView payment;
        final TextView time;
        final TextView initials;
        final TextView productSummary;
        final TextView statusRibbon;
        final TextView paidBadge;
        final View actions;
        final View acceptButton;
        final View rejectButton;
        final View viewButton;
        final View copyOrderId;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            traderName = itemView.findViewById(R.id.traderName);
            orderId = itemView.findViewById(R.id.orderId);
            location = itemView.findViewById(R.id.orderLocation);
            amount = itemView.findViewById(R.id.orderAmount);
            payment = itemView.findViewById(R.id.paymentBadge);
            time = itemView.findViewById(R.id.orderTime);
            initials = itemView.findViewById(R.id.avatarText);
            productSummary = itemView.findViewById(R.id.productSummary);
            statusRibbon = itemView.findViewById(R.id.statusRibbon);
            paidBadge = itemView.findViewById(R.id.paidBadge);
            actions = itemView.findViewById(R.id.orderActions);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            viewButton = itemView.findViewById(R.id.viewButton);
            copyOrderId = itemView.findViewById(R.id.copyOrderId);
        }
    }
}
