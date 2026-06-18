package com.anokix.distributor.ui;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.anokix.distributor.R;
import com.anokix.distributor.model.BusinessSettings;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.AutocompleteData;
import com.anokix.distributor.network.dto.CoordinateData;
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

public class SettingsBusinessActivity extends BaseSettingsFormActivity implements OnMapReadyCallback {

    private MapView businessMap;
    private GoogleMap googleMap;
    private Marker marker;

    private Double pickedLat;
    private Double pickedLng;

    private android.widget.ListPopupWindow addressPopup;
    private final List<AutocompleteData.Prediction> predictions = new ArrayList<>();
    private final Handler autocompleteHandler = new Handler(Looper.getMainLooper());
    private Runnable autocompleteRunnable;
    private boolean suppressAddressWatcher;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_settings_business;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.settings_business_title);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.settings_business_sub);
    }

    @Override
    protected void bindViews() {
        super.bindViews();
        setupAddressAutocomplete();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        businessMap = findViewById(R.id.businessMap);
        businessMap.onCreate(savedInstanceState);
        businessMap.getMapAsync(this);

        ScrollView scroll = findViewById(R.id.businessScroll);
        businessMap.setOnTouchListener((v, ev) -> {
            int action = ev.getActionMasked();
            boolean holding = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE;
            scroll.requestDisallowInterceptTouchEvent(holding);
            return false;
        });
    }

    @Override
    protected void loadData() {
        BusinessSettings data = repository.getBusinessSettings();
        SettingsUiHelper.setText(findViewById(R.id.etBusinessName), data.businessName);
        SettingsUiHelper.setText(findViewById(R.id.etRegistrationNumber), data.registrationNumber);
        SettingsUiHelper.setText(findViewById(R.id.etVatNumber), data.vatNumber);
        setAddressText(data.address);
        SettingsUiHelper.setText(findViewById(R.id.etCity), data.city);
        pickedLat = data.latitude;
        pickedLng = data.longitude;
        if (googleMap != null && pickedLat != null && pickedLng != null) {
            moveMapTo(new LatLng(pickedLat, pickedLng), true);
        }
    }

    @Override
    protected void onSave() {
        String businessName = SettingsUiHelper.textOf(findViewById(R.id.etBusinessName));
        if (TextUtils.isEmpty(businessName)) {
            toast(R.string.err_business_name);
            return;
        }

        BusinessSettings updated = new BusinessSettings(
                businessName,
                SettingsUiHelper.textOf(findViewById(R.id.etRegistrationNumber)),
                SettingsUiHelper.textOf(findViewById(R.id.etVatNumber)),
                SettingsUiHelper.textOf(findViewById(R.id.etAddressSearch)),
                SettingsUiHelper.textOf(findViewById(R.id.etCity)),
                pickedLat,
                pickedLng
        );
        repository.saveBusinessSettings(updated);
        toastSaved();
        finish();
    }

    private void setupAddressAutocomplete() {
        EditText field = findViewById(R.id.etAddressSearch);
        addressPopup = new android.widget.ListPopupWindow(this);
        addressPopup.setAnchorView(field);
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (suppressAddressWatcher) {
                    return;
                }
                String keyword = s.toString().trim();
                if (autocompleteRunnable != null) {
                    autocompleteHandler.removeCallbacks(autocompleteRunnable);
                }
                if (keyword.length() < 3) {
                    addressPopup.dismiss();
                    return;
                }
                autocompleteRunnable = () -> queryAutocomplete(keyword);
                autocompleteHandler.postDelayed(autocompleteRunnable, 350);
            }
        });
    }

    private void queryAutocomplete(String keyword) {
        ApiClient.get(this).getAddressAutocomplete(keyword, new ApiCallback<AutocompleteData>() {
            @Override
            public void onSuccess(AutocompleteData data) {
                predictions.clear();
                if (data != null && data.predictions != null) {
                    predictions.addAll(data.predictions);
                }
                showPredictions();
            }

            @Override
            public void onError(String message) {
                // Best-effort autocomplete.
            }
        });
    }

    private void showPredictions() {
        if (predictions.isEmpty()) {
            addressPopup.dismiss();
            return;
        }
        List<String> labels = new ArrayList<>();
        for (AutocompleteData.Prediction p : predictions) {
            labels.add(p.description);
        }
        addressPopup.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, labels));
        addressPopup.setOnItemClickListener((parent, view, position, id) -> {
            addressPopup.dismiss();
            AutocompleteData.Prediction p = predictions.get(position);
            setAddressText(p.description);
            resolvePlace(p.place_id);
        });
        addressPopup.show();
    }

    private void resolvePlace(String placeId) {
        ApiClient.get(this).getCoordinateFromPlaceId(placeId, new ApiCallback<CoordinateData>() {
            @Override
            public void onSuccess(CoordinateData data) {
                if (data == null) {
                    return;
                }
                pickedLat = data.latitude;
                pickedLng = data.longitude;
                if (data.formatted_address != null && !data.formatted_address.isEmpty()) {
                    setAddressText(data.formatted_address);
                }
                moveMapTo(new LatLng(data.latitude, data.longitude), true);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SettingsBusinessActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setAddressText(String value) {
        suppressAddressWatcher = true;
        EditText field = findViewById(R.id.etAddressSearch);
        field.setText(value == null ? "" : value);
        field.setSelection(field.getText().length());
        suppressAddressWatcher = false;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setOnMapClickListener(latLng -> {
            pickedLat = latLng.latitude;
            pickedLng = latLng.longitude;
            placeMarker(latLng);
            reverseGeocode(latLng);
        });
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override public void onMarkerDragStart(Marker m) { }
            @Override public void onMarkerDrag(Marker m) { }
            @Override public void onMarkerDragEnd(Marker m) {
                pickedLat = m.getPosition().latitude;
                pickedLng = m.getPosition().longitude;
                reverseGeocode(m.getPosition());
            }
        });

        if (pickedLat != null && pickedLng != null) {
            moveMapTo(new LatLng(pickedLat, pickedLng), true);
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(-29.8587, 31.0218), 9f));
        }
    }

    private void moveMapTo(LatLng latLng, boolean placePin) {
        if (googleMap == null) {
            return;
        }
        if (placePin) {
            placeMarker(latLng);
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
    }

    private void placeMarker(LatLng latLng) {
        if (googleMap == null) {
            return;
        }
        if (marker == null) {
            marker = googleMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
        } else {
            marker.setPosition(latLng);
        }
    }

    private void reverseGeocode(LatLng latLng) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocation(
                        latLng.latitude, latLng.longitude, 1);
                if (results != null && !results.isEmpty()) {
                    Address address = results.get(0);
                    runOnUiThread(() -> fillAddressFromPin(address));
                }
            } catch (Exception ignored) {
                // Pin coordinates are still captured.
            }
        }).start();
    }

    private void fillAddressFromPin(Address address) {
        if (address.getMaxAddressLineIndex() >= 0) {
            setAddressText(address.getAddressLine(0));
        }
        if (address.getLocality() != null) {
            SettingsUiHelper.setText(findViewById(R.id.etCity), address.getLocality());
        }
    }

    @Override protected void onStart() {
        super.onStart();
        if (businessMap != null) businessMap.onStart();
    }

    @Override protected void onResume() {
        super.onResume();
        if (businessMap != null) businessMap.onResume();
    }

    @Override protected void onPause() {
        if (businessMap != null) businessMap.onPause();
        super.onPause();
    }

    @Override protected void onStop() {
        if (businessMap != null) businessMap.onStop();
        super.onStop();
    }

    @Override protected void onDestroy() {
        if (businessMap != null) businessMap.onDestroy();
        super.onDestroy();
    }

    @Override public void onLowMemory() {
        super.onLowMemory();
        if (businessMap != null) businessMap.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (businessMap != null) {
            businessMap.onSaveInstanceState(outState);
        }
    }
}
