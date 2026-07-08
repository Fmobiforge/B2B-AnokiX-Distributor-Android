package com.anokix.distributorapp.ui.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.dto.VasTransactionsData;
import com.anokix.distributorapp.ui.VasFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Recent VAS transactions list (product · number · time, amount + status pill). */
public class VasTransactionAdapter extends RecyclerView.Adapter<VasTransactionAdapter.VH> {

    private final SimpleDateFormat inFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US);
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.US);

    private final List<VasTransactionsData.Transaction> items = new ArrayList<>();

    public void setItems(List<VasTransactionsData.Transaction> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vas_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VasTransactionsData.Transaction t = items.get(position);
        h.product.setText(t.name == null ? "—" : t.name);
        h.meta.setText(formatNumber(t.msisdn) + " · " + formatTime(t.created_at));
        h.amount.setText(VasFormat.money(t.amount));
        styleStatus(h.status, t.status);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatTime(String createdAt) {
        if (createdAt == null) return "";
        try {
            Date d = inFmt.parse(createdAt);
            if (d != null) return timeFmt.format(d);
        } catch (ParseException ignored) {
        }
        return createdAt;
    }

    /** 27821234567 / 0821234567 → "082 123 4567" when it resolves to a 10-digit local number. */
    private String formatNumber(String msisdn) {
        if (msisdn == null) return "";
        String digits = msisdn.replaceAll("[^0-9]", "");
        if (digits.startsWith("27") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        if (digits.length() == 10) {
            return digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6);
        }
        return msisdn;
    }

    private void styleStatus(TextView view, String status) {
        String s = status == null ? "" : status.toLowerCase(Locale.US);
        int bg, fg;
        String label;
        if (s.contains("fail") || s.contains("error") || s.contains("declin")) {
            bg = R.color.danger_bg;
            fg = R.color.danger;
            label = "Failed";
        } else if (s.contains("success") || s.contains("complete") || s.contains("paid")) {
            bg = R.color.success_bg;
            fg = R.color.success;
            label = "Success";
        } else {
            bg = R.color.purple_light;
            fg = R.color.purple_primary;
            label = status == null || status.isEmpty() ? "Pending" : capitalize(status);
        }
        view.setText(label);
        view.setTextColor(ContextCompat.getColor(view.getContext(), fg));
        ViewCompat.setBackgroundTintList(view,
                ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), bg)));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView product;
        final TextView meta;
        final TextView amount;
        final TextView status;

        VH(@NonNull View v) {
            super(v);
            product = v.findViewById(R.id.txnProduct);
            meta = v.findViewById(R.id.txnMeta);
            amount = v.findViewById(R.id.txnAmount);
            status = v.findViewById(R.id.txnStatus);
        }
    }
}
