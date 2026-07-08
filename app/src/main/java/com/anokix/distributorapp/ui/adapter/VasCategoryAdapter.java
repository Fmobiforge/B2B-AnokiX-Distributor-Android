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
import com.anokix.distributorapp.network.dto.VasCategoriesData;
import com.anokix.distributorapp.ui.VasIcons;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal / grid list of selectable VAS leaf categories. In the collapsed state the
 * cards are a fixed width (horizontal scroll); when expanded they fill the grid column.
 * The selected card gets a green outline, mirroring the web portal.
 */
public class VasCategoryAdapter extends RecyclerView.Adapter<VasCategoryAdapter.VH> {

    public interface OnCategoryClick {
        void onClick(VasCategoriesData.Node node);
    }

    private final List<VasCategoriesData.Node> items = new ArrayList<>();
    private final OnCategoryClick listener;
    private String selectedId;
    private boolean expanded;

    public VasCategoryAdapter(OnCategoryClick listener) {
        this.listener = listener;
    }

    public void setItems(List<VasCategoriesData.Node> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public void setSelectedId(String id) {
        this.selectedId = id;
        notifyDataSetChanged();
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vas_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VasCategoriesData.Node n = items.get(position);
        h.name.setText(n.name);
        h.count.setText(h.itemView.getResources()
                .getQuantityString(R.plurals.vas_bundles, n.productCount, n.productCount));
        h.icon.setImageResource(VasIcons.iconFor(n.id, n.name));

        boolean selected = n.id != null && n.id.equals(selectedId);
        int stroke = ContextCompat.getColor(h.itemView.getContext(),
                selected ? R.color.success : R.color.border);
        h.card.setStrokeColor(stroke);
        h.card.setStrokeWidth(dp(h.itemView.getResources(), selected ? 2f : 1f));

        // Fixed width for horizontal scroll; fill the column when expanded to a grid.
        ViewGroup.LayoutParams lp = h.card.getLayoutParams();
        lp.width = expanded ? ViewGroup.LayoutParams.MATCH_PARENT : dp(h.itemView.getResources(), 112f);
        h.card.setLayoutParams(lp);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(n);
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
        final ImageView icon;
        final TextView name;
        final TextView count;

        VH(@NonNull View v) {
            super(v);
            card = (MaterialCardView) v;
            icon = v.findViewById(R.id.categoryIcon);
            name = v.findViewById(R.id.categoryName);
            count = v.findViewById(R.id.categoryCount);
        }
    }
}
