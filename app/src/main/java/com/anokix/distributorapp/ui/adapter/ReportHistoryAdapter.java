package com.anokix.distributorapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.ReportHistoryItem;

import java.util.ArrayList;
import java.util.List;

public class ReportHistoryAdapter extends RecyclerView.Adapter<ReportHistoryAdapter.ViewHolder> {

    private final List<ReportHistoryItem> items = new ArrayList<>();

    public void setItems(List<ReportHistoryItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReportHistoryItem item = items.get(position);
        holder.name.setText(item.name);
        holder.date.setText(item.generatedDate);
        holder.format.setText(item.format);
        holder.divider.setVisibility(position < items.size() - 1 ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v ->
                Toast.makeText(v.getContext(), item.name, Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, date, format;
        final View divider;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            date = itemView.findViewById(R.id.date);
            format = itemView.findViewById(R.id.format);
            divider = itemView.findViewById(R.id.divider);
        }
    }
}
