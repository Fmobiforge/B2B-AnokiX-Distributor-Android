package com.anokix.distributorapp.ui.vas;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.VasCategoriesData;
import com.anokix.distributorapp.network.dto.VasProductsData;
import com.anokix.distributorapp.ui.AirtimeActivity;
import com.anokix.distributorapp.ui.VasFormat;
import com.anokix.distributorapp.ui.adapter.VasCategoryAdapter;
import com.anokix.distributorapp.ui.adapter.VasProductAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Airtime &amp; VAS › Catalog &amp; orders. Flattens the VAS category tree to selectable
 * leaves (horizontal scroll, expandable to a grid), loads that category's products, and
 * mirrors the web portal's "Order summary" — recipient, optional custom amount for ad-hoc
 * products, reference, and a wallet-paid "Sell now".
 */
public class CatalogSection {

    private static final int PRODUCT_LIMIT = 60;

    private final AirtimeActivity activity;

    private final VasCategoryAdapter categoryAdapter;
    private final VasProductAdapter productAdapter;

    private final RecyclerView categoryList;
    private final ProgressBar categoryLoading, productLoading;
    private final TextView categoryToggle, productCategoryLabel, productEmpty;
    private final TextView summaryProductName, summaryProductAmount, summaryServiceFee,
            summaryCommission, summaryTotal, referenceCounter;
    private final EditText recipientInput, customAmountInput, referenceInput;
    private final View customAmountBlock;
    private final MaterialButton buyButton;

    private VasCategoriesData.Node selectedCategory;
    private VasProductsData.Product selectedProduct;
    private boolean categoriesExpanded;
    private boolean loaded;
    /** Distributor VAS commission rate from api/common/vas/categories (e.g. 0.05 = 5%). */
    private double commissionRate;

    public CatalogSection(AirtimeActivity activity, View root) {
        this.activity = activity;

        categoryList = root.findViewById(R.id.categoryList);
        categoryLoading = root.findViewById(R.id.categoryLoading);
        productLoading = root.findViewById(R.id.productLoading);
        categoryToggle = root.findViewById(R.id.categoryToggle);
        productCategoryLabel = root.findViewById(R.id.productCategoryLabel);
        productEmpty = root.findViewById(R.id.productEmpty);

        summaryProductName = root.findViewById(R.id.summaryProductName);
        summaryProductAmount = root.findViewById(R.id.summaryProductAmount);
        summaryServiceFee = root.findViewById(R.id.summaryServiceFee);
        summaryCommission = root.findViewById(R.id.summaryCommission);
        summaryTotal = root.findViewById(R.id.summaryTotal);

        recipientInput = root.findViewById(R.id.recipientInput);
        customAmountInput = root.findViewById(R.id.customAmountInput);
        referenceInput = root.findViewById(R.id.referenceInput);
        referenceCounter = root.findViewById(R.id.referenceCounter);
        customAmountBlock = root.findViewById(R.id.customAmountBlock);
        buyButton = root.findViewById(R.id.buyButton);

        categoryAdapter = new VasCategoryAdapter(this::selectCategory);
        categoryList.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        categoryList.setAdapter(categoryAdapter);

        productAdapter = new VasProductAdapter(this::selectProduct);
        RecyclerView productList = root.findViewById(R.id.productList);
        productList.setLayoutManager(new GridLayoutManager(activity, 2));
        productList.setAdapter(productAdapter);

        categoryToggle.setOnClickListener(v -> toggleCategories());

        VasUi.SimpleWatcher summaryWatcher = new VasUi.SimpleWatcher(this::updateSummary);
        recipientInput.addTextChangedListener(summaryWatcher);
        customAmountInput.addTextChangedListener(summaryWatcher);
        referenceInput.addTextChangedListener(new VasUi.SimpleWatcher(() ->
                referenceCounter.setText(activity.getString(R.string.vas_ref_counter,
                        referenceInput.getText().length()))));

        buyButton.setOnClickListener(v -> purchase());
        // Sell now stays enabled so a tap always validates + prompts; only disabled mid-request.
        buyButton.setEnabled(true);
        updateSummary();
    }

