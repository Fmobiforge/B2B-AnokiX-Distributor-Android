package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.data.ApiMapper;
import com.anokix.distributorapp.data.MockData;
import com.anokix.distributorapp.model.StoreItem;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.StoresData;
import com.anokix.distributorapp.ui.adapter.StoreAdapter;
import com.anokix.distributorapp.ui.util.FilterUiHelper;

import java.util.ArrayList;
import java.util.List;

public class StoresActivity extends BaseSecondaryActivity {

    private StoreAdapter adapter;
    private List<StoreItem> allItems = new ArrayList<>();
    private String selectedArea = "All Areas";

    private final ActivityResultLauncher<Intent> editorLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadData();
                }
            });

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_stores;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.stores);
    }

    @Override
    protected void bindViews() {
        RecyclerView list = findViewById(R.id.storesList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StoreAdapter();
        adapter.setOnItemClickListener(this::openEditor);
        list.setAdapter(adapter);

        findViewById(R.id.addStoreButton).setOnClickListener(v -> openCreate());

        TextView areaLabel = findViewById(R.id.areaFilterLabel);
        FilterUiHelper.setDropdownContentDescription(areaLabel, getString(R.string.all_areas));
        FilterUiHelper.bindDropdown(
                findViewById(R.id.areaFilterButton),
                areaLabel,
                MockData.getFilterStatus(),
                0,
                (index, value) -> {
                    selectedArea = value;
                    applyFilters();
                });
    }

    @Override
    protected void loadData() {
        ApiClient.get(this).getStores("", new ApiCallback<StoresData>() {
            @Override
            public void onSuccess(StoresData data) {
                allItems = ApiMapper.toStoreItems(data);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(StoresActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openCreate() {
        editorLauncher.launch(new Intent(this, AddEditStoreActivity.class));
    }

    private void openEditor(StoreItem item) {
        Intent intent = new Intent(this, AddEditStoreActivity.class);
        intent.putExtra(AddEditStoreActivity.EXTRA_STORE_ID, item.id);
        intent.putExtra(AddEditStoreActivity.EXTRA_STORE_NAME, item.name);
        editorLauncher.launch(intent);
    }

    private void applyFilters() {
        if ("All Areas".equals(selectedArea)) {
            adapter.setItems(allItems);
            return;
        }
        List<StoreItem> filtered = new ArrayList<>();
        for (StoreItem item : allItems) {
            if (selectedArea.equals(item.area)) {
                filtered.add(item);
            }
        }
        adapter.setItems(filtered);
    }
}
