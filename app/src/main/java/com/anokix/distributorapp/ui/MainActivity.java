package com.anokix.distributorapp.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.data.MockData;
import com.anokix.distributorapp.messaging.PushManager;
import com.anokix.distributorapp.model.MenuItem;
import com.anokix.distributorapp.session.SessionManager;
import com.anokix.distributorapp.ui.adapter.DrawerMenuAdapter;
import com.anokix.distributorapp.ui.fragment.DashboardFragment;
import com.anokix.distributorapp.ui.fragment.InventoryFragment;
import com.anokix.distributorapp.ui.fragment.OrdersFragment;
import com.anokix.distributorapp.ui.fragment.ProductsFragment;
import com.anokix.distributorapp.ui.views.TypefaceCache;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;
    private DrawerMenuAdapter drawerAdapter;

    /** POST_NOTIFICATIONS prompt (Android 13+); result is ignored — pushes degrade gracefully. */
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        bottomNav = findViewById(R.id.bottomNav);

        setupDrawer();
        setupBottomNav();

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // FCM: ensure the channel exists, ask for the runtime permission, register the
        // token, then apply any routing a notification tap carried in.
        PushManager.ensureChannel(this);
        requestNotificationPermission();
        PushManager.syncToken(this);
        applyPushRouting(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyPushRouting(intent);
    }

    /** Open the screen a tapped notification points at (order/return/delivery/stock), then clear it. */
    private void applyPushRouting(Intent intent) {
        PushManager.handleRouting(this, intent);
        PushManager.clearRouting(intent);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void setupBottomNav() {
        styleBottomNavMenu();

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment;
            String key;
            if (id == R.id.nav_orders) {
                fragment = new OrdersFragment();
                key = "orders";
            } else if (id == R.id.nav_inventory) {
                fragment = new InventoryFragment();
                key = "inventory";
            } else if (id == R.id.nav_products) {
                fragment = new ProductsFragment();
                key = "products";
            } else {
                fragment = new DashboardFragment();
                key = "home";
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
            if (drawerAdapter != null) {
                drawerAdapter.setSelectedByKey(key);
            }
            return true;
        });
    }

    private void styleBottomNavMenu() {
        float textSizeSp = getResources().getDimension(R.dimen.bottom_nav_text_size)
                / getResources().getDisplayMetrics().scaledDensity;
        bottomNav.post(() -> TypefaceCache.applyFontToBottomNavigationViewMenu(bottomNav, this, textSizeSp));
    }

    private void setupDrawer() {
        RecyclerView drawerList = findViewById(R.id.drawerMenuList);
        drawerList.setLayoutManager(new LinearLayoutManager(this));
        drawerAdapter = new DrawerMenuAdapter(MockData.getDrawerMenuItems(), this::onDrawerItemClicked);
        drawerList.setAdapter(drawerAdapter);

        findViewById(R.id.drawerLogout).setOnClickListener(v -> confirmLogout());
        bindDrawerHeader();

        // Portal chrome: the app runs under a white status bar with dark icons. The drawer
        // (#000920) sits below the status bar, so while it is mostly open the bar switches to
        // the drawer navy with light icons, and back to white once it closes again.
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            private boolean darkBar;

            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                boolean shouldBeDark = slideOffset > 0.5f;
                if (shouldBeDark != darkBar) {
                    darkBar = shouldBeDark;
                    applyStatusBar(darkBar);
                }
            }
        });
    }

    /** White bar + dark icons (default), or drawer navy + light icons while the drawer is open. */
    private void applyStatusBar(boolean drawerOpen) {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this,
                drawerOpen ? R.color.drawer_bg : R.color.surface));
        new WindowInsetsControllerCompat(window, window.getDecorView())
                .setAppearanceLightStatusBars(!drawerOpen);
    }

    /**
     * Names the distributor whose account is signed in, under the portal badge.
     *
     * <p>The legal name is cached in the session by the dashboard's base-info call, which
     * may land after the drawer is built — so this also runs from {@link #openDrawer()},
     * the moment the header actually becomes visible.
     */
    private void bindDrawerHeader() {
        TextView companyName = findViewById(R.id.drawerCompanyName);
        if (companyName == null) {
            return;
        }
        String name = SessionManager.get(this).getDistributorName();
        companyName.setText(name);
        companyName.setVisibility(name.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> performLogout())
                .show();
    }

    private void performLogout() {
        PushManager.unregister(this);   // drop this device's token server-side
        SessionManager.get(this).clear();
        Glide.get(this).clearMemory();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void onDrawerItemClicked(MenuItem item) {
        closeDrawer();
        switch (item.key) {
            case "home":
                bottomNav.setSelectedItemId(R.id.nav_home);
                break;
            case "orders":
                bottomNav.setSelectedItemId(R.id.nav_orders);
                break;
            case "inventory":
                bottomNav.setSelectedItemId(R.id.nav_inventory);
                break;
            case "products":
                bottomNav.setSelectedItemId(R.id.nav_products);
                break;
            case "airtime":
                startActivity(new Intent(this, AirtimeActivity.class));
                break;
            case "stores":
                startActivity(new Intent(this, StoresActivity.class));
                break;
            case "traders":
                startActivity(new Intent(this, TradersActivity.class));
                break;
            case "deliveries":
                startActivity(new Intent(this, DeliveriesActivity.class));
                break;
            case "returns":
                startActivity(new Intent(this, ReturnsActivity.class));
                break;
            case "promotions":
                startActivity(new Intent(this, PromotionsActivity.class));
                break;
            case "finances":
                startActivity(new Intent(this, FinancesActivity.class));
                break;
            case "wallet":
                startActivity(new Intent(this, WalletActivity.class));
                break;
            case "card_machine":
                startActivity(new Intent(this, CardMachineActivity.class));
                break;
            case "warehouse_staff":
                startActivity(new Intent(this, WarehouseStaffActivity.class));
                break;
            case "reports":
                startActivity(new Intent(this, ReportsActivity.class));
                break;
            case "notifications":
                startActivity(new Intent(this, NotificationsActivity.class));
                break;
            case "settings":
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case "support":
                startActivity(new Intent(this, SupportActivity.class));
                break;
            default:
                // Any menu entry whose screen is still to come.
                Toast.makeText(this, getString(R.string.feature_coming_soon, item.title),
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void openDrawer() {
        if (drawerLayout != null) {
            bindDrawerHeader();   // the legal name may have arrived since the drawer was built
            drawerLayout.openDrawer(findViewById(R.id.drawerContent));
        }
    }

    public void closeDrawer() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
    }
}