    public void onShown() {
        if (loaded) return;
        loaded = true;
        loadCategories();
    }

    /**
     * The distributor is now acting as a different customer (or none): the catalog is read on
     * the customer's token, so what is on screen is dropped and re-read for the new one.
     */
    public void onCustomerChanged() {
        loaded = false;
        selectedCategory = null;
        selectedProduct = null;
        categoryAdapter.setItems(null);
        productAdapter.setItems(null);
        productAdapter.setSelectedId(null);
        productEmpty.setVisibility(View.GONE);
        customAmountBlock.setVisibility(View.GONE);
        customAmountInput.setText("");
        updateSummary();
        if (activity.isSectionVisible(AirtimeActivity.TAB_CATALOG)
                && !activity.actingCustomerId().isEmpty()) {
            onShown();
        }
    }

    // ---- Categories ------------------------------------------------------

    private void loadCategories() {
        categoryLoading.setVisibility(View.VISIBLE);
        categoryList.setVisibility(View.GONE);
        ApiClient.get(activity).getVasCategories(activity.actingCustomerId(),
                new ApiCallback<VasCategoriesData>() {
            @Override
            public void onSuccess(VasCategoriesData data) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                categoryLoading.setVisibility(View.GONE);
                categoryList.setVisibility(View.VISIBLE);
                commissionRate = data == null ? 0 : data.commission_rate;
                List<VasCategoriesData.Node> leaves = data == null ? null : data.leaves();
                categoryAdapter.setItems(leaves);
                if (leaves != null && !leaves.isEmpty()) {
                    selectCategory(leaves.get(0));
                }
            }

            @Override
            public void onError(String message) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                categoryLoading.setVisibility(View.GONE);
                categoryList.setVisibility(View.VISIBLE);
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleCategories() {
        categoriesExpanded = !categoriesExpanded;
        categoryToggle.setText(categoriesExpanded ? R.string.vas_show_less : R.string.vas_view_all);
        categoryList.setLayoutManager(categoriesExpanded
                ? new GridLayoutManager(activity, 3)
                : new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter.setExpanded(categoriesExpanded);
    }

    private void selectCategory(VasCategoriesData.Node node) {
        selectedCategory = node;
        categoryAdapter.setSelectedId(node.id);
        productCategoryLabel.setText(buildPoweredByLabel(node.name));
        // Reset the product selection when switching category.
        selectedProduct = null;
        productAdapter.setSelectedId(null);
        customAmountBlock.setVisibility(View.GONE);
        updateSummary();
        loadProducts(node.id);
    }

    /**
     * Builds the "&lt;Category&gt; powered by Limes" label as three styled parts:
     * the category name (primary text), " powered by " (dark gray) and "Limes" (green).
     */
    private CharSequence buildPoweredByLabel(String categoryName) {
        String name = categoryName == null ? "" : categoryName;
        SpannableStringBuilder sb = new SpannableStringBuilder();

        int start = sb.length();
        sb.append(name);
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(activity, R.color.text_primary)),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = sb.length();
        sb.append(" powered by ");
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(activity, R.color.text_secondary)),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = sb.length();
        sb.append("Limes");
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(activity, R.color.success)),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return sb;
    }

    // ---- Products --------------------------------------------------------

