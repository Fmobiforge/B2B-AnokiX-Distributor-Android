package com.anokix.distributorapp.ui;

import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.ui.adapter.ReportHistoryAdapter;
import com.anokix.distributorapp.ui.adapter.ReportTypeAdapter;

public class ReportsActivity extends BaseSecondaryActivity {

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_reports;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.reports);
    }

    @Override
    protected void bindViews() {
        RecyclerView typesGrid = findViewById(R.id.reportTypesGrid);
        typesGrid.setLayoutManager(new GridLayoutManager(this, 2));
        typesGrid.setHasFixedSize(true);
        ReportTypeAdapter typeAdapter = new ReportTypeAdapter();
        typesGrid.setAdapter(typeAdapter);
        typeAdapter.setItems(repository.getReportTypes());

        RecyclerView historyList = findViewById(R.id.recentReportsList);
        historyList.setLayoutManager(new LinearLayoutManager(this));
        historyList.setHasFixedSize(true);
        ReportHistoryAdapter historyAdapter = new ReportHistoryAdapter();
        historyList.setAdapter(historyAdapter);
        historyAdapter.setItems(repository.getRecentReports());

        findViewById(R.id.generateReportButton).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.generate_report), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void loadData() {
        // Data bound in bindViews via repository
    }
}
