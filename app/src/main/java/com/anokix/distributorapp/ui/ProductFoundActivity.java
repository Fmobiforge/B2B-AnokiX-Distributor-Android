package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.bumptech.glide.Glide;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * "Product found in our library" — the barcode already belongs to a product in the
 * distributor's catalogue. "Add To Catalogue" continues to {@link CatalogueSetupActivity}
 * (price, stock and promotion), as on the portal.
 */
public class ProductFoundActivity extends AppCompatActivity {

    private static final String DASH = "–";

    private JsonObject product;

    private final ActivityResultLauncher<Intent> setupLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    setResult(RESULT_OK);
                    finish();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_found);

        try {
            product = JsonParser.parseString(
                    getIntent().getStringExtra(AddProductFlow.EXTRA_PRODUCT_JSON)).getAsJsonObject();
        } catch (Exception e) {
            finish();
            return;
        }

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        AddProductFlow.clipToOutline(findViewById(R.id.panel));

        ((TextView) findViewById(R.id.productName)).setText(displayName(product));

        String image = AddProductFlow.absoluteUrl(AddProductFlow.str(product,
                "primary_product_image_url", "thumbnail_image", "image_url", "thumbnail", "image", "media"));
        ImageView imageView = findViewById(R.id.productImage);
        if (image != null) {
            Glide.with(this).load(image).fitCenter().into(imageView);
        } else {
            imageView.setBackgroundColor(ContextCompat.getColor(this, R.color.hc_slate_bg));
        }

        addDetail(getString(R.string.label_barcode), orDash(AddProductFlow.str(product, "barcode")));
        addDetail(getString(R.string.label_brand), orDash(brandName(product)));
        addDetail(getString(R.string.label_category), orDash(categoryName(product)));
        addDetail(getString(R.string.label_supplier), orDash(supplierName(product)));

        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            Intent i = new Intent(this, CatalogueSetupActivity.class);
            i.putExtra(AddProductFlow.EXTRA_PRODUCT_JSON, product.toString());
            setupLauncher.launch(i);
        });
        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            setResult(AddProductFlow.RESULT_CLEAR_BARCODE);
            finish();
        });
    }

    static String displayName(JsonObject p) {
        String name = AddProductFlow.str(p, "name", "description", "title");
        return name != null ? name : "Product";
    }

    /** {@code Brand} may be an object or a string; the list endpoints add {@code brand_name}. */
    @Nullable
    private static String brandName(JsonObject p) {
        String fromObject = nameOf(p.get("Brand"));
        if (fromObject != null) return fromObject;
        String fromBrand = nameOf(p.get("brand"));
        return fromBrand != null ? fromBrand : AddProductFlow.str(p, "brand_name");
    }

    /** "Parent > Child" when the category comes with its parent, otherwise its title. */
    @Nullable
    private static String categoryName(JsonObject p) {
        JsonElement c = p.has("Category") ? p.get("Category") : p.get("category");
        if (c != null && c.isJsonObject()) {
            JsonObject cat = c.getAsJsonObject();
            JsonElement parent = cat.has("parent") ? cat.get("parent") : cat.get("Parent");
            String parentName = nameOf(parent);
            String own = nameOf(cat);
            if (parentName != null && own != null) return parentName + " > " + own;
            if (own != null) return own;
        } else if (c != null && c.isJsonPrimitive() && !AddProductFlow.isBlank(c.getAsString())) {
            return c.getAsString();
        }
        return AddProductFlow.str(p, "category_title", "category_name");
    }

    @Nullable
    private static String supplierName(JsonObject p) {
        String s = nameOf(p.has("Supplier") ? p.get("Supplier") : p.get("supplier"));
        return s != null ? s : AddProductFlow.str(p, "supplier_name");
    }

    /** A plain string, or the title / name of a nested object. */
    @Nullable
    private static String nameOf(@Nullable JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive()) {
            String s = e.getAsString();
            return AddProductFlow.isBlank(s) ? null : s.trim();
        }
        if (e.isJsonObject()) return AddProductFlow.str(e.getAsJsonObject(), "title", "name");
        return null;
    }

    private static String orDash(@Nullable String s) {
        return AddProductFlow.isBlank(s) ? DASH : s;
    }

    private void addDetail(String label, String value) {
        LinearLayout container = findViewById(R.id.detailsContainer);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = AddProductFlow.dp(this, 12);
        row.setLayoutParams(lp);

        RobotoBoldTextView labelView = new RobotoBoldTextView(this);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        row.addView(labelView);

        RobotoBoldTextView valueView = new RobotoBoldTextView(this);
        valueView.setText(value);
        valueView.setTextSize(14);
        valueView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vlp.topMargin = AddProductFlow.dp(this, 2);
        valueView.setLayoutParams(vlp);
        row.addView(valueView);

        container.addView(row);
    }
}
