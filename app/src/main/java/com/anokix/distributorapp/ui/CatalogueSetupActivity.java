package com.anokix.distributorapp.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.ProductDetailData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Product: …" — price, stock and promotion for a library product being added to the
 * distributor's catalogue (the portal's catalogue setup step after "Add To Catalogue").
 *
 * Submits through products/create with the found record's own fields plus the values
 * entered here, the same payload the portal builds.
 */
public class CatalogueSetupActivity extends AppCompatActivity {

    /** Record fields the portal forwards to products/create (blank values are left out). */
    private static final String[] FORWARDED_FIELDS = {
            "barcode", "sku", "name", "supplier_id", "description", "brand_id", "pack_size_id",
            "unit_id", "parent_category_id", "category_id", "weight", "dim_type", "dim_weight",
            "dim_length", "dim_width", "dim_height", "tiered_price", "shrink_code", "unit_code",
            "units_per_case", "min_order_quantity", "max_order_quantity", "supplier_product_code",
            "tag_list", "cost_price", "hc_gtin", "hc_image"
    };

    private JsonObject product;
    private EditText etSellingPrice, etStock, etReorder, etMaxStock, etPromoPrice;
    private TextView promoStart, promoEnd, availableLabel;
    private SwitchCompat switchAvailable;
    private View btnSave, saveLabel, saveProgress;

