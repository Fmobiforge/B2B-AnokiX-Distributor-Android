package com.anokix.distributorapp.ui.vas;

import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.VasCustomersData;
import com.anokix.distributorapp.ui.AirtimeActivity;
import com.anokix.distributorapp.ui.adapter.VasCustomerAdapter;

import java.util.List;

/**
 * Airtime &amp; VAS › Customers. The end-customers this distributor has onboarded, with the
 * Limes account number to quote when activating their SIM on the Subscription tab.
 */
public class CustomersSection {

    private static final int LIMIT = 500;

    private final AirtimeActivity activity;
    private final VasCustomerAdapter adapter = new VasCustomerAdapter();

    private final EditText searchInput;
    private final TextView count, empty;
    private final ProgressBar loading;

    private boolean loaded;

    public CustomersSection(AirtimeActivity activity, View root) {
        this.activity = activity;

        searchInput = root.findViewById(R.id.customerSearchInput);
        count = root.findViewById(R.id.customerCount);
        empty = root.findViewById(R.id.customerEmpty);
        loading = root.findViewById(R.id.customerLoading);

        RecyclerView list = root.findViewById(R.id.customerList);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(adapter);

        root.findViewById(R.id.customerRefresh).setOnClickListener(v -> load());
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                load();
                return true;
            }
            return false;
        });
    }

    /** Loads once when the tab is first opened; later visits reuse what is on screen. */
    public void onShown() {
        if (!loaded) load();
    }

    /** Forces a reload — used after an onboard so the new customer appears straight away. */
    public void reload() {
        loaded = false;
        if (activity.isSectionVisible(AirtimeActivity.TAB_CUSTOMERS)) load();
    }

    private void load() {
        loaded = true;
        loading.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        String search = searchInput.getText() == null ? "" : searchInput.getText().toString().trim();

        ApiClient.get(activity).getVasCustomers(search, LIMIT, new ApiCallback<VasCustomersData>() {
            @Override
            public void onSuccess(VasCustomersData data) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                loading.setVisibility(View.GONE);
                List<VasCustomersData.Customer> customers = data == null ? null : data.customers;
                adapter.setItems(customers);
                int total = customers == null ? 0 : customers.size();
                count.setText(activity.getResources()
                        .getQuantityString(R.plurals.vas_customer_count, total, total));
                empty.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                loading.setVisibility(View.GONE);
                adapter.setItems(null);
                count.setText("");
                empty.setVisibility(View.VISIBLE);
                VasUi.showError(activity, message);
            }
        });
    }
}
