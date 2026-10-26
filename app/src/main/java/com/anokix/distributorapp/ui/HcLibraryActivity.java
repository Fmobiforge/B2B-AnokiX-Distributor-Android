package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.HcLibraryData;
import com.anokix.distributorapp.network.dto.HcLookupData;
import com.anokix.distributorapp.ui.adapter.HcLibraryAdapter;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Hirt &amp; Carter Library — browse and search the national product catalogue
 * (api/distributor/products/hc-library), as on the portal.
 *
 * Search (debounced 350 ms), a brand filter and "With images only" (on by default) drive the
 * query. Pages of 24 load as the list scrolls. Tapping a card fetches the full record
 * (external-lookup) and opens {@link HcProductActivity}; "Not this product" there returns
 * here with the search and filters untouched.
 */
public class HcLibraryActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 24;
    private static final long SEARCH_DEBOUNCE_MS = 350;

    private EditText etSearch;
    private View btnClearSearch;
    private TextView brandLabel;
    private ImageView brandIcon;
    private SwitchCompat switchImages;
    private TextView countLabel;
    private RecyclerView list;
    private View loading;
    private View stateBox;
    private TextView stateTitle, stateText;
    private LinearLayout stateActions;
    private HcLibraryAdapter adapter;

    // Query
    private String search = "";
    @Nullable private String brand;
    private boolean imagesOnly = true;

    // Result
    private final List<HcLibraryData.Item> items = new ArrayList<>();
    private final List<String> brands = new ArrayList<>();
    private int page = 1;
    private int total;
    private boolean hasMore;
    private boolean loadedOnce;
    private boolean available = true;
    private boolean libraryReady = true;
    @Nullable private String error;
    private boolean loadingFirst;
    private boolean loadingMore;
    /** Bumped on every new query so late responses for an older query are dropped. */
    private int generation;
    @Nullable private String lookupGtin;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable applyTypedSearch = () -> applySearch(text(etSearch));

    private final ActivityResultLauncher<Intent> flowLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    setResult(RESULT_OK);
                    finish();
                }
                // RESULT_REJECTED ("Not this product") just comes back to the list.
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hc_library);

        ((TextView) findViewById(R.id.screenTitle)).setText(R.string.hc_library_title);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        brandLabel = findViewById(R.id.brandLabel);
        brandIcon = findViewById(R.id.brandIcon);
        switchImages = findViewById(R.id.switchImages);
        countLabel = findViewById(R.id.countLabel);
        list = findViewById(R.id.hcList);
        loading = findViewById(R.id.hcLoading);
        stateBox = findViewById(R.id.stateBox);
        stateTitle = findViewById(R.id.stateTitle);
        stateText = findViewById(R.id.stateText);
        stateActions = findViewById(R.id.stateActions);

        setupList();
        setupToolbar();
        load(true);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ---- Setup ---------------------------------------------------------------

    private void setupList() {
        // Two columns on phones, more as the screen widens (portal: 150 dp minimum card width).
        float widthDp = getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density;
        int span = Math.max(2, (int) (widthDp / 170));
        GridLayoutManager layout = new GridLayoutManager(this, span);
        adapter = new HcLibraryAdapter(item -> openProduct(item.gtin));
        layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isFooter(position) ? span : 1;
            }
        });
        list.setLayoutManager(layout);
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || !hasMore || loadingFirst || loadingMore || error != null) return;
                if (layout.findLastVisibleItemPosition() >= adapter.getItemCount() - 1 - span * 2) {
                    page++;
                    load(false);
                }
            }
        });
    }

    private void setupToolbar() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                handler.removeCallbacks(applyTypedSearch);
                handler.postDelayed(applyTypedSearch, SEARCH_DEBOUNCE_MS);
            }
        });
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                handler.removeCallbacks(applyTypedSearch);
                applySearch(text(etSearch));
                hideKeyboard();
                return true;
            }
            return false;
        });
        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));

        findViewById(R.id.brandField).setOnClickListener(v -> {
            if (brand != null) {
                setBrand(null);   // the field's "×" clears the filter
            } else {
                showBrandPicker();
            }
        });

        switchImages.setChecked(imagesOnly);
        switchImages.setOnCheckedChangeListener((b, checked) -> {
            if (checked == imagesOnly) return;
            imagesOnly = checked;
            load(true);
        });
        findViewById(R.id.imagesLabel).setOnClickListener(v -> switchImages.toggle());
        renderBrand();
    }

    private void applySearch(String value) {
        String trimmed = value.trim();
        if (trimmed.equals(search)) return;
        search = trimmed;
        load(true);
    }

    private void setBrand(@Nullable String value) {
        brand = value;
        renderBrand();
        load(true);
    }

    private void renderBrand() {
        boolean selected = brand != null;
        brandLabel.setText(selected ? brand : getString(R.string.hc_all_brands));
        brandLabel.setTextColor(ContextCompat.getColor(this, selected ? R.color.text_primary : R.color.text_tertiary));
        brandIcon.setImageResource(selected ? R.drawable.ic_close : R.drawable.ic_chevron_down);
    }

    /** Searchable single-choice brand list ("All brands" first). */
    private void showBrandPicker() {
        if (brands.isEmpty()) {
            AddProductFlow.toast(this, R.string.hc_no_brands);
            return;
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(8), dp(20), 0);

        EditText filter = new EditText(this);
        filter.setHint(R.string.hc_search_brands);
        filter.setSingleLine(true);
        filter.setTextSize(14);
        root.addView(filter, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        List<String> options = new ArrayList<>();
        options.add(getString(R.string.hc_all_brands));
        options.addAll(brands);
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new ArrayList<>(options));
        ListView listView = new ListView(this);
        listView.setAdapter(listAdapter);
        root.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(360)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.label_brand)
                .setView(root)
                .setNegativeButton(R.string.cancel_btn, null)
                .create();

        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim().toLowerCase(java.util.Locale.ROOT);
                listAdapter.clear();
                if (q.isEmpty()) {
                    listAdapter.addAll(options);
                } else {
                    for (String b : brands) {
                        if (b.toLowerCase(java.util.Locale.ROOT).contains(q)) listAdapter.add(b);
                    }
                }
                listAdapter.notifyDataSetChanged();
            }
        });
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String picked = listAdapter.getItem(position);
            dialog.dismiss();
            boolean all = picked == null || picked.equals(getString(R.string.hc_all_brands));
            setBrand(all ? null : picked);
        });
        dialog.show();
    }

    private void clearFilters() {
        handler.removeCallbacks(applyTypedSearch);
        search = "";
        etSearch.setText("");
        handler.removeCallbacks(applyTypedSearch);   // the text change above re-queued it
        brand = null;
        renderBrand();
        imagesOnly = false;
        switchImages.setChecked(false);
        load(true);
    }

    private boolean hasFilters() {
        return !search.isEmpty() || brand != null || imagesOnly;
    }

    // ---- Loading -------------------------------------------------------------

    private void load(boolean reset) {
        final int gen = ++generation;
        if (reset) {
            page = 1;
            hasMore = false;
            loadingFirst = true;
            loadingMore = false;
            error = null;
        } else {
            loadingMore = true;
            adapter.setLoadingMore(true);
        }
        render();

        ApiClient.get(this).getHcLibrary(search, brand, imagesOnly, page, PAGE_SIZE,
                new ApiCallback<HcLibraryData>() {
                    @Override
                    public void onSuccess(HcLibraryData data) {
                        if (gen != generation || isFinishing() || isDestroyed()) return;
                        loadedOnce = true;
                        loadingFirst = false;
                        loadingMore = false;
                        adapter.setLoadingMore(false);
                        if (data == null) {
                            onFailure(reset, getString(R.string.hc_load_failed));
                            return;
                        }
                        available = data.available;
                        libraryReady = data.available && data.library_ready;
                        total = Math.max(0, data.total);
                        if (data.brands != null && !data.brands.isEmpty()) {
                            brands.clear();
                            brands.addAll(data.brands);
                        }
                        if (reset) items.clear();
                        List<HcLibraryData.Item> pageItems = data.items != null ? data.items : new ArrayList<>();
                        for (HcLibraryData.Item item : pageItems) {
                            if (item != null && !AddProductFlow.isBlank(item.gtin)) items.add(item);
                        }
                        hasMore = !pageItems.isEmpty() && items.size() < total;
                        adapter.setItems(items);
                        if (reset) list.scrollToPosition(0);
                        render();
                    }

                    @Override
                    public void onError(String message) {
                        if (gen != generation || isFinishing() || isDestroyed()) return;
                        loadingFirst = false;
                        loadingMore = false;
                        adapter.setLoadingMore(false);
                        onFailure(reset, AddProductFlow.isBlank(message) ? getString(R.string.hc_load_failed) : message);
                    }
                });
    }

    private void onFailure(boolean reset, String message) {
        if (reset) {
            error = message;
            items.clear();
            adapter.setItems(items);
        } else {
            page = Math.max(1, page - 1);   // let the next scroll retry this page
            AddProductFlow.toast(this, message);
        }
        render();
    }

    private void render() {
        // Count line
        if (loadedOnce && available && libraryReady && error == null) {
            countLabel.setText(loadingFirst ? getString(R.string.hc_searching)
                    : getString(total == 1 ? R.string.hc_count_one : R.string.hc_count_many,
                    AddProductFlow.count(total)));
        } else {
            countLabel.setText(loadingFirst ? getString(R.string.hc_searching) : "");
        }

        loading.setVisibility(loadingFirst ? View.VISIBLE : View.GONE);
        boolean showList = !loadingFirst && error == null && available && libraryReady && !items.isEmpty();
        list.setVisibility(showList ? View.VISIBLE : View.INVISIBLE);

        stateActions.removeAllViews();
        stateText.setVisibility(View.GONE);
        if (loadingFirst || showList) {
            stateBox.setVisibility(View.GONE);
            return;
        }
        stateBox.setVisibility(View.VISIBLE);
        if (error != null) {
            stateTitle.setText(R.string.hc_load_failed_title);
            showStateText(error);
            addStateButton(R.string.hc_retry, true, v -> load(true));
        } else if (!available) {
            stateTitle.setText(R.string.hc_unavailable);
            addStateButton(R.string.back, false, v -> finish());
        } else if (!libraryReady) {
            stateTitle.setText(R.string.hc_preparing);
            showStateText(getString(R.string.hc_preparing_hint));
            addStateButton(R.string.hc_enter_barcode, true,
                    v -> flowLauncher.launch(new Intent(this, BarcodeVerifyActivity.class)));
        } else {
            stateTitle.setText(R.string.hc_no_match);
            if (hasFilters()) {
                addStateButton(R.string.hc_clear_filters, false, v -> clearFilters());
            }
            if (AddProductFlow.isBarcode(search)) {
                final String barcode = search;
                addStateButton(R.string.hc_lookup_barcode, true, v -> openProduct(barcode));
            }
        }
    }

    private void showStateText(String text) {
        stateText.setText(text);
        stateText.setVisibility(View.VISIBLE);
    }

    private void addStateButton(int textRes, boolean primary, View.OnClickListener onClick) {
        RobotoBoldTextView button = new RobotoBoldTextView(this);
        button.setText(textRes);
        button.setTextSize(14);
        button.setGravity(android.view.Gravity.CENTER);
        button.setMinWidth(dp(160));
        button.setPadding(dp(20), 0, dp(20), 0);
        button.setBackgroundResource(primary ? R.drawable.bg_hc_button : R.drawable.bg_hc_button_outline);
        button.setTextColor(primary ? 0xFFFFFFFF : ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        if (stateActions.getChildCount() > 0) lp.topMargin = dp(10);
        button.setOnClickListener(onClick);
        stateActions.addView(button, lp);
    }

    // ---- Product lookup ------------------------------------------------------

    /** Fetch the full Hirt &amp; Carter record for a card (or a typed barcode) and open it. */
    private void openProduct(String gtin) {
        if (lookupGtin != null || AddProductFlow.isBlank(gtin)) return;
        lookupGtin = gtin;
        adapter.setBusyGtin(gtin);
        ApiClient.get(this).hcLookup(gtin, new ApiCallback<HcLookupData>() {
            @Override
            public void onSuccess(HcLookupData data) {
                if (isFinishing() || isDestroyed()) return;
                endLookup();
                if (data != null && data.available && data.found && data.product != null) {
                    Intent i = new Intent(HcLibraryActivity.this, HcProductActivity.class);
                    i.putExtra(AddProductFlow.EXTRA_HC_PRODUCT, data.product);
                    i.putExtra(AddProductFlow.EXTRA_BARCODE, gtin);
                    flowLauncher.launch(i);
                } else {
                    AddProductFlow.toast(HcLibraryActivity.this, data != null && data.available
                            ? R.string.hc_not_in_catalogue : R.string.hc_catalogue_unavailable);
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                endLookup();
                AddProductFlow.toast(HcLibraryActivity.this, R.string.hc_details_failed);
            }
        });
    }

    private void endLookup() {
        lookupGtin = null;
        adapter.setBusyGtin(null);
    }

    // ---- Helpers -------------------------------------------------------------

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }

    private static String text(TextView v) {
        return v.getText() == null ? "" : v.getText().toString();
    }

    private int dp(int v) {
        return AddProductFlow.dp(this, v);
    }
}
