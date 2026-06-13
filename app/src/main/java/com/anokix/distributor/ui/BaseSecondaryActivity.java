package com.anokix.distributor.ui;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.anokix.distributor.R;
import com.anokix.distributor.data.DistributorRepository;
import com.anokix.distributor.data.RepositoryProvider;
import com.anokix.distributor.ui.views.RobotoBoldTextView;
import com.anokix.distributor.ui.views.RobotoLightTextView;

/**
 * Base for secondary screens opened from the drawer. Provides back navigation
 * and access to the shared {@link DistributorRepository}.
 */
public abstract class BaseSecondaryActivity extends AppCompatActivity {

    protected DistributorRepository repository = RepositoryProvider.get();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutRes());
        setupHeader();
        bindViews();
        loadData();
    }

    @LayoutRes
    protected abstract int getLayoutRes();

    protected abstract String getScreenTitle();

    @Nullable
    protected String getScreenSubtitle() {
        return null;
    }

    protected void setupHeader() {
        ImageButton back = findViewById(R.id.backButton);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }
        RobotoBoldTextView title = findViewById(R.id.screenTitle);
        if (title != null) {
            title.setText(getScreenTitle());
        }
        RobotoLightTextView subtitle = findViewById(R.id.screenSubtitle);
        if (subtitle != null) {
            String sub = getScreenSubtitle();
            if (sub != null) {
                subtitle.setText(sub);
                subtitle.setVisibility(android.view.View.VISIBLE);
            } else {
                subtitle.setVisibility(android.view.View.GONE);
            }
        }
    }

    protected abstract void bindViews();

    protected abstract void loadData();

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
