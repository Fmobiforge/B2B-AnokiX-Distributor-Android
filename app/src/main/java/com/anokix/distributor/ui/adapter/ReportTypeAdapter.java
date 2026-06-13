package com.anokix.distributor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.ReportTypeItem;

import java.util.ArrayList;
import java.util.List;

public class ReportTypeAdapter extends RecyclerView.Adapter<ReportTypeAdapter.ViewHolder> {

    private final List<ReportTypeItem> items = new ArrayList<>();

    public void setItems(List<ReportTypeItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_type, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReportTypeItem item = items.get(position);
        holder.title.setText(item.title);
        holder.description.setText(item.description);
        holder.icon.setImageResource(item.iconRes);
        holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.purple_primary));
        holder.itemView.setOnClickListener(v ->
                Toast.makeText(v.getContext(), item.title, Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, description;
        final ImageView icon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.description);
            icon = itemView.findViewById(R.id.icon);
        }
    }
}
