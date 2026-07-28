package com.anokix.distributorapp.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.WalletFormat;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.TopupCreateData;
import com.anokix.distributorapp.network.dto.TopupStatusData;
import com.anokix.distributorapp.network.dto.WalletData;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * anokiX wallet (drawer › anokiX wallet, between Finances and Airtime &amp; VAS).
 * Mobile adaptation of the web layout in newDesign/wallet/wallet02.png.
 *
 * <p>Live data comes from {@code GET api/common/wallet}: the balance hero, the settlement
 * account number and the Recent Transactions list. Top Up Wallet opens the amount dialog
 * (wallet03.png) and drives the real PayCloud flow — {@code topup/create} returns a
 * checkout URL that is opened in the browser, and on the way back the app polls
 * {@code topup/status} until the wallet is credited.
 *
 * <p>The remaining panels (quick actions, recipients, loan offers, statements,
 * microfinance) are the web page's placeholder content — there is no API behind them yet,
 * so they are presented with sample data and answer taps with a "coming soon" note.
 */
public class WalletActivity extends BaseSecondaryActivity {

    /** Where PayCloud sends the user back; matches the deep link declared in the manifest. */
    private static final String RETURN_URL = "anokix://wallet";
    /** Transactions shown before "View All" expands the list. */
    private static final int TXN_PREVIEW_COUNT = 6;
    private static final double TOPUP_MIN = 1;
    private static final double TOPUP_MAX = 500000;
    private static final double[] QUICK_AMOUNTS = {50, 100, 200, 500, 1000};
    private static final String STATE_PENDING_ORDER = "pending_order_no";
    private static final String STATE_BALANCE_HIDDEN = "balance_hidden";

    private WalletData wallet;
    private boolean balanceHidden;
    private boolean showAllTransactions;
    /** Set once a checkout is opened; polled when the user returns to the app. */
    private String pendingOrderNo;

