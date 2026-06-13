package com.anokix.distributor.ui;

import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.ReturnItem;
import com.anokix.distributor.ui.adapter.ReturnAdapter;

import java.util.Map;

public class ReturnsActivity extends BaseSecondaryActivity {

    private ReturnAdapter adapter;
    private String currentFilter = "pending";
    private TextView tabPending, tabApproved, tabRejected;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_returns;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.returns);
    }

    @Override
    protected void bindViews() {
        RecyclerView list = findViewById(R.id.returnsList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReturnAdapter(new ReturnAdapter.ActionListener() {
            @Override
            public void onApprove(ReturnItem item) {
                Toast.makeText(ReturnsActivity.this, "Approved " + item.id, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReject(ReturnItem item) {
                Toast.makeText(ReturnsActivity.this, "Rejected " + item.id, Toast.LENGTH_SHORT).show();
            }
        });
        list.setAdapter(adapter);

        tabPending = findViewById(R.id.tabPending);
        tabApproved = findViewById(R.id.tabApproved);
        tabRejected = findViewById(R.id.tabRejected);

        tabPending.setOnClickListener(v -> selectTab("pending", tabPending, tabApproved, tabRejected));
        tabApproved.setOnClickListener(v -> selectTab("approved", tabApproved, tabPending, tabRejected));
        tabRejected.setOnClickListener(v -> selectTab("rejected", tabRejected, tabPending, tabApproved));
    }

    @Override
    protected void loadData() {
        Map<String, Integer> counts = repository.getReturnSummaryCounts();
        ((TextView) findViewById(R.id.countPending)).setText(String.valueOf(counts.get("pending")));
        ((TextView) findViewById(R.id.countApproved)).setText(String.valueOf(counts.get("approved")));
        ((TextView) findViewById(R.id.countRejected)).setText(String.valueOf(counts.get("rejected")));
        adapter.setItems(repository.getReturns(currentFilter));
    }

    private void selectTab(String filter, TextView selected, TextView other1, TextView other2) {
        currentFilter = filter;
        selected.setTextColor(getResources().getColor(R.color.purple_primary));
        other1.setTextColor(getResources().getColor(R.color.text_secondary));
        other2.setTextColor(getResources().getColor(R.color.text_secondary));
        adapter.setItems(repository.getReturns(currentFilter));
    }
}
