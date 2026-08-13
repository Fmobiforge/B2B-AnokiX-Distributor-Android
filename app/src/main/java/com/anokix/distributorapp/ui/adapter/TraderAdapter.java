package com.anokix.distributorapp.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.data.ApiMapper;
import com.anokix.distributorapp.network.dto.TradersData;

import java.util.ArrayList;
import java.util.List;

/**
 * Trader cards for the Trader Management screen — avatar, business name, code and
 * classification, address / phone, then the Status, Credit Status and Registration
 * badges the web table shows as separate columns.
 *
 * <p>Rows are bound straight from {@link TradersData.Trader} because the same record
 * is handed to the Edit wizard; flattening it into a view model would only throw away
 * the fields the wizard prefills from.
 */
public class TraderAdapter extends RecyclerView.Adapter<TraderAdapter.ViewHolder> {

    /** The overflow button opens the Edit / Delete menu (2.Trader-Item-Menu.png). */
    public interface OnRowAction {
        void onMore(TradersData.Trader trader, View anchor);
    }

    /** Fills the KPI header that scrolls above the trader cards. */
    public interface HeaderBinder {
        void bindHeader(View header);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ROW = 1;
    private static final int TYPE_EMPTY = 2;

    private final List<TradersData.Trader> items = new ArrayList<>();
    private OnRowAction listener;
    private HeaderBinder headerBinder;
    private String emptyMessage = "";

    public void setOnRowActionListener(OnRowAction listener) {
        this.listener = listener;
    }

    /**
     * Adds the KPI summary as the first row. Keeping it inside the list means the trader
     * cards always own the leftover height — a fixed header left nothing to scroll on
     * short screens.
     */
    public void setHeaderBinder(HeaderBinder binder) {
        this.headerBinder = binder;
        notifyDataSetChanged();
    }

    /** Shown in place of the rows when nothing matches; keeps the header visible. */
    public void setItems(List<TradersData.Trader> data, String emptyMessage) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        this.emptyMessage = emptyMessage == null ? "" : emptyMessage;
        notifyDataSetChanged();
    }

    private boolean hasHeader() {
        return headerBinder != null;
    }

    private boolean showsEmptyState() {
        return items.isEmpty() && hasHeader();
    }

