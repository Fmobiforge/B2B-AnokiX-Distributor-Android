package com.anokix.distributor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.CampaignProduct;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Catalogue rows shown in the product picker dialog. Supports a "selected" set
 * (for multi / buy1get1 modes) rendered with a filled radio; tapping a row fires
 * the callback.
 */
public class PickProductAdapter extends RecyclerView.Adapter<PickProductAdapter.ViewHolder> {

    public interface OnPick {
        void onPick(CampaignProduct product);
    }

    private final List<CampaignProduct> all = new ArrayList<>();
    private final List<CampaignProduct> shown = new ArrayList<>();
    private final List<String> selectedIds = new ArrayList<>();
    private boolean showCheck = false;
    private String query = "";
    private OnPick onPick;

    public void setOnPick(OnPick listener) {
        this.onPick = listener;
    }

    public void setShowCheck(boolean show) {
        this.showCheck = show;
    }

    public void setItems(List<CampaignProduct> data) {
        all.clear();
        if (data != null) all.addAll(data);
        applyFilter();
    }

    public void setSelectedIds(List<String> ids) {
        selectedIds.clear();
        if (ids != null) selectedIds.addAll(ids);
        notifyDataSetChanged();
    }

    public void filter(String text) {
        query = text == null ? "" : text.trim().toLowerCase(Locale.getDefault());
        applyFilter();
    }

    private void applyFilter() {
        shown.clear();
        for (CampaignProduct p : all) {
            if (query.isEmpty() || contains(p.name) || contains(p.sku)
                    || contains(p.category) || contains(p.brand)) {
                shown.add(p);
            }
        }
        notifyDataSetChanged();
    }

    private boolean contains(String value) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(query);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pick_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        CampaignProduct p = shown.get(position);
        h.name.setText(p.name);
        h.meta.setText(p.sku + " • " + p.category);
        h.price.setText(String.format(Locale.US, "R%,.0f", p.originalPrice));
        if (p.imageUrl != null && !p.imageUrl.isEmpty()) {
            Glide.with(h.image.getContext()).load(p.imageUrl).fitCenter().into(h.image);
        } else {
            h.image.setImageDrawable(null);
        }
        h.check.setVisibility(showCheck ? View.VISIBLE : View.GONE);
        h.check.setImageResource(selectedIds.contains(p.productId)
                ? R.drawable.ic_radio_selected : R.drawable.ic_radio_unselected);
        h.itemView.setOnClickListener(v -> {
            if (onPick != null) onPick.onPick(p);
        });
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, meta, price;
        final ImageView image, check;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.pickName);
            meta = itemView.findViewById(R.id.pickMeta);
            price = itemView.findViewById(R.id.pickPrice);
            image = itemView.findViewById(R.id.pickImage);
            check = itemView.findViewById(R.id.pickCheck);
        }
    }
}
