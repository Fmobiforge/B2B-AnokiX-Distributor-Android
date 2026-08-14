package com.anokix.distributorapp.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.VasTransactionsData;
import com.anokix.distributorapp.ui.adapter.VasTransactionAdapter;
import com.anokix.distributorapp.ui.vas.ActingAsSelector;
import com.anokix.distributorapp.ui.vas.CatalogSection;
import com.anokix.distributorapp.ui.vas.CustomersSection;
import com.anokix.distributorapp.ui.vas.DynamicServicesSection;
import com.anokix.distributorapp.ui.vas.OnboardSection;
import com.anokix.distributorapp.ui.vas.SubscriptionSection;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

/**
 * Airtime &amp; VAS (Limes, live). Mirrors the web portal: a KPI strip and an explainer
 * that always stay on screen, then one of six sections at a time —
 * <ol>
 *   <li><b>Onboard customer</b> — register an end-customer in the Limes CRM</li>
 *   <li><b>Customers</b> — who you have onboarded, with their Limes account no.</li>
 *   <li><b>Subscription</b> — assign a SIM; Limes hands back the phone number</li>
 *   <li><b>Catalog &amp; orders</b> — sell a catalog product to a number</li>
 *   <li><b>Dynamic services</b> — compose an exact-value bundle</li>
 *   <li><b>Recent transactions</b> — the VAS history + KPI figures</li>
 * </ol>
 * Only onboarding + subscription need each other; airtime and data top-ups go straight to
 * any number that is already active on the network.
 *
 * <p>Limes issues its token per customer, so Subscription, Catalog &amp; orders and Dynamic
 * services run behind the shared "Acting as" bar: nothing on those three tabs is shown or
 * called until one of the distributor's active customers has been authenticated, and every
 * request they then make carries that {@code customer_id}.
 */
public class AirtimeActivity extends AppCompatActivity {

    public static final int TAB_ONBOARD = 0;
    public static final int TAB_CUSTOMERS = 1;
    public static final int TAB_SUBSCRIPTION = 2;
    public static final int TAB_CATALOG = 3;
    public static final int TAB_DYNAMIC = 4;
    public static final int TAB_TRANSACTIONS = 5;

    private static final int TXN_PER_PAGE = 20;

    private static final int[] TAB_TITLES = {
            R.string.vas_tab_onboard,
            R.string.vas_tab_customers,
            R.string.vas_tab_subscription,
            R.string.vas_tab_catalog,
            R.string.vas_tab_dynamic,
            R.string.vas_tab_transactions
    };

    private final View[] sections = new View[TAB_TITLES.length];
    private final TextView[] tabs = new TextView[TAB_TITLES.length];

    private CustomersSection customersSection;
    private SubscriptionSection subscriptionSection;
    private CatalogSection catalogSection;

    /** Shared by the three customer-scoped tabs — one pick serves all of them. */
    private ActingAsSelector actingAs;
    private View gateCard;

    private VasTransactionAdapter txnAdapter;
    private TextView txnEmpty;
    private TextView kpiSpendToday, kpiTxnToday, kpiMonth, kpiWallet;

