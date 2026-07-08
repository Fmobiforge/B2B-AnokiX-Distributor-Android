package com.anokix.distributorapp.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.graphics.Typeface;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.VasCategoriesData;
import com.anokix.distributorapp.network.dto.VasProductsData;
import com.anokix.distributorapp.network.dto.VasTransactionsData;
import com.anokix.distributorapp.ui.adapter.VasCategoryAdapter;
import com.anokix.distributorapp.ui.adapter.VasProductAdapter;
import com.anokix.distributorapp.ui.adapter.VasTransactionAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Airtime & VAS (Limes, live). Flattens the VAS category tree to selectable leaves
 * (horizontal scroll, expandable to a grid), loads that category's products, and mirrors
 * the web portal's "Order summary" — recipient, optional custom amount for ad-hoc
 * products, reference, and a wallet-paid "Buy now". Recent transactions + KPI summary
 * come from {@code api/common/vas/transactions}.
 */
public class AirtimeActivity extends AppCompatActivity {

    private static final int PRODUCT_LIMIT = 60;
    private static final int TXN_PER_PAGE = 20;

    private VasCategoryAdapter categoryAdapter;
    private VasProductAdapter productAdapter;
    private VasTransactionAdapter txnAdapter;

    private RecyclerView categoryList;
    private ProgressBar categoryLoading, productLoading;
    private TextView categoryToggle, productCategoryLabel, productEmpty, txnEmpty;
    private TextView kpiSpendToday, kpiTxnToday, kpiMonth, kpiWallet;
    private TextView summaryProductName, summaryProductAmount, summaryServiceFee, summaryCommission, summaryTotal;
    private TextView referenceCounter;
    private android.widget.EditText recipientInput, customAmountInput, referenceInput;
    private View customAmountBlock;
    private MaterialButton buyButton;

