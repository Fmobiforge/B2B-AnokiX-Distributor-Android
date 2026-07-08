package com.anokix.distributorapp.ui.adapter;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.dto.VasProductsData;
import com.anokix.distributorapp.ui.VasFormat;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Grid of purchasable VAS products. Ad-hoc products show "Custom" over the product name
 * (the buyer sets the amount); fixed products show the name over the price. The selected
 * card gets a green outline + check, matching the web portal.
 */
public class VasProductAdapter extends RecyclerView.Adapter<VasProductAdapter.VH> {

    public interface OnProductClick {
        void onClick(VasProductsData.Product product);
    }

    private final List<VasProductsData.Product> items = new ArrayList<>();
    private final OnProductClick listener;
    private String selectedId;
    private boolean priceFirst;

    public VasProductAdapter(OnProductClick listener) {
        this.listener = listener;
    }

    public void setItems(List<VasProductsData.Product> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    /**
     * POS "Select amount" style: fixed products show the price on top (bold) and the name
     * below. The default (Airtime screen) shows the name on top and the price below.
     */
    public void setPriceFirst(boolean priceFirst) {
        this.priceFirst = priceFirst;
        notifyDataSetChanged();
    }

    public void setSelectedId(String id) {
        this.selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vas_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VasProductsData.Product p = items.get(position);
        if (p.isAdHoc) {
            h.title.setText(R.string.vas_custom);
            h.subtitle.setText(p.name);
        } else if (priceFirst) {
            h.title.setText(VasFormat.money(p.price));
            h.subtitle.setText(p.name);
        } else {
            h.title.setText(p.name);
            h.subtitle.setText(VasFormat.money(p.price));
        }

        boolean selected = p.id != null && p.id.equals(selectedId);
        h.check.setVisibility(selected ? View.VISIBLE : View.GONE);
        int stroke = ContextCompat.getColor(h.itemView.getContext(),
                selected ? R.color.success : R.color.border);
        h.card.setStrokeColor(stroke);
        h.card.setStrokeWidth(dp(h.itemView.getResources(), selected ? 2f : 1f));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static int dp(Resources res, float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, res.getDisplayMetrics()));
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView title;
        final TextView subtitle;
        final ImageView check;

        VH(@NonNull View v) {
            super(v);
            card = (MaterialCardView) v;
            title = v.findViewById(R.id.productTitle);
            subtitle = v.findViewById(R.id.productSubtitle);
            check = v.findViewById(R.id.productCheck);
        }
    }
}