    private int currentTab = TAB_ONBOARD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_airtime);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindHeader();
        bindSections();
        buildTabs();
        selectTab(TAB_ONBOARD);

        loadTransactions();
    }

    private void bindHeader() {
        kpiSpendToday = findViewById(R.id.kpiSpendToday);
        kpiTxnToday = findViewById(R.id.kpiTxnToday);
        kpiMonth = findViewById(R.id.kpiMonth);
        kpiWallet = findViewById(R.id.kpiWallet);

        // The explainer carries <b> markup so each step name stands out, as on the web.
        TextView howItWorks = findViewById(R.id.vasHowItWorks);
        howItWorks.setText(HtmlCompat.fromHtml(getString(R.string.vas_how_it_works),
                HtmlCompat.FROM_HTML_MODE_COMPACT));
    }

    private void bindSections() {
        sections[TAB_ONBOARD] = findViewById(R.id.sectionOnboard);
        sections[TAB_CUSTOMERS] = findViewById(R.id.sectionCustomers);
        sections[TAB_SUBSCRIPTION] = findViewById(R.id.sectionSubscription);
        sections[TAB_CATALOG] = findViewById(R.id.sectionCatalog);
        sections[TAB_DYNAMIC] = findViewById(R.id.sectionDynamic);
        sections[TAB_TRANSACTIONS] = findViewById(R.id.sectionTransactions);

        // Onboard and Dynamic services are self-contained — they wire their own views and
        // are kept alive by those listeners, so only the sections this activity calls back
        // into are held as fields.
        new OnboardSection(this, sections[TAB_ONBOARD]);
        new DynamicServicesSection(this, sections[TAB_DYNAMIC]);
        customersSection = new CustomersSection(this, sections[TAB_CUSTOMERS]);
        subscriptionSection = new SubscriptionSection(this, sections[TAB_SUBSCRIPTION]);
        catalogSection = new CatalogSection(this, sections[TAB_CATALOG]);

        gateCard = findViewById(R.id.vasGateCard);
        actingAs = new ActingAsSelector(this, findViewById(R.id.vasActingAs),
                customer -> onActingCustomerChanged());

        txnEmpty = sections[TAB_TRANSACTIONS].findViewById(R.id.txnEmpty);
        txnAdapter = new VasTransactionAdapter();
        RecyclerView txnList = sections[TAB_TRANSACTIONS].findViewById(R.id.txnList);
        txnList.setLayoutManager(new LinearLayoutManager(this));
        txnList.setAdapter(txnAdapter);
    }

    // ---- Tabs ------------------------------------------------------------

    /**
     * Builds the pill tab bar. It lives in a HorizontalScrollView because six labels never
     * fit across a phone, and each pill is a plain TextView so the selected state is a
     * single background swap.
     */
    private void buildTabs() {
        LinearLayout strip = findViewById(R.id.vasTabStrip);
        float density = getResources().getDisplayMetrics().density;
        int hPad = (int) (16 * density);
        int vPad = (int) (9 * density);
        int gap = (int) (8 * density);

        for (int i = 0; i < TAB_TITLES.length; i++) {
            // RobotoBoldTextView so the pills carry the same Inter face as the rest of the app.
            TextView tab = new RobotoBoldTextView(this);
            tab.setText(TAB_TITLES[i]);
            tab.setTextSize(13);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(hPad, vPad, hPad, vPad);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.setMarginStart(gap);
            tab.setLayoutParams(lp);

            final int index = i;
            tab.setOnClickListener(v -> selectTab(index));
            strip.addView(tab);
            tabs[i] = tab;
        }
    }

    private void selectTab(int index) {
        currentTab = index;
        boolean gated = isCustomerScoped(index);
        boolean unlocked = !gated || actingAs.hasCustomer();

        for (int i = 0; i < tabs.length; i++) {
            boolean selected = i == index;
            tabs[i].setBackgroundResource(
                    selected ? R.drawable.bg_vas_tab_active : R.drawable.bg_filter_pill);
            tabs[i].setTextColor(ContextCompat.getColor(this,
                    selected ? R.color.success : R.color.text_secondary));
            // A gated tab shows the gate card instead of its panel until Limes has approved
            // the customer, so a selected-but-locked section stays hidden.
            sections[i].setVisibility(selected && unlocked ? View.VISIBLE : View.GONE);
        }
        actingAs.setVisible(gated);
        gateCard.setVisibility(gated && !unlocked ? View.VISIBLE : View.GONE);

        // Sections fetch their own data the first time they are opened, so switching tabs
        // never costs a request that the operator did not ask for.
        if (!unlocked) return;
        switch (index) {
            case TAB_CUSTOMERS:
                customersSection.onShown();
                break;
            case TAB_SUBSCRIPTION:
                subscriptionSection.onShown();
                break;
            case TAB_CATALOG:
                catalogSection.onShown();
                break;
            default:
                break;
        }
    }

    /** The three tabs Limes runs on the customer's own token, so they need "Acting as" first. */
    private static boolean isCustomerScoped(int tab) {
        return tab == TAB_SUBSCRIPTION || tab == TAB_CATALOG || tab == TAB_DYNAMIC;
    }

    /** True while {@code tab} is the section on screen — used to skip pointless reloads. */
    public boolean isSectionVisible(int tab) {
        return currentTab == tab;
    }

    /** Switches tabs from inside a section (e.g. "Onboard a customer" on the Acting as bar). */
    public void openTab(int tab) {
        selectTab(tab);
    }

    // ---- Acting as -------------------------------------------------------

    /**
     * The customer every request on the three scoped tabs is made for, or "" while none has
     * been authenticated. Sections read it at call time so they always send the current pick.
     */
    public String actingCustomerId() {
        return actingAs == null ? "" : actingAs.customerId();
    }

    /**
     * A different customer was approved (or the pick was cleared): everything those tabs hold
     * belongs to the previous customer, so it is dropped and re-read on their token.
     */
    private void onActingCustomerChanged() {
        subscriptionSection.onCustomerChanged();
        catalogSection.onCustomerChanged();
        selectTab(currentTab);
    }

    // ---- Cross-section callbacks -----------------------------------------

    /** A new customer was onboarded: the Customers tab must not show a stale list. */
    public void onCustomerOnboarded() {
        customersSection.reload();
        actingAs.reloadCustomers();
    }

    /** Re-reads the VAS history + KPI figures after any sale or provisioning. */
    public void refreshTransactions() {
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

    /** True for a SA mobile number as 10 digits (0xxxxxxxxx) or 11 digits (27xxxxxxxxx). */
    public static boolean isValidMsisdn(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("[^0-9]", "");
        return (digits.length() == 10 && digits.startsWith("0"))
                || (digits.length() == 11 && digits.startsWith("27"));
    }
}