    private SwipeRefreshLayout swipeRefresh;
    private TextView totalBalance, availableBalance, pendingBalance, asOfLabel, walletNotice;
    private TextView settlementAccountNo, settlementAvailable, settlementNext;
    private TextView txnViewAll;
    private ImageView refreshIcon, hideIcon;
    private ViewGroup txnList, txnEmpty, recipientStrip, loanList, statementList;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_wallet;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.wallet_screen_title);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.wallet_screen_subtitle);
    }

    @Override
    protected void bindViews() {
        totalBalance = findViewById(R.id.totalBalance);
        availableBalance = findViewById(R.id.availableBalance);
        pendingBalance = findViewById(R.id.pendingBalance);
        asOfLabel = findViewById(R.id.asOfLabel);
        walletNotice = findViewById(R.id.walletNotice);
        settlementAccountNo = findViewById(R.id.settlementAccountNo);
        settlementAvailable = findViewById(R.id.settlementAvailable);
        settlementNext = findViewById(R.id.settlementNext);
        refreshIcon = findViewById(R.id.refreshIcon);
        hideIcon = findViewById(R.id.hideIcon);
        txnList = findViewById(R.id.txnList);
        txnEmpty = findViewById(R.id.txnEmpty);
        txnViewAll = findViewById(R.id.btnTxnViewAll);
        recipientStrip = findViewById(R.id.recipientStrip);
        loanList = findViewById(R.id.loanList);
        statementList = findViewById(R.id.statementList);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.purple_primary);
        swipeRefresh.setOnRefreshListener(() -> load(true, false));

        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            spinRefreshIcon();
            load(true, false);
        });
        findViewById(R.id.btnHideBalance).setOnClickListener(v -> toggleBalanceVisibility());
        findViewById(R.id.btnTopUp).setOnClickListener(v -> showTopUpDialog());

        txnViewAll.setOnClickListener(v -> {
            showAllTransactions = !showAllTransactions;
            bindTransactions();
        });

        comingSoon(R.id.btnManageSettlement, R.string.wallet_manage);
        comingSoon(R.id.btnSettlementHistory, R.string.wallet_view_settlement_history);
        comingSoon(R.id.btnRecipientsViewAll, R.string.wallet_send_money_quickly);
        comingSoon(R.id.btnLoansViewAll, R.string.wallet_loan_offers);
        comingSoon(R.id.btnStatementsViewAll, R.string.wallet_statements);
        comingSoon(R.id.btnExploreMicrofinance, R.string.wallet_explore_microfinance);
        comingSoon(R.id.btnApplyMicrofinance, R.string.wallet_apply_microfinance);

        buildQuickActions();
        buildRecipients();
        buildLoanOffers();
        buildStatements();
        bindSettlementPlaceholders();
    }

    @Override
    protected void loadData() {
        load(false, true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);   // the PayCloud return deep link (anokix://wallet)
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Survive a rotation while the checkout is still open.
        outState.putString(STATE_PENDING_ORDER, pendingOrderNo);
        outState.putBoolean(STATE_BALANCE_HIDDEN, balanceHidden);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        pendingOrderNo = savedInstanceState.getString(STATE_PENDING_ORDER);
        balanceHidden = savedInstanceState.getBoolean(STATE_BALANCE_HIDDEN);
        hideIcon.setImageResource(balanceHidden ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Covers both routes back from the checkout: the return deep link and the user
        // simply backing out of the browser.
        if (pendingOrderNo != null) {
            pollTopupStatus(pendingOrderNo, 0);
        }
    }

    // ---- Wallet data ---------------------------------------------------------

    private void load(boolean forceRefresh, boolean showSpinner) {
        if (showSpinner) {
            swipeRefresh.setRefreshing(true);
        }
        ApiClient.get(this).getWallet(forceRefresh, new ApiCallback<WalletData>() {
            @Override
            public void onSuccess(WalletData data) {
                swipeRefresh.setRefreshing(false);
                wallet = data;
                bindWallet();
            }

            @Override
            public void onError(String message) {
                swipeRefresh.setRefreshing(false);
                toast(message != null ? message : getString(R.string.biz_wallet_load_failed));
                bindWallet();
            }
        });
    }

    private void bindWallet() {
        WalletData.Balance balance = wallet != null ? wallet.balance : null;
        double current = balance != null ? balance.current : 0;
        double available = balance != null ? balance.available : 0;
        double pending = balance != null ? balance.pending : 0;

        totalBalance.setText(maskable(current));
        availableBalance.setText(maskable(available));
        pendingBalance.setText(maskable(pending));

        String asOf = balance != null ? WalletFormat.asOfTime(balance.as_of) : "";
        asOfLabel.setVisibility(asOf.isEmpty() ? View.GONE : View.VISIBLE);
        if (!asOf.isEmpty()) {
            asOfLabel.setText(getString(R.string.wallet_as_of, asOf));
        }

        bindNotice(balance);
        bindSettlement(available);
        bindTransactions();
    }

    /** Surfaces a non-active wallet or a stale (cached) balance read above the cards. */
    private void bindNotice(WalletData.Balance balance) {
        boolean active = wallet != null && "active".equalsIgnoreCase(wallet.status);
        if (wallet != null && !active) {
            walletNotice.setText(getString(R.string.wallet_inactive_body));
            walletNotice.setVisibility(View.VISIBLE);
        } else if (balance != null && balance.stale) {
            walletNotice.setText(R.string.wallet_stale_notice);
            walletNotice.setVisibility(View.VISIBLE);
        } else {
            walletNotice.setVisibility(View.GONE);
        }
    }

    private void bindSettlement(double available) {
        String account = wallet != null ? wallet.account_number : null;
        settlementAccountNo.setText(WalletFormat.maskAccount(account));
        settlementAvailable.setText(maskable(available));
    }

    /** The settlement schedule has no API yet — show a plausible fixed cadence. */
    private void bindSettlementPlaceholders() {
        settlementAccountNo.setText(WalletFormat.maskAccount(null));
        settlementNext.setText(getString(R.string.wallet_next_settlement, nextSettlementLabel()));
    }

    private String nextSettlementLabel() {
        java.util.Calendar next = java.util.Calendar.getInstance();
        next.add(java.util.Calendar.DAY_OF_YEAR, 1);
        next.set(java.util.Calendar.HOUR_OF_DAY, 12);
        next.set(java.util.Calendar.MINUTE, 0);
        return new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(next.getTime());
    }

    // ---- Transactions --------------------------------------------------------

    private void bindTransactions() {
        txnList.removeAllViews();
        List<WalletData.Transaction> all = wallet != null && wallet.balance != null
                && wallet.balance.transactions != null
                ? wallet.balance.transactions : new ArrayList<>();

        boolean empty = all.isEmpty();
        txnEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        txnList.setVisibility(empty ? View.GONE : View.VISIBLE);
        txnViewAll.setVisibility(all.size() > TXN_PREVIEW_COUNT ? View.VISIBLE : View.GONE);
        txnViewAll.setText(showAllTransactions ? R.string.vas_show_less : R.string.view_all);
        if (empty) return;

        int limit = showAllTransactions ? all.size() : Math.min(TXN_PREVIEW_COUNT, all.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                txnList.addView(buildHairline());   // matches the divided rows in the web table
            }
            txnList.addView(buildTransactionRow(all.get(i)));
        }
    }

    private View buildHairline() {
        View divider = new View(this);
        divider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border_light));
        return divider;
    }

    private View buildTransactionRow(WalletData.Transaction txn) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_wallet_txn, txnList, false);
        double amount = WalletFormat.parseAmount(txn.amount);
        boolean credit = amount >= 0;

        ImageView icon = row.findViewById(R.id.txnIcon);
        icon.setImageResource(credit ? R.drawable.ic_arrow_down_left : R.drawable.ic_arrow_up_right);
        icon.setImageTintList(ContextCompat.getColorStateList(this,
                credit ? R.color.success : R.color.danger));
        row.findViewById(R.id.txnIconTile).setBackgroundTintList(
                ContextCompat.getColorStateList(this, credit ? R.color.success_bg : R.color.danger_bg));

        ((TextView) row.findViewById(R.id.txnTitle))
                .setText(credit ? R.string.wallet_money_received : R.string.wallet_money_sent);
        ((TextView) row.findViewById(R.id.txnSubtitle))
                .setText(getString(R.string.wallet_txn_reference, String.valueOf(txn.transaction_id)));

        TextView amountView = row.findViewById(R.id.txnAmount);
        amountView.setText(balanceHidden ? getString(R.string.wallet_balance_masked)
                : WalletFormat.signedMoney(amount));
        amountView.setTextColor(ContextCompat.getColor(this, credit ? R.color.success : R.color.danger));

        ((TextView) row.findViewById(R.id.txnDate)).setText(WalletFormat.relativeDateTime(txn.date));

        TextView status = row.findViewById(R.id.txnStatus);
        status.setText(txn.pending ? R.string.wallet_txn_pending : R.string.wallet_txn_success);
        status.setBackgroundResource(txn.pending ? R.drawable.bg_badge_warning : R.drawable.bg_badge_success);
        status.setTextColor(ContextCompat.getColor(this, txn.pending ? R.color.warning : R.color.success));
        return row;
    }

    // ---- Balance visibility --------------------------------------------------

    private void toggleBalanceVisibility() {
        balanceHidden = !balanceHidden;
        hideIcon.setImageResource(balanceHidden ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
        bindWallet();
    }

    private String maskable(double value) {
        return balanceHidden ? getString(R.string.wallet_balance_masked) : WalletFormat.money(value);
    }

    private void spinRefreshIcon() {
        refreshIcon.animate().rotationBy(360f).setDuration(600)
                .setInterpolator(new LinearInterpolator()).start();
    }

    // ---- Top-up (PayCloud) ---------------------------------------------------

    private void showTopUpDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_topup, null, false);

        EditText amountInput = content.findViewById(R.id.topupAmount);
        TextView error = content.findViewById(R.id.topupError);
        TextView proceed = content.findViewById(R.id.topupProceed);
        ProgressBar progress = content.findViewById(R.id.topupProgress);
        ViewGroup chips = content.findViewById(R.id.amountChips);

        content.findViewById(R.id.topupClose).setOnClickListener(v -> dialog.dismiss());

        // Quick-amount pills — tapping one fills the field and highlights that pill.
        List<TextView> chipViews = new ArrayList<>();
        for (int i = 0; i < QUICK_AMOUNTS.length; i++) {
            double amount = QUICK_AMOUNTS[i];
            TextView chip = buildAmountChip(amount, i);
            chip.setOnClickListener(v -> {
                amountInput.setText(String.format(Locale.US, "%.0f", amount));
                amountInput.setSelection(amountInput.getText().length());
                highlightChip(chipViews, chip);
            });
            chips.addView(chip);
            chipViews.add(chip);
        }

        amountInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                error.setVisibility(View.GONE);
                // Keep a pill lit only while the field still matches it.
                double typed = parseInput(s.toString());
                TextView match = null;
                for (int i = 0; i < QUICK_AMOUNTS.length; i++) {
                    if (Math.abs(QUICK_AMOUNTS[i] - typed) < 0.005) {
                        match = chipViews.get(i);
                        break;
                    }
                }
                highlightChip(chipViews, match);
            }
        });

        proceed.setOnClickListener(v -> {
            double amount = parseInput(amountInput.getText().toString());
            if (amount <= 0) {
                showDialogError(error, getString(R.string.topup_err_amount));
                return;
            }
            if (amount < TOPUP_MIN) {
                showDialogError(error, getString(R.string.topup_err_min));
                return;
            }
            if (amount > TOPUP_MAX) {
                showDialogError(error, getString(R.string.topup_err_max));
                return;
            }
            proceed.setText("");
            proceed.setEnabled(false);
            progress.setVisibility(View.VISIBLE);
            createTopup(amount, dialog, proceed, progress, error);
        });

        dialog.setContentView(content);
        // The sheet holds an input, so open it fully rather than at the collapsed peek height.
        dialog.getBehavior().setSkipCollapsed(true);
        dialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        dialog.show();
    }

    private void createTopup(double amount, BottomSheetDialog dialog, TextView proceed,
                             ProgressBar progress, TextView error) {
        ApiClient.get(this).createTopup(amount, RETURN_URL, new ApiCallback<TopupCreateData>() {
            @Override
            public void onSuccess(TopupCreateData data) {
                progress.setVisibility(View.GONE);
                proceed.setEnabled(true);
                proceed.setText(R.string.topup_proceed);
                if (data == null || data.pay_url == null || data.pay_url.isEmpty()) {
                    showDialogError(error, getString(R.string.topup_failed_notice));
                    return;
                }
                pendingOrderNo = data.merchant_order_no;
                dialog.dismiss();
                openCheckout(data.pay_url);
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
                proceed.setEnabled(true);
                proceed.setText(R.string.topup_proceed);
                showDialogError(error, message != null ? message : getString(R.string.topup_failed_notice));
            }
        });
    }

    private void openCheckout(String payUrl) {
        try {
            toast(getString(R.string.topup_opening));
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(payUrl)));
        } catch (ActivityNotFoundException e) {
            pendingOrderNo = null;
            toast(getString(R.string.topup_no_browser));
        }
    }

    /**
     * Poll {@code topup/status} after the checkout. The webhook that credits the wallet is
     * asynchronous, so a paid-but-not-yet-credited result is retried a few times before the
     * user is told the credit is on its way.
     */
    private void pollTopupStatus(String orderNo, int attempt) {
        final int maxAttempts = 5;
        if (attempt == 0) {
            toast(getString(R.string.topup_checking));
        }
        ApiClient.get(this).getTopupStatus(orderNo, new ApiCallback<TopupStatusData>() {
            @Override
            public void onSuccess(TopupStatusData data) {
                if (data == null) {
                    pendingOrderNo = null;
                    return;
                }
                if (data.isCredited()) {
                    pendingOrderNo = null;
                    toast(getString(R.string.topup_credited, WalletFormat.money(data.amount)));
                    load(true, false);
                    return;
                }
                if (data.isFailed()) {
                    pendingOrderNo = null;
                    toast(getString(R.string.topup_failed_notice));
                    return;
                }
                if (attempt + 1 < maxAttempts) {
                    // Give the PayCloud webhook a moment, then look again.
                    totalBalance.postDelayed(() -> pollTopupStatus(orderNo, attempt + 1), 2500);
                    return;
                }
                pendingOrderNo = null;
                // status=paid means the card was charged and only the credit is outstanding;
                // still-pending means the user never completed (or abandoned) the checkout.
                if ("paid".equalsIgnoreCase(data.status) || "pending".equalsIgnoreCase(data.credit_status)) {
                    toast(getString(R.string.topup_pending_notice));
                    load(true, false);
                } else {
                    toast(getString(R.string.topup_not_completed));
                }
            }

            @Override
            public void onError(String message) {
                pendingOrderNo = null;
                toast(message != null ? message : getString(R.string.topup_failed_notice));
            }
        });
    }

    private TextView buildAmountChip(double amount, int index) {
        TextView chip = new com.anokix.distributorapp.ui.views.RobotoBoldTextView(this);
        chip.setText("R" + String.format(Locale.US, "%,.0f", amount).replace(',', ' '));
        chip.setTextSize(13);
        chip.setSingleLine(true);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        chip.setBackgroundResource(R.drawable.bg_amount_chip);
        chip.setPadding(dp(4), dp(10), dp(4), dp(10));

        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (index > 0) params.leftMargin = dp(7);
        chip.setLayoutParams(params);
        return chip;
    }

    private void highlightChip(List<TextView> chips, @Nullable TextView selected) {
        for (TextView chip : chips) {
            boolean on = chip == selected;
            chip.setBackgroundResource(on ? R.drawable.bg_amount_chip_selected : R.drawable.bg_amount_chip);
            chip.setTextColor(ContextCompat.getColor(this,
                    on ? R.color.purple_primary : R.color.text_primary));
            chip.setPadding(dp(4), dp(10), dp(4), dp(10));   // a background swap clears padding
        }
    }

    private void showDialogError(TextView error, String message) {
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    /** Accepts "1 000,50", "1000.50" and "R1000" alike. */
    private static double parseInput(String raw) {
        if (raw == null) return 0;
        String cleaned = raw.replaceAll("[^0-9.,]", "").replace(" ", "");
        if (cleaned.isEmpty()) return 0;
        // A lone comma is the decimal separator in ZA formatting.
        if (cleaned.contains(",") && !cleaned.contains(".")) {
            cleaned = cleaned.replace(',', '.');
        } else {
            cleaned = cleaned.replace(",", "");
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ---- Placeholder panels --------------------------------------------------

    /**
     * The 4-per-row Quick Actions grid. Built as plain weighted rows rather than a
     * RecyclerView — a wrap_content RecyclerView inside this ScrollView only ever
     * measures its first row.
     */
    private void buildQuickActions() {
        String[][] actions = {
                {"send", getString(R.string.wallet_action_send_money)},
                {"receive", getString(R.string.wallet_action_receive_money)},
                {"airtime", getString(R.string.wallet_action_buy_airtime)},
                {"bills", getString(R.string.wallet_action_pay_bills)},
                {"add", getString(R.string.wallet_action_add_money)},
                {"bank", getString(R.string.wallet_action_move_to_bank)},
                {"qr", getString(R.string.wallet_action_qr_payment)},
                {"more", getString(R.string.wallet_action_more)}};
        int[] icons = {R.drawable.ic_send, R.drawable.ic_arrow_down_left, R.drawable.ic_phone,
                R.drawable.ic_receipt, R.drawable.ic_add, R.drawable.ic_bank,
                R.drawable.ic_qr_code, R.drawable.ic_dots_horizontal};

        ViewGroup grid = findViewById(R.id.quickActionGrid);
        android.widget.LinearLayout row = null;
        for (int i = 0; i < actions.length; i++) {
            if (i % 4 == 0) {
                row = new android.widget.LinearLayout(this);
                row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                row.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                grid.addView(row);
            }
            View tile = LayoutInflater.from(this).inflate(R.layout.item_wallet_quick_action, row, false);
            tile.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            ((ImageView) tile.findViewById(R.id.actionIcon)).setImageResource(icons[i]);
            ((TextView) tile.findViewById(R.id.actionLabel)).setText(actions[i][1]);
            String key = actions[i][0];
            String label = actions[i][1];
            tile.setOnClickListener(v -> onQuickAction(key, label));
            row.addView(tile);
        }
    }

    private void onQuickAction(String key, String label) {
        switch (key) {
            case "add":
                showTopUpDialog();
                break;
            case "airtime":
                startActivity(new Intent(this, AirtimeActivity.class));
                break;
            default:
                toast(getString(R.string.wallet_coming_soon, label));
                break;
        }
    }

    private void buildRecipients() {
        String[][] recipients = {
                {"Thabo M.", "**** 4521", "T"},
                {"Nomsa K.", "**** 8892", "N"},
                {"Sipho D.", "**** 1103", "S"},
                {"Lerato P.", "**** 7744", "L"},
                {"Zanele N.", "**** 3390", "Z"}};
        int[] tints = {R.color.info, R.color.success, R.color.warning, R.color.danger, R.color.purple_primary};

        // "New Recipient" comes first — a dashed circle with a plus, as on the web.
        View addView = LayoutInflater.from(this).inflate(R.layout.item_wallet_recipient, recipientStrip, false);
        addView.findViewById(R.id.recipientAvatar).setBackgroundResource(R.drawable.bg_circle_outline_dashed);
        addView.findViewById(R.id.recipientAvatar).setBackgroundTintList(null);
        addView.findViewById(R.id.recipientInitial).setVisibility(View.GONE);
        addView.findViewById(R.id.recipientAddIcon).setVisibility(View.VISIBLE);
        ((TextView) addView.findViewById(R.id.recipientName)).setText(R.string.wallet_new_recipient);
        addView.findViewById(R.id.recipientAccount).setVisibility(View.INVISIBLE);
        addView.setOnClickListener(v ->
                toast(getString(R.string.wallet_coming_soon, getString(R.string.wallet_new_recipient))));
        recipientStrip.addView(addView);

        for (int i = 0; i < recipients.length; i++) {
            View item = LayoutInflater.from(this).inflate(R.layout.item_wallet_recipient, recipientStrip, false);
            item.findViewById(R.id.recipientAvatar).setBackgroundTintList(
                    ContextCompat.getColorStateList(this, tints[i % tints.length]));
            ((TextView) item.findViewById(R.id.recipientInitial)).setText(recipients[i][2]);
            ((TextView) item.findViewById(R.id.recipientName)).setText(recipients[i][0]);
            ((TextView) item.findViewById(R.id.recipientAccount)).setText(recipients[i][1]);
            String name = recipients[i][0];
            item.setOnClickListener(v -> toast(getString(R.string.wallet_coming_soon, name)));
            recipientStrip.addView(item);
        }
    }

    private void buildLoanOffers() {
        String[][] offers = {
                {getString(R.string.wallet_loan_business_boost), getString(R.string.wallet_badge_recommended),
                        "Up to R50 000", "From 1.75% p.m."},
                {getString(R.string.wallet_loan_stock_finance), getString(R.string.wallet_badge_popular),
                        "Up to R100 000", "From 1.5% p.m."},
                {getString(R.string.wallet_loan_equipment_finance), null,
                        "Up to R200 000", "From 1.9% p.m."}};
        int[] icons = {R.drawable.ic_finances, R.drawable.ic_inventory, R.drawable.ic_wrench};

        for (int i = 0; i < offers.length; i++) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_wallet_loan, loanList, false);
            ((ImageView) card.findViewById(R.id.loanIcon)).setImageResource(icons[i]);
            ((TextView) card.findViewById(R.id.loanTitle)).setText(offers[i][0]);
            ((TextView) card.findViewById(R.id.loanAmount)).setText(offers[i][2]);
            ((TextView) card.findViewById(R.id.loanRate)).setText(offers[i][3]);

            TextView badge = card.findViewById(R.id.loanBadge);
            if (offers[i][1] != null) {
                badge.setText(offers[i][1]);
                badge.setVisibility(View.VISIBLE);
            }

            String title = offers[i][0];
            card.findViewById(R.id.loanApply).setOnClickListener(v ->
                    toast(getString(R.string.wallet_coming_soon, title)));

            if (i > 0) {
                ((android.widget.LinearLayout.LayoutParams) card.getLayoutParams())
                        .topMargin = dp(10);
            }
            loanList.addView(card);
        }
    }

    private void buildStatements() {
        int[] labels = {R.string.wallet_statement_today, R.string.wallet_statement_yesterday,
                R.string.wallet_statement_this_week, R.string.wallet_statement_this_month,
                R.string.wallet_statement_custom};

        for (int i = 0; i < labels.length; i++) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_wallet_statement, statementList, false);
            ((TextView) row.findViewById(R.id.statementLabel)).setText(labels[i]);
            if (i == labels.length - 1) {
                ((ImageView) row.findViewById(R.id.statementIcon)).setImageResource(R.drawable.ic_calendar);
            }
            String label = getString(labels[i]);
            row.setOnClickListener(v -> toast(getString(R.string.wallet_coming_soon,
                    getString(R.string.wallet_statements) + " · " + label)));
            statementList.addView(row);
        }
    }

    // ---- Helpers -------------------------------------------------------------

    /** Wires a view that has no backend yet to a "coming soon" note naming the feature. */
    private void comingSoon(int viewId, int labelRes) {
        findViewById(viewId).setOnClickListener(v ->
                toast(getString(R.string.wallet_coming_soon, getString(labelRes))));
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
