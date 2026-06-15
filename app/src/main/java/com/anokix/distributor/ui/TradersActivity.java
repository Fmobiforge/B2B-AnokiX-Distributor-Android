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

import java.util.List;

public class TradersActivity extends BaseSecondaryActivity {

    private static final String ALL_STATUS = "All Status";

    private TraderAdapter adapter;

    // Server-side filters: empty string means "no filter" (All Status).
    private String selectedStatus = "";
    private String selectedRegistrationStatus = "";

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

        TextView statusLabel = findViewById(R.id.areaFilterLabel);
        TextView registrationLabel = findViewById(R.id.segmentFilterLabel);
        FilterUiHelper.setDropdownContentDescription(statusLabel, getString(R.string.filter_status));
        FilterUiHelper.setDropdownContentDescription(registrationLabel,
                getString(R.string.filter_registration_status));

        // Status filter → ?status=
        FilterUiHelper.bindDropdown(
                findViewById(R.id.areaFilterButton),
                statusLabel,
                MockData.getFilterStatus(),
                0,
                (index, value) -> {
                    selectedStatus = toParam(value);
                    loadData();
                });

        // Registration status filter → ?registration_status=
        FilterUiHelper.bindDropdown(
                findViewById(R.id.segmentFilterButton),
                registrationLabel,
                MockData.getFilterRegistrationStatus(),
                0,
                (index, value) -> {
                    selectedRegistrationStatus = toParam(value);
                    loadData();
                });
    }

    @Override
    protected void loadData() {
        ApiClient.get(this).getTraders("", selectedStatus, selectedRegistrationStatus,
                new ApiCallback<TradersData>() {
                    @Override
                    public void onSuccess(TradersData data) {
                        adapter.setItems(ApiMapper.toTraderItems(data));
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(TradersActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** "All Status" means no filter; everything else is sent verbatim to the API. */
    private String toParam(String value) {
        return ALL_STATUS.equals(value) ? "" : value;
    }
}
