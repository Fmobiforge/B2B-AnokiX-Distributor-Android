package com.anokix.distributor.ui;

import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.ui.adapter.PromotionAdapter;

public class PromotionsActivity extends BaseSecondaryActivity {

    private PromotionAdapter adapter;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_promotions;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.promotions);
    }

    @Override
    protected void bindViews() {
        RecyclerView list = findViewById(R.id.promotionsList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PromotionAdapter();
        list.setAdapter(adapter);

        findViewById(R.id.createPromotionButton).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.create_promotion), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void loadData() {
        adapter.setItems(repository.getPromotions());
    }
}
