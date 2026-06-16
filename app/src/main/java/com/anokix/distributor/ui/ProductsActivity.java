package com.anokix.distributor.ui;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.data.ApiMapper;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.ProductsData;
import com.anokix.distributor.ui.adapter.ProductAdapter;

public class ProductsActivity extends BaseSecondaryActivity {

    private ProductAdapter adapter;

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

        EditText searchInput = findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.addProductFab).setOnClickListener(v ->
                wizardLauncher.launch(new Intent(this, AddProductOptionsActivity.class)));
    }

    @Override
    protected void loadData() {
        ApiClient.get(this).getProducts(new ApiCallback<ProductsData>() {
            @Override
            public void onSuccess(ProductsData data) {
                adapter.setItems(ApiMapper.toProductItems(data));
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProductsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
