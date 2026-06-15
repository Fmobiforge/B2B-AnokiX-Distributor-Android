package com.anokix.distributor.ui;

import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.data.ApiMapper;
import com.anokix.distributor.data.MockData;
import com.anokix.distributor.model.TraderItem;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.TradersData;
import com.anokix.distributor.ui.adapter.TraderAdapter;
import com.anokix.distributor.ui.util.FilterUiHelper;

import java.util.ArrayList;
import java.util.List;

public class TradersActivity extends BaseSecondaryActivity {

    private TraderAdapter adapter;
    private List<TraderItem> allItems = new ArrayList<>();
    private String selectedArea = "All Status";
    private String selectedSegment = "All Status";

    private final ActivityResultLauncher<Intent> createTraderLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadData(); // refresh the list after a new trader is registered
                }
            });

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_traders;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.traders);
    }

    @Override
    protected void bindViews() {
        RecyclerView list = findViewById(R.id.tradersList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TraderAdapter();
        list.setAdapter(adapter);

        findViewById(R.id.addTraderButton).setOnClickListener(v ->
                createTraderLauncher.launch(new Intent(this, CreateTraderActivity.class)));

        TextView areaLabel = findViewById(R.id.areaFilterLabel);
        TextView segmentLabel = findViewById(R.id.segmentFilterLabel);
        FilterUiHelper.setDropdownContentDescription(areaLabel, getString(R.string.all_areas));
        FilterUiHelper.setDropdownContentDescription(segmentLabel, getString(R.string.all_segments));

        FilterUiHelper.bindDropdown(
                findViewById(R.id.areaFilterButton),
                areaLabel,
                MockData.getFilterStatus(),
                0,
                (index, value) -> {
                    selectedArea = value;
                    applyFilters();
                });

        FilterUiHelper.bindDropdown(
                findViewById(R.id.segmentFilterButton),
                segmentLabel,
                MockData.getFilterRegistrationStatus(),
                0,
                (index, value) -> {
                    selectedSegment = value;
                    applyFilters();
                });
    }

    @Override
    protected void loadData() {
        ApiClient.get(this).getTraders("", new ApiCallback<TradersData>() {
            @Override
            public void onSuccess(TradersData data) {
                allItems = ApiMapper.toTraderItems(data);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(TradersActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyFilters() {
        List<TraderItem> filtered = new ArrayList<>();
        for (TraderItem item : allItems) {
            boolean areaMatch = "All Status".equals(selectedArea) || selectedArea.equals(item.area);
            boolean segmentMatch = "All Status".equals(selectedSegment)
                    || selectedSegment.equalsIgnoreCase(item.segment);
            if (areaMatch && segmentMatch) {
                filtered.add(item);
            }
        }
        adapter.setItems(filtered);
    }
}
