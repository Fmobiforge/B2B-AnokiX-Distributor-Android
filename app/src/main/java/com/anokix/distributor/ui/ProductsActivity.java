package com.anokix.distributor.ui;

import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.data.MockData;
import com.anokix.distributor.ui.adapter.ProductAdapter;

public class ProductsActivity extends BaseSecondaryActivity {

    private ProductAdapter adapter;

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
        list.setAdapter(adapter);

        findViewById(R.id.addProductFab).setOnClickListener(v ->
                Toast.makeText(this, "Add Product – coming soon", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void loadData() {
        adapter.setItems(MockData.getProductItems());
    }
}
