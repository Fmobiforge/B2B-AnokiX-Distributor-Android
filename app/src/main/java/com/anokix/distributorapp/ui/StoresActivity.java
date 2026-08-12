package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.data.ApiMapper;
import com.anokix.distributorapp.model.StoreItem;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.StoresData;
import com.anokix.distributorapp.ui.adapter.StoreAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StoresActivity extends BaseSecondaryActivity {

    private StoreAdapter adapter;
    private List<StoreItem> allItems = new ArrayList<>();
    private String keyword = "";

    private TextView countPill;
    private TextView emptyView;

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

        countPill = findViewById(R.id.storeCountPill);
        emptyView = findViewById(R.id.emptyView);

        findViewById(R.id.addStoreButton).setOnClickListener(v -> openCreate());

        EditText search = findViewById(R.id.searchInput);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                keyword = s == null ? "" : s.toString().trim();
                applyFilters();
            }
        });

        applyFilters();
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

    /** Local search over name, address and tags — mirrors the web store list search. */
    private void applyFilters() {
        List<StoreItem> filtered;
        if (keyword.isEmpty()) {
            filtered = allItems;
        } else {
            String needle = keyword.toLowerCase(Locale.getDefault());
            filtered = new ArrayList<>();
            for (StoreItem item : allItems) {
                if (matches(item, needle)) {
                    filtered.add(item);
                }
            }
        }
        adapter.setItems(filtered);

        countPill.setText(getResources().getQuantityString(
                R.plurals.stores_count, filtered.size(), filtered.size()));
        emptyView.setText(allItems.isEmpty() ? R.string.no_stores_yet : R.string.no_stores_found);
        emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean matches(StoreItem item, String needle) {
        if (contains(item.name, needle) || contains(item.address, needle)) {
            return true;
        }
        for (String tag : item.tags) {
            if (contains(tag, needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(needle);
    }
}
