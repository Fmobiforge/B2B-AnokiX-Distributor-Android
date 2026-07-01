package com.anokix.distributorapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.FinanceTransaction;

import java.util.ArrayList;
import java.util.List;

public class FinanceTransactionAdapter extends RecyclerView.Adapter<FinanceTransactionAdapter.ViewHolder> {

    private final List<FinanceTransaction> items = new ArrayList<>();

    public void setItems(List<FinanceTransaction> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_finance_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FinanceTransaction item = items.get(position);
        holder.title.setText(item.title);
        holder.invoiceId.setText(item.invoiceId);
        holder.date.setText(item.date);
        holder.amount.setText(item.amount);
        holder.statusBadge.setText(item.status);
        holder.icon.setImageResource(item.iconRes);
        holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.purple_primary));
        holder.divider.setVisibility(position < items.size() - 1 ? View.VISIBLE : View.GONE);

        int color = ContextCompat.getColor(holder.itemView.getContext(),
                item.isIncome ? R.color.success : R.color.danger);
        holder.amount.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, invoiceId, date, amount, statusBadge;
        final ImageView icon;
        final View divider;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            invoiceId = itemView.findViewById(R.id.invoiceId);
            date = itemView.findViewById(R.id.date);
            amount = itemView.findViewById(R.id.amount);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            icon = itemView.findViewById(R.id.icon);
            divider = itemView.findViewById(R.id.divider);
        }
    }
}
