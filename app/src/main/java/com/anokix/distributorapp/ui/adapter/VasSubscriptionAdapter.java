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
import com.anokix.distributorapp.network.dto.VasSubscriptionsData;
import com.anokix.distributorapp.ui.VasFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SIMs this distributor has activated. A row leads with the MSISDN Limes assigned, because
 * that number — not the ICCID — is what later top-ups are sold to; SIMs activated while the
 * tenant number pool was empty show "Number pending" until a sync recovers it.
 */
public class VasSubscriptionAdapter extends RecyclerView.Adapter<VasSubscriptionAdapter.VH> {

    private final List<VasSubscriptionsData.Subscription> items = new ArrayList<>();

    public void setItems(List<VasSubscriptionsData.Subscription> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vas_subscription, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VasSubscriptionsData.Subscription s = items.get(position);
        boolean hasNumber = s.msisdn != null && !s.msisdn.trim().isEmpty();
        h.msisdn.setText(hasNumber
                ? VasFormat.msisdn(s.msisdn)
                : h.msisdn.getContext().getString(R.string.vas_msisdn_pending));

        String iccid = s.iccid == null || s.iccid.isEmpty() ? "eSIM" : "ICCID " + s.iccid;
        h.meta.setText(iccid);
        styleStatus(h.status, s.status);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

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
        final TextView msisdn, meta, status;

        VH(@NonNull View v) {
            super(v);
            msisdn = v.findViewById(R.id.simMsisdn);
            meta = v.findViewById(R.id.simMeta);
            status = v.findViewById(R.id.simStatus);
        }
    }
}