    @Override
    public int getItemViewType(int position) {
        if (hasHeader() && position == 0) {
            return TYPE_HEADER;
        }
        return showsEmptyState() ? TYPE_EMPTY : TYPE_ROW;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        if (viewType == TYPE_HEADER) {
            layout = R.layout.item_traders_summary;
        } else if (viewType == TYPE_EMPTY) {
            layout = R.layout.item_traders_empty;
        } else {
            layout = R.layout.item_trader;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == TYPE_HEADER) {
            headerBinder.bindHeader(holder.itemView);
            return;
        }
        if (type == TYPE_EMPTY) {
            ((TextView) holder.itemView).setText(emptyMessage);
            return;
        }

        int index = hasHeader() ? position - 1 : position;
        TradersData.Trader trader = items.get(index);
        Context context = holder.itemView.getContext();

        String name = trader.displayName();
        holder.name.setText(name);
        holder.avatar.setText(ApiMapper.traderInitials(name));
        holder.avatar.setBackgroundTintList(ContextCompat.getColorStateList(
                context, ApiMapper.traderAvatarColor(index)));

        setOrHide(holder.code, trader.trader_code);
        setOrHide(holder.address, trader.address);
        setOrHide(holder.phone, phoneOf(trader));

        // Trading name is only worth a second line when it differs from the headline.
        String secondary = isBlank(trader.trading_name) || trader.trading_name.equals(name)
                ? "" : trader.trading_name;
        setOrHide(holder.type, secondary);

        applyBadge(holder.statusBadge, trader.status, statusColor(trader.status),
                statusBackground(trader.status));
        applyBadge(holder.creditBadge, trader.credit_status, creditColor(trader.credit_status),
                creditBackground(trader.credit_status));
        // The registration badge only earns its space while approval is still outstanding.
        String registration = isApproved(trader.registration_status) ? "" : trader.registration_status;
        applyBadge(holder.registrationBadge, registration, R.color.info, R.drawable.bg_badge_info);

        holder.moreButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMore(trader, v);
            }
        });
    }

    /** Prefer the business line the list actually returns, then the owner's mobile. */
    private String phoneOf(TradersData.Trader trader) {
        if (!isBlank(trader.business_phone_number)) {
            return format(trader.business_phone_number);
        }
        if (!isBlank(trader.mobile_number)) {
            return format(trader.mobile_number);
        }
        return isBlank(trader.phone_number) ? "" : format(trader.phone_number);
    }

    /** The API stores numbers as bare MSISDNs ("27821234567"); show them dialable. */
    private String format(String raw) {
        String value = raw.trim();
        return value.startsWith("+") ? value : "+" + value;
    }

    private void setOrHide(TextView view, String value) {
        view.setText(isBlank(value) ? "" : value);
        view.setVisibility(isBlank(value) ? View.GONE : View.VISIBLE);
    }

    private void applyBadge(TextView badge, String label, int colorRes, int backgroundRes) {
        if (isBlank(label)) {
            badge.setVisibility(View.GONE);
            return;
        }
        badge.setVisibility(View.VISIBLE);
        badge.setText(label);
        badge.setTextColor(ContextCompat.getColor(badge.getContext(), colorRes));
        badge.setBackgroundResource(backgroundRes);
    }

    private static boolean isApproved(String registrationStatus) {
        return isBlank(registrationStatus) || "Approved".equalsIgnoreCase(registrationStatus.trim());
    }

    private static int statusColor(String status) {
        if (equalsAny(status, "Active")) {
            return R.color.success;
        }
        if (equalsAny(status, "Suspended")) {
            return R.color.danger;
        }
        if (equalsAny(status, "New")) {
            return R.color.purple_primary;
        }
        return R.color.text_secondary;
    }

    private static int statusBackground(String status) {
        if (equalsAny(status, "Active")) {
            return R.drawable.bg_badge_success;
        }
        if (equalsAny(status, "Suspended")) {
            return R.drawable.bg_badge_danger;
        }
        if (equalsAny(status, "New")) {
            return R.drawable.bg_badge_purple;
        }
        return R.drawable.bg_badge_muted;
    }

    private static int creditColor(String creditStatus) {
        if (equalsAny(creditStatus, "Good Standing")) {
            return R.color.success;
        }
        if (equalsAny(creditStatus, "Warning")) {
            return R.color.warning;
        }
        if (equalsAny(creditStatus, "Overdue", "Suspended")) {
            return R.color.danger;
        }
        return R.color.text_secondary;
    }

    private static int creditBackground(String creditStatus) {
        if (equalsAny(creditStatus, "Good Standing")) {
            return R.drawable.bg_badge_success;
        }
        if (equalsAny(creditStatus, "Warning")) {
            return R.drawable.bg_badge_warning;
        }
        if (equalsAny(creditStatus, "Overdue", "Suspended")) {
            return R.drawable.bg_badge_danger;
        }
        return R.drawable.bg_badge_muted;
    }

    private static boolean equalsAny(String value, String... candidates) {
        if (isBlank(value)) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty() || "null".equals(value);
    }

    @Override
    public int getItemCount() {
        int rows = showsEmptyState() ? 1 : items.size();
        return rows + (hasHeader() ? 1 : 0);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView avatar, name, code, type, address, phone;
        final TextView statusBadge, creditBadge, registrationBadge;
        final ImageButton moreButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.traderName);
            code = itemView.findViewById(R.id.traderCode);
            type = itemView.findViewById(R.id.traderType);
            address = itemView.findViewById(R.id.traderAddress);
            phone = itemView.findViewById(R.id.traderPhone);
            statusBadge = itemView.findViewById(R.id.traderStatusBadge);
            creditBadge = itemView.findViewById(R.id.traderCreditBadge);
            registrationBadge = itemView.findViewById(R.id.traderRegistrationBadge);
            moreButton = itemView.findViewById(R.id.traderMoreButton);
        }
    }
}
