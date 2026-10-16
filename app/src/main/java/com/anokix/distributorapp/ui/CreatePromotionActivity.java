package com.anokix.distributorapp.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.Http;
import com.anokix.distributorapp.network.dto.AutocompleteData;
import com.anokix.distributorapp.network.dto.CoordinateData;
import com.anokix.distributorapp.network.dto.ProductsData;
import com.anokix.distributorapp.network.dto.PromotionDetailData;
import com.anokix.distributorapp.network.dto.ReferenceData;
import com.anokix.distributorapp.ui.views.FlowLayout;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Seven-step "Create Ads &amp; Promotion" wizard wired to
 * /api/distributor/promotions/create (multipart).
 *
 * Steps: 1 Type · 2 Details · 3 Targeting · 4 Creative ·
 *        5 Products · 6 Budget · 7 Review.
 *
 * Lookup data: trader segments come from {@link ApiClient#getReference}
 * (trader_types), the product catalogue from {@link ApiClient#getProducts}.
 * Every step lives in a {@link ViewFlipper} so field values survive
 * back/forward navigation. Mirrors the Create Trader / Create Product wizards.
 */
public class CreatePromotionActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int STEP_COUNT = 7;
    private static final int STEP_TARGETING = 2;   // 0-based
    private static final int STEP_BUDGET = 5;
    private static final int STEP_REVIEW = 6;

    private static final String[] STEP_TITLES = {
            "Type", "Details", "Targeting", "Creative", "Products", "Budget", "Review"};

    // Step 1 — campaign types (label, description, server value).
    private static final String[] TYPE_VALUES = {
            "best_deal", "cant_miss_deal", "exclusive_deal", "featured_store",
            "promotion", "post", "story"};
    private static final String[] TYPE_LABELS = {
            "Best Deal", "Can't Miss Deal", "Exclusive Deal", "Featured Store",
            "Promotion", "Post", "Story"};
    private static final String[] TYPE_DESCS = {
            "Promote price drops and great value offers.",
            "Premium hero placement on homepage.",
            "Exclusive offers available only from you.",
            "Highlight your store to drive more orders.",
            "Discounts, bundles and special offers.",
            "Share news, updates and product highlights.",
            "Short, engaging stories that drive attention."};

    private static final int[] TYPE_ICON_RES = {
            R.drawable.ic_promo_gift,
            R.drawable.ic_promo_rocket,
            R.drawable.ic_promo_crown,
            R.drawable.ic_promo_store,
            R.drawable.ic_promo_tag,
            R.drawable.ic_promo_post,
            R.drawable.ic_promo_story};
    private static final int[] TYPE_ICON_BG = {
            R.drawable.bg_promo_icon_pink,
            R.drawable.bg_promo_icon_purple,
            R.drawable.bg_promo_icon_purple,
            R.drawable.bg_promo_icon_green,
            R.drawable.bg_promo_icon_orange,
            R.drawable.bg_promo_icon_blue,
            R.drawable.bg_promo_icon_purple};
    private static final int[] TYPE_ICON_TINT = {
            R.color.danger,
            R.color.purple_primary,
            R.color.purple_primary,
            R.color.success,
            R.color.warning,
            R.color.info,
            R.color.purple_primary};

    // Step 2 — campaign objectives (label, server value).
    private static final String[] OBJECTIVE_VALUES = {
            "awareness", "traffic", "sales", "new_product_launch", "clear_inventory"};
    private static final String[] OBJECTIVE_LABELS = {
            "Awareness", "Traffic", "Sales", "New Product Launch", "Clear Inventory"};

    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;

    /** Pass an existing promotion id to open the wizard in edit mode. */
    public static final String EXTRA_PROMOTION_ID = "promotion_id";

    @Nullable private String editPromotionId;   // non-null in edit mode
    private boolean typeLocked = false;         // Step 1 type cannot change when editing

    private ViewFlipper flipper;
    private LinearLayout stepperContainer;
    private TextView btnContinue, btnBack;
    private final TextView[] stepCircles = new TextView[STEP_COUNT];
    private final TextView[] stepLabels = new TextView[STEP_COUNT];
    private int currentStep = 0;

    // Step 1
    private String selectedTypeValue;
    private final List<View> typeCards = new ArrayList<>();

    // Step 2
    private String selectedObjectiveValue;
    private String startDate = "";   // yyyy-MM-dd
    private String endDate = "";

    // Step 3 — map + segments
    private MapView promoMap;
    private GoogleMap googleMap;
    private Marker marker;
    private Double pickedLat, pickedLng;
    private String pickedPlaceId = "";
    private final List<ReferenceData.TraderType> segments = new ArrayList<>();
    private final List<Long> selectedSegmentIds = new ArrayList<>();
    private final Handler autocompleteHandler = new Handler(Looper.getMainLooper());
    private Runnable autocompleteRunnable;
    private boolean suppressAddressWatcher;
    private android.widget.ListPopupWindow addressPopup;
    private final List<AutocompleteData.Prediction> predictions = new ArrayList<>();

    // Step 4 — creative
    private boolean videoTabSelected = false;
    private Uri bannerUri;
    private Uri videoUri;
    private ActivityResultLauncher<String> imagePicker;
    private ActivityResultLauncher<String> videoPicker;

    // Step 5 — products
    private final List<ProductLine> singleLines = new ArrayList<>();
    private final List<ComboDeal> comboDeals = new ArrayList<>();
    private final List<B1G1Deal> b1g1Deals = new ArrayList<>();
    private final List<ProductsData.Product> catalogue = new ArrayList<>();

    private static final class ProductLine {
        final ProductsData.Product product;
        String price;    // campaign_selling_price
        String stock;    // available_stock
        ProductLine(ProductsData.Product p) {
            this.product = p;
            this.price = p.selling_price != null ? p.selling_price : "";
            this.stock = p.stockValue() != null ? p.stockValue() : "";
        }
    }

    private static final class ComboDeal {
        final List<ProductLine> lines = new ArrayList<>();
    }

    private static final class B1G1Deal {
        final List<ProductsData.Product> buy = new ArrayList<>();
        final List<ProductsData.Product> free = new ArrayList<>();
        String price = "";
        String stock = "";
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_promotion);

        flipper = findViewById(R.id.stepFlipper);
        stepperContainer = findViewById(R.id.stepperContainer);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);

        editPromotionId = getIntent().getStringExtra(EXTRA_PROMOTION_ID);

        buildStepper();
        buildTypeCards();
        setupNavigation();
        setupDetailFields();
        setupCreative();
        setupPickers();
        setupProductButtons();
        setupAddressAutocomplete();
        setupMap(savedInstanceState);

        showStep(0);
        loadReference();
        loadCatalogue();

        if (editPromotionId != null) {
            ((TextView) findViewById(R.id.screenTitle)).setText(R.string.edit_promotion_title);
            loadPromotionForEdit();
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
            label.setText(STEP_TITLES[i]);
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
                label.setTypeface(null, Typeface.NORMAL);
            } else if (i == currentStep) {
                circle.setBackgroundResource(R.drawable.bg_step_circle_active);
                circle.setText(String.valueOf(i + 1));
                circle.setTextColor(0xFFFFFFFF);
                label.setTextColor(ContextCompat.getColor(this, R.color.purple_primary));
                label.setTypeface(null, Typeface.BOLD);
            } else {
                circle.setBackgroundResource(R.drawable.bg_step_circle_inactive);
                circle.setText(String.valueOf(i + 1));
                circle.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                label.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                label.setTypeface(null, Typeface.NORMAL);
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
                submit("pending_approval");
            } else {
                showStep(currentStep + 1);
            }
        });
    }

    private void showStep(int index) {
        currentStep = index;
        flipper.setDisplayedChild(index);
        btnContinue.setText(index == STEP_REVIEW ? R.string.promo_submit : R.string.continue_btn);
        btnBack.setText(index == 0 ? R.string.cancel_btn : R.string.back_btn);
        if (index == STEP_BUDGET) {
            buildBudget();
        } else if (index == STEP_REVIEW) {
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
                if (selectedTypeValue == null) return fail("Please choose a campaign type.");
                return true;
            case 1:
                if (text(R.id.etCampaignName).isEmpty()) return fail("Enter a campaign name.");
                if (selectedObjectiveValue == null) return fail("Select a campaign objective.");
                if (startDate.isEmpty() || endDate.isEmpty()) return fail("Select start and end dates.");
                if (text(R.id.etBudget).isEmpty()) return fail("Enter a budget.");
                return true;
            case STEP_TARGETING:
                if (pickedLat == null || pickedLng == null) {
                    return fail("Search an address or drop a pin on the map.");
                }
                if (selectedSegmentIds.isEmpty()) return fail("Select at least one trader segment.");
                return true;
            case 4:
                if (singleLines.isEmpty() && comboDeals.isEmpty() && b1g1Deals.isEmpty()) {
                    return fail("Add at least one product to continue.");
                }
                return true;
            default:
                return true;
        }
    }

    private boolean fail(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        return false;
    }

    // ---- Step 1: type cards ----------------------------------------------

    private void buildTypeCards() {
        FlowLayout container = findViewById(R.id.typeContainer);
        LayoutInflater inflater = LayoutInflater.from(this);
        int cardWidth = (getResources().getDisplayMetrics().widthPixels - dp(32) - dp(10)) / 2;
        for (int i = 0; i < TYPE_VALUES.length; i++) {
            final int index = i;
            View card = inflater.inflate(R.layout.item_promo_type_card, container, false);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.setMargins(0, 0, dp(10), dp(10));
            card.setLayoutParams(clp);

            View iconTile = card.findViewById(R.id.iconTile);
            ImageView icon = card.findViewById(R.id.icon);
            TextView title = card.findViewById(R.id.title);
            TextView desc = card.findViewById(R.id.description);

            iconTile.setBackgroundResource(TYPE_ICON_BG[index]);
            icon.setImageResource(TYPE_ICON_RES[index]);
            icon.setColorFilter(ContextCompat.getColor(this, TYPE_ICON_TINT[index]));
            title.setText(TYPE_LABELS[index]);
            desc.setText(TYPE_DESCS[index]);

            card.setOnClickListener(v -> {
                if (typeLocked) {
                    Toast.makeText(this, R.string.promo_type_locked, Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedTypeValue = TYPE_VALUES[index];
                applyTypeSelection();
            });

            container.addView(card);
            typeCards.add(card);
        }
    }

    private void applyTypeSelection() {
        for (int i = 0; i < typeCards.size(); i++) {
            View card = typeCards.get(i);
            boolean selected = TYPE_VALUES[i].equals(selectedTypeValue);
            card.setBackgroundResource(selected
                    ? R.drawable.bg_promo_type_card_selected : R.drawable.bg_promo_type_card);
            TextView title = card.findViewById(R.id.title);
            title.setTextColor(ContextCompat.getColor(this,
                    selected ? R.color.purple_primary : R.color.top_title));
            card.setAlpha(typeLocked && !selected ? 0.4f : 1f);
        }
    }

    // ---- Step 2: details -------------------------------------------------

    private void setupDetailFields() {
        findViewById(R.id.objectiveField).setOnClickListener(v -> showObjectiveDialog());
        findViewById(R.id.startDateField).setOnClickListener(v -> pickDate(true));
        findViewById(R.id.endDateField).setOnClickListener(v -> pickDate(false));
    }

    private void showObjectiveDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.promo_objective)
                .setItems(OBJECTIVE_LABELS, (d, which) -> {
                    selectedObjectiveValue = OBJECTIVE_VALUES[which];
                    setLabel(R.id.objectiveLabel, OBJECTIVE_LABELS[which]);
                })
                .show();
    }

    private void pickDate(boolean isStart) {
        Calendar c = Calendar.getInstance();
        new android.app.DatePickerDialog(this, (view, year, month, day) -> {
            String api = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            String shown = formatDisplayDate(api);
            if (isStart) {
                startDate = api;
                setLabel(R.id.startDateLabel, shown);
            } else {
                endDate = api;
                setLabel(R.id.endDateLabel, shown);
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ---- Step 4: creative ------------------------------------------------

    private void setupCreative() {
        findViewById(R.id.tabImage).setOnClickListener(v -> selectCreativeTab(false));
        findViewById(R.id.tabVideo).setOnClickListener(v -> selectCreativeTab(true));
        findViewById(R.id.imageUploadBox).setOnClickListener(v -> imagePicker.launch("image/*"));
        findViewById(R.id.videoUploadBox).setOnClickListener(v -> videoPicker.launch("video/*"));
        selectCreativeTab(false);
    }

    private void selectCreativeTab(boolean video) {
        videoTabSelected = video;
        TextView tabImage = findViewById(R.id.tabImage);
        TextView tabVideo = findViewById(R.id.tabVideo);
        tabImage.setTextColor(ContextCompat.getColor(this,
                video ? R.color.text_secondary : R.color.purple_primary));
        tabVideo.setTextColor(ContextCompat.getColor(this,
                video ? R.color.purple_primary : R.color.text_secondary));
        findViewById(R.id.imageUploadBox).setVisibility(video ? View.GONE : View.VISIBLE);
        findViewById(R.id.videoUploadBox).setVisibility(video ? View.VISIBLE : View.GONE);
    }

    private void setupPickers() {
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            bannerUri = uri;
            ImageView preview = findViewById(R.id.imagePreview);
            preview.setVisibility(View.VISIBLE);
            findViewById(R.id.imagePlaceholder).setVisibility(View.GONE);
            Glide.with(this).load(uri).fitCenter().into(preview);
        });
        videoPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            videoUri = uri;
            String name = queryFileName(uri);
            TextView label = findViewById(R.id.videoLabel);
            label.setText(name != null ? name : "Video selected");
            label.setTextColor(ContextCompat.getColor(this, R.color.success));
        });
    }

    // ---- Step 5: products ------------------------------------------------

    private void setupProductButtons() {
        findViewById(R.id.btnAddSingle).setOnClickListener(v -> showSinglePicker());
        findViewById(R.id.btnAddCombo).setOnClickListener(v -> showComboPicker());
        findViewById(R.id.btnAddB1g1).setOnClickListener(v -> showB1g1Picker());
        renderSingle();
        renderCombo();
        renderB1g1();
    }

    // ----- Single products -----

    private void showSinglePicker() {
        if (catalogue.isEmpty()) {
            Toast.makeText(this, "No products in your catalogue yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        final List<ProductsData.Product> selected = new ArrayList<>();
        AlertDialog dialog = buildPickerDialog("Your Products",
                "Click a product row to add it. Choose another row to replace that selection.",
                false, selected, () -> {
                    if (!selected.isEmpty()) {
                        singleLines.add(new ProductLine(selected.get(0)));
                        renderSingle();
                    }
                });
        dialog.show();
    }

    private void renderSingle() {
        ((TextView) findViewById(R.id.singleHeader))
                .setText("Single Products (" + singleLines.size() + ")");
        LinearLayout container = findViewById(R.id.singleContainer);
        container.removeAllViews();
        if (singleLines.isEmpty()) {
            container.addView(emptyHint("No single products added yet."));
            return;
        }
        for (ProductLine line : singleLines) {
            container.addView(buildLineRow(line, () -> {
                singleLines.remove(line);
                renderSingle();
            }));
        }
    }

    // ----- Combo deals -----

    private void showComboPicker() {
        if (catalogue.isEmpty()) {
            Toast.makeText(this, "No products in your catalogue yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        final List<ProductsData.Product> selected = new ArrayList<>();
        AlertDialog dialog = buildPickerDialog("Your Products",
                "Select several products for this combo deal.",
                true, selected, () -> {
                    if (!selected.isEmpty()) {
                        ComboDeal deal = new ComboDeal();
                        for (ProductsData.Product p : selected) deal.lines.add(new ProductLine(p));
                        comboDeals.add(deal);
                        renderCombo();
                    }
                });
        dialog.show();
    }

    private void renderCombo() {
        ((TextView) findViewById(R.id.comboHeader))
                .setText("Combo Deals (" + comboDeals.size() + ")");
        LinearLayout container = findViewById(R.id.comboContainer);
        container.removeAllViews();
        if (comboDeals.isEmpty()) {
            container.addView(emptyHint("No combo deals yet."));
            return;
        }
        for (int i = 0; i < comboDeals.size(); i++) {
            final ComboDeal deal = comboDeals.get(i);
            LinearLayout group = card();
            LinearLayout head = dealHeader(getString(R.string.promo_deal, i + 1), () -> {
                comboDeals.remove(deal);
                renderCombo();
            });
            group.addView(head);
            for (ProductLine line : deal.lines) {
                group.addView(buildLineRow(line, () -> {
                    deal.lines.remove(line);
                    if (deal.lines.isEmpty()) comboDeals.remove(deal);
                    renderCombo();
                }));
            }
            container.addView(group);
        }
    }

    // ----- Buy1Get1 deals -----

    private void showB1g1Picker() {
        if (catalogue.isEmpty()) {
            Toast.makeText(this, "No products in your catalogue yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        final B1G1Deal deal = new B1G1Deal();
        final boolean[] freeTab = {false};

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(8));

        RobotoTextView hint = new RobotoTextView(this);
        hint.setText("Switch between Buy and Free products, then tap a row to add it. "
                + "Each deal must include at least one free product.");
        hint.setTextSize(12);
        hint.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        content.addView(hint);

        // Buy / Free toggle.
        LinearLayout toggle = new LinearLayout(this);
        toggle.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(10);
        toggle.setLayoutParams(tlp);
        final TextView buyTab = tabButton(getString(R.string.promo_buy_products), true);
        final TextView freeTabBtn = tabButton(getString(R.string.promo_free_products), false);
        toggle.addView(buyTab);
        toggle.addView(freeTabBtn);
        content.addView(toggle);

        final LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        final EditText search = pickerSearchField();
        content.addView(search);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
        scroll.addView(rows);
        content.addView(scroll);

        // Live preview of what's in the current deal.
        final LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.topMargin = dp(8);
        preview.setLayoutParams(plp);
        content.addView(preview);

        final Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            buyTab.setBackgroundResource(freeTab[0]
                    ? R.drawable.bg_chip_unselected : R.drawable.bg_trader_type_card_selected);
            buyTab.setTextColor(ContextCompat.getColor(this,
                    freeTab[0] ? R.color.text_secondary : R.color.purple_primary));
            freeTabBtn.setBackgroundResource(freeTab[0]
                    ? R.drawable.bg_trader_type_card_selected : R.drawable.bg_chip_unselected);
            freeTabBtn.setTextColor(ContextCompat.getColor(this,
                    freeTab[0] ? R.color.purple_primary : R.color.text_secondary));
            List<ProductsData.Product> target = freeTab[0] ? deal.free : deal.buy;
            renderPickerRows(rows, search.getText().toString(), target, true, p -> {
                if (target.contains(p)) target.remove(p); else target.add(p);
                refresh[0].run();
            });
            renderB1g1Preview(preview, deal, refresh[0]);
        };
        buyTab.setOnClickListener(v -> { freeTab[0] = false; refresh[0].run(); });
        freeTabBtn.setOnClickListener(v -> { freeTab[0] = true; refresh[0].run(); });
        search.addTextChangedListener(simpleWatcher(() -> refresh[0].run()));
        refresh[0].run();

        new AlertDialog.Builder(this)
                .setTitle(R.string.promo_b1g1_deals)
                .setView(wrapDialog(content))
                .setNegativeButton(R.string.cancel_btn, null)
                .setPositiveButton(R.string.promo_done, (d, w) -> {
                    if (deal.buy.isEmpty() || deal.free.isEmpty()) {
                        Toast.makeText(this, "Add at least one buy and one free product.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    b1g1Deals.add(deal);
                    renderB1g1();
                })
                .show();
    }

    private void renderB1g1Preview(LinearLayout preview, B1G1Deal deal, Runnable refresh) {
        preview.removeAllViews();
        preview.addView(previewGroup(getString(R.string.promo_buy_products) + " (" + deal.buy.size() + ")",
                deal.buy, refresh));
        preview.addView(previewGroup(getString(R.string.promo_free_products) + " (" + deal.free.size() + ")",
                deal.free, refresh));
    }

    private View previewGroup(String title, List<ProductsData.Product> items, Runnable refresh) {
        LinearLayout group = card();
        RobotoBoldTextView t = new RobotoBoldTextView(this);
        t.setText(title);
        t.setTextSize(13);
        t.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        group.addView(t);
        for (ProductsData.Product p : items) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));
            RobotoTextView name = new RobotoTextView(this);
            name.setText(p.name);
            name.setTextSize(13);
            name.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(name);
            ImageView del = new ImageView(this);
            del.setLayoutParams(new LinearLayout.LayoutParams(dp(18), dp(18)));
            del.setImageResource(R.drawable.ic_cancel_circle);
            del.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.text_tertiary));
            del.setOnClickListener(v -> { items.remove(p); refresh.run(); });
            row.addView(del);
            group.addView(row);
        }
        return group;
    }

    private void renderB1g1() {
        ((TextView) findViewById(R.id.b1g1Header))
                .setText("Buy1Get1 Free Deals (" + b1g1Deals.size() + ")");
        LinearLayout container = findViewById(R.id.b1g1Container);
        container.removeAllViews();
        if (b1g1Deals.isEmpty()) {
            container.addView(emptyHint("No Buy1Get1 deals yet."));
            return;
        }
        for (int i = 0; i < b1g1Deals.size(); i++) {
            final B1G1Deal deal = b1g1Deals.get(i);
            LinearLayout group = card();
            group.addView(dealHeader(getString(R.string.promo_deal, i + 1), () -> {
                b1g1Deals.remove(deal);
                renderB1g1();
            }));

            RobotoBoldTextView buyT = sectionLabel(getString(R.string.promo_buy_products));
            group.addView(buyT);
            for (ProductsData.Product p : deal.buy) group.addView(simpleProductRow(p));

            RobotoBoldTextView freeT = sectionLabel(getString(R.string.promo_free_products));
            group.addView(freeT);
            for (ProductsData.Product p : deal.free) group.addView(simpleProductRow(p));

            // Deal-level campaign price + available stock.
            group.addView(dealStockPriceRow(deal));
            container.addView(group);
        }
    }

    private View dealStockPriceRow(B1G1Deal deal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        row.setLayoutParams(lp);

        EditText stock = numberField("Available stock", deal.stock);
        stock.addTextChangedListener(simpleWatcher(() -> deal.stock = stock.getText().toString().trim()));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.rightMargin = dp(6);
        stock.setLayoutParams(slp);
        row.addView(stock);

        EditText price = numberField(getString(R.string.promo_selling_price_hint), deal.price);
        price.addTextChangedListener(simpleWatcher(() -> deal.price = price.getText().toString().trim()));
        LinearLayout.LayoutParams prlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        prlp.leftMargin = dp(6);
        price.setLayoutParams(prlp);
        row.addView(price);
        return row;
    }

    // ----- Shared product-row builders -----

    /** A product row with editable campaign price + available stock and a delete action. */
    private View buildLineRow(ProductLine line, Runnable onRemove) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        glp.topMargin = dp(8);
        group.setLayoutParams(glp);
        group.setBackgroundResource(R.drawable.bg_trader_type_card);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        ImageView thumb = new ImageView(this);
        thumb.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(line.product.imageUrl()).into(thumb);
        top.addView(thumb);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ilp.leftMargin = dp(10);
        info.setLayoutParams(ilp);
        RobotoBoldTextView name = new RobotoBoldTextView(this);
        name.setText(line.product.name);
        name.setTextSize(14);
        name.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        info.addView(name);
        RobotoTextView sub = new RobotoTextView(this);
        sub.setText(productSubtitle(line.product));
        sub.setTextSize(12);
        sub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        info.addView(sub);
        top.addView(info);

        ImageView del = new ImageView(this);
        del.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));
        del.setImageResource(R.drawable.ic_cancel_circle);
        del.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.text_tertiary));
        del.setOnClickListener(v -> onRemove.run());
        top.addView(del);
        group.addView(top);

        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.HORIZONTAL);
        fields.setBaselineAligned(false);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.topMargin = dp(8);
        fields.setLayoutParams(flp);

        EditText stock = numberField("Available stock", line.stock);
        stock.addTextChangedListener(simpleWatcher(() -> line.stock = stock.getText().toString().trim()));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.rightMargin = dp(6);
        stock.setLayoutParams(slp);
        fields.addView(stock);

        EditText price = numberField(getString(R.string.promo_selling_price_hint), line.price);
        price.addTextChangedListener(simpleWatcher(() -> line.price = price.getText().toString().trim()));
        LinearLayout.LayoutParams prlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        prlp.leftMargin = dp(6);
        price.setLayoutParams(prlp);
        fields.addView(price);

        group.addView(fields);
        return group;
    }

    private View simpleProductRow(ProductsData.Product p) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        ImageView thumb = new ImageView(this);
        thumb.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(28)));
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(p.imageUrl()).into(thumb);
        row.addView(thumb);
        RobotoTextView name = new RobotoTextView(this);
        name.setText(p.name);
        name.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(8);
        name.setLayoutParams(lp);
        row.addView(name);
        return row;
    }

    // ----- Generic picker dialog (single / multi select) -----

    private interface OnRowTap { void tap(ProductsData.Product p); }

    private AlertDialog buildPickerDialog(String title, String hintText, boolean multi,
                                          List<ProductsData.Product> selected, Runnable onDone) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(8));

        RobotoTextView hint = new RobotoTextView(this);
        hint.setText(hintText);
        hint.setTextSize(12);
        hint.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        content.addView(hint);

        final EditText search = pickerSearchField();
        content.addView(search);

        final LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(320)));
        scroll.addView(rows);
        content.addView(scroll);

        final Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> renderPickerRows(rows, search.getText().toString(), selected, multi, p -> {
            if (multi) {
                if (selected.contains(p)) selected.remove(p); else selected.add(p);
            } else {
                selected.clear();
                selected.add(p);
            }
            refresh[0].run();
        });
        search.addTextChangedListener(simpleWatcher(() -> refresh[0].run()));
        refresh[0].run();

        return new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(wrapDialog(content))
                .setNegativeButton(R.string.cancel_btn, null)
                .setPositiveButton(R.string.promo_done, (d, w) -> onDone.run())
                .create();
    }

    private void renderPickerRows(LinearLayout rows, String query,
                                  List<ProductsData.Product> selected, boolean multi, OnRowTap tap) {
        rows.removeAllViews();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.US);
        for (ProductsData.Product p : catalogue) {
            if (!q.isEmpty() && !matchesQuery(p, q)) continue;
            boolean isSelected = selected.contains(p);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(8), dp(8), dp(8));
            row.setBackgroundColor(isSelected
                    ? 0x1A0D47A1 : 0x00000000);   // 10% brand blue on the selected row
            row.setOnClickListener(v -> tap.tap(p));

            ImageView thumb = new ImageView(this);
            thumb.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(p.imageUrl()).into(thumb);
            row.addView(thumb);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            ilp.leftMargin = dp(8);
            info.setLayoutParams(ilp);
            RobotoBoldTextView name = new RobotoBoldTextView(this);
            name.setText(p.name);
            name.setTextSize(13);
            name.setTextColor(ContextCompat.getColor(this, R.color.purple_primary));
            info.addView(name);
            RobotoTextView sub = new RobotoTextView(this);
            sub.setText(productSubtitle(p));
            sub.setTextSize(11);
            sub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            info.addView(sub);
            row.addView(info);

            RobotoBoldTextView price = new RobotoBoldTextView(this);
            price.setText("R" + (p.selling_price != null ? p.selling_price : "0"));
            price.setTextSize(13);
            price.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            row.addView(price);

            rows.addView(row);
        }
        if (rows.getChildCount() == 0) {
            rows.addView(emptyHint("No products match your search."));
        }
    }

    private boolean matchesQuery(ProductsData.Product p, String q) {
        return contains(p.name, q) || contains(p.sku, q) || contains(p.barcode, q)
                || contains(p.brand_name, q) || contains(p.category_title, q);
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.US).contains(q);
    }

    private String productSubtitle(ProductsData.Product p) {
        StringBuilder sb = new StringBuilder();
        if (p.sku != null && !p.sku.isEmpty()) sb.append(p.sku);
        if (p.category_title != null && !p.category_title.isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(p.category_title);
        }
        if (p.brand_name != null && !p.brand_name.isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(p.brand_name);
        }
        return sb.toString();
    }

    // ---- Step 6: budget --------------------------------------------------

    private double singleCost() {
        double sum = 0;
        for (ProductLine l : singleLines) sum += lineValue(l);
        return sum;
    }

    private double comboCost() {
        double sum = 0;
        for (ComboDeal d : comboDeals) for (ProductLine l : d.lines) sum += lineValue(l);
        return sum;
    }

    private double b1g1Cost() {
        double sum = 0;
        for (B1G1Deal d : b1g1Deals) sum += parseDouble(d.price) * parseDouble(d.stock);
        return sum;
    }

    private double lineValue(ProductLine l) {
        return parseDouble(l.price) * parseDouble(l.stock);
    }

    private void buildBudget() {
        double total = parseDouble(text(R.id.etBudget));
        double daily = parseDouble(text(R.id.etDailyBudget));
        double single = singleCost(), combo = comboCost(), b1g1 = b1g1Cost();
        double productsCost = single + combo + b1g1;
        int days = durationDays();

        ((TextView) findViewById(R.id.budgetTotal)).setText(formatRand(total));
        ((TextView) findViewById(R.id.budgetDaily)).setText(formatRand(daily));
        ((TextView) findViewById(R.id.budgetDuration)).setText(
                (startDate.isEmpty() ? "-" : formatDisplayDate(startDate) + " – " + formatDisplayDate(endDate))
                        + (days > 0 ? "  (" + days + " days)" : ""));
        ((TextView) findViewById(R.id.budgetProductsCost)).setText(formatRand(productsCost));
        ((TextView) findViewById(R.id.budgetBreakdown)).setText(
                "Single Products: " + formatRand(single)
                        + "\nCombo Deals: " + formatRand(combo)
                        + "\nBuy1Get1 Free Deals: " + formatRand(b1g1));

        // Estimated performance — heuristic ranges derived from the budget.
        LinearLayout est = findViewById(R.id.estimateCard);
        est.removeAllViews();
        long reachLo = Math.round(total * 10), reachHi = Math.round(total * 14);
        long imprLo = reachLo * 5, imprHi = reachHi * 5;
        long clickLo = Math.round(imprLo * 0.025), clickHi = Math.round(imprHi * 0.03);
        long orderLo = Math.round(clickLo * 0.27), orderHi = Math.round(clickHi * 0.27);
        long salesLo = Math.round(productsCost * 0.99), salesHi = Math.round(productsCost * 1.386);
        addEstimateRow(est, "Estimated Reach", range(reachLo, reachHi) + " Traders");
        addEstimateRow(est, "Estimated Impressions", range(imprLo, imprHi));
        addEstimateRow(est, "Estimated Clicks", range(clickLo, clickHi));
        addEstimateRow(est, "Estimated CTR", "2.5% – 3.2%");
        addEstimateRow(est, "Estimated Orders", range(orderLo, orderHi));
        addEstimateRow(est, "Estimated Sales Impact", formatRand(salesLo) + " – " + formatRand(salesHi));
    }

    private void addEstimateRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(10);
        row.setLayoutParams(rlp);
        RobotoTextView left = new RobotoTextView(this);
        left.setText(label);
        left.setTextSize(13);
        left.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        left.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(left);
        RobotoBoldTextView right = new RobotoBoldTextView(this);
        right.setText(value);
        right.setTextSize(13);
        right.setGravity(Gravity.END);
        right.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        right.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(right);
        parent.addView(row);
    }

    // ---- Step 7: review --------------------------------------------------

    private void buildReview() {
        LinearLayout container = findViewById(R.id.reviewCard);
        container.removeAllViews();
        addReviewRow("Type", typeLabel(selectedTypeValue));
        addReviewRow("Objective", objectiveLabel(selectedObjectiveValue));
        addReviewRow("Dates", startDate.isEmpty() ? "-"
                : formatDisplayDate(startDate) + " – " + formatDisplayDate(endDate));
        addReviewRow("Budget", formatRand(parseDouble(text(R.id.etBudget))));
        addReviewRow("Targeting", selectedSegmentIds.size() + " segment(s)");
        addReviewRow("Products", singleLines.size() + " single · "
                + comboDeals.size() + " combo · " + b1g1Deals.size() + " B1G1");
        addReviewRow("Products Cost", formatRand(singleCost() + comboCost() + b1g1Cost()));

        // Save-as-draft option (the primary button submits for approval).
        TextView draft = new TextView(this);
        draft.setText(R.string.promo_save_draft);
        draft.setTextSize(14);
        draft.setGravity(Gravity.CENTER);
        draft.setHeight(dp(46));
        draft.setTextColor(ContextCompat.getColor(this, R.color.purple_primary));
        draft.setBackgroundResource(R.drawable.bg_login_input);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(16);
        draft.setLayoutParams(dlp);
        draft.setOnClickListener(v -> submit("draft"));
        container.addView(draft);
    }

    private void addReviewRow(String label, String value) {
        LinearLayout container = findViewById(R.id.reviewCard);
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

    private void submit(String status) {
        Map<String, String> form = new LinkedHashMap<>();
        if (editPromotionId != null) form.put("id", editPromotionId);
        form.put("status", status);
        form.put("campaign_type", selectedTypeValue);
        form.put("name", text(R.id.etCampaignName));
        form.put("description", text(R.id.etDescription));
        if (selectedObjectiveValue != null) form.put("objective", selectedObjectiveValue);
        form.put("start_date", startDate);
        form.put("end_date", endDate);
        form.put("budget", text(R.id.etBudget));
        form.put("daily_budget", text(R.id.etDailyBudget));
        form.put("products_cost", trimNumber(singleCost() + comboCost() + b1g1Cost()));
        form.put("address", text(R.id.etAddressSearch));
        if (pickedPlaceId != null && !pickedPlaceId.isEmpty()) form.put("place_id", pickedPlaceId);
        if (pickedLat != null) form.put("lat", String.valueOf(pickedLat));
        if (pickedLng != null) form.put("lng", String.valueOf(pickedLng));

        Gson gson = new Gson();
        List<String> segmentIds = new ArrayList<>();
        for (Long id : selectedSegmentIds) segmentIds.add(String.valueOf(id));
        form.put("trader_segments", gson.toJson(segmentIds));
        form.put("selected_products", gson.toJson(buildSelectedProducts()));

        // Media goes through the dedicated upload-media endpoint (after the record
        // exists), as the API recommends for large banner/video payloads. Only
        // newly-picked files are uploaded; existing media is left untouched.
        final List<Http.FilePart> mediaFiles = buildMediaFiles();
        if (mediaFiles == null) return;   // a file was too large — already warned

        btnContinue.setEnabled(false);
        btnContinue.setText(R.string.promo_submitting);

        final boolean editing = editPromotionId != null;
        ApiCallback<PromotionDetailData> phase1 = new ApiCallback<PromotionDetailData>() {
            @Override
            public void onSuccess(PromotionDetailData data) {
                String id = data != null && data.promotion != null ? data.promotion.id : editPromotionId;
                if (!mediaFiles.isEmpty() && id != null) {
                    uploadMediaThenFinish(id, mediaFiles, editing);
                } else {
                    finishSaved(editing);
                }
            }

            @Override
            public void onError(String message) {
                btnContinue.setEnabled(true);
                btnContinue.setText(R.string.promo_submit);
                Toast.makeText(CreatePromotionActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };

        // Create/update carry text fields only; media is uploaded in phase 2.
        List<Http.FilePart> noFiles = new ArrayList<>();
        if (editing) {
            ApiClient.get(this).updatePromotion(form, noFiles, phase1);
        } else {
            ApiClient.get(this).createPromotion(form, noFiles, phase1);
        }
    }

    /** Newly-picked banner/video parts, or null if a file exceeded the size cap. */
    @Nullable
    private List<Http.FilePart> buildMediaFiles() {
        List<Http.FilePart> files = new ArrayList<>();
        Http.FilePart banner = readImagePart("banner_file", bannerUri);
        if (banner != null) files.add(banner);
        if (videoUri != null) {
            Http.FilePart video = readFilePart("video_file", videoUri);
            if (video == null) return null;   // too large — readFilePart already warned
            files.add(video);
        }
        return files;
    }

    private void uploadMediaThenFinish(String id, List<Http.FilePart> files, boolean editing) {
        ApiClient.get(this).uploadPromotionMedia(id, files, new ApiCallback<PromotionDetailData>() {
            @Override
            public void onSuccess(PromotionDetailData data) {
                finishSaved(editing);
            }

            @Override
            public void onError(String message) {
                // The campaign itself was saved; only the media upload failed.
                Toast.makeText(CreatePromotionActivity.this,
                        getString(R.string.promo_media_failed, message), Toast.LENGTH_LONG).show();
                finishSaved(editing);
            }
        });
    }

    private void finishSaved(boolean editing) {
        Toast.makeText(this, editing ? R.string.promo_updated_msg : R.string.promo_created_msg,
                Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    /** Lean selected_products payload grouped into single / combo / buy1get1. */
    private Map<String, Object> buildSelectedProducts() {
        Map<String, Object> root = new LinkedHashMap<>();

        List<Object> single = new ArrayList<>();
        for (ProductLine l : singleLines) single.add(lineMap(l));
        root.put("single_products", single);

        List<Object> combos = new ArrayList<>();
        for (ComboDeal d : comboDeals) {
            Map<String, Object> deal = new LinkedHashMap<>();
            List<Object> lines = new ArrayList<>();
            for (ProductLine l : d.lines) lines.add(lineMap(l));
            deal.put("products", lines);
            combos.add(deal);
        }
        root.put("combo_deals", combos);

        List<Object> b1g1 = new ArrayList<>();
        for (B1G1Deal d : b1g1Deals) {
            Map<String, Object> deal = new LinkedHashMap<>();
            List<Object> buy = new ArrayList<>();
            for (ProductsData.Product p : d.buy) buy.add(productIdMap(p));
            List<Object> free = new ArrayList<>();
            for (ProductsData.Product p : d.free) free.add(productIdMap(p));
            deal.put("buy_products", buy);
            deal.put("free_products", free);
            deal.put("campaign_selling_price", d.price);
            deal.put("available_stock", d.stock);
            b1g1.add(deal);
        }
        root.put("buy1_get1_deals", b1g1);
        return root;
    }

    private Map<String, Object> lineMap(ProductLine l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product_id", l.product.id);
        m.put("campaign_selling_price", l.price);
        m.put("available_stock", l.stock);
        m.put("original_price", l.product.selling_price);
        m.put("original_stock", l.product.stockValue());
        return m;
    }

    private Map<String, Object> productIdMap(ProductsData.Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product_id", p.id);
        m.put("original_price", p.selling_price);
        m.put("original_stock", p.stockValue());
        return m;
    }

    // ---- Reference + catalogue -------------------------------------------

    private void loadReference() {
        ApiClient.get(this).getReference(new ApiCallback<ReferenceData>() {
            @Override
            public void onSuccess(ReferenceData data) {
                if (data != null && data.trader_types != null) {
                    segments.addAll(data.trader_types);
                }
                renderSegments();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CreatePromotionActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadCatalogue() {
        ApiClient.get(this).getProducts(new ApiCallback<ProductsData>() {
            @Override
            public void onSuccess(ProductsData data) {
                if (data != null && data.products != null) {
                    catalogue.addAll(data.products);
                }
            }

            @Override
            public void onError(String message) {
                // Catalogue is needed only on the Products step; surface lazily there.
            }
        });
    }

    private void renderSegments() {
        FlowLayout container = findViewById(R.id.segmentContainer);
        container.removeAllViews();
        for (ReferenceData.TraderType seg : segments) {
            TextView chip = new TextView(this);
            chip.setText(seg.name);
            chip.setTextSize(13);
            chip.setPadding(dp(16), dp(8), dp(16), dp(8));
            boolean selected = selectedSegmentIds.contains(seg.id);
            applyChipState(chip, selected);
            chip.setOnClickListener(v -> {
                if (selectedSegmentIds.contains(seg.id)) {
                    selectedSegmentIds.remove(seg.id);
                    applyChipState(chip, false);
                } else {
                    selectedSegmentIds.add(seg.id);
                    applyChipState(chip, true);
                }
            });
            container.addView(chip);
        }
    }

    private void applyChipState(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected
                ? R.drawable.bg_trader_type_card_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(ContextCompat.getColor(this,
                selected ? R.color.purple_primary : R.color.text_secondary));
    }

    // ---- Edit mode: load + prefill --------------------------------------

    private void loadPromotionForEdit() {
        ApiClient.get(this).getPromotionById(editPromotionId, new ApiCallback<PromotionDetailData>() {
            @Override
            public void onSuccess(PromotionDetailData data) {
                // prefillFromPromotion calls Glide.with(this); skip if the activity is gone.
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (data != null && data.promotion != null) {
                    prefillFromPromotion(data.promotion);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CreatePromotionActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void prefillFromPromotion(PromotionDetailData.Promotion p) {
        // Step 1 — type is fixed for an existing campaign.
        if (p.campaign_type != null) {
            selectedTypeValue = p.campaign_type;
            typeLocked = true;
            applyTypeSelection();
        }

        // Step 2 — details.
        setField(R.id.etCampaignName, p.name);
        setField(R.id.etDescription, p.description);
        setField(R.id.etBudget, p.budget);
        setField(R.id.etDailyBudget, p.daily_budget);
        if (p.objective != null && !p.objective.isEmpty()) {
            selectedObjectiveValue = p.objective;
            setLabel(R.id.objectiveLabel, objectiveLabel(p.objective));
        }
        if (p.start_date != null && !p.start_date.isEmpty()) {
            startDate = p.start_date;
            setLabel(R.id.startDateLabel, formatDisplayDate(startDate));
        }
        if (p.end_date != null && !p.end_date.isEmpty()) {
            endDate = p.end_date;
            setLabel(R.id.endDateLabel, formatDisplayDate(endDate));
        }

        // Step 3 — targeting.
        if (p.address != null && !p.address.isEmpty()) setAddressText(p.address);
        if (p.place_id != null) pickedPlaceId = p.place_id;
        Double lat = parseNullableDouble(p.lat);
        Double lng = parseNullableDouble(p.lng);
        if (lat != null && lng != null) {
            pickedLat = lat;
            pickedLng = lng;
            moveMapTo(new LatLng(lat, lng), true);
        }
        parseSegments(p.trader_segments);
        renderSegments();

        // Step 5 — products.
        parseSelectedProducts(p.selected_products);
        renderSingle();
        renderCombo();
        renderB1g1();

        // Step 4 — existing media previews (kept unless the user picks new files).
        if (p.banner_file != null && p.banner_file.bestUrl() != null
                && !p.banner_file.bestUrl().isEmpty()) {
            ImageView preview = findViewById(R.id.imagePreview);
            preview.setVisibility(View.VISIBLE);
            findViewById(R.id.imagePlaceholder).setVisibility(View.GONE);
            Glide.with(this).load(p.banner_file.bestUrl()).fitCenter().into(preview);
        }
        if (p.video_file != null && p.video_file.bestUrl() != null
                && !p.video_file.bestUrl().isEmpty()) {
            TextView label = findViewById(R.id.videoLabel);
            label.setText(R.string.promo_video_uploaded);
            label.setTextColor(ContextCompat.getColor(this, R.color.success));
        }
    }

    // ----- Defensive JSON parsing (shapes vary across server builds) -----

    private void parseSegments(JsonElement el) {
        el = unwrap(el);
        if (el == null || !el.isJsonArray()) return;
        for (JsonElement e : el.getAsJsonArray()) {
            String id = "";
            if (e.isJsonPrimitive()) {
                id = e.getAsString();
            } else if (e.isJsonObject()) {
                id = jStr(e.getAsJsonObject(), "id");
            }
            Long v = parseLong(id);
            if (v != null && !selectedSegmentIds.contains(v)) selectedSegmentIds.add(v);
        }
    }

    private void parseSelectedProducts(JsonElement el) {
        el = unwrap(el);
        if (el == null || !el.isJsonObject()) return;
        JsonObject root = el.getAsJsonObject();

        for (JsonElement e : optArray(root, "single_products")) {
            if (e.isJsonObject()) singleLines.add(lineFromObject(e.getAsJsonObject()));
        }

        for (JsonElement e : optArray(root, "combo_deals")) {
            ComboDeal deal = new ComboDeal();
            if (e.isJsonObject()) {
                for (JsonElement pe : optArray(e.getAsJsonObject(), "products")) {
                    if (pe.isJsonObject()) deal.lines.add(lineFromObject(pe.getAsJsonObject()));
                }
            } else if (e.isJsonArray()) {                  // deal may be a bare product array
                for (JsonElement pe : e.getAsJsonArray()) {
                    if (pe.isJsonObject()) deal.lines.add(lineFromObject(pe.getAsJsonObject()));
                }
            }
            if (!deal.lines.isEmpty()) comboDeals.add(deal);
        }

        for (JsonElement e : optArray(root, "buy1_get1_deals")) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            B1G1Deal deal = new B1G1Deal();
            for (JsonElement be : optArray(o, "buy_products")) {
                if (be.isJsonObject()) deal.buy.add(productFromObject(be.getAsJsonObject()));
            }
            for (JsonElement fe : optArray(o, "free_products")) {
                if (fe.isJsonObject()) deal.free.add(productFromObject(fe.getAsJsonObject()));
            }
            deal.price = jStr(o, "campaign_selling_price");
            deal.stock = jStr(o, "available_stock");
            if (!deal.buy.isEmpty() || !deal.free.isEmpty()) b1g1Deals.add(deal);
        }
    }

    private ProductLine lineFromObject(JsonObject o) {
        ProductLine line = new ProductLine(productFromObject(o));
        String cp = jStr(o, "campaign_selling_price");
        if (!cp.isEmpty()) line.price = cp;
        String av = jStr(o, "available_stock");
        if (!av.isEmpty()) line.stock = av;
        return line;
    }

    private ProductsData.Product productFromObject(JsonObject o) {
        ProductsData.Product prod = new ProductsData.Product();
        prod.id = firstNonEmpty(jStr(o, "product_id"), jStr(o, "id"));
        prod.name = firstNonEmpty(jStr(o, "name"), jStr(o, "product_name"), "Product");
        prod.sku = jStr(o, "sku");
        prod.barcode = jStr(o, "barcode");
        prod.selling_price = firstNonEmpty(jStr(o, "original_price"), jStr(o, "selling_price"),
                jStr(o, "campaign_selling_price"));
        prod.stock = firstNonEmpty(jStr(o, "original_stock"), jStr(o, "available_stock"),
                jStr(o, "stock"));
        prod.image_url = firstNonEmpty(jStr(o, "image_url"), jStr(o, "primary_product_image_url"));
        prod.primary_product_image_url = jStr(o, "primary_product_image_url");
        prod.brand_name = firstNonEmpty(jStr(o, "brand_name"), nestedStr(o, "brand", "name"));
        prod.category_title = firstNonEmpty(jStr(o, "category_title"), nestedStr(o, "category", "title"));
        return prod;
    }

    /** Unwraps a value that may be a JSON-encoded string into its parsed element. */
    @Nullable
    private JsonElement unwrap(@Nullable JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            try {
                return JsonParser.parseString(el.getAsString());
            } catch (Exception e) {
                return null;
            }
        }
        return el;
    }

    private JsonArray optArray(JsonObject o, String key) {
        if (o != null && o.has(key) && o.get(key).isJsonArray()) return o.getAsJsonArray(key);
        return new JsonArray();
    }

    private String jStr(JsonObject o, String key) {
        if (o != null && o.has(key) && !o.get(key).isJsonNull() && o.get(key).isJsonPrimitive()) {
            return o.get(key).getAsString();
        }
        return "";
    }

    private String nestedStr(JsonObject o, String parent, String key) {
        if (o != null && o.has(parent) && o.get(parent).isJsonObject()) {
            return jStr(o.getAsJsonObject(parent), key);
        }
        return "";
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return "";
    }

    private void setField(int id, String value) {
        if (value != null) ((TextView) findViewById(id)).setText(value);
    }

    @Nullable
    private Long parseLong(String value) {
        try {
            return value == null || value.trim().isEmpty() ? null : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private Double parseNullableDouble(String value) {
        try {
            return value == null || value.trim().isEmpty() ? null : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- Address autocomplete -------------------------------------------

    private void setupAddressAutocomplete() {
        TextView field = findViewById(R.id.etAddressSearch);
        addressPopup = new android.widget.ListPopupWindow(this);
        addressPopup.setAnchorView(field);
        ((EditText) field).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (suppressAddressWatcher) return;
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
                if (data != null && data.predictions != null) predictions.addAll(data.predictions);
                showPredictions();
            }

            @Override
            public void onError(String message) {
                // Best-effort; the user can still drop a pin on the map.
            }
        });
    }

    private void showPredictions() {
        if (predictions.isEmpty()) {
            addressPopup.dismiss();
            return;
        }
        List<String> labels = new ArrayList<>();
        for (AutocompleteData.Prediction p : predictions) labels.add(p.description);
        addressPopup.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, labels));
        addressPopup.setOnItemClickListener((parent, view, position, id) -> {
            addressPopup.dismiss();
            AutocompleteData.Prediction p = predictions.get(position);
            pickedPlaceId = p.place_id == null ? "" : p.place_id;
            setAddressText(p.description);
            resolvePlace(p.place_id);
        });
        addressPopup.show();
    }

    private void resolvePlace(String placeId) {
        ApiClient.get(this).getCoordinateFromPlaceId(placeId, new ApiCallback<CoordinateData>() {
            @Override
            public void onSuccess(CoordinateData data) {
                if (data == null) return;
                pickedLat = data.latitude;
                pickedLng = data.longitude;
                if (data.formatted_address != null && !data.formatted_address.isEmpty()) {
                    setAddressText(data.formatted_address);
                }
                moveMapTo(new LatLng(data.latitude, data.longitude), true);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CreatePromotionActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setAddressText(String value) {
        suppressAddressWatcher = true;
        EditText field = findViewById(R.id.etAddressSearch);
        field.setText(value);
        field.setSelection(value.length());
        suppressAddressWatcher = false;
    }

    // ---- Map -------------------------------------------------------------

    private void setupMap(Bundle savedInstanceState) {
        promoMap = findViewById(R.id.promoMap);
        promoMap.onCreate(savedInstanceState);
        promoMap.getMapAsync(this);

        final NestedScrollView scroll = findViewById(R.id.step3Scroll);
        promoMap.setOnTouchListener((v, ev) -> {
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
        googleMap.setOnMapClickListener(latLng -> {
            pickedLat = latLng.latitude;
            pickedLng = latLng.longitude;
            pickedPlaceId = "";   // a manually dropped pin has no Places id
            placeMarker(latLng);
            reverseGeocode(latLng);
        });
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override public void onMarkerDragStart(Marker m) { }
            @Override public void onMarkerDrag(Marker m) { }
            @Override public void onMarkerDragEnd(Marker m) {
                pickedLat = m.getPosition().latitude;
                pickedLng = m.getPosition().longitude;
                pickedPlaceId = "";
                reverseGeocode(m.getPosition());
            }
        });

        if (pickedLat != null && pickedLng != null) {
            moveMapTo(new LatLng(pickedLat, pickedLng), true);
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(-29.8587, 31.0218), 9f)); // South Africa
        }
    }

    private void moveMapTo(LatLng latLng, boolean placePin) {
        if (googleMap == null) return;
        if (placePin) placeMarker(latLng);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
    }

    private void placeMarker(LatLng latLng) {
        if (googleMap == null) return;
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
                if (results != null && !results.isEmpty()
                        && results.get(0).getMaxAddressLineIndex() >= 0) {
                    final String line = results.get(0).getAddressLine(0);
                    runOnUiThread(() -> setAddressText(line));
                }
            } catch (Exception ignored) {
                // Offline / unavailable geocoder: the pin lat/lng is still captured.
            }
        }).start();
    }

    // ---- File helpers ----------------------------------------------------

    @Nullable
    private Http.FilePart readImagePart(String field, Uri uri) {
        if (uri == null) return null;
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

    // ---- Small view builders / helpers -----------------------------------

    private LinearLayout card() {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        group.setLayoutParams(lp);
        group.setBackgroundResource(R.drawable.bg_trader_type_card);
        return group;
    }

    private LinearLayout dealHeader(String title, Runnable onRemove) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        RobotoBoldTextView t = new RobotoBoldTextView(this);
        t.setText(title);
        t.setTextSize(14);
        t.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        t.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(t);
        ImageView del = new ImageView(this);
        del.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));
        del.setImageResource(R.drawable.ic_cancel_circle);
        del.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.text_tertiary));
        del.setOnClickListener(v -> onRemove.run());
        head.addView(del);
        return head;
    }

    private RobotoBoldTextView sectionLabel(String text) {
        RobotoBoldTextView t = new RobotoBoldTextView(this);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        t.setLayoutParams(lp);
        return t;
    }

    private TextView emptyHint(String text) {
        RobotoTextView t = new RobotoTextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(20), dp(8), dp(20));
        t.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        return t;
    }

    private TextView tabButton(String text, boolean selected) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        t.setLayoutParams(lp);
        applyChipState(t, selected);
        return t;
    }

    private EditText pickerSearchField() {
        EditText search = new EditText(this);
        search.setHint(R.string.promo_search_products);
        search.setTextSize(13);
        search.setSingleLine(true);
        search.setBackgroundResource(R.drawable.bg_login_input);
        search.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        lp.bottomMargin = dp(8);
        search.setLayoutParams(lp);
        return search;
    }

    private EditText numberField(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setTextSize(13);
        e.setSingleLine(true);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setBackgroundResource(R.drawable.bg_login_input);
        e.setPadding(dp(10), dp(8), dp(10), dp(8));
        return e;
    }

    private ScrollView wrapDialog(View content) {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        return scroll;
    }

    private TextWatcher simpleWatcher(Runnable onChange) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { onChange.run(); }
        };
    }

    private void setLabel(int id, String value) {
        TextView tv = findViewById(id);
        tv.setText(value);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
    }

    private String typeLabel(String value) {
        for (int i = 0; i < TYPE_VALUES.length; i++) {
            if (TYPE_VALUES[i].equals(value)) return TYPE_LABELS[i];
        }
        return "-";
    }

    private String objectiveLabel(String value) {
        for (int i = 0; i < OBJECTIVE_VALUES.length; i++) {
            if (OBJECTIVE_VALUES[i].equals(value)) return OBJECTIVE_LABELS[i];
        }
        return "-";
    }

    private int durationDays() {
        if (startDate.isEmpty() || endDate.isEmpty()) return 0;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date s = f.parse(startDate);
            Date e = f.parse(endDate);
            if (s == null || e == null) return 0;
            long diff = e.getTime() - s.getTime();
            int days = (int) (diff / (1000L * 60 * 60 * 24)) + 1;
            return Math.max(days, 0);
        } catch (Exception ex) {
            return 0;
        }
    }

    private String formatDisplayDate(String api) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy", Locale.US);
            Date d = in.parse(api);
            return d == null ? api : out.format(d);
        } catch (Exception e) {
            return api;
        }
    }

    private String range(long lo, long hi) {
        return groupThousands(lo) + " – " + groupThousands(hi);
    }

    private String groupThousands(long value) {
        return String.format(Locale.US, "%,d", value).replace(',', ' ');
    }

    private String formatRand(double value) {
        return "R" + groupThousands(Math.round(value));
    }

    private String trimNumber(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.valueOf(value);
    }

    private double parseDouble(String value) {
        try {
            return value == null || value.trim().isEmpty() ? 0 : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String text(int id) {
        TextView tv = findViewById(id);
        return tv.getText() == null ? "" : tv.getText().toString().trim();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ---- MapView lifecycle ----------------------------------------------

    @Override protected void onStart() { super.onStart(); if (promoMap != null) promoMap.onStart(); }
    @Override protected void onResume() { super.onResume(); if (promoMap != null) promoMap.onResume(); }
    @Override protected void onPause() { if (promoMap != null) promoMap.onPause(); super.onPause(); }
    @Override protected void onStop() { if (promoMap != null) promoMap.onStop(); super.onStop(); }
    @Override protected void onDestroy() { if (promoMap != null) promoMap.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (promoMap != null) promoMap.onLowMemory(); }

    @Override
    protected void onSaveInstanceState(@Nullable Bundle outState) {
        super.onSaveInstanceState(outState);
        if (promoMap != null && outState != null) {
            promoMap.onSaveInstanceState(outState);
        }
    }
}
