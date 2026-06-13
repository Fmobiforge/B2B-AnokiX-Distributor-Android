package com.anokix.distributor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.SettingsMenuEntry;

import java.util.ArrayList;
import java.util.List;

public class SettingsMenuAdapter extends RecyclerView.Adapter<SettingsMenuAdapter.ViewHolder> {

    private final List<SettingsMenuEntry> items = new ArrayList<>();

    public void setItems(List<SettingsMenuEntry> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_settings_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SettingsMenuEntry item = items.get(position);
        holder.title.setText(item.title);
        holder.icon.setImageResource(item.iconRes);
        holder.divider.setVisibility(position < items.size() - 1 ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v ->
                Toast.makeText(v.getContext(), item.title, Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final ImageView icon;
        final View divider;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            icon = itemView.findViewById(R.id.icon);
            divider = itemView.findViewById(R.id.divider);
        }
    }
}
