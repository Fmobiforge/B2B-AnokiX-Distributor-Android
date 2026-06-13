package com.anokix.distributor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.ReturnItem;

import java.util.ArrayList;
import java.util.List;

public class ReturnAdapter extends RecyclerView.Adapter<ReturnAdapter.ViewHolder> {

    public interface ActionListener {
        void onApprove(ReturnItem item);
        void onReject(ReturnItem item);
    }

    private final List<ReturnItem> items = new ArrayList<>();
    private final ActionListener listener;

    public ReturnAdapter(ActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ReturnItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_return, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReturnItem item = items.get(position);
        holder.returnId.setText(item.id);
        holder.traderName.setText(item.traderName);
        holder.reason.setText(item.reason);
        holder.amount.setText(item.amount);
        holder.timestamp.setText(item.timestamp);

        boolean pending = "pending".equalsIgnoreCase(item.status);
        holder.actionRow.setVisibility(pending ? View.VISIBLE : View.GONE);

        holder.approveButton.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(item);
            else Toast.makeText(v.getContext(), "Approved " + item.id, Toast.LENGTH_SHORT).show();
        });
        holder.rejectButton.setOnClickListener(v -> {
            if (listener != null) listener.onReject(item);
            else Toast.makeText(v.getContext(), "Rejected " + item.id, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView returnId, traderName, reason, amount, timestamp;
        final View actionRow, approveButton, rejectButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            returnId = itemView.findViewById(R.id.returnId);
            traderName = itemView.findViewById(R.id.traderName);
            reason = itemView.findViewById(R.id.reason);
            amount = itemView.findViewById(R.id.amount);
            timestamp = itemView.findViewById(R.id.timestamp);
            actionRow = itemView.findViewById(R.id.actionRow);
            approveButton = itemView.findViewById(R.id.approveButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
    }
}
