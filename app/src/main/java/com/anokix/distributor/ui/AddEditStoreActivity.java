package com.anokix.distributor.ui;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.anokix.distributor.R;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.BaseInfoData;
import com.anokix.distributor.network.dto.StoreDetailData;
import com.anokix.distributor.ui.views.RobotoTextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Two-step Add / Edit Store screen wired to /stores/create and /stores/update.
 *
 * Step 1 (Store Information): fulfillment options, profile fields, tags, time zone,
 * currency, and a trading days/times grid.
 * Step 2 (Address & Shipping): address search + shipping address fields.
 *
 * Pass {@link #EXTRA_STORE_ID} to open in edit mode (the form is prefilled from the API).
 */
public class AddEditStoreActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_STORE_ID = "store_id";
    public static final String EXTRA_STORE_NAME = "store_name";

    private static final String[] DAY_KEYS = {"day_sun", "day_mon", "day_tue", "day_wed",
            "day_thu", "day_fri", "day_sat"};

    private ViewFlipper flipper;
    private TextView stepDot2;
    private TextView stepLabel2;

    private EditText etName, etBuilding, etContact, etVat, etTag, etDescription;
    private EditText etAddressSearch, etStreet1, etStreet2, etSuburb, etCity, etState,
            etCountry, etPostal;
    private Spinner timeZoneSpinner, currencySpinner;
    private LinearLayout tagContainer, tradingContainer;
    private View statsRow;
    private TextView followerCount, reviewCount;
    private View cardCollect, cardDeliver;
    private View checkCollect, checkDeliver;
    private TextView btnSubmit;

    private boolean clickCollect = true;
    private boolean clickDeliver = true;

    private final List<String> tags = new ArrayList<>();
    private final List<TradingRow> tradingRows = new ArrayList<>();
    private final List<BaseInfoData.Timezone> timezones = new ArrayList<>();
    private final List<BaseInfoData.Currency> currencies = new ArrayList<>();

    private String storeId;
    // pending values prefilled before spinner data arrives
    private String pendingTimeZone;
    private String pendingCurrencyId;

    private MapView storeMap;
    private GoogleMap googleMap;
    private Marker storeMarker;
    private Double pickedLat;
    private Double pickedLng;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_store);

        storeId = getIntent().getStringExtra(EXTRA_STORE_ID);

        bindViews();
        buildTradingRows();
        setupHeader();
        setupFulfillment();
        setupTags();
        setupStepNavigation();
        setupMap(savedInstanceState);

        loadBaseInfo();
        if (isEditMode()) {
            btnSubmit.setText(R.string.update_btn);
            loadStore();
        } else {
            btnSubmit.setText(R.string.create_btn);
        }
    }

    private boolean isEditMode() {
        return storeId != null && !storeId.isEmpty();
    }

    private void bindViews() {
        flipper = findViewById(R.id.storeFlipper);
        stepDot2 = findViewById(R.id.stepDot2);
        stepLabel2 = findViewById(R.id.stepLabel2);

        etName = findViewById(R.id.etName);
        etBuilding = findViewById(R.id.etBuilding);
        etContact = findViewById(R.id.etContact);
        etVat = findViewById(R.id.etVat);
        etTag = findViewById(R.id.etTag);
        etDescription = findViewById(R.id.etDescription);
        etAddressSearch = findViewById(R.id.etAddressSearch);
        etStreet1 = findViewById(R.id.etStreet1);
        etStreet2 = findViewById(R.id.etStreet2);
        etSuburb = findViewById(R.id.etSuburb);
        etCity = findViewById(R.id.etCity);
        etState = findViewById(R.id.etState);
        etCountry = findViewById(R.id.etCountry);
        etPostal = findViewById(R.id.etPostal);

        timeZoneSpinner = findViewById(R.id.timeZoneSpinner);
        currencySpinner = findViewById(R.id.currencySpinner);
        tagContainer = findViewById(R.id.tagContainer);
        tradingContainer = findViewById(R.id.tradingContainer);
        statsRow = findViewById(R.id.statsRow);
        followerCount = findViewById(R.id.followerCount);
        reviewCount = findViewById(R.id.reviewCount);

        cardCollect = findViewById(R.id.cardClickCollect);
        cardDeliver = findViewById(R.id.cardClickDeliver);
        checkCollect = findViewById(R.id.checkCollect);
        checkDeliver = findViewById(R.id.checkDeliver);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void setupHeader() {
        ImageButton back = findViewById(R.id.backButton);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }
        TextView title = findViewById(R.id.screenTitle);
        if (title != null) {
            String name = getIntent().getStringExtra(EXTRA_STORE_NAME);
            if (isEditMode() && name != null && !name.isEmpty()) {
                title.setText(getString(R.string.edit_store_prefix, name));
            } else {
                title.setText(R.string.add_store);
            }
        }
    }

    // ---- Fulfillment -----------------------------------------------------

    private void setupFulfillment() {
        cardCollect.setOnClickListener(v -> {
            clickCollect = !clickCollect;
            applyFulfillment();
        });
        cardDeliver.setOnClickListener(v -> {
            clickDeliver = !clickDeliver;
            applyFulfillment();
        });
        applyFulfillment();
    }

    private void applyFulfillment() {
        cardCollect.setBackgroundResource(clickCollect
                ? R.drawable.bg_fulfillment_selected : R.drawable.bg_fulfillment_unselected);
        checkCollect.setVisibility(clickCollect ? View.VISIBLE : View.INVISIBLE);
        cardDeliver.setBackgroundResource(clickDeliver
                ? R.drawable.bg_fulfillment_selected : R.drawable.bg_fulfillment_unselected);
        checkDeliver.setVisibility(clickDeliver ? View.VISIBLE : View.INVISIBLE);
    }

    // ---- Tags ------------------------------------------------------------

    private void setupTags() {
        etTag.setOnEditorActionListener((v, actionId, event) -> {
            commitTag();
            return true;
        });
    }

    private void commitTag() {
        String raw = etTag.getText() != null ? etTag.getText().toString() : "";
        for (String part : raw.split(",")) {
            String tag = part.trim();
            if (!tag.isEmpty() && !tags.contains(tag)) {
                tags.add(tag);
            }
        }
        etTag.setText("");
        renderTags();
    }

    private void renderTags() {
        tagContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String tag : tags) {
            View chip = inflater.inflate(R.layout.item_tag_chip, tagContainer, false);
            ((TextView) chip.findViewById(R.id.tagText)).setText(tag);
            chip.findViewById(R.id.tagRemove).setOnClickListener(v -> {
                tags.remove(tag);
                renderTags();
            });
            tagContainer.addView(chip);
        }
    }

    // ---- Trading days grid ----------------------------------------------

    private void buildTradingRows() {
        LayoutInflater inflater = LayoutInflater.from(this);
        // header
        tradingContainer.addView(buildTradingHeader());
        for (int i = 0; i < 7; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));

            CheckBox cb = new CheckBox(this);
            cb.setText(resString(DAY_KEYS[i]));
            cb.setTextSize(11);
            cb.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f);
            row.addView(cb, cbLp);

            EditText o1 = buildCell();
            EditText c1 = buildCell();
            EditText o2 = buildCell();
            EditText c2 = buildCell();
            row.addView(o1, cellParams());
            row.addView(c1, cellParams());
            row.addView(o2, cellParams());
            row.addView(c2, cellParams());

            tradingContainer.addView(row);
            tradingRows.add(new TradingRow(cb, o1, c1, o2, c2));
        }
    }

    private View buildTradingHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.addView(headerCell(R.string.col_day, 1.4f));
        header.addView(headerCell(R.string.col_open, 1f));
        header.addView(headerCell(R.string.col_close, 1f));
        header.addView(headerCell(R.string.col_open, 1f));
        header.addView(headerCell(R.string.col_close, 1f));
        return header;
    }

    private TextView headerCell(int textRes, float weight) {
        RobotoTextView tv = new RobotoTextView(this);
        tv.setText(textRes);
        tv.setTextSize(9);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        return tv;
    }

    private EditText buildCell() {
        EditText cell = new EditText(this);
        cell.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        cell.setBackgroundResource(R.drawable.bg_login_input);
        cell.setTextSize(12);
        cell.setGravity(Gravity.CENTER);
        cell.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        cell.setPadding(dp(2), dp(6), dp(2), dp(6));
        return cell;
    }

    private LinearLayout.LayoutParams cellParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        return lp;
    }

    // ---- Step navigation -------------------------------------------------

    private void setupStepNavigation() {
        findViewById(R.id.btnNext).setOnClickListener(v -> showStep(1));
        findViewById(R.id.btnBack).setOnClickListener(v -> showStep(0));
        btnSubmit.setOnClickListener(v -> submit());
    }

    private void showStep(int index) {
        flipper.setDisplayedChild(index);
        boolean second = index == 1;
        stepDot2.setBackgroundResource(second
                ? R.drawable.bg_step_dot_active : R.drawable.bg_step_dot_inactive);
        stepLabel2.setTextColor(ContextCompat.getColor(this,
                second ? R.color.purple_primary : R.color.text_secondary));
    }

    // ---- Map (geolocation picker) ---------------------------------------

    private void setupMap(Bundle savedInstanceState) {
        storeMap = findViewById(R.id.storeMap);
        storeMap.onCreate(savedInstanceState);
        storeMap.getMapAsync(this);

        final ScrollView scroll = findViewById(R.id.step2Scroll);
        // Let the map pan/zoom without the surrounding ScrollView stealing the gesture.
        storeMap.setOnTouchListener((v, ev) -> {
            int action = ev.getActionMasked();
            boolean holding = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE;
            scroll.requestDisallowInterceptTouchEvent(holding);
            return false;
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setOnMapClickListener(this::placePin);
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) { }

            @Override
            public void onMarkerDrag(Marker marker) { }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                updatePicked(marker.getPosition());
            }
        });

        LatLng start;
        if (pickedLat != null && pickedLng != null) {
            start = new LatLng(pickedLat, pickedLng);
            placeMarkerAt(start);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 15f));
        } else {
            start = new LatLng(-29.8587, 31.0218); // Durban, South Africa
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 9f));
        }
    }

    private void placePin(LatLng latLng) {
        placeMarkerAt(latLng);
        updatePicked(latLng);
    }

    private void placeMarkerAt(LatLng latLng) {
        if (googleMap == null) {
            return;
        }
        if (storeMarker == null) {
            storeMarker = googleMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
        } else {
            storeMarker.setPosition(latLng);
        }
    }

    private void updatePicked(LatLng latLng) {
        pickedLat = latLng.latitude;
        pickedLng = latLng.longitude;
        reverseGeocode(latLng);
    }

    /** Best-effort: fill the shipping fields from the dropped pin. Runs off the UI thread. */
    private void reverseGeocode(LatLng latLng) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (results != null && !results.isEmpty()) {
                    final Address a = results.get(0);
                    runOnUiThread(() -> fillAddressFromPin(a));
                }
            } catch (Exception ignored) {
                // Geocoder may be unavailable/offline; pin lat/lng is still captured.
            }
        }).start();
    }

    private void fillAddressFromPin(Address a) {
        if (a.getMaxAddressLineIndex() >= 0) {
            etAddressSearch.setText(a.getAddressLine(0));
        }
        if (a.getThoroughfare() != null) {
            etStreet1.setText(a.getThoroughfare());
        }
        if (a.getSubLocality() != null) {
            etSuburb.setText(a.getSubLocality());
        }
        if (a.getLocality() != null) {
            etCity.setText(a.getLocality());
        }
        if (a.getAdminArea() != null) {
            etState.setText(a.getAdminArea());
        }
        if (a.getCountryName() != null) {
            etCountry.setText(a.getCountryName());
        }
        if (a.getPostalCode() != null) {
            etPostal.setText(a.getPostalCode());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (storeMap != null) {
            storeMap.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (storeMap != null) {
            storeMap.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (storeMap != null) {
            storeMap.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (storeMap != null) {
            storeMap.onStop();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (storeMap != null) {
            storeMap.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@Nullable Bundle outState) {
        super.onSaveInstanceState(outState);
        if (storeMap != null && outState != null) {
            storeMap.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (storeMap != null) {
            storeMap.onLowMemory();
        }
    }

    // ---- Load base info + store -----------------------------------------

    private void loadBaseInfo() {
        ApiClient.get(this).getBaseInfo(new ApiCallback<BaseInfoData>() {
            @Override
            public void onSuccess(BaseInfoData data) {
                if (data != null) {
                    if (data.timezones != null) {
                        timezones.addAll(data.timezones);
                    }
                    currencies.add(null); // placeholder "select a currency"
                    if (data.currencies != null) {
                        currencies.addAll(data.currencies);
                    }
                    populateSpinners();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AddEditStoreActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void populateSpinners() {
        List<String> tzLabels = new ArrayList<>();
        for (BaseInfoData.Timezone tz : timezones) {
            tzLabels.add(tzLabel(tz));
        }
        timeZoneSpinner.setAdapter(simpleAdapter(tzLabels));

        List<String> curLabels = new ArrayList<>();
        for (BaseInfoData.Currency c : currencies) {
            curLabels.add(c == null ? getString(R.string.select_currency) : c.code + " - " + c.name);
        }
        currencySpinner.setAdapter(simpleAdapter(curLabels));

        applyPendingSelections();
    }

    private void applyPendingSelections() {
        if (pendingTimeZone != null) {
            for (int i = 0; i < timezones.size(); i++) {
                if (pendingTimeZone.equals(timezones.get(i).time_zone)) {
                    timeZoneSpinner.setSelection(i);
                    break;
                }
            }
        }
        if (pendingCurrencyId != null) {
            for (int i = 0; i < currencies.size(); i++) {
                BaseInfoData.Currency c = currencies.get(i);
                if (c != null && pendingCurrencyId.equals(c.id)) {
                    currencySpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private ArrayAdapter<String> simpleAdapter(List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.item_spinner, items);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        return adapter;
    }

    private String tzLabel(BaseInfoData.Timezone tz) {
        long hours = tz.time_offset / 3600;
        String sign = hours >= 0 ? "+" : "";
        return tz.name + " (GMT" + sign + hours + ")";
    }

    private void loadStore() {
        ApiClient.get(this).getStore(storeId, new ApiCallback<StoreDetailData>() {
            @Override
            public void onSuccess(StoreDetailData data) {
                if (data != null && data.store != null) {
                    prefill(data.store);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AddEditStoreActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void prefill(StoreDetailData.Store s) {
        TextView title = findViewById(R.id.screenTitle);
        if (title != null && s.name != null) {
            title.setText(getString(R.string.edit_store_prefix, s.name));
        }
        statsRow.setVisibility(View.VISIBLE);
        followerCount.setText(String.valueOf(s.follower_count));
        reviewCount.setText(String.valueOf(s.review_count));

        etName.setText(s.name);
        etBuilding.setText(s.building_number);
        etContact.setText(s.contact_number);
        etVat.setText(s.vat_number);
        etDescription.setText(s.description);
        etAddressSearch.setText(s.street != null ? s.street : s.address);
        etStreet1.setText(s.address_street1);
        etStreet2.setText(s.address_street2);
        etSuburb.setText(s.address_suburb);
        etCity.setText(s.address_city);
        etState.setText(s.address_state);
        etCountry.setText(s.address_country);
        etPostal.setText(s.address_postal_code);

        clickCollect = s.is_click_collect;
        clickDeliver = s.is_click_deliver;
        applyFulfillment();

        if (s.tag_list != null && !s.tag_list.trim().isEmpty()) {
            for (String t : s.tag_list.split(",")) {
                String tag = t.trim();
                if (!tag.isEmpty() && !tags.contains(tag)) {
                    tags.add(tag);
                }
            }
            renderTags();
        }

        pendingTimeZone = s.time_zone;
        pendingCurrencyId = s.currency_id;
        applyPendingSelections();

        parseTradingTime(s.trading_time);

        if (s.latitude != null && s.longitude != null) {
            pickedLat = s.latitude;
            pickedLng = s.longitude;
            if (googleMap != null) {
                LatLng ll = new LatLng(pickedLat, pickedLng);
                placeMarkerAt(ll);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 15f));
            }
        }
    }

    private void parseTradingTime(String tradingTime) {
        if (tradingTime == null || tradingTime.trim().isEmpty()) {
            return;
        }
        for (String part : tradingTime.split("/")) {
            if (part.trim().isEmpty()) {
                continue;
            }
            String[] kv = part.split(":", 2);
            if (kv.length < 2) {
                continue;
            }
            int idx;
            try {
                idx = Integer.parseInt(kv[0].trim());
            } catch (NumberFormatException e) {
                continue;
            }
            if (idx < 0 || idx >= tradingRows.size()) {
                continue;
            }
            TradingRow row = tradingRows.get(idx);
            row.enabled.setChecked(true);
            String[] ranges = kv[1].split(",");
            fillRange(ranges.length > 0 ? ranges[0] : "", row.open1, row.close1);
            fillRange(ranges.length > 1 ? ranges[1] : "", row.open2, row.close2);
        }
    }

    private void fillRange(String range, EditText open, EditText close) {
        if (range == null || range.trim().isEmpty()) {
            return;
        }
        String[] oc = range.split("-");
        if (oc.length >= 1) {
            open.setText(cleanHour(oc[0]));
        }
        if (oc.length >= 2) {
            close.setText(cleanHour(oc[1]));
        }
    }

    private String cleanHour(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        int dot = v.indexOf('.');
        return dot >= 0 ? v.substring(0, dot) : v;
    }

    // ---- Submit ----------------------------------------------------------

    private void submit() {
        String name = text(etName);
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.store_name_required, Toast.LENGTH_SHORT).show();
            showStep(0);
            etName.requestFocus();
            return;
        }

        Map<String, String> form = new TreeMap<>();
        if (isEditMode()) {
            form.put("id", storeId);
        }
        form.put("name", name);
        form.put("building_number", text(etBuilding));
        form.put("contact_number", text(etContact));
        form.put("vat_number", text(etVat));
        form.put("description", text(etDescription));
        form.put("tag_list", TextUtils.join(",", tags));
        form.put("is_click_collect", clickCollect ? "1" : "0");
        form.put("is_click_deliver", clickDeliver ? "1" : "0");

        // Address / shipping
        form.put("street", text(etAddressSearch));
        form.put("address_street1", text(etStreet1));
        form.put("address_street2", text(etStreet2));
        form.put("address_suburb", text(etSuburb));
        form.put("address_city", text(etCity));
        form.put("address_state", text(etState));
        form.put("address_country", text(etCountry));
        form.put("address_postal_code", text(etPostal));

        // Time zone + offset
        int tzPos = timeZoneSpinner.getSelectedItemPosition();
        if (tzPos >= 0 && tzPos < timezones.size()) {
            BaseInfoData.Timezone tz = timezones.get(tzPos);
            form.put("time_zone", tz.time_zone);
            form.put("time_offset", String.valueOf(tz.time_offset));
        }

        // Currency
        int curPos = currencySpinner.getSelectedItemPosition();
        if (curPos >= 0 && curPos < currencies.size() && currencies.get(curPos) != null) {
            form.put("currency_id", currencies.get(curPos).id);
        }

        // Geolocation from the map pin
        if (pickedLat != null && pickedLng != null) {
            form.put("latitude", String.valueOf(pickedLat));
            form.put("longitude", String.valueOf(pickedLng));
        }

        // Trading times + working days
        form.put("trading_time", buildTradingTime());
        form.put("working_days", buildWorkingDays());

        btnSubmit.setEnabled(false);
        ApiCallback<StoreDetailData> cb = new ApiCallback<StoreDetailData>() {
            @Override
            public void onSuccess(StoreDetailData data) {
                Toast.makeText(AddEditStoreActivity.this,
                        isEditMode() ? R.string.store_updated_msg : R.string.store_created_msg,
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String message) {
                btnSubmit.setEnabled(true);
                Toast.makeText(AddEditStoreActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };
        if (isEditMode()) {
            ApiClient.get(this).updateStore(form, cb);
        } else {
            ApiClient.get(this).createStore(form, cb);
        }
    }

    private String buildTradingTime() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tradingRows.size(); i++) {
            TradingRow row = tradingRows.get(i);
            if (!row.enabled.isChecked()) {
                continue;
            }
            String o1 = text(row.open1);
            String c1 = text(row.close1);
            if (o1.isEmpty() || c1.isEmpty()) {
                continue;
            }
            StringBuilder day = new StringBuilder();
            day.append(i).append(':').append(o1).append(".00-").append(c1);
            String o2 = text(row.open2);
            String c2 = text(row.close2);
            if (!o2.isEmpty() && !c2.isEmpty()) {
                day.append(',').append(o2).append('-').append(c2);
            }
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(day);
        }
        return sb.toString();
    }

    private String buildWorkingDays() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tradingRows.size(); i++) {
            if (tradingRows.get(i).enabled.isChecked()) {
                sb.append(i);
            }
        }
        return sb.toString();
    }

    // ---- helpers ---------------------------------------------------------

    private String text(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private String resString(String name) {
        int id = getResources().getIdentifier(name, "string", getPackageName());
        return id != 0 ? getString(id) : name;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static class TradingRow {
        final CheckBox enabled;
        final EditText open1, close1, open2, close2;

        TradingRow(CheckBox enabled, EditText open1, EditText close1, EditText open2, EditText close2) {
            this.enabled = enabled;
            this.open1 = open1;
            this.close1 = close1;
            this.open2 = open2;
            this.close2 = close2;
        }
    }
}