    private String promoStartDate = "";   // yyyy-MM-dd
    private String promoEndDate = "";
    private boolean saving;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalogue_setup);

        try {
            product = JsonParser.parseString(
                    getIntent().getStringExtra(AddProductFlow.EXTRA_PRODUCT_JSON)).getAsJsonObject();
        } catch (Exception e) {
            finish();
            return;
        }

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.catalogueTitle)).setText(
                getString(R.string.catalogue_product_title, ProductFoundActivity.displayName(product)));

        etSellingPrice = findViewById(R.id.etSellingPrice);
        etStock = findViewById(R.id.etStock);
        etReorder = findViewById(R.id.etReorder);
        etMaxStock = findViewById(R.id.etMaxStock);
        etPromoPrice = findViewById(R.id.etPromoPrice);
        promoStart = findViewById(R.id.promoStart);
        promoEnd = findViewById(R.id.promoEnd);
        switchAvailable = findViewById(R.id.switchAvailable);
        availableLabel = findViewById(R.id.availableLabel);
        btnSave = findViewById(R.id.btnSave);
        saveLabel = findViewById(R.id.saveLabel);
        saveProgress = findViewById(R.id.saveProgress);

        for (EditText field : requiredFields()) {
            field.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    field.setBackgroundResource(R.drawable.bg_login_input);
                }
            });
        }

        promoStart.setOnClickListener(v -> pickDate(true));
        promoEnd.setOnClickListener(v -> pickDate(false));
        switchAvailable.setOnCheckedChangeListener((b, checked) -> renderAvailable());
        renderAvailable();

        btnSave.setOnClickListener(v -> save());
    }

    private EditText[] requiredFields() {
        return new EditText[]{etSellingPrice, etStock, etReorder, etMaxStock, etPromoPrice};
    }

    private void renderAvailable() {
        boolean on = switchAvailable.isChecked();
        availableLabel.setText(on ? R.string.catalogue_yes : R.string.catalogue_no);
        availableLabel.setTextColor(ContextCompat.getColor(this, on ? R.color.success : R.color.text_secondary));
    }

    private void pickDate(boolean isStart) {
        Calendar c = Calendar.getInstance();
        int[] current = split(isStart ? promoStartDate : promoEndDate);
        if (current != null) c.set(current[0], current[1] - 1, current[2]);
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            String api = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            String shown = String.format(Locale.US, "%02d/%02d/%04d", day, month + 1, year);
            if (isStart) {
                promoStartDate = api;
                promoStart.setText(shown);
                // An end date before the new start no longer makes sense.
                if (!promoEndDate.isEmpty() && promoEndDate.compareTo(api) < 0) {
                    promoEndDate = "";
                    promoEnd.setText("");
                }
            } else {
                promoEndDate = api;
                promoEnd.setText(shown);
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        if (!isStart) {
            int[] start = split(promoStartDate);
            if (start != null) {
                Calendar min = Calendar.getInstance();
                min.set(start[0], start[1] - 1, start[2], 0, 0, 0);
                dialog.getDatePicker().setMinDate(min.getTimeInMillis());
            }
        }
        dialog.show();
    }

    @Nullable
    private static int[] split(String apiDate) {
        if (apiDate == null || apiDate.isEmpty()) return null;
        String[] p = apiDate.split("-");
        if (p.length != 3) return null;
        try {
            return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- Save --------------------------------------------------------------

    /** Every number here is required and must be above zero, as on the portal. */
    private boolean validate() {
        List<EditText> invalid = new ArrayList<>();
        for (EditText field : requiredFields()) {
            if (!isPositiveNumber(text(field))) invalid.add(field);
        }
        for (EditText field : requiredFields()) {
            field.setBackgroundResource(invalid.contains(field)
                    ? R.drawable.bg_hc_input_invalid : R.drawable.bg_login_input);
        }
        if (!invalid.isEmpty()) {
            AddProductFlow.toast(this, R.string.err_fill_required);
            invalid.get(0).requestFocus();
            return false;
        }
        return true;
    }

    private static boolean isPositiveNumber(String s) {
        try {
            return !s.isEmpty() && Double.parseDouble(s) != 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void save() {
        if (saving || !validate()) return;

        Map<String, String> form = new LinkedHashMap<>();
        // The portal sends the record's id along with its fields.
        put(form, "id", AddProductFlow.str(product, "id", "product_id"));
        for (String key : FORWARDED_FIELDS) {
            put(form, key, value(product.get(key)));
        }
        // The portal's record mapping falls back to these aliases.
        if (form.get("name") == null) put(form, "name", AddProductFlow.str(product, "description"));
        if (form.get("sku") == null) put(form, "sku", AddProductFlow.str(product, "barcode"));
        if (form.get("units_per_case") == null) put(form, "units_per_case", AddProductFlow.str(product, "case_code"));
        if (form.get("tag_list") == null) put(form, "tag_list", value(product.get("tags")));
        put(form, "case_code", AddProductFlow.str(product, "case_code"));

        form.put("stock", text(etStock));
        form.put("selling_price", text(etSellingPrice));
        form.put("promotion_price", text(etPromoPrice));
        form.put("reorder_level", text(etReorder));
        form.put("max_stock_level", text(etMaxStock));
        put(form, "promotion_start_date", promoStartDate);
        put(form, "promotion_end_date", promoEndDate);
        form.put("available_for_ordering", switchAvailable.isChecked() ? "1" : "0");

        setSaving(true);
        ApiClient.get(this).createProduct(form, new ArrayList<>(), new ApiCallback<ProductDetailData>() {
            @Override
            public void onSuccess(ProductDetailData data) {
                if (isFinishing() || isDestroyed()) return;
                AddProductFlow.toast(CatalogueSetupActivity.this, R.string.catalogue_saved);
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                setSaving(false);
                AddProductFlow.toast(CatalogueSetupActivity.this,
                        AddProductFlow.isBlank(message) ? getString(R.string.err_save_product) : message);
            }
        });
    }

    /** A form value the way the portal's FormData sees it: booleans → 1/0, lists → comma-joined. */
    @Nullable
    private static String value(@Nullable JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive()) {
            if (e.getAsJsonPrimitive().isBoolean()) return e.getAsBoolean() ? "1" : "0";
            return e.getAsString();
        }
        if (e.isJsonArray()) {
            JsonArray array = e.getAsJsonArray();
            StringBuilder sb = new StringBuilder();
            for (JsonElement item : array) {
                if (item == null || !item.isJsonPrimitive()) continue;
                if (sb.length() > 0) sb.append(',');
                sb.append(item.getAsString());
            }
            return sb.length() == 0 ? null : sb.toString();
        }
        return null;   // nested objects are not form fields
    }

    private static void put(Map<String, String> form, String key, @Nullable String value) {
        if (!AddProductFlow.isBlank(value)) form.put(key, value.trim());
    }

    private void setSaving(boolean value) {
        saving = value;
        btnSave.setEnabled(!value);
        saveLabel.setVisibility(value ? View.INVISIBLE : View.VISIBLE);
        saveProgress.setVisibility(value ? View.VISIBLE : View.GONE);
    }

    private static String text(TextView v) {
        return v.getText() == null ? "" : v.getText().toString().trim();
    }
}
