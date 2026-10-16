package com.anokix.distributorapp.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.ReturnFormat;
import com.anokix.distributorapp.network.dto.ReturnsData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Renders {@link ReturnsData.Return} rows with a View button and a conditional overflow menu. */
public class ReturnAdapter extends RecyclerView.Adapter<ReturnAdapter.ViewHolder> {

    public interface OnReturn {
        void onAction(ReturnsData.Return item);
    }

    public interface OnReturnMenu {
        void onMenu(View anchor, ReturnsData.Return item);
    }

    private static final String[] AVATAR_COLORS = {
            "#EC4899", "#8B5CF6", "#2563EB", "#0891B2", "#16A34A", "#EA580C", "#D97706"
    };

    private final List<ReturnsData.Return> items = new ArrayList<>();
    private OnReturn onView;
    private OnReturnMenu onMenu;

    public void setOnView(OnReturn listener) {
        this.onView = listener;
    }

    public void setOnMenu(OnReturnMenu listener) {
        this.onMenu = listener;
    }

    public void setItems(List<ReturnsData.Return> data) {
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
        ReturnsData.Return r = items.get(position);

        holder.returnNumber.setText(r.grn_number);
        holder.reasonLabel.setText(emptyDash(r.reason_label));

        String name = r.traderName();
        holder.avatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.US));
        // Portal-style initials avatar: the gradient in bg_avatar shows through (no per-row tint).
        ViewCompat.setBackgroundTintList(holder.avatar, null);
        holder.traderName.setText(name);

        String phone = r.trader_phone == null ? "" : r.trader_phone;
        holder.traderPhone.setText(phone);
        holder.traderPhone.setVisibility(phone.isEmpty() ? View.GONE : View.VISIBLE);

        holder.dateValue.setText(emptyDash(ReturnFormat.date(r.created_at)));
        holder.itemsValue.setText(String.valueOf(r.item_count));
        holder.qtyValue.setText(String.valueOf(r.total_quantity));
        holder.creditValue.setText(emptyDash(r.credit_note_number));

        holder.statusBadge.setText(ReturnFormat.label(r.status));
        holder.statusBadge.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), ReturnFormat.colorRes(r.status)));
        holder.statusBadge.setBackgroundResource(ReturnFormat.badgeBg(r.status));

        holder.viewButton.setOnClickListener(v -> {
            if (onView != null) onView.onAction(r);
        });

        boolean pending = isPending(r.status);
        holder.menuButton.setVisibility(pending ? View.VISIBLE : View.GONE);
        holder.menuButton.setOnClickListener(v -> {
            if (onMenu != null) onMenu.onMenu(v, r);
        });
    }

    /** Pending returns can still be approved / rejected. */
    public static boolean isPending(String status) {
        return status != null && "pending".equalsIgnoreCase(status.trim());
    }

    private static String emptyDash(String s) {
        return s == null || s.trim().isEmpty() ? "—" : s;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView returnNumber, reasonLabel, avatar, traderName, traderPhone,
                dateValue, itemsValue, qtyValue, creditValue, statusBadge;
        final View viewButton;
        final ImageButton menuButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            returnNumber = itemView.findViewById(R.id.returnNumber);
            reasonLabel = itemView.findViewById(R.id.reasonLabel);
            avatar = itemView.findViewById(R.id.avatar);
            traderName = itemView.findViewById(R.id.traderName);
            traderPhone = itemView.findViewById(R.id.traderPhone);
            dateValue = itemView.findViewById(R.id.dateValue);
            itemsValue = itemView.findViewById(R.id.itemsValue);
            qtyValue = itemView.findViewById(R.id.qtyValue);
            creditValue = itemView.findViewById(R.id.creditValue);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            viewButton = itemView.findViewById(R.id.viewButton);
            menuButton = itemView.findViewById(R.id.menuButton);
        }
    }
}