    private void loadProducts(String categoryId) {
        productLoading.setVisibility(View.VISIBLE);
        productEmpty.setVisibility(View.GONE);
        productAdapter.setItems(null);
        ApiClient.get(activity).getVasProducts(categoryId, 1, PRODUCT_LIMIT,
                activity.actingCustomerId(), new ApiCallback<VasProductsData>() {
                    @Override
                    public void onSuccess(VasProductsData data) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        productLoading.setVisibility(View.GONE);
                        List<VasProductsData.Product> products = data == null ? null : data.products;
                        productAdapter.setItems(products);
                        productEmpty.setVisibility(
                                products == null || products.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(String message) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        productLoading.setVisibility(View.GONE);
                        productEmpty.setVisibility(View.VISIBLE);
                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void selectProduct(VasProductsData.Product product) {
        selectedProduct = product;
        productAdapter.setSelectedId(product.id);
        customAmountBlock.setVisibility(product.isAdHoc ? View.VISIBLE : View.GONE);
        updateSummary();
    }

    // ---- Order summary ---------------------------------------------------

    /** The amount that will be charged: ad-hoc → custom input; fixed → the product price. */
    private double currentAmount() {
        if (selectedProduct == null) return 0;
        if (selectedProduct.isAdHoc) {
            return VasFormat.parse(customAmountInput.getText().toString());
        }
        return selectedProduct.price;
    }

    private void updateSummary() {
        double amount = currentAmount();
        double serviceFee = 0; // No per-product fee in the VAS feed — wallet-paid at face value.
        double total = amount + serviceFee;

        if (selectedProduct == null) {
            summaryProductName.setText(R.string.vas_no_product_selected);
            summaryProductName.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
            summaryProductAmount.setText("");
        } else {
            summaryProductName.setText(selectedProduct.name);
            summaryProductName.setTextColor(ContextCompat.getColor(activity, R.color.text_primary));
            summaryProductAmount.setText(VasFormat.money(amount));
        }
        summaryServiceFee.setText(VasFormat.money(serviceFee));
        // Distributor's estimated earning — informational, not added to the wallet-paid total.
        summaryCommission.setText(VasFormat.money(amount * commissionRate));
        summaryTotal.setText(VasFormat.money(total));

        // Clear any validation flag as soon as the buyer edits the fields.
        recipientInput.setError(null);
        customAmountInput.setError(null);
    }

    // ---- Purchase --------------------------------------------------------

    /**
     * Validate the order before purchase; on the first problem, prompt the user, flag the
     * offending field and return false. Sell now stays clickable so tapping it always
     * explains what is missing rather than silently doing nothing.
     */
    private boolean validateOrder() {
        if (selectedProduct == null) {
            VasUi.showValidation(activity, R.string.vas_err_select_product, null);
            return false;
        }
        String msisdn = recipientInput.getText().toString().trim();
        if (msisdn.isEmpty()) {
            VasUi.showValidation(activity, R.string.vas_err_recipient_empty, recipientInput);
            return false;
        }
        if (!AirtimeActivity.isValidMsisdn(msisdn)) {
            VasUi.showValidation(activity, R.string.vas_err_recipient_invalid, recipientInput);
            return false;
        }
        if (currentAmount() <= 0) {
            VasUi.showValidation(activity, R.string.vas_err_amount,
                    selectedProduct.isAdHoc ? customAmountInput : null);
            return false;
        }
        return true;
    }

    private void purchase() {
        if (!validateOrder() || selectedCategory == null) return;
        String customerId = activity.actingCustomerId();
        if (customerId.isEmpty()) {
            VasUi.showValidation(activity, R.string.vas_err_no_acting_customer, null);
            return;
        }
        String msisdn = recipientInput.getText().toString().trim();
        double amount = currentAmount();

        buyButton.setEnabled(false);
        buyButton.setText(R.string.processing);
        final VasProductsData.Product product = selectedProduct;
        ApiClient.get(activity).purchaseVas(product.id, msisdn, amount, product.sku, product.name,
                selectedCategory.name, customerId, new ApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        resetButton();
                        onPurchaseComplete();
                    }

                    @Override
                    public void onError(String message) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        resetButton();
                        updateSummary();
                        VasUi.showError(activity, message);
                    }
                });
    }

    private void resetButton() {
        buyButton.setText(R.string.vas_buy_now);
        buyButton.setEnabled(true);
    }

    private void onPurchaseComplete() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.vas_purchase_success_title)
                .setMessage(R.string.vas_purchase_success_body)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        // Reset the form and refresh the dashboard.
        selectedProduct = null;
        productAdapter.setSelectedId(null);
        customAmountBlock.setVisibility(View.GONE);
        customAmountInput.setText("");
        referenceInput.setText("");
        updateSummary();
        activity.refreshTransactions();
    }
}
