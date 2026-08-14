package com.anokix.distributorapp.ui.vas;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.VasCustomersData;
import com.anokix.distributorapp.network.dto.VasSelectCustomerData;
import com.anokix.distributorapp.ui.AirtimeActivity;
import com.anokix.distributorapp.ui.VasFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * The "Acting as" bar shared by Subscription, Catalog &amp; orders and Dynamic services.
 *
 * <p>Limes issues its token per customer, so those three tabs cannot call anything until the
 * distributor has picked one of their onboarded customers and {@code select-customer} has come
 * back approved. Only customers with status {@code ACT} are offered — Limes refuses to
 * authenticate as anyone else — and the approved {@code customer_id} then travels with every
 * request those tabs make.
 */
public class ActingAsSelector {

    private static final int LIMIT = 500;

    /** Told when the approved customer changes (including back to none). */
    public interface Listener {
        void onActingCustomerChanged(VasSelectCustomerData.Customer customer);
    }

    private final AirtimeActivity activity;
    private final Listener listener;

    private final View root, picker, field, empty;
    private final TextView label, note, chip;
    private final ProgressBar loading;

    private final List<VasCustomersData.Customer> customers = new ArrayList<>();

    private VasCustomersData.Customer pending;
    private VasSelectCustomerData.Customer approved;
    private boolean customersLoaded;
    private boolean busy;

    public ActingAsSelector(AirtimeActivity activity, View root, Listener listener) {
        this.activity = activity;
        this.root = root;
        this.listener = listener;

        picker = root.findViewById(R.id.actingAsPicker);
        field = root.findViewById(R.id.actingAsField);
        empty = root.findViewById(R.id.actingAsEmpty);
        label = root.findViewById(R.id.actingAsLabel);
        note = root.findViewById(R.id.actingAsNote);
        chip = root.findViewById(R.id.actingAsChip);
        loading = root.findViewById(R.id.actingAsLoading);

        field.setOnClickListener(v -> showPicker());
        root.findViewById(R.id.actingAsOnboardLink)
                .setOnClickListener(v -> activity.openTab(AirtimeActivity.TAB_ONBOARD));
    }

    // ---- State ------------------------------------------------------------

    /** The approved customer's id, or "" while no customer has been authenticated. */
    public String customerId() {
        return approved == null || approved.id == null ? "" : approved.id;
    }

    public boolean hasCustomer() {
        return !customerId().isEmpty();
    }

