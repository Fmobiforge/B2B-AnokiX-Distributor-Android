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
import com.anokix.distributorapp.network.dto.VasCustomersData;
import com.anokix.distributorapp.ui.VasFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** End-customers onboarded with Limes (name, ID/phone, email, Limes account no. + status). */
public class VasCustomerAdapter extends RecyclerView.Adapter<VasCustomerAdapter.VH> {

    private final List<VasCustomersData.Customer> items = new ArrayList<>();

    public void setItems(List<VasCustomersData.Customer> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vas_customer, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VasCustomersData.Customer c = items.get(position);
        h.name.setText(c.displayName());

        StringBuilder meta = new StringBuilder();
        if (c.id_number != null && !c.id_number.isEmpty()) meta.append(c.id_number);
        String phone = VasFormat.msisdn(c.phone);
        if (!phone.isEmpty()) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append(phone);
        }
        h.meta.setText(meta.length() == 0 ? "—" : meta.toString());

        h.email.setText(c.email == null ? "" : c.email);
        h.email.setVisibility(c.email == null || c.email.isEmpty() ? View.GONE : View.VISIBLE);

        boolean hasAccount = c.limes_account_id != null && !c.limes_account_id.isEmpty();
        h.account.setVisibility(hasAccount ? View.VISIBLE : View.GONE);
        if (hasAccount) {
            h.account.setText(h.account.getContext()
                    .getString(R.string.vas_customer_account, c.limes_account_id));
        }
        styleStatus(h.status, c.status);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Limes status codes: ACT is live, anything else is still pending or suspended. */
    private void styleStatus(TextView view, String status) {
        String s = status == null ? "" : status.trim();
        boolean active = s.equalsIgnoreCase("ACT") || s.equalsIgnoreCase("ACTIVE");
        int fg = active ? R.color.success : R.color.warning;
        int bg = active ? R.color.success_bg : R.color.warning_bg;
        view.setText(s.isEmpty() ? "—" : s.toUpperCase(Locale.US));
        view.setTextColor(ContextCompat.getColor(view.getContext(), fg));
        ViewCompat.setBackgroundTintList(view,
                ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), bg)));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, meta, email, account, status;

        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.customerName);
            meta = v.findViewById(R.id.customerMeta);
            email = v.findViewById(R.id.customerEmailText);
            account = v.findViewById(R.id.customerAccount);
            status = v.findViewById(R.id.customerStatus);
        }
    }
}
