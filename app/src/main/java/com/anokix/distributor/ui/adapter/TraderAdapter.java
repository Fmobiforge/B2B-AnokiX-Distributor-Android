package com.anokix.distributor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.TraderItem;

import java.util.ArrayList;
import java.util.List;

public class TraderAdapter extends RecyclerView.Adapter<TraderAdapter.ViewHolder> {

    private final List<TraderItem> items = new ArrayList<>();

    public void setItems(List<TraderItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trader, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TraderItem item = items.get(position);
        holder.name.setText(item.name);
        holder.location.setText(item.location);
        holder.code.setText(item.traderCode);
        holder.segment.setText(item.segment);
        holder.avatar.setText(item.initials);
        holder.avatar.setBackgroundTintList(ContextCompat.getColorStateList(
                holder.itemView.getContext(), item.avatarColorRes));
        holder.newTag.setVisibility(item.isNew ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, location, code, segment, avatar, newTag;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.traderName);
            location = itemView.findViewById(R.id.traderLocation);
            code = itemView.findViewById(R.id.traderCode);
            segment = itemView.findViewById(R.id.segmentTag);
            avatar = itemView.findViewById(R.id.avatar);
            newTag = itemView.findViewById(R.id.newTag);
        }
    }
}