    /** Shown with the three customer-scoped tabs; hidden everywhere else. */
    public void setVisible(boolean visible) {
        root.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible && !customersLoaded) loadCustomers(false);
    }

    /**
     * A customer was just onboarded, so the picker list is stale. The new customer is not
     * active on Limes yet, but the operator will expect to find them once they are.
     */
    public void reloadCustomers() {
        customersLoaded = false;
        if (root.getVisibility() == View.VISIBLE) loadCustomers(false);
    }

    // ---- Customers --------------------------------------------------------

    private void loadCustomers(boolean openPickerWhenDone) {
        if (busy) return;
        busy = true;
        customersLoaded = true;
        setLoading(true, R.string.vas_acting_as_loading);

        ApiClient.get(activity).getVasCustomers("", LIMIT, true,
                new ApiCallback<VasCustomersData>() {
                    @Override
                    public void onSuccess(VasCustomersData data) {
                        if (gone()) return;
                        busy = false;
                        setLoading(false, 0);
                        customers.clear();
                        if (data != null && data.customers != null) {
                            // The endpoint is asked for active customers only; the filter is
                            // repeated here so a server that ignores the flag cannot offer a
                            // customer Limes would refuse to authenticate as.
                            for (VasCustomersData.Customer c : data.customers) {
                                if (isActive(c)) customers.add(c);
                            }
                        }
                        // A customer that is no longer active must not stay selected.
                        if (approved != null && findById(approved.id) == null) clearSelection();
                        render();
                        if (openPickerWhenDone) showPicker();
                    }

                    @Override
                    public void onError(String message) {
                        if (gone()) return;
                        busy = false;
                        customersLoaded = false;
                        setLoading(false, 0);
                        render();
                        VasUi.showError(activity, message);
                    }
                });
    }

    private static boolean isActive(VasCustomersData.Customer c) {
        String s = c == null || c.status == null ? "" : c.status.trim();
        return s.equalsIgnoreCase("ACT") || s.equalsIgnoreCase("ACTIVE");
    }

    private VasCustomersData.Customer findById(String id) {
        if (id == null) return null;
        for (VasCustomersData.Customer c : customers) {
            if (id.equals(c.id)) return c;
        }
        return null;
    }

    private void showPicker() {
        if (busy) return;
        if (customers.isEmpty()) {
            // Either the list has never loaded or it failed — the operator tapped because they
            // want the options, so go and fetch them and open the picker on the way back.
            loadCustomers(true);
            return;
        }
        String[] labels = new String[customers.size()];
        for (int i = 0; i < customers.size(); i++) labels[i] = optionLabel(customers.get(i));
        new AlertDialog.Builder(activity)
                .setTitle(R.string.vas_acting_as_picker_title)
                .setItems(labels, (d, which) -> select(customers.get(which)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** "Mr Tam Ho · 023 123 1132" — name plus the number, as on the web portal. */
    private String optionLabel(VasCustomersData.Customer c) {
        String phone = VasFormat.msisdn(c.phone);
        return phone.isEmpty() ? c.displayName() : c.displayName() + " · " + phone;
    }

    // ---- select-customer --------------------------------------------------

    private void select(VasCustomersData.Customer customer) {
        if (customer == null || busy) return;
        pending = customer;
        busy = true;
        // Show the pick immediately, then say what is happening — the call is a round trip to
        // Limes and the operator should see which name they chose while it runs.
        VasUi.setDropdownValue(activity, label, optionLabel(customer));
        chip.setVisibility(View.GONE);
        setLoading(true, R.string.vas_acting_as_authenticating);

        ApiClient.get(activity).selectVasCustomer(customer.id,
                new ApiCallback<VasSelectCustomerData>() {
                    @Override
                    public void onSuccess(VasSelectCustomerData data) {
                        if (gone()) return;
                        busy = false;
                        setLoading(false, 0);
                        if (data == null || !data.authenticated) {
                            // Approval is what unlocks the tabs; anything else leaves them shut.
                            clearSelection();
                            render();
                            VasUi.showError(activity,
                                    activity.getString(R.string.vas_acting_as_failed));
                            return;
                        }
                        approved = data.customer != null ? data.customer : fallbackCustomer();
                        if (approved.id == null || approved.id.trim().isEmpty()) {
                            approved.id = pending.id;
                        }
                        chip.setText(data.subjectLabel());
                        chip.setVisibility(data.subjectLabel().isEmpty() ? View.GONE : View.VISIBLE);
                        render();
                        listener.onActingCustomerChanged(approved);
                    }

                    @Override
                    public void onError(String message) {
                        if (gone()) return;
                        busy = false;
                        setLoading(false, 0);
                        clearSelection();
                        render();
                        VasUi.showError(activity, message);
                    }
                });
    }

    /** Stands in when the response omits the customer block — the picked row is what we know. */
    private VasSelectCustomerData.Customer fallbackCustomer() {
        VasSelectCustomerData.Customer c = new VasSelectCustomerData.Customer();
        c.id = pending.id;
        c.firstname = pending.firstname;
        c.lastname = pending.lastname;
        c.email = pending.email;
        c.phone = pending.phone;
        c.status = pending.status;
        c.limes_account_id = pending.limes_account_id;
        return c;
    }

    private void clearSelection() {
        boolean had = approved != null;
        approved = null;
        chip.setVisibility(View.GONE);
        label.setText(R.string.vas_acting_as_hint);
        label.setTextColor(androidx.core.content.ContextCompat.getColor(
                activity, R.color.text_secondary));
        if (had) listener.onActingCustomerChanged(null);
    }

    // ---- Rendering --------------------------------------------------------

    private void render() {
        boolean none = customers.isEmpty() && approved == null;
        picker.setVisibility(none ? View.GONE : View.VISIBLE);
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        note.setVisibility(approved == null ? View.VISIBLE : View.GONE);
        note.setText(R.string.vas_acting_as_note);
    }

    private void setLoading(boolean active, int noteRes) {
        loading.setVisibility(active ? View.VISIBLE : View.GONE);
        field.setEnabled(!active);
        if (active) {
            note.setVisibility(View.VISIBLE);
            note.setText(noteRes);
        }
    }

    private boolean gone() {
        return activity.isFinishing() || activity.isDestroyed();
    }
}
