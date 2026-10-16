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
import com.anokix.distributorapp.model.DeliveryFormat;
import com.anokix.distributorapp.model.OrderFormat;
import com.anokix.distributorapp.network.dto.DeliveriesData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Renders {@link DeliveriesData.Delivery} rows with a View button and a conditional overflow menu. */
public class DeliveryAdapter extends RecyclerView.Adapter<DeliveryAdapter.ViewHolder> {

    public interface OnDelivery {
        void onAction(DeliveriesData.Delivery delivery);
    }

    public interface OnDeliveryMenu {
        void onMenu(View anchor, DeliveriesData.Delivery delivery);
    }

    private static final String[] AVATAR_COLORS = {
            "#EC4899", "#8B5CF6", "#2563EB", "#0891B2", "#16A34A", "#EA580C", "#D97706"
    };

    private final List<DeliveriesData.Delivery> items = new ArrayList<>();
    private OnDelivery onView;
    private OnDeliveryMenu onMenu;

    public void setOnView(OnDelivery listener) {
        this.onView = listener;
    }

    public void setOnMenu(OnDeliveryMenu listener) {
        this.onMenu = listener;
    }

    public void setItems(List<DeliveriesData.Delivery> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_delivery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeliveriesData.Delivery d = items.get(position);

        holder.deliveryNumber.setText(d.delivery_number);
        holder.orderNumber.setText(d.order_number);

        String name = d.traderName();
        holder.avatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.US));
        // Portal-style initials avatar: the gradient in bg_avatar shows through (no per-row tint).
        ViewCompat.setBackgroundTintList(holder.avatar, null);
        holder.traderName.setText(name);

        String phone = d.trader_phone == null ? "" : d.trader_phone;
        holder.traderPhone.setText(phone);
        holder.traderPhone.setVisibility(phone.isEmpty() ? View.GONE : View.VISIBLE);

        holder.areaValue.setText(emptyDash(d.area));
        holder.dateValue.setText(emptyDash(DeliveryFormat.deliveryAt(d.delivery_at)));
        holder.itemsValue.setText(String.valueOf(d.item_count));
        holder.amountValue.setText(OrderFormat.money(d.total_amount, "R"));

        holder.statusBadge.setText(DeliveryFormat.label(d.status));
        holder.statusBadge.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), DeliveryFormat.colorRes(d.status)));
        holder.statusBadge.setBackgroundResource(DeliveryFormat.badgeBg(d.status));

        holder.viewButton.setOnClickListener(v -> {
            if (onView != null) onView.onAction(d);
        });

        boolean hasAction = actionFor(d.raw_status) != null;
        holder.menuButton.setVisibility(hasAction ? View.VISIBLE : View.GONE);
        holder.menuButton.setOnClickListener(v -> {
            if (onMenu != null) onMenu.onMenu(v, d);
        });
    }

    /** "dispatch" / "deliver", or null when the row has no follow-up action. */
    public static String actionFor(String rawStatus) {
        if (rawStatus == null) return null;
        switch (rawStatus) {
            case "ReadyForDelivery": return "dispatch";
            case "OutForDelivery":   return "deliver";
            default:                 return null;
        }
    }

    private static String emptyDash(String s) {
        return s == null || s.trim().isEmpty() ? "—" : s;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView deliveryNumber, orderNumber, avatar, traderName, traderPhone,
                areaValue, dateValue, itemsValue, amountValue, statusBadge;
        final View viewButton;
        final ImageButton menuButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            deliveryNumber = itemView.findViewById(R.id.deliveryNumber);
            orderNumber = itemView.findViewById(R.id.orderNumber);
            avatar = itemView.findViewById(R.id.avatar);
            traderName = itemView.findViewById(R.id.traderName);
            traderPhone = itemView.findViewById(R.id.traderPhone);
            areaValue = itemView.findViewById(R.id.areaValue);
            dateValue = itemView.findViewById(R.id.dateValue);
            itemsValue = itemView.findViewById(R.id.itemsValue);
            amountValue = itemView.findViewById(R.id.amountValue);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            viewButton = itemView.findViewById(R.id.viewButton);
            menuButton = itemView.findViewById(R.id.menuButton);
        }
    }
}
