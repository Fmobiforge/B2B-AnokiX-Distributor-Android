package com.anokix.distributorapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.MenuItem;
import com.anokix.distributorapp.ui.views.RobotoLightTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;

import java.util.List;

public class DrawerMenuAdapter extends RecyclerView.Adapter<DrawerMenuAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(MenuItem item);
    }

    private final List<MenuItem> items;
    private final OnItemClickListener listener;
    private int selectedPosition = 0;

    public DrawerMenuAdapter(List<MenuItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setSelectedByKey(String key) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).key.equals(key)) {
                int prev = selectedPosition;
                selectedPosition = i;
                if (prev != selectedPosition) {
                    notifyItemChanged(prev);
                    notifyItemChanged(selectedPosition);
                }
                return;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drawer_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = items.get(position);
        holder.title.setText(item.title);
        holder.icon.setImageResource(item.iconRes);

        boolean selected = position == selectedPosition;
        int white = ContextCompat.getColor(holder.itemView.getContext(), R.color.drawer_text);
        int muted = ContextCompat.getColor(holder.itemView.getContext(), R.color.drawer_text_muted);

        holder.icon.setColorFilter(selected ? white : muted);
        holder.title.setTextColor(selected ? white : muted);

        boolean hasSubtitle = item.subtitle != null && !item.subtitle.isEmpty();
        holder.subtitle.setVisibility(hasSubtitle ? View.VISIBLE : View.GONE);
        if (hasSubtitle) {
            holder.subtitle.setText(item.subtitle);
            // Keep the second line readable but secondary on both the plain and purple rows.
            holder.subtitle.setTextColor(selected ? white : muted);
            holder.subtitle.setAlpha(selected ? 0.85f : 0.75f);
        }

        if (selected) {
            holder.itemRoot.setBackgroundResource(R.drawable.bg_drawer_item_selected);
            holder.title.setTypeface(holder.title.getTypeface(), android.graphics.Typeface.BOLD);
        } else {
            holder.itemRoot.setBackgroundResource(android.R.color.transparent);
            holder.title.setTypeface(holder.title.getTypeface(), android.graphics.Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            int prev = selectedPosition;
            selectedPosition = adapterPosition;
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RobotoTextView title;
        final RobotoLightTextView subtitle;
        final ImageView icon;
        final LinearLayout itemRoot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemRoot = itemView.findViewById(R.id.drawerItemRoot);
            title = itemView.findViewById(R.id.drawerItemTitle);
            subtitle = itemView.findViewById(R.id.drawerItemSubtitle);
            icon = itemView.findViewById(R.id.drawerItemIcon);
        }
    }
}