    private VasCategoriesData.Node selectedCategory;
    private VasProductsData.Product selectedProduct;
    private boolean categoriesExpanded;
    /** Distributor VAS commission rate from api/common/vas/categories (e.g. 0.05 = 5%). */
    private double commissionRate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_airtime);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        setupLists();
        setupInputs();

        loadTransactions();
        loadCategories();
    }

    private void bindViews() {
        categoryList = findViewById(R.id.categoryList);
        categoryLoading = findViewById(R.id.categoryLoading);
        productLoading = findViewById(R.id.productLoading);
        categoryToggle = findViewById(R.id.categoryToggle);
        productCategoryLabel = findViewById(R.id.productCategoryLabel);
        productEmpty = findViewById(R.id.productEmpty);
        txnEmpty = findViewById(R.id.txnEmpty);

        kpiSpendToday = findViewById(R.id.kpiSpendToday);
        kpiTxnToday = findViewById(R.id.kpiTxnToday);
        kpiMonth = findViewById(R.id.kpiMonth);
        kpiWallet = findViewById(R.id.kpiWallet);

        summaryProductName = findViewById(R.id.summaryProductName);
        summaryProductAmount = findViewById(R.id.summaryProductAmount);
        summaryServiceFee = findViewById(R.id.summaryServiceFee);
        summaryCommission = findViewById(R.id.summaryCommission);
        summaryTotal = findViewById(R.id.summaryTotal);

        recipientInput = findViewById(R.id.recipientInput);
        customAmountInput = findViewById(R.id.customAmountInput);
        referenceInput = findViewById(R.id.referenceInput);
        referenceCounter = findViewById(R.id.referenceCounter);
        customAmountBlock = findViewById(R.id.customAmountBlock);
        buyButton = findViewById(R.id.buyButton);
    }

    private void setupLists() {
        categoryAdapter = new VasCategoryAdapter(this::selectCategory);
        categoryList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryList.setAdapter(categoryAdapter);

        productAdapter = new VasProductAdapter(this::selectProduct);
        RecyclerView productList = findViewById(R.id.productList);
        productList.setLayoutManager(new GridLayoutManager(this, 2));
        productList.setAdapter(productAdapter);

        txnAdapter = new VasTransactionAdapter();
        RecyclerView txnList = findViewById(R.id.txnList);
        txnList.setLayoutManager(new LinearLayoutManager(this));
        txnList.setAdapter(txnAdapter);

        categoryToggle.setOnClickListener(v -> toggleCategories());
    }

    private void setupInputs() {
        SimpleWatcher summaryWatcher = new SimpleWatcher(this::updateSummary);
        recipientInput.addTextChangedListener(summaryWatcher);
        customAmountInput.addTextChangedListener(summaryWatcher);
        referenceInput.addTextChangedListener(new SimpleWatcher(() ->
                referenceCounter.setText(getString(R.string.vas_ref_counter,
                        referenceInput.getText().length()))));

        buyButton.setOnClickListener(v -> purchase());
        // Buy Now stays enabled so a tap always validates + prompts; only disabled mid-request.
        buyButton.setEnabled(true);
        updateSummary();
    }

    // ---- Categories ------------------------------------------------------

    private void loadCategories() {
        categoryLoading.setVisibility(View.VISIBLE);
        categoryList.setVisibility(View.GONE);
        ApiClient.get(this).getVasCategories(new ApiCallback<VasCategoriesData>() {
            @Override
            public void onSuccess(VasCategoriesData data) {
                if (isFinishing() || isDestroyed()) return;
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
                if (isFinishing() || isDestroyed()) return;
                categoryLoading.setVisibility(View.GONE);
                categoryList.setVisibility(View.VISIBLE);
                Toast.makeText(AirtimeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleCategories() {
        categoriesExpanded = !categoriesExpanded;
        categoryToggle.setText(categoriesExpanded ? R.string.vas_show_less : R.string.vas_view_all);
        categoryList.setLayoutManager(categoriesExpanded
                ? new GridLayoutManager(this, 3)
                : new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
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
        String poweredBy = " powered by ";
        String limes = "Limes";
        SpannableStringBuilder sb = new SpannableStringBuilder();

        int start = sb.length();
        sb.append(name);
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_primary)),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = sb.length();
        sb.append(poweredBy);
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_secondary)),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = sb.length();
        sb.append(limes);
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.success)),
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
        ApiClient.get(this).getVasProducts(categoryId, 1, PRODUCT_LIMIT,
                new ApiCallback<VasProductsData>() {
                    @Override
                    public void onSuccess(VasProductsData data) {
                        if (isFinishing() || isDestroyed()) return;
                        productLoading.setVisibility(View.GONE);
                        List<VasProductsData.Product> products = data == null ? null : data.products;
                        productAdapter.setItems(products);
                        productEmpty.setVisibility(
                                products == null || products.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) return;
                        productLoading.setVisibility(View.GONE);
                        productEmpty.setVisibility(View.VISIBLE);
                        Toast.makeText(AirtimeActivity.this, message, Toast.LENGTH_SHORT).show();
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
            String raw = customAmountInput.getText().toString().trim();
            try {
                return raw.isEmpty() ? 0 : Double.parseDouble(raw);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return selectedProduct.price;
    }

    private void updateSummary() {
        double amount = currentAmount();
        double serviceFee = 0; // No per-product fee in the VAS feed — wallet-paid at face value.
        double total = amount + serviceFee;

        if (selectedProduct == null) {
            summaryProductName.setText(R.string.vas_no_product_selected);
            summaryProductName.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            summaryProductAmount.setText("");
        } else {
            summaryProductName.setText(selectedProduct.name);
            summaryProductName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
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
     * offending field and return false. Buy Now stays clickable so tapping it always
     * explains what is missing rather than silently doing nothing.
     */
    private boolean validateOrder() {
        if (selectedProduct == null) {
            showValidation(R.string.vas_err_select_product, null);
            return false;
        }
        String msisdn = recipientInput.getText().toString().trim();
        if (msisdn.isEmpty()) {
            showValidation(R.string.vas_err_recipient_empty, recipientInput);
            return false;
        }
        if (!isValidMsisdn(msisdn)) {
            showValidation(R.string.vas_err_recipient_invalid, recipientInput);
            return false;
        }
        if (currentAmount() <= 0) {
            showValidation(R.string.vas_err_amount,
                    selectedProduct.isAdHoc ? customAmountInput : null);
            return false;
        }
        return true;
    }

    /** True for a SA mobile number as 10 digits (0xxxxxxxxx) or 11 digits (27xxxxxxxxx). */
    private boolean isValidMsisdn(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        return (digits.length() == 10 && digits.startsWith("0"))
                || (digits.length() == 11 && digits.startsWith("27"));
    }

    private void showValidation(int messageRes, @Nullable android.widget.EditText field) {
        if (field != null) {
            field.setError(getString(messageRes));
            field.requestFocus();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.vas_validation_title)
                .setMessage(messageRes)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void purchase() {
        if (!validateOrder() || selectedCategory == null) return;
        String msisdn = recipientInput.getText().toString().trim();
        double amount = currentAmount();

        buyButton.setEnabled(false);
        buyButton.setText(R.string.processing);
        final VasProductsData.Product product = selectedProduct;
        ApiClient.get(this).purchaseVas(product.id, msisdn, amount, product.sku, product.name,
                selectedCategory.name, new ApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        if (isFinishing() || isDestroyed()) return;
                        buyButton.setText(R.string.vas_buy_now);
                        buyButton.setEnabled(true);
                        onPurchaseComplete();
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) return;
                        buyButton.setText(R.string.vas_buy_now);
                        buyButton.setEnabled(true);
                        updateSummary();
                        new AlertDialog.Builder(AirtimeActivity.this)
                                .setTitle(R.string.vas_buy_now)
                                .setMessage(message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                });
    }

    private void onPurchaseComplete() {
        new AlertDialog.Builder(this)
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
        loadTransactions();
    }

    // ---- Transactions + KPI summary --------------------------------------

    private void loadTransactions() {
        ApiClient.get(this).getVasTransactions(1, TXN_PER_PAGE,
                new ApiCallback<VasTransactionsData>() {
                    @Override
                    public void onSuccess(VasTransactionsData data) {
                        if (isFinishing() || isDestroyed()) return;
                        if (data != null && data.summary != null) bindSummary(data.summary);
                        List<VasTransactionsData.Transaction> txns = data == null ? null : data.transactions;
                        txnAdapter.setItems(txns);
                        txnEmpty.setVisibility(txns == null || txns.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) return;
                        txnEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void bindSummary(VasTransactionsData.Summary s) {
        kpiSpendToday.setText(VasFormat.money(s.sales_today));
        kpiTxnToday.setText(String.valueOf((int) s.count_today));
        kpiMonth.setText(VasFormat.money(s.commission_month));
        kpiWallet.setText(VasFormat.money(s.wallet_balance));
    }

    /** Minimal TextWatcher that runs a callback on every change. */
    private static class SimpleWatcher implements TextWatcher {
        private final Runnable onChange;

        SimpleWatcher(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            onChange.run();
        }
    }
}
