package com.anokix.distributor.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.OrderFormat;
import com.anokix.distributor.network.dto.OrdersData;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/**
 * Order cards for the distributor Orders screen, bound to {@link OrdersData.Order}:
 * trader logo/name, order number + placed date, product chips, amount, status badge,
 * delivery window, and the per-status fulfilment actions (View / Accept / Reject /
 * Pick / Pack / Mark Ready / Dispatch / Delivered).
 */
public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    /** Fulfilment action keys posted to {@code api/distributor/orders/{action}}. */
    public interface OnOrderAction {
        void onView(OrdersData.Order order);

        void onAction(OrdersData.Order order, String action);
    }

    private static final String[] LOGO_COLORS = {
            "#EC4899", "#7C3AED", "#2563EB", "#0891B2", "#16A34A", "#EA580C", "#D97706"
    };

    private final List<OrdersData.Order> items;
    private final String currency;
    private OnOrderAction listener;

    public OrderAdapter(List<OrdersData.Order> items, String currency) {
        this.items = items;
        this.currency = currency;
    }

    public void setOnOrderAction(OnOrderAction listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        OrdersData.Order o = items.get(position);
        String name = o.traderName();

        h.traderName.setText(name);
        h.orderId.setText(o.order_number);
        h.orderTime.setText(OrderFormat.createdAt(o.created_at));
        h.amount.setText(OrderFormat.money(o.total_amount, currency));

        h.avatar.setText(initials(name));
        ViewCompat.setBackgroundTintList(h.avatar,
                ColorStateList.valueOf(Color.parseColor(logoColor(o.trader_id))));
        h.avatar.setTextColor(Color.WHITE);

        int color = OrderFormat.statusColor(o.statusKey());
        h.statusRibbon.setText(OrderFormat.humanize(o.statusKey()));
        h.statusRibbon.setTextColor(color);
        ViewCompat.setBackgroundTintList(h.statusRibbon,
                ColorStateList.valueOf(Color.argb(28, Color.red(color), Color.green(color), Color.blue(color))));

        h.deliveryInfo.setText(deliveryInfo(o));

        int productCount = o.products != null ? o.products.size() : 0;
        h.productsLabel.setText(h.itemView.getContext().getString(R.string.products_count, productCount));
        bindProductChips(h.productChips, o.products);

        bindActions(h, o);
    }

    private void bindActions(ViewHolder h, OrdersData.Order o) {
        h.btnView.setOnClickListener(v -> {
            if (listener != null) listener.onView(o);
        });

        String key = o.statusKey();
        String primaryAction = null;
        int primaryLabel = 0;
        boolean showReject = false;

        switch (key) {
            case "pending":
                primaryAction = "accept";
                primaryLabel = R.string.order_action_accept;
                showReject = true;
                break;
            case "accepted":
                primaryAction = "pick";
                primaryLabel = R.string.order_action_pick;
                break;
            case "picking":
                primaryAction = "pack";
                primaryLabel = R.string.order_action_pack;
                break;
            case "packing":
                primaryAction = "ready";
                primaryLabel = R.string.order_action_mark_ready;
                break;
            case "ready_for_delivery":
                primaryAction = "dispatch";
                primaryLabel = R.string.order_action_dispatch;
                break;
            case "out_for_delivery":
                primaryAction = "deliver";
                primaryLabel = R.string.order_action_delivered;
                break;
            default:
                break;
        }

        if (primaryAction != null) {
            final String action = primaryAction;
            h.primaryLabel.setText(primaryLabel);
            h.btnPrimary.setVisibility(View.VISIBLE);
            h.btnPrimary.setOnClickListener(v -> {
                if (listener != null) listener.onAction(o, action);
            });
        } else {
            h.btnPrimary.setVisibility(View.GONE);
            h.btnPrimary.setOnClickListener(null);
        }

        if (showReject) {
            h.btnReject.setVisibility(View.VISIBLE);
            h.btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onAction(o, "reject");
            });
        } else {
            h.btnReject.setVisibility(View.GONE);
            h.btnReject.setOnClickListener(null);
        }
    }

    private void bindProductChips(ChipGroup group, List<String> products) {
        group.removeAllViews();
        if (products == null) return;
        for (String p : products) {
            Chip chip = new Chip(group.getContext());
            chip.setText(p);
            chip.setTextSize(11);
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipMinHeight(dp(group, 26));
            chip.setChipBackgroundColorResource(R.color.surface_variant);
            chip.setTextColor(ContextCompat.getColor(group.getContext(), R.color.text_primary));
            chip.setChipStrokeColorResource(R.color.divider);
            chip.setChipStrokeWidth(dp(group, 1));
            group.addView(chip);
        }
    }

    /** "25 Jun, 08:00–22:00" — delivery date + time window, when present. */
    private String deliveryInfo(OrdersData.Order o) {
        String date = OrderFormat.deliveryDate(o.delivery_date);
        String start = OrderFormat.trimSeconds(o.delivery_start_time);
        String end = OrderFormat.trimSeconds(o.delivery_end_time);
        String window = (!start.isEmpty() || !end.isEmpty()) ? start + "–" + end : "";
        if (date.isEmpty()) return window;
        return window.isEmpty() ? date : date + ", " + window;
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() >= 2) break;
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    private static String logoColor(long traderId) {
        return LOGO_COLORS[(int) (Math.abs(traderId) % LOGO_COLORS.length)];
    }

    private static int dp(View v, int value) {
        return (int) (value * v.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView traderName, orderId, orderTime, amount, avatar, statusRibbon,
                deliveryInfo, productsLabel, primaryLabel;
        final ChipGroup productChips;
        final View btnView, btnPrimary, btnReject;
        final ImageView primaryIcon;

        ViewHolder(@NonNull View v) {
            super(v);
            traderName = v.findViewById(R.id.traderName);
            orderId = v.findViewById(R.id.orderId);
            orderTime = v.findViewById(R.id.orderTime);
            amount = v.findViewById(R.id.orderAmount);
            avatar = v.findViewById(R.id.avatarText);
            statusRibbon = v.findViewById(R.id.statusRibbon);
            deliveryInfo = v.findViewById(R.id.deliveryInfo);
            productsLabel = v.findViewById(R.id.productsLabel);
            productChips = v.findViewById(R.id.productChips);
            btnView = v.findViewById(R.id.btnViewOrder);
            btnPrimary = v.findViewById(R.id.btnPrimaryAction);
            btnReject = v.findViewById(R.id.btnRejectAction);
            primaryLabel = v.findViewById(R.id.primaryActionLabel);
            primaryIcon = v.findViewById(R.id.primaryActionIcon);
        }
    }
}
