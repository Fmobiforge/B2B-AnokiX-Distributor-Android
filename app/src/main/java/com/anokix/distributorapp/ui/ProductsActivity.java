package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.data.ApiMapper;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.BaseInfoData;
import com.anokix.distributorapp.network.dto.ProductsData;
import com.anokix.distributorapp.ui.adapter.ProductAdapter;
import com.anokix.distributorapp.ui.util.ProductListSection;

public class ProductsActivity extends BaseSecondaryActivity {

    private ProductAdapter adapter;
    private ProductListSection section;

    // Re-load the list whenever the create/edit wizard reports a change.
    private final ActivityResultLauncher<Intent> wizardLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadData();
                }
            });

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_products;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.products_title);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.products_subtitle);
    }

    @Override
    protected void bindViews() {
        RecyclerView list = findViewById(R.id.productsList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter();
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(this, CreateProductActivity.class);
            intent.putExtra(CreateProductActivity.EXTRA_PRODUCT_ID, item.id);
            wizardLauncher.launch(intent);
        });
        list.setAdapter(adapter);

        section = new ProductListSection(this, adapter::setItems);
        section.bind(findViewById(android.R.id.content));

        findViewById(R.id.addProductFab).setOnClickListener(v ->
                wizardLauncher.launch(new Intent(this, AddProductOptionsActivity.class)));

        loadFilterOptions();
    }

    @Override
    protected void loadData() {
        ApiClient.get(this).getProducts(new ApiCallback<ProductsData>() {
            @Override
            public void onSuccess(ProductsData data) {
                section.setProducts(ApiMapper.toProductItems(data),
                        data == null ? null : data.summary);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProductsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Categories + brands for the filter pickers; failures leave the product-derived lists. */
    private void loadFilterOptions() {
        ApiClient.get(this).getBaseInfo(new ApiCallback<BaseInfoData>() {
            @Override
            public void onSuccess(BaseInfoData data) {
                section.setBaseInfo(data);
            }

            @Override
            public void onError(String message) {
                // Non-fatal: the pickers fall back to values found on the loaded products.
            }
        });
    }
}
