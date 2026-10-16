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
import com.anokix.distributorapp.model.FinanceFormat;
import com.google.android.material.button.MaterialButton;
import com.anokix.distributorapp.network.dto.FinancesData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Renders {@link FinancesData.Transaction} rows (type/status badges, signed amount, actions). */
public class FinanceTransactionAdapter extends RecyclerView.Adapter<FinanceTransactionAdapter.ViewHolder> {

    public interface OnTransaction {
        void onAction(FinancesData.Transaction item);
    }

    public interface OnTransactionMenu {
        void onMenu(View anchor, FinancesData.Transaction item);
    }

    private static final String[] AVATAR_COLORS = {
            "#EC4899", "#8B5CF6", "#2563EB", "#0891B2", "#16A34A", "#EA580C", "#D97706"
    };

    private final List<FinancesData.Transaction> items = new ArrayList<>();
    private OnTransaction onView;
    private OnTransaction onRowClick;
    private OnTransactionMenu onMenu;

    public void setOnView(OnTransaction listener) {
        this.onView = listener;
    }

    public void setOnRowClick(OnTransaction listener) {
        this.onRowClick = listener;
    }

    public void setOnMenu(OnTransactionMenu listener) {
        this.onMenu = listener;
    }

    public void setItems(List<FinancesData.Transaction> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_finance_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        FinancesData.Transaction t = items.get(position);
        android.content.Context ctx = h.itemView.getContext();

        // Type badge
        h.type.setText(FinanceFormat.typeLabel(t.type));
        h.type.setBackgroundResource(FinanceFormat.typeBadgeBg(t.type));
        h.type.setTextColor(ContextCompat.getColor(ctx, FinanceFormat.typeColorRes(t.type)));

        // Status badge
        h.status.setText(FinanceFormat.statusLabel(t.status));
        h.status.setBackgroundResource(FinanceFormat.statusBadgeBg(t.status));
        h.status.setTextColor(ContextCompat.getColor(ctx, FinanceFormat.statusColorRes(t.status)));

        // Counterparty
        String name = nzDash(t.trader_name);
        h.avatar.setText(initial(name));
        // Portal-style initials avatar: the gradient in bg_avatar shows through (no per-row tint).
        ViewCompat.setBackgroundTintList(h.avatar, null);
        h.counterparty.setText(name);
        boolean hasPhone = t.trader_phone != null && !t.trader_phone.isEmpty();
        h.phone.setText(hasPhone ? t.trader_phone : "");
        h.phone.setVisibility(hasPhone ? View.VISIBLE : View.GONE);

        // Signed, colour-coded amount (money in = green, money out = red)
        h.amount.setText(signedMoney(t.amount, t.currency));
        h.amount.setTextColor(ContextCompat.getColor(ctx, t.amount < 0 ? R.color.danger : R.color.success));

        h.description.setText(nzDash(t.description));
        h.meta.setText(shortRef(t.reference) + " · " + FinanceFormat.date(t.created_at));

        // Method (falls back to the wallet label)
        boolean hasMethod = t.payment_method != null && !t.payment_method.isEmpty();
        h.method.setText(hasMethod ? t.payment_method : ctx.getString(R.string.finance_wallet_method));

        h.btnView.setOnClickListener(v -> { if (onView != null) onView.onAction(t); });
        h.btnMore.setOnClickListener(v -> { if (onMenu != null) onMenu.onMenu(v, t); });
        h.itemView.setOnClickListener(v -> { if (onRowClick != null) onRowClick.onAction(t); });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ---- Helpers ----

    private static String nzDash(String s) {
        return s == null || s.trim().isEmpty() ? "—" : s;
    }

    private static String initial(String name) {
        String n = name == null ? "" : name.trim();
        return n.isEmpty() || "—".equals(n) ? "?" : n.substring(0, 1).toUpperCase(Locale.US);
    }

    /** First 8 chars of a UUID reference (matches the web portal's truncation). */
    private static String shortRef(String ref) {
        if (ref == null || ref.isEmpty()) return "—";
        return ref.length() > 10 ? ref.substring(0, 8) + "…" : ref;
    }

    private static String currencySymbol(String currency) {
        if (currency == null) return "R";
        switch (currency.trim().toUpperCase(Locale.US)) {
            case "USD": return "$";
            case "EUR": return "€";
            case "GBP": return "£";
            default:    return "R";
        }
    }

    /** Signed money: leading "-" before the symbol for negatives. */
    public static String signedMoney(double value, String currency) {
        String sym = currencySymbol(currency);
        return (value < 0 ? "-" : "") + sym + String.format(Locale.US, "%,.2f", Math.abs(value));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView type, status, avatar, counterparty, phone, amount, description, meta, method;
        final MaterialButton btnView;
        final ImageButton btnMore;

        ViewHolder(@NonNull View v) {
            super(v);
            type = v.findViewById(R.id.txnType);
            status = v.findViewById(R.id.txnStatus);
            avatar = v.findViewById(R.id.txnAvatar);
            counterparty = v.findViewById(R.id.txnCounterparty);
            phone = v.findViewById(R.id.txnPhone);
            amount = v.findViewById(R.id.txnAmount);
            description = v.findViewById(R.id.txnDescription);
            meta = v.findViewById(R.id.txnMeta);
            method = v.findViewById(R.id.txnMethod);
            btnView = v.findViewById(R.id.btnView);
            btnMore = v.findViewById(R.id.btnMore);
        }
    }
}
