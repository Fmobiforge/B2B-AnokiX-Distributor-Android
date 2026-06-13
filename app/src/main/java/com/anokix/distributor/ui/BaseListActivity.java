package com.anokix.distributor.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.ListItem;
import com.anokix.distributor.ui.adapter.SimpleListAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public abstract class BaseListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_screen);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getScreenTitle());
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView list = findViewById(R.id.recyclerView);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new SimpleListAdapter(getItems()));
    }

    protected abstract String getScreenTitle();

    protected abstract List<ListItem> getItems();

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
