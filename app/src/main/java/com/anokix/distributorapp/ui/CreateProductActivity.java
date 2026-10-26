package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.Http;
import com.anokix.distributorapp.network.dto.BaseInfoData;
import com.anokix.distributorapp.network.dto.ProductDetailData;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;
import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Five-step "Create New Product" wizard wired to /api/distributor/products/create
 * (and …/update when opened with an existing product id, …/delete from the header).
 *
 * Steps: 1 Basic Information · 2 Packaging · 3 Images &amp; Documents ·
 *        4 Pricing &amp; Inventory · 5 Review &amp; Submit.
 *
 * Lookup data (brands, categories, units) comes from {@link ApiClient#getBaseInfo}.
 * Every step lives in a {@link ViewFlipper} so field values survive back/forward
 * navigation. Mirrors the Create Trader wizard.
 */
public class CreateProductActivity extends AppCompatActivity {

    /** Pass an existing product id to open the wizard in edit mode. */
    public static final String EXTRA_PRODUCT_ID = "product_id";

    // Create mode only: values carried over from a barcode lookup / the Hirt & Carter catalogue
    // ("Use this product"). Brand and unit are matched by name once the lookups load, and the
    // chosen catalogue image is imported server-side (hc_gtin + hc_image) unless the user
    // uploads their own.
    public static final String EXTRA_BARCODE = "prefill_barcode";
    public static final String EXTRA_HC_NAME = "hc_name";
    public static final String EXTRA_HC_BRAND = "hc_brand";
    public static final String EXTRA_HC_SIZE = "hc_size";
    public static final String EXTRA_HC_UOM = "hc_uom";
    public static final String EXTRA_HC_IMAGE_GTIN = "hc_image_gtin";
    public static final String EXTRA_HC_IMAGE_FILE = "hc_image_file";
    public static final String EXTRA_HC_IMAGE_URL = "hc_image_url";

    private static final int STEP_COUNT = 5;
    private static final int STEP_REVIEW = 4;

    private static final int[] STEP_LABELS = {
            R.string.step_basic_info, R.string.step_packaging, R.string.step_images_docs,
            R.string.step_pricing, R.string.step_review
    };

    // Measurement type → (dim_type value, weight unit, dimension unit)
    private static final String[] DIM_TYPE_VALUES = {"kilo_centi", "pound_inch"};
    private static final int[] WEIGHT_UNIT_LABELS = {R.string.unit_kg, R.string.unit_lb};
    private static final int[] DIM_UNIT_LABELS = {R.string.unit_cm, R.string.unit_in};

    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    private static final int MAX_ADDITIONAL_IMAGES = 5;

    private ViewFlipper flipper;
    private LinearLayout stepperContainer;
    private TextView btnContinue, btnBack;
    private final TextView[] stepCircles = new TextView[STEP_COUNT];
    private final TextView[] stepLabels = new TextView[STEP_COUNT];
    private int currentStep = 0;

    // Edit mode
    @Nullable private String productId;

    // Hirt & Carter prefill (create mode)
    @Nullable private String hcBrandName;
    @Nullable private String hcUom;
    @Nullable private String hcImageGtin;
    @Nullable private String hcImageFile;
    @Nullable private String hcImageUrl;

    // Base info lookups
    private final List<BaseInfoData.Brand> brands = new ArrayList<>();
    private final List<BaseInfoData.ProductCategory> parentCategories = new ArrayList<>();
    private final List<BaseInfoData.ProductCategory> subCategories = new ArrayList<>();
    private final List<BaseInfoData.ProductUnit> units = new ArrayList<>();

    // Selections
    private Long selectedBrandId;
    private Long selectedParentCategoryId;
    private Long selectedSubCategoryId;
    private String selectedUnitId;
    private int measurementIndex = 0;
    private String promoStartDate = "";   // yyyy-MM-dd
    private String promoEndDate = "";

    // Files
    private enum ImageTarget { PRIMARY, ADDITIONAL }
    private ImageTarget pendingImageTarget;
    private Uri primaryImageUri;
    private final List<Uri> additionalImageUris = new ArrayList<>();
    private Uri datasheetUri;

    // Edit mode: additional images already on the server, plus the ids the user
    // removed (sent back as remove_additional_image_ids on update).
    private static final class ExistingImage {
        final String id, url;
        ExistingImage(String id, String url) { this.id = id; this.url = url; }
    }
    private final List<ExistingImage> existingAdditionalImages = new ArrayList<>();
    private final List<String> removeAdditionalImageIds = new ArrayList<>();
    private ActivityResultLauncher<String> imagePicker;
    private ActivityResultLauncher<String> filePicker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_product);

        flipper = findViewById(R.id.stepFlipper);
        stepperContainer = findViewById(R.id.stepperContainer);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);

        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);

        buildStepper();
        setupNavigation();
        setupSelectionFields();
        setupPickers();
        applyMeasurement(0);   // default: Kilograms / Centimeters
        if (productId == null) {
            applyInitialData();
        }

        showStep(0);
        loadBaseInfo();

        if (productId != null) {
            ((TextView) findViewById(R.id.screenTitle)).setText(R.string.edit_product);
            View delete = findViewById(R.id.btnDelete);
            delete.setVisibility(View.VISIBLE);
            delete.setOnClickListener(v -> confirmDelete());
        }
    }

    // ---- Stepper ---------------------------------------------------------

    private void buildStepper() {
        for (int i = 0; i < STEP_COUNT; i++) {
            if (i > 0) {
                View connector = new View(this);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(20), dp(1));
                clp.setMargins(dp(6), 0, dp(6), 0);
                connector.setLayoutParams(clp);
                connector.setBackgroundColor(ContextCompat.getColor(this, R.color.border));
                stepperContainer.addView(connector);
            }

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);

            RobotoBoldTextView circle = new RobotoBoldTextView(this);
            circle.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(28)));
            circle.setGravity(Gravity.CENTER);
            circle.setTextSize(12);
            circle.setText(String.valueOf(i + 1));
            item.addView(circle);

            RobotoTextView label = new RobotoTextView(this);
            label.setText(STEP_LABELS[i]);
            label.setTextSize(13);
            label.setPadding(dp(6), 0, 0, 0);
            item.addView(label);

            stepperContainer.addView(item);
            stepCircles[i] = circle;
            stepLabels[i] = label;
        }
    }

    private void renderStepper() {
        for (int i = 0; i < STEP_COUNT; i++) {
            TextView circle = stepCircles[i];
            TextView label = stepLabels[i];
            if (i < currentStep) {
                circle.setBackgroundResource(R.drawable.bg_step_circle_active);
                circle.setText("✓");
                circle.setTextColor(0xFFFFFFFF);
                label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                label.setTypeface(null, android.graphics.Typeface.NORMAL);
            } else if (i == currentStep) {
                circle.setBackgroundResource(R.drawable.bg_step_circle_active);
                circle.setText(String.valueOf(i + 1));
                circle.setTextColor(0xFFFFFFFF);
                label.setTextColor(ContextCompat.getColor(this, R.color.purple_primary));
                label.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                circle.setBackgroundResource(R.drawable.bg_step_circle_inactive);
                circle.setText(String.valueOf(i + 1));
                circle.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                label.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                label.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    // ---- Navigation ------------------------------------------------------

    private void setupNavigation() {
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> {
            if (currentStep == 0) {
                finish();
            } else {
                showStep(currentStep - 1);
            }
        });
        btnContinue.setOnClickListener(v -> {
            if (!validateStep(currentStep)) {
                return;
            }
            if (currentStep == STEP_REVIEW) {
                submit();
            } else {
                showStep(currentStep + 1);
            }
        });
    }

    private void showStep(int index) {
        currentStep = index;
        flipper.setDisplayedChild(index);
        btnContinue.setText(index == STEP_REVIEW ? R.string.submit_btn : R.string.continue_btn);
        btnBack.setText(index == 0 ? R.string.cancel_btn : R.string.back_btn);
        if (index == STEP_REVIEW) {
            buildReview();
        }
        renderStepper();
        stepperContainer.post(() -> {
            View item = stepLabels[index];
            if (item != null && item.getParent() instanceof View) {
                View parent = (View) item.getParent();
                ((HorizontalScrollView) stepperContainer.getParent())
                        .smoothScrollTo(Math.max(0, parent.getLeft() - dp(40)), 0);
            }
        });
    }

    // ---- Validation ------------------------------------------------------

    private boolean validateStep(int step) {
        switch (step) {
            case 0:
                if (text(R.id.etName).isEmpty()) return fail(R.string.err_product_name);
                if (selectedBrandId == null) return fail(R.string.err_brand);
                if (selectedParentCategoryId == null) return fail(R.string.err_category);
                if (selectedSubCategoryId == null) return fail(R.string.err_sub_category);
                if (text(R.id.etBarcode).isEmpty()) return fail(R.string.err_barcode);
                if (text(R.id.etSku).isEmpty()) return fail(R.string.err_sku);
                return true;
            case 1:
                if (selectedUnitId == null) return fail(R.string.err_unit_type);
                if (text(R.id.etWeight).isEmpty()) return fail(R.string.err_weight);
                if (text(R.id.etDimLength).isEmpty() || text(R.id.etDimWidth).isEmpty()
                        || text(R.id.etDimHeight).isEmpty()) {
                    return fail(R.string.err_dimensions);
                }
                return true;
            case 2:
                if (primaryImageUri == null && hcImageFile == null) return fail(R.string.err_primary_image);
                return true;
            case 3:
                if (text(R.id.etSellingPrice).isEmpty()) return fail(R.string.err_selling_price);
                if (text(R.id.etStock).isEmpty()) return fail(R.string.err_stock);
                if (text(R.id.etReorderLevel).isEmpty()) return fail(R.string.err_reorder);
                return true;
            default:
                return true;
        }
    }

    private boolean fail(int msgRes) {
        Toast.makeText(this, msgRes, Toast.LENGTH_SHORT).show();
        return false;
    }

    // ---- Dropdown selection fields --------------------------------------

    private void setupSelectionFields() {
        findViewById(R.id.brandField).setOnClickListener(v -> showBrandDialog());
        findViewById(R.id.categoryField).setOnClickListener(v -> showCategoryDialog());
        findViewById(R.id.subCategoryField).setOnClickListener(v -> showSubCategoryDialog());
        findViewById(R.id.unitTypeField).setOnClickListener(v -> showUnitDialog());
        findViewById(R.id.measurementField).setOnClickListener(v -> showMeasurementDialog());
        findViewById(R.id.promoStartField).setOnClickListener(v -> pickDate(true));
        findViewById(R.id.promoEndField).setOnClickListener(v -> pickDate(false));
    }

    private void showBrandDialog() {
        if (brands.isEmpty()) return;
        String[] names = new String[brands.size()];
        for (int i = 0; i < brands.size(); i++) names[i] = brands.get(i).name;
        new AlertDialog.Builder(this)
                .setTitle(R.string.brand_req)
                .setItems(names, (d, which) -> {
                    selectedBrandId = brands.get(which).id;
                    setDropdownLabel(R.id.brandLabel, names[which]);
                })
                .show();
    }

    private void showCategoryDialog() {
        if (parentCategories.isEmpty()) return;
        String[] names = new String[parentCategories.size()];
        for (int i = 0; i < parentCategories.size(); i++) names[i] = parentCategories.get(i).title;
        new AlertDialog.Builder(this)
                .setTitle(R.string.category_req)
                .setItems(names, (d, which) -> {
                    BaseInfoData.ProductCategory cat = parentCategories.get(which);
                    selectedParentCategoryId = cat.id;
                    setDropdownLabel(R.id.categoryLabel, cat.title);
                    // Reset sub category and repopulate the available children.
                    selectedSubCategoryId = null;
                    setDropdownPlaceholder(R.id.subCategoryLabel, R.string.select_sub_category);
                    rebuildSubCategories();
                })
                .show();
    }

    private void showSubCategoryDialog() {
        if (selectedParentCategoryId == null) {
            Toast.makeText(this, R.string.err_category, Toast.LENGTH_SHORT).show();
            return;
        }
        if (subCategories.isEmpty()) return;
        String[] names = new String[subCategories.size()];
        for (int i = 0; i < subCategories.size(); i++) names[i] = subCategories.get(i).title;
        new AlertDialog.Builder(this)
                .setTitle(R.string.sub_category_req)
                .setItems(names, (d, which) -> {
                    selectedSubCategoryId = subCategories.get(which).id;
                    setDropdownLabel(R.id.subCategoryLabel, names[which]);
                })
                .show();
    }

    private void rebuildSubCategories() {
        subCategories.clear();
        if (selectedParentCategoryId != null) {
            for (BaseInfoData.ProductCategory c : allCategories) {
                if (c.parent_id != null && c.parent_id == (long) selectedParentCategoryId) {
                    subCategories.add(c);
                }
            }
        }
    }

    private void showUnitDialog() {
        if (units.isEmpty()) return;
        String[] names = new String[units.size()];
        for (int i = 0; i < units.size(); i++) names[i] = units.get(i).name;
        new AlertDialog.Builder(this)
                .setTitle(R.string.unit_type_req)
                .setItems(names, (d, which) -> {
                    selectedUnitId = units.get(which).id;
                    setDropdownLabel(R.id.unitTypeLabel, names[which]);
                })
                .show();
    }

    private void showMeasurementDialog() {
        String[] options = {getString(R.string.measurement_kg_cm), getString(R.string.measurement_lb_in)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.measurements_type_req)
                .setItems(options, (d, which) -> applyMeasurement(which))
                .show();
    }

    private void applyMeasurement(int index) {
        measurementIndex = index;
        setDropdownLabel(R.id.measurementLabel,
                getString(index == 0 ? R.string.measurement_kg_cm : R.string.measurement_lb_in));
        ((TextView) findViewById(R.id.weightUnitLabel)).setText(WEIGHT_UNIT_LABELS[index]);
        ((TextView) findViewById(R.id.dimUnitLabel)).setText(DIM_UNIT_LABELS[index]);
    }

    private void pickDate(boolean isStart) {
        Calendar c = Calendar.getInstance();
        // Open on the date the product already carries, not on today.
        int[] parts = splitApiDate(isStart ? promoStartDate : promoEndDate);
        if (parts != null) {
            c.set(parts[0], parts[1] - 1, parts[2]);
        }
        new android.app.DatePickerDialog(this, (view, year, month, day) -> {
            String api = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            String shown = String.format(Locale.US, "%02d/%02d/%04d", day, month + 1, year);
            if (isStart) {
                promoStartDate = api;
                setDropdownLabel(R.id.promoStartLabel, shown);
            } else {
                promoEndDate = api;
                setDropdownLabel(R.id.promoEndLabel, shown);
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Shows a promotion date returned by the API (yyyy-MM-dd) on the matching field.
     * The form keeps the API format for submission and displays dd/MM/yyyy, so editing
     * a product no longer leaves the promotion dates looking empty.
     */
    private void applyPromoDate(String apiDate, boolean isStart) {
        int[] parts = splitApiDate(apiDate);
        if (parts == null) {
            return;
        }
        String api = String.format(Locale.US, "%04d-%02d-%02d", parts[0], parts[1], parts[2]);
        String shown = String.format(Locale.US, "%02d/%02d/%04d", parts[2], parts[1], parts[0]);
        if (isStart) {
            promoStartDate = api;
            setDropdownLabel(R.id.promoStartLabel, shown);
        } else {
            promoEndDate = api;
            setDropdownLabel(R.id.promoEndLabel, shown);
        }
    }

    /**
     * Splits an API date into {year, month, day}, tolerating a trailing time component.
     * Returns null for empty values and the "0000-00-00" placeholder the backend uses
     * when no promotion window is set.
     */
    private int[] splitApiDate(String apiDate) {
        if (apiDate == null || apiDate.trim().isEmpty()) {
            return null;
        }
        String[] parts = apiDate.trim().split("[ T]")[0].split("-");
        if (parts.length != 3) {
            return null;
        }
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            if (year <= 0 || month <= 0 || day <= 0) {
                return null;
            }
            return new int[]{year, month, day};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setDropdownLabel(int id, String value) {
        TextView tv = findViewById(id);
        tv.setText(value);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
    }

    private void setDropdownPlaceholder(int id, int textRes) {
        TextView tv = findViewById(id);
        tv.setText(textRes);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
    }

    // ---- Images & documents ---------------------------------------------

    private void setupPickers() {
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null || pendingImageTarget == null) return;
            if (pendingImageTarget == ImageTarget.PRIMARY) {
                primaryImageUri = uri;
                ImageView preview = findViewById(R.id.primaryImagePreview);
                preview.setVisibility(View.VISIBLE);
                findViewById(R.id.primaryImagePlaceholder).setVisibility(View.GONE);
                Glide.with(this).load(uri).fitCenter().into(preview);
                renderHcImage();   // an upload replaces the catalogue image
            } else {
                if (additionalImageCount() >= MAX_ADDITIONAL_IMAGES) {
                    Toast.makeText(this, R.string.err_max_images, Toast.LENGTH_SHORT).show();
                } else {
                    additionalImageUris.add(uri);
                    renderAdditionalImages();
                }
            }
            pendingImageTarget = null;
        });

        filePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            datasheetUri = uri;
            String name = queryFileName(uri);
            TextView label = findViewById(R.id.datasheetLabel);
            label.setText(name != null ? name : getString(R.string.click_to_upload_file));
            label.setTextColor(ContextCompat.getColor(this, R.color.success));
        });

        findViewById(R.id.primaryImageBox).setOnClickListener(v -> {
            pendingImageTarget = ImageTarget.PRIMARY;
            imagePicker.launch("image/*");
        });
        findViewById(R.id.datasheetBox).setOnClickListener(v -> filePicker.launch("*/*"));

        renderAdditionalImages();
    }

    private int additionalImageCount() {
        return existingAdditionalImages.size() + additionalImageUris.size();
    }

    /** Existing (server) + newly picked thumbnails, then an "Add More" tile (up to 5). */
    private void renderAdditionalImages() {
        ViewGroup container = findViewById(R.id.additionalImagesContainer);
        container.removeAllViews();
        for (int i = 0; i < existingAdditionalImages.size(); i++) {
            final ExistingImage existing = existingAdditionalImages.get(i);
            container.addView(buildThumbnail(existing.url, () -> {
                existingAdditionalImages.remove(existing);
                if (existing.id != null) removeAdditionalImageIds.add(existing.id);
                renderAdditionalImages();
            }));
        }
        for (int i = 0; i < additionalImageUris.size(); i++) {
            final int index = i;
            container.addView(buildThumbnail(additionalImageUris.get(i), () -> {
                additionalImageUris.remove(index);
                renderAdditionalImages();
            }));
        }
        if (additionalImageCount() < MAX_ADDITIONAL_IMAGES) {
            container.addView(buildAddMoreTile());
        }
    }

    private View buildThumbnail(Object source, Runnable onRemove) {
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(64), dp(64));
        lp.setMargins(0, 0, dp(8), dp(8));
        frame.setLayoutParams(lp);

        ImageView image = new ImageView(this);
        image.setLayoutParams(new android.widget.FrameLayout.LayoutParams(dp(64), dp(64)));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackgroundResource(R.drawable.bg_trader_type_card);
        Glide.with(this).load(source).fitCenter().into(image);
        frame.addView(image);

        ImageView remove = new ImageView(this);
        android.widget.FrameLayout.LayoutParams rlp =
                new android.widget.FrameLayout.LayoutParams(dp(20), dp(20), Gravity.TOP | Gravity.END);
        remove.setLayoutParams(rlp);
        remove.setImageResource(R.drawable.ic_cancel_circle);
        remove.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
        remove.setOnClickListener(v -> onRemove.run());
        frame.addView(remove);
        return frame;
    }

    private View buildAddMoreTile() {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(64), dp(64));
        lp.setMargins(0, 0, dp(8), dp(8));
        tile.setLayoutParams(lp);
        tile.setGravity(Gravity.CENTER);
        tile.setBackgroundResource(R.drawable.bg_upload_dashed);

        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(18), dp(18)));
        icon.setImageResource(R.drawable.ic_add);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.purple_primary));
        tile.addView(icon);

        RobotoTextView label = new RobotoTextView(this);
        label.setText(R.string.add_more);
        label.setTextSize(11);
        label.setTextColor(ContextCompat.getColor(this, R.color.purple_primary));
        label.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tile.addView(label);

        tile.setOnClickListener(v -> {
            pendingImageTarget = ImageTarget.ADDITIONAL;
            imagePicker.launch("image/*");
        });
        return tile;
    }

    // ---- Base info -------------------------------------------------------

    private final List<BaseInfoData.ProductCategory> allCategories = new ArrayList<>();

    private void loadBaseInfo() {
        ApiClient.get(this).getBaseInfo(new ApiCallback<BaseInfoData>() {
            @Override
            public void onSuccess(BaseInfoData data) {
                if (data == null) return;
                if (data.brands != null) brands.addAll(data.brands);
                if (data.product_units != null) units.addAll(data.product_units);
                if (data.product_categories != null) {
                    allCategories.addAll(data.product_categories);
                    for (BaseInfoData.ProductCategory c : data.product_categories) {
                        if (c.parent_id == null) parentCategories.add(c);
                    }
                }
                if (productId != null) {
                    loadProductForEdit();
                } else {
                    matchHcLookups();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CreateProductActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- Prefill from a barcode / the Hirt & Carter catalogue -------------

    /** Seeds a new product from the extras set by the Add Product flow (portal "initialData"). */
    private void applyInitialData() {
        Intent in = getIntent();
        String barcode = AddProductFlow.digits(in.getStringExtra(EXTRA_BARCODE), 14);
        if (!barcode.isEmpty()) setText(R.id.etBarcode, barcode);
        String name = in.getStringExtra(EXTRA_HC_NAME);
        if (!AddProductFlow.isBlank(name)) setText(R.id.etName, name.trim());

        hcBrandName = in.getStringExtra(EXTRA_HC_BRAND);
        hcUom = in.getStringExtra(EXTRA_HC_UOM);
        String file = in.getStringExtra(EXTRA_HC_IMAGE_FILE);
        String gtin = in.getStringExtra(EXTRA_HC_IMAGE_GTIN);
        if (!AddProductFlow.isBlank(file) && !AddProductFlow.isBlank(gtin)) {
            hcImageFile = file.trim();
            hcImageGtin = gtin.trim();
            hcImageUrl = in.getStringExtra(EXTRA_HC_IMAGE_URL);
        }

        String size = in.getStringExtra(EXTRA_HC_SIZE);
        if (hcImageFile != null || !AddProductFlow.isBlank(size)) {
            StringBuilder text = new StringBuilder();
            if (!AddProductFlow.isBlank(size)) {
                String pack = size.trim() + (AddProductFlow.isBlank(hcUom) ? "" : " " + hcUom.trim());
                text.append(getString(R.string.hc_prefill_pack_size, pack)).append(" · ");
            }
            text.append(getString(R.string.hc_prefill_body));
            ((TextView) findViewById(R.id.hcPrefillText)).setText(text);
            findViewById(R.id.hcPrefillBanner).setVisibility(View.VISIBLE);
        }

        findViewById(R.id.btnRemoveHcImage).setOnClickListener(v -> {
            hcImageFile = null;
            hcImageGtin = null;
            hcImageUrl = null;
            renderHcImage();
        });
        renderHcImage();
    }

    /** Select the brand and unit whose names match the catalogue's (case-insensitive). */
    private void matchHcLookups() {
        if (selectedBrandId == null && !AddProductFlow.isBlank(hcBrandName)) {
            String wanted = hcBrandName.trim().toLowerCase(Locale.ROOT);
            for (BaseInfoData.Brand b : brands) {
                if (b.name != null && b.name.trim().toLowerCase(Locale.ROOT).equals(wanted)) {
                    selectedBrandId = b.id;
                    setDropdownLabel(R.id.brandLabel, b.name);
                    break;
                }
            }
        }
        if (selectedUnitId == null && !AddProductFlow.isBlank(hcUom)) {
            String wanted = hcUom.trim().toLowerCase(Locale.ROOT);
            for (BaseInfoData.ProductUnit u : units) {
                if (u.name != null && u.name.trim().toLowerCase(Locale.ROOT).equals(wanted)) {
                    selectedUnitId = u.id;
                    setDropdownLabel(R.id.unitTypeLabel, u.name);
                    break;
                }
            }
        }
    }

    /** Shows the catalogue image as the primary image until the user uploads their own. */
    private void renderHcImage() {
        boolean showHc = hcImageFile != null && primaryImageUri == null;
        ImageView preview = findViewById(R.id.primaryImagePreview);
        if (showHc) {
            preview.setVisibility(View.VISIBLE);
            findViewById(R.id.primaryImagePlaceholder).setVisibility(View.GONE);
            Glide.with(this).load(hcImageUrl).fitCenter().into(preview);
        } else if (primaryImageUri == null) {
            Glide.with(this).clear(preview);
            preview.setVisibility(View.GONE);
            findViewById(R.id.primaryImagePlaceholder).setVisibility(View.VISIBLE);
        }
        findViewById(R.id.btnRemoveHcImage).setVisibility(showHc ? View.VISIBLE : View.GONE);
        findViewById(R.id.hcImageHint).setVisibility(showHc ? View.VISIBLE : View.GONE);
    }

    // ---- Edit mode -------------------------------------------------------

    private void loadProductForEdit() {
        ApiClient.get(this).getProductById(productId, new ApiCallback<ProductDetailData>() {
            @Override
            public void onSuccess(ProductDetailData data) {
                // The response can arrive after the user has left this screen; touching the
                // (destroyed) activity — e.g. Glide.with(this) in prefill — would crash.
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (data != null && data.product != null) {
                    prefill(data.product);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CreateProductActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void prefill(ProductDetailData.Product p) {
        setText(R.id.etName, p.name);
        setText(R.id.etBarcode, p.barcode);
        setText(R.id.etSku, p.sku);
        setText(R.id.etDescription, p.description);
        setText(R.id.etUnitsPerCase, p.units_per_case);
        setText(R.id.etWeight, p.dim_weight != null ? p.dim_weight : p.weight);
        setText(R.id.etDimLength, p.dim_length);
        setText(R.id.etDimWidth, p.dim_width);
        setText(R.id.etDimHeight, p.dim_height);
        setText(R.id.etSellingPrice, p.selling_price);
        setText(R.id.etCostPrice, p.cost_price);
        setText(R.id.etStock, p.stock_quantity != null ? p.stock_quantity : p.stock);
        setText(R.id.etReorderLevel, p.reorder_level);
        setText(R.id.etPromoPrice, p.promotion_price);

        selectedBrandId = parseLong(p.brand_id);
        if (selectedBrandId != null) {
            setDropdownLabel(R.id.brandLabel, brandName(selectedBrandId));
        }
        selectedParentCategoryId = parseLong(p.parent_category_id);
        if (selectedParentCategoryId != null) {
            setDropdownLabel(R.id.categoryLabel, categoryName(selectedParentCategoryId));
            rebuildSubCategories();
        }
        selectedSubCategoryId = parseLong(p.category_id);
        if (selectedSubCategoryId != null) {
            setDropdownLabel(R.id.subCategoryLabel, categoryName(selectedSubCategoryId));
        }
        selectedUnitId = p.unit_id;
        if (selectedUnitId != null) {
            setDropdownLabel(R.id.unitTypeLabel, unitName(selectedUnitId));
        }
        String dimType = p.dim_type != null ? p.dim_type : p.measurements_type;
        applyMeasurement("pound_inch".equals(dimType) ? 1 : 0);

        applyPromoDate(p.promotion_start_date, true);
        applyPromoDate(p.promotion_end_date, false);

        ((SwitchCompat) findViewById(R.id.switchAvailable))
                .setChecked(!"0".equals(p.available_for_ordering));

        // Existing primary image: show it and treat the requirement as satisfied.
        String img = p.primary_product_image_url != null ? p.primary_product_image_url : p.image_url;
        if (img != null && !img.isEmpty()) {
            ImageView preview = findViewById(R.id.primaryImagePreview);
            preview.setVisibility(View.VISIBLE);
            findViewById(R.id.primaryImagePlaceholder).setVisibility(View.GONE);
            Glide.with(this).load(img).fitCenter().into(preview);
            primaryImageUri = Uri.parse("existing://primary");
        }

        if (p.additional_images != null) {
            for (ProductDetailData.AdditionalImage ai : p.additional_images) {
                if (ai != null && ai.file_url != null) {
                    existingAdditionalImages.add(new ExistingImage(ai.id, ai.file_url));
                }
            }
            renderAdditionalImages();
        }
    }

    // ---- Review ----------------------------------------------------------

    private void buildReview() {
        LinearLayout container = findViewById(R.id.reviewContainer);
        container.removeAllViews();
        addReviewRow(getString(R.string.review_product_name), text(R.id.etName));
        addReviewRow(getString(R.string.review_brand),
                selectedBrandId == null ? "" : brandName(selectedBrandId));
        addReviewRow(getString(R.string.review_category),
                selectedSubCategoryId == null ? "" : categoryName(selectedSubCategoryId));
        addReviewRow(getString(R.string.review_barcode), text(R.id.etBarcode));
        addReviewRow(getString(R.string.review_selling_price), "R" + text(R.id.etSellingPrice));
        addReviewRow(getString(R.string.review_stock), text(R.id.etStock));
    }

    private void addReviewRow(String label, String value) {
        LinearLayout container = findViewById(R.id.reviewContainer);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(10);
        row.setLayoutParams(rlp);

        RobotoTextView left = new RobotoTextView(this);
        left.setText(label);
        left.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        left.setTextSize(14);
        left.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(left);

        RobotoBoldTextView right = new RobotoBoldTextView(this);
        right.setText(value);
        right.setTextColor(ContextCompat.getColor(this, R.color.top_title));
        right.setTextSize(14);
        right.setGravity(Gravity.END);
        right.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(right);

        container.addView(row);
    }

    // ---- Submit ----------------------------------------------------------

    private void submit() {
        Map<String, String> form = new LinkedHashMap<>();
        if (productId != null) form.put("id", productId);
        form.put("name", text(R.id.etName));
        if (selectedBrandId != null) form.put("brand_id", String.valueOf(selectedBrandId));
        if (selectedParentCategoryId != null) {
            form.put("parent_category_id", String.valueOf(selectedParentCategoryId));
        }
        if (selectedSubCategoryId != null) form.put("category_id", String.valueOf(selectedSubCategoryId));
        form.put("barcode", text(R.id.etBarcode));
        form.put("sku", text(R.id.etSku));
        form.put("description", text(R.id.etDescription));

        if (selectedUnitId != null) form.put("unit_id", selectedUnitId);
        form.put("units_per_case", text(R.id.etUnitsPerCase));
        form.put("dim_type", DIM_TYPE_VALUES[measurementIndex]);
        form.put("measurements_type", DIM_TYPE_VALUES[measurementIndex]);
        form.put("dim_weight", text(R.id.etWeight));
        form.put("weight", text(R.id.etWeight));
        form.put("dim_length", text(R.id.etDimLength));
        form.put("dim_width", text(R.id.etDimWidth));
        form.put("dim_height", text(R.id.etDimHeight));

        form.put("selling_price", text(R.id.etSellingPrice));
        form.put("cost_price", text(R.id.etCostPrice));
        form.put("stock", text(R.id.etStock));
        form.put("reorder_level", text(R.id.etReorderLevel));
        form.put("promotion_price", text(R.id.etPromoPrice));
        form.put("promotion_start_date", promoStartDate);
        form.put("promotion_end_date", promoEndDate);
        form.put("available_for_ordering",
                ((SwitchCompat) findViewById(R.id.switchAvailable)).isChecked() ? "1" : "0");
        if (!removeAdditionalImageIds.isEmpty()) {
            form.put("remove_additional_image_ids", android.text.TextUtils.join(",", removeAdditionalImageIds));
        }

        List<Http.FilePart> files = new ArrayList<>();
        Http.FilePart primary = readImagePart("image", primaryImageUri);
        if (primary != null) files.add(primary);
        if (productId == null && primaryImageUri == null && hcImageFile != null && hcImageGtin != null) {
            // No upload: ask the server to import the chosen Hirt & Carter image instead.
            form.put("hc_gtin", hcImageGtin);
            form.put("hc_image", hcImageFile);
        }
        for (int i = 0; i < additionalImageUris.size(); i++) {
            Http.FilePart part = readImagePart("additional_image_" + i, additionalImageUris.get(i));
            if (part != null) files.add(part);
        }
        if (datasheetUri != null) {
            Http.FilePart sheet = readFilePart("datasheet", datasheetUri);
            if (sheet == null) return; // too large — readFilePart already warned
            files.add(sheet);
        }

        btnContinue.setEnabled(false);
        btnContinue.setText(R.string.submitting);
        ApiCallback<ProductDetailData> cb = new ApiCallback<ProductDetailData>() {
            @Override
            public void onSuccess(ProductDetailData data) {
                Toast.makeText(CreateProductActivity.this,
                        productId == null ? R.string.product_created_msg : R.string.product_updated_msg,
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String message) {
                btnContinue.setEnabled(true);
                btnContinue.setText(R.string.submit_btn);
                Toast.makeText(CreateProductActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };
        if (productId == null) {
            ApiClient.get(this).createProduct(form, files, cb);
        } else {
            ApiClient.get(this).updateProduct(form, files, cb);
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_msg)
                .setNegativeButton(R.string.cancel_btn, null)
                .setPositiveButton(R.string.delete, (d, w) -> ApiClient.get(this)
                        .deleteProduct(productId, new ApiCallback<ProductDetailData>() {
                            @Override
                            public void onSuccess(ProductDetailData data) {
                                Toast.makeText(CreateProductActivity.this,
                                        R.string.product_deleted_msg, Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(CreateProductActivity.this, message,
                                        Toast.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }

    // ---- File helpers ----------------------------------------------------

    /** Downscaled JPEG of an image Uri (keeps uploads small + under the 5 MB cap). */
    @Nullable
    private Http.FilePart readImagePart(String field, Uri uri) {
        if (uri == null || "existing".equals(uri.getScheme())) {
            return null; // nothing picked, or an unchanged server image in edit mode
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            int longest = Math.max(bounds.outWidth, bounds.outHeight);
            while (longest / sample > 1440) sample *= 2;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bitmap;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bitmap == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
            bitmap.recycle();
            return new Http.FilePart(field, field + ".jpg", "image/jpeg", out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    /** Raw bytes of a picked file (e.g. datasheet), rejected if over the 5 MB cap. */
    @Nullable
    private Http.FilePart readFilePart(String field, Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (out.size() > MAX_UPLOAD_BYTES) {
                    Toast.makeText(this, R.string.err_file_too_large, Toast.LENGTH_LONG).show();
                    return null;
                }
            }
            String mime = getContentResolver().getType(uri);
            String name = queryFileName(uri);
            return new Http.FilePart(field, name != null ? name : field, mime, out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private String queryFileName(Uri uri) {
        String name = null;
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Exception ignored) {
        }
        return name;
    }

    // ---- Lookup helpers --------------------------------------------------

    private String brandName(long id) {
        for (BaseInfoData.Brand b : brands) if (b.id == id) return b.name;
        return "";
    }

    private String categoryName(long id) {
        for (BaseInfoData.ProductCategory c : allCategories) if (c.id == id) return c.title;
        return "";
    }

    private String unitName(String id) {
        for (BaseInfoData.ProductUnit u : units) if (u.id != null && u.id.equals(id)) return u.name;
        return "";
    }

    // ---- Small helpers ---------------------------------------------------

    private String text(int id) {
        TextView tv = findViewById(id);
        return tv.getText() == null ? "" : tv.getText().toString().trim();
    }

    private void setText(int id, String value) {
        if (value != null) ((TextView) findViewById(id)).setText(value);
    }

    @Nullable
    private Long parseLong(String value) {
        try {
            return value == null || value.isEmpty() ? null : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
