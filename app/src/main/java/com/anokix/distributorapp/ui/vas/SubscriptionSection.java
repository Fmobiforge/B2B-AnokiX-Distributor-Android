package com.anokix.distributorapp.ui.vas;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.VasSubscriptionProductsData;
import com.anokix.distributorapp.network.dto.VasSubscriptionsData;
import com.anokix.distributorapp.ui.AirtimeActivity;
import com.anokix.distributorapp.ui.adapter.VasSubscriptionAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Airtime &amp; VAS › Subscription. Assigns a SIM to a subscription product; Limes replies
 * with the MSISDN, which is the number every later top-up is sold to. eSIMs carry no
 * printed serial, so switching eSIM on hides the ICCID block and sends {@code e_sim=1}.
 */
public class SubscriptionSection {

    private final AirtimeActivity activity;
    private final VasSubscriptionAdapter simAdapter = new VasSubscriptionAdapter();

    private final View productField, iccidBlock;
    private final TextView productLabel, simEmpty;
    private final EditText iccidInput;
    private final com.anokix.distributorapp.ui.views.RobotoSwitchCompat esimSwitch;
    private final MaterialButton createButton;

    private final List<VasSubscriptionProductsData.Product> products = new ArrayList<>();
    private VasSubscriptionProductsData.Product selectedProduct;
    private boolean loaded;

    public SubscriptionSection(AirtimeActivity activity, View root) {
        this.activity = activity;

        productField = root.findViewById(R.id.subProductField);
        productLabel = root.findViewById(R.id.subProductLabel);
        iccidBlock = root.findViewById(R.id.subIccidBlock);
        iccidInput = root.findViewById(R.id.subIccidInput);
        esimSwitch = root.findViewById(R.id.subEsimSwitch);
        createButton = root.findViewById(R.id.subCreateButton);
        simEmpty = root.findViewById(R.id.simEmpty);

        RecyclerView simList = root.findViewById(R.id.simList);
        simList.setLayoutManager(new LinearLayoutManager(activity));
        simList.setAdapter(simAdapter);

        // An eSIM has no ICCID to hand in — hide the field rather than ask for a serial
        // that does not exist.
        esimSwitch.setOnCheckedChangeListener((b, checked) ->
                iccidBlock.setVisibility(checked ? View.GONE : View.VISIBLE));

        productField.setOnClickListener(v -> showProductPicker());
        createButton.setOnClickListener(v -> subscribe());
    }

    public void onShown() {
        if (loaded) return;
        loaded = true;
        loadProducts();
        loadSubscriptions();
    }

    /**
     * The distributor is now acting as a different customer (or none): the products and the
     * activated SIMs on screen belong to the previous one, so they are dropped and re-read on
     * the new customer's token the next time the tab is shown.
     */
    public void onCustomerChanged() {
        loaded = false;
        products.clear();
        selectedProduct = null;
        productLabel.setText(R.string.vas_f_product_hint);
        productLabel.setTextColor(androidx.core.content.ContextCompat.getColor(
                activity, R.color.text_secondary));
        iccidInput.setText("");
        simAdapter.setItems(null);
        simEmpty.setVisibility(View.GONE);
        if (activity.isSectionVisible(AirtimeActivity.TAB_SUBSCRIPTION)
                && !activity.actingCustomerId().isEmpty()) {
            onShown();
        }
    }

    // ---- Products --------------------------------------------------------

    private void loadProducts() {
        ApiClient.get(activity).getVasSubscriptionProducts(activity.actingCustomerId(),
                new ApiCallback<VasSubscriptionProductsData>() {
                    @Override
                    public void onSuccess(VasSubscriptionProductsData data) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        products.clear();
                        if (data != null && data.products != null) products.addAll(data.products);
                        // Default to the first product so the form is usable in one tap.
                        if (!products.isEmpty()) selectProduct(0);
                    }

                    @Override
                    public void onError(String message) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        VasUi.showError(activity, message);
                    }
                });
    }

    private void showProductPicker() {
        if (products.isEmpty()) {
            // Nothing to choose from means the first fetch failed or is still running —
            // the operator tapped because they want options, so go and get them.
            loadProducts();
            return;
        }
        String[] labels = new String[products.size()];
        for (int i = 0; i < products.size(); i++) labels[i] = products.get(i).label();
        new AlertDialog.Builder(activity)
                .setTitle(R.string.vas_f_product)
                .setItems(labels, (d, which) -> selectProduct(which))
                .show();
    }

    private void selectProduct(int index) {
        selectedProduct = products.get(index);
        VasUi.setDropdownValue(activity, productLabel, selectedProduct.label());
    }

    // ---- Activated SIMs --------------------------------------------------

    private void loadSubscriptions() {
        ApiClient.get(activity).getVasSubscriptions(activity.actingCustomerId(),
                new ApiCallback<VasSubscriptionsData>() {
            @Override
            public void onSuccess(VasSubscriptionsData data) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                List<VasSubscriptionsData.Subscription> subs =
                        data == null ? null : data.subscriptions;
                simAdapter.setItems(subs);
                simEmpty.setVisibility(subs == null || subs.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                simAdapter.setItems(null);
                simEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    // ---- Create ----------------------------------------------------------

    private void subscribe() {
        if (selectedProduct == null) {
            VasUi.showValidation(activity, R.string.vas_err_select_subscription_product, null);
            return;
        }
        boolean eSim = esimSwitch.isChecked();
        String iccid = iccidInput.getText() == null ? "" : iccidInput.getText().toString().trim();
        if (!eSim && !isValidIccid(iccid)) {
            VasUi.showValidation(activity, R.string.vas_err_iccid, iccidInput);
            return;
        }
        String customerId = activity.actingCustomerId();
        if (customerId.isEmpty()) {
            VasUi.showValidation(activity, R.string.vas_err_no_acting_customer, null);
            return;
        }

        createButton.setEnabled(false);
        createButton.setText(R.string.processing);
        ApiClient.get(activity).subscribeVas(selectedProduct.id, iccid, eSim, customerId,
                new ApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        resetButton();
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.vas_subscription_success_title)
                                .setMessage(R.string.vas_subscription_success_body)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        iccidInput.setText("");
                        loadSubscriptions();
                    }

                    @Override
                    public void onError(String message) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        resetButton();
                        VasUi.showError(activity, message);
                    }
                });
    }

    private void resetButton() {
        createButton.setEnabled(true);
        createButton.setText(R.string.vas_create_subscription);
    }

    /** ICCIDs printed on SA SIMs are 19–20 digits — a phone number pasted here is a typo. */
    private boolean isValidIccid(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        return digits.length() >= 18 && digits.length() <= 22;
    }
}
