package com.anokix.distributor.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.FinanceSummary;
import com.anokix.distributor.ui.adapter.FinanceTransactionAdapter;

public class FinancesActivity extends BaseSecondaryActivity {

    private FinanceTransactionAdapter adapter;
    private String currentFilter = "all";
    private TextView tabAll, tabIncome, tabExpenses;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_finances;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.finances);
    }

    @Override
    protected void bindViews() {
        RecyclerView list = findViewById(R.id.transactionsList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setNestedScrollingEnabled(false);
        adapter = new FinanceTransactionAdapter();
        list.setAdapter(adapter);

        tabAll = findViewById(R.id.tabAll);
        tabIncome = findViewById(R.id.tabIncome);
        tabExpenses = findViewById(R.id.tabExpenses);

        tabAll.setOnClickListener(v -> selectTab("all", tabAll, tabIncome, tabExpenses));
        tabIncome.setOnClickListener(v -> selectTab("income", tabIncome, tabAll, tabExpenses));
        tabExpenses.setOnClickListener(v -> selectTab("expenses", tabExpenses, tabAll, tabIncome));
    }

    @Override
    protected void loadData() {
        buildSummaryCards();
        selectTab("all", tabAll, tabIncome, tabExpenses);
    }

    private void buildSummaryCards() {
        LinearLayout row = findViewById(R.id.summaryRow);
        row.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (FinanceSummary summary : repository.getFinanceSummaries()) {
            View card = inflater.inflate(R.layout.item_finance_summary, row, false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.finance_summary_card_width),
                    (int) getResources().getDimension(R.dimen.finance_summary_card_height));
            params.setMarginEnd((int) getResources().getDimension(R.dimen.spacing_sm));
            card.setLayoutParams(params);
            ((TextView) card.findViewById(R.id.summaryTitle)).setText(summary.title);
            TextView amount = card.findViewById(R.id.summaryAmount);
            amount.setText(summary.amount);
            amount.setTextColor(ContextCompat.getColor(this, summary.valueColorRes));
            TextView trend = card.findViewById(R.id.summaryTrend);
            trend.setText(summary.trend);
            trend.setTextColor(ContextCompat.getColor(this, summary.trendColorRes));
            row.addView(card);
        }
    }

    private void selectTab(String filter, TextView selected, TextView other1, TextView other2) {
        currentFilter = filter;
        styleSegmentTab(selected, true);
        styleSegmentTab(other1, false);
        styleSegmentTab(other2, false);
        adapter.setItems(repository.getFinanceTransactions(currentFilter));
    }

    private void styleSegmentTab(TextView tab, boolean selected) {
        if (selected) {
            tab.setBackgroundResource(R.drawable.bg_segment_tab_selected);
            tab.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            tab.setBackgroundResource(android.R.color.transparent);
            tab.setTextColor(getResources().getColor(R.color.text_primary));
        }
    }
}
