package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.Http;
import com.anokix.distributorapp.network.dto.AutocompleteData;
import com.anokix.distributorapp.network.dto.CoordinateData;
import com.anokix.distributorapp.network.dto.CreateTraderData;
import com.anokix.distributorapp.network.dto.ReferenceData;
import com.anokix.distributorapp.ui.views.FlowLayout;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;
import com.hbb20.CountryCodePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Eight-step "Add New Trader" wizard wired to /api/distributor/traders/create.
 *
 * Steps: 1 Owner · 2 Business · 3 Trader · 4 Location · 5 Verify ·
 *        6 Distributor · 7 Wallet · 8 Terms.
 *
 * All step views live inside a {@link ViewFlipper}, so every field keeps its
 * value when the user navigates back and forth. Lookup data (trader types,
 * product categories, distributors) comes from {@link ApiClient#getReference}.
 */
public class CreateTraderActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int STEP_COUNT = 8;
    private static final int STEP_TRADER = 2;     // 0-based
    private static final int STEP_LOCATION = 3;
    private static final int STEP_VERIFY = 4;
    private static final int STEP_TERMS = 7;

    private static final String[] STEP_KEYS = {
            "step_owner", "step_business", "step_trader", "step_location",
            "step_verify", "step_distributor", "step_wallet", "step_terms"
    };

    private static final String[] DAY_LABELS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private static final String[] PAYMENT_LABELS = {"Wallet", "EFT", "Cash", "Card"};
    private static final String[] PAYMENT_VALUES = {"wallet", "eft", "cash", "card"};

    private ViewFlipper flipper;
    private LinearLayout stepperContainer;
    private TextView btnContinue;
    private TextView btnBack;

    private final TextView[] stepCircles = new TextView[STEP_COUNT];
    private final TextView[] stepLabels = new TextView[STEP_COUNT];
    private int currentStep = 0;

    // Reference data
    private final List<ReferenceData.TraderType> traderTypes = new ArrayList<>();
    private final List<ReferenceData.ProductCategory> primaryCategories = new ArrayList<>();
    private final List<ReferenceData.Distributor> distributors = new ArrayList<>();

    // Selections
    private String selectedTraderTypeSlug;
    private final List<View> traderTypeCards = new ArrayList<>();
    private final List<Long> selectedCategoryIds = new ArrayList<>();
    private final List<Long> selectedDistributorIds = new ArrayList<>();
    private final List<String> selectedDays = new ArrayList<>();
    private String paymentMethodValue;
    private boolean createWalletNow = true;

    // Phone country pickers (hbb20 CCP)
    private CountryCodePicker ccpOwner;
    private CountryCodePicker ccpBusiness;

    // Documents
    private static final class DocDef {
        final String key, title, sub;
        final boolean required;
        DocDef(String key, String title, String sub, boolean required) {
            this.key = key; this.title = title; this.sub = sub; this.required = required;
        }
    }
    private final List<DocDef> docDefs = new ArrayList<>();
    private final Map<String, Uri> docUris = new LinkedHashMap<>();
    private final Map<String, TextView> docSubtitleViews = new LinkedHashMap<>();
    private final Map<String, TextView> docButtons = new LinkedHashMap<>();
    private String pendingDocKey;
    private ActivityResultLauncher<String> docPicker;

    // Map
    private MapView traderMap;
    private GoogleMap googleMap;
    private Marker marker;
    private Double pickedLat, pickedLng;

    // Address autocomplete
    private final Handler autocompleteHandler = new Handler(Looper.getMainLooper());
    private Runnable autocompleteRunnable;
    private boolean suppressAddressWatcher;
    private android.widget.ListPopupWindow addressPopup;
    private final List<AutocompleteData.Prediction> predictions = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_trader);

        flipper = findViewById(R.id.stepFlipper);
        stepperContainer = findViewById(R.id.stepperContainer);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);

        buildStepper();
        buildDeliveryDayChips();
        buildBenefits();
        buildDocumentRows();
        setupCountryPickers();
        setupSelectionFields();
        setupWalletOptions();
        setupNavigation();
        setupDocumentPicker();
        setupAddressAutocomplete();
        setupMap(savedInstanceState);

        showStep(0);
        loadReference();
    }

    // ---- Stepper ---------------------------------------------------------

    private void buildStepper() {
        LayoutInflater inflater = LayoutInflater.from(this);
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
            LinearLayout.LayoutParams circleLp = new LinearLayout.LayoutParams(dp(28), dp(28));
            circle.setLayoutParams(circleLp);
            circle.setGravity(Gravity.CENTER);
            circle.setTextSize(12);
            circle.setText(String.valueOf(i + 1));
            item.addView(circle);

            RobotoTextView label = new RobotoTextView(this);
            label.setText(resString(STEP_KEYS[i]));
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
            if (currentStep == STEP_TERMS) {
                submit();
            } else {
                showStep(currentStep + 1);
            }
        });
    }

    private void showStep(int index) {
        currentStep = index;
        flipper.setDisplayedChild(index);
        btnContinue.setText(index == STEP_TERMS ? R.string.register_btn : R.string.continue_btn);
        renderStepper();
        // Keep the active step chip visible in the horizontal stepper.
        stepperContainer.post(() -> {
            View item = stepLabels[index];
            if (item != null && item.getParent() instanceof View) {
                View parent = (View) item.getParent();
                ((android.widget.HorizontalScrollView) stepperContainer.getParent())
                        .smoothScrollTo(Math.max(0, parent.getLeft() - dp(40)), 0);
            }
        });
    }

    // ---- Validation ------------------------------------------------------

    private boolean validateStep(int step) {
        switch (step) {
            case 0:
                if (text(R.id.etFirstName).isEmpty()) return fail(R.string.err_first_name);
                if (text(R.id.etLastName).isEmpty()) return fail(R.string.err_last_name);
                if (text(R.id.etIdNumber).isEmpty() && text(R.id.etPassportNumber).isEmpty()) {
                    return fail(R.string.err_id_or_passport);
                }
                if (text(R.id.etOwnerMobile).isEmpty()) return fail(R.string.err_mobile);
                if (!isValidEmail(text(R.id.etOwnerEmail))) return fail(R.string.err_email);
                return true;
            case 1:
                if (text(R.id.etBusinessName).isEmpty()) return fail(R.string.err_business_name);
                return true;
            case STEP_TRADER:
                if (selectedTraderTypeSlug == null) return fail(R.string.err_trader_type);
                return true;
            case STEP_LOCATION:
                if (pickedLat == null || pickedLng == null) return fail(R.string.err_location);
                return true;
            case STEP_VERIFY:
                for (DocDef d : docDefs) {
                    if (d.required && !docUris.containsKey(d.key)) {
                        return fail(R.string.err_documents);
                    }
                }
                return true;
            case STEP_TERMS:
                if (!checked(R.id.cbAccurate) || !checked(R.id.cbTos)
                        || !checked(R.id.cbKyc) || !checked(R.id.cbComms)) {
                    return fail(R.string.err_terms);
                }
                return true;
            default:
                return true;
        }
    }

    private boolean fail(int msgRes) {
        Toast.makeText(this, msgRes, Toast.LENGTH_SHORT).show();
        return false;
    }

    // ---- Phone country pickers (CCP) -------------------------------------

    private void setupCountryPickers() {
        ccpOwner = findViewById(R.id.ccpOwner);
        ccpBusiness = findViewById(R.id.ccpBusiness);
        // Wire each picker to its number field so the CCP can produce a full,
        // correctly-formatted international number (e.g. +27821234567).
        ccpOwner.registerCarrierNumberEditText(findViewById(R.id.etOwnerMobile));
        ccpBusiness.registerCarrierNumberEditText(findViewById(R.id.etBusinessPhone));
    }

    /** Full international number, or "" when the carrier field is empty. */
    private String fullPhone(CountryCodePicker ccp, int numberFieldId) {
        return text(numberFieldId).isEmpty() ? "" : ccp.getFullNumberWithPlus();
    }

    // ---- Trader types / categories / distributors / payment / days -------

    private void setupSelectionFields() {
        findViewById(R.id.categoryField).setOnClickListener(v -> showCategoryDialog());
        findViewById(R.id.distributorField).setOnClickListener(v -> showDistributorDialog());
        findViewById(R.id.paymentMethodField).setOnClickListener(v -> showPaymentDialog());
    }

    private void renderTraderTypes() {
        FlowLayout container = findViewById(R.id.traderTypeContainer);
        container.removeAllViews();
        traderTypeCards.clear();
        int cardWidth = (getResources().getDisplayMetrics().widthPixels - dp(32) - dp(8)) / 2;
        for (ReferenceData.TraderType type : traderTypes) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(8), dp(16), dp(8), dp(16));
            card.setLayoutParams(new ViewGroup.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            ImageView icon = new ImageView(this);
            icon.setLayoutParams(new LinearLayout.LayoutParams(dp(24), dp(24)));
            icon.setImageResource(R.drawable.ic_stores);
            card.addView(icon);

            RobotoTextView label = new RobotoTextView(this);
            label.setText(type.name);
            label.setTextSize(13);
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llp.topMargin = dp(8);
            label.setLayoutParams(llp);
            card.addView(label);

            card.setOnClickListener(v -> {
                selectedTraderTypeSlug = type.slug;
                applyTraderTypeSelection();
            });

            container.addView(card);
            traderTypeCards.add(card);
        }
        applyTraderTypeSelection();
    }

    private void applyTraderTypeSelection() {
        for (int i = 0; i < traderTypeCards.size(); i++) {
            View card = traderTypeCards.get(i);
            boolean selected = traderTypes.get(i).slug.equals(selectedTraderTypeSlug);
            card.setBackgroundResource(selected
                    ? R.drawable.bg_trader_type_card_selected : R.drawable.bg_trader_type_card);
            ImageView icon = (ImageView) ((LinearLayout) card).getChildAt(0);
            TextView label = (TextView) ((LinearLayout) card).getChildAt(1);
            int color = ContextCompat.getColor(this,
                    selected ? R.color.purple_primary : R.color.text_primary);
            icon.setColorFilter(color);
            label.setTextColor(color);
        }
    }

    private void showCategoryDialog() {
        if (primaryCategories.isEmpty()) {
            return;
        }
        String[] names = new String[primaryCategories.size()];
        boolean[] checkedItems = new boolean[primaryCategories.size()];
        for (int i = 0; i < primaryCategories.size(); i++) {
            names[i] = primaryCategories.get(i).title;
            checkedItems[i] = selectedCategoryIds.contains(primaryCategories.get(i).id);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.primary_product_categories)
                .setMultiChoiceItems(names, checkedItems, (d, which, isChecked) -> {
                    long id = primaryCategories.get(which).id;
                    if (isChecked) {
                        if (!selectedCategoryIds.contains(id)) selectedCategoryIds.add(id);
                    } else {
                        selectedCategoryIds.remove(id);
                    }
                })
                .setPositiveButton(android.R.string.ok, (d, w) -> renderCategoryChips())
                .show();
    }

    private void renderCategoryChips() {
        FlowLayout chips = findViewById(R.id.categoryChips);
        chips.removeAllViews();
        TextView hint = findViewById(R.id.categoryHint);
        hint.setVisibility(selectedCategoryIds.isEmpty() ? View.VISIBLE : View.GONE);
        for (ReferenceData.ProductCategory cat : primaryCategories) {
            if (!selectedCategoryIds.contains(cat.id)) {
                continue;
            }
            chips.addView(buildChip(cat.title, () -> {
                selectedCategoryIds.remove(cat.id);
                renderCategoryChips();
            }));
        }
    }

    private void showDistributorDialog() {
        if (distributors.isEmpty()) {
            return;
        }
        String[] names = new String[distributors.size()];
        boolean[] checkedItems = new boolean[distributors.size()];
        for (int i = 0; i < distributors.size(); i++) {
            names[i] = distributors.get(i).displayName();
            checkedItems[i] = selectedDistributorIds.contains(distributors.get(i).id);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.preferred_distributors)
                .setMultiChoiceItems(names, checkedItems, (d, which, isChecked) -> {
                    long id = distributors.get(which).id;
                    if (isChecked) {
                        if (!selectedDistributorIds.contains(id)) selectedDistributorIds.add(id);
                    } else {
                        selectedDistributorIds.remove(id);
                    }
                })
                .setPositiveButton(android.R.string.ok, (d, w) -> renderDistributorChips())
                .show();
    }

    private void renderDistributorChips() {
        FlowLayout chips = findViewById(R.id.distributorChips);
        chips.removeAllViews();
        TextView hint = findViewById(R.id.distributorHint);
        hint.setVisibility(selectedDistributorIds.isEmpty() ? View.VISIBLE : View.GONE);
        for (ReferenceData.Distributor dist : distributors) {
            if (!selectedDistributorIds.contains(dist.id)) {
                continue;
            }
            chips.addView(buildChip(dist.displayName(), () -> {
                selectedDistributorIds.remove(dist.id);
                renderDistributorChips();
            }));
        }
    }

    private void showPaymentDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.preferred_payment_method)
                .setItems(PAYMENT_LABELS, (d, which) -> {
                    paymentMethodValue = PAYMENT_VALUES[which];
                    TextView label = findViewById(R.id.paymentMethodLabel);
                    label.setText(PAYMENT_LABELS[which]);
                    label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                })
                .show();
    }

    private void buildDeliveryDayChips() {
        FlowLayout container = findViewById(R.id.deliveryDaysContainer);
        for (String day : DAY_LABELS) {
            TextView chip = new TextView(this);
            chip.setText(day);
            chip.setTextSize(13);
            chip.setPadding(dp(16), dp(8), dp(16), dp(8));
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            chip.setOnClickListener(v -> {
                if (selectedDays.contains(day)) {
                    selectedDays.remove(day);
                    chip.setBackgroundResource(R.drawable.bg_chip_unselected);
                    chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                } else {
                    selectedDays.add(day);
                    chip.setBackgroundResource(R.drawable.bg_trader_type_card_selected);
                    chip.setTextColor(ContextCompat.getColor(this, R.color.purple_primary));
                }
            });
            container.addView(chip);
        }
    }

    /** A removable purple chip (reuses item_tag_chip). */
    private View buildChip(String label, Runnable onRemove) {
        View chip = LayoutInflater.from(this).inflate(R.layout.item_tag_chip,
                (ViewGroup) findViewById(R.id.categoryChips), false);
        ((TextView) chip.findViewById(R.id.tagText)).setText(label);
        chip.findViewById(R.id.tagRemove).setOnClickListener(v -> onRemove.run());
        return chip;
    }

    // ---- Wallet ----------------------------------------------------------

    private void setupWalletOptions() {
        findViewById(R.id.optionCreateWallet).setOnClickListener(v -> setWallet(true));
        findViewById(R.id.optionActivateLater).setOnClickListener(v -> setWallet(false));
        setWallet(true);
    }

    private void setWallet(boolean now) {
        createWalletNow = now;
        ((ImageView) findViewById(R.id.radioCreateWallet)).setImageResource(
                now ? R.drawable.ic_radio_selected : R.drawable.ic_radio_unselected);
        ((ImageView) findViewById(R.id.radioActivateLater)).setImageResource(
                now ? R.drawable.ic_radio_unselected : R.drawable.ic_radio_selected);
    }

    private void buildBenefits() {
        LinearLayout container = findViewById(R.id.benefitsContainer);
        int[] benefits = {R.string.benefit_faster_payments, R.string.benefit_access_credit,
                R.string.benefit_insurance, R.string.benefit_rewards, R.string.benefit_growth_tools};
        for (int res : benefits) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));

            ImageView check = new ImageView(this);
            check.setLayoutParams(new LinearLayout.LayoutParams(dp(18), dp(18)));
            check.setImageResource(R.drawable.ic_check_green);
            row.addView(check);

            RobotoTextView label = new RobotoTextView(this);
            label.setText(res);
            label.setTextSize(14);
            label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = dp(10);
            label.setLayoutParams(lp);
            row.addView(label);

            container.addView(row);
        }
    }

    // ---- Documents -------------------------------------------------------

    private void buildDocumentRows() {
        docDefs.add(new DocDef("business_registration_document",
                getString(R.string.doc_business_reg), getString(R.string.doc_business_reg_sub), true));
        docDefs.add(new DocDef("owner_id_document",
                getString(R.string.doc_owner_id), getString(R.string.doc_owner_id_sub), true));
        docDefs.add(new DocDef("proof_of_address",
                getString(R.string.doc_proof_address), getString(R.string.doc_proof_address_sub), true));
        docDefs.add(new DocDef("store_front_photo",
                getString(R.string.doc_store_front), getString(R.string.doc_store_front_sub), true));
        docDefs.add(new DocDef("store_interior_photo",
                getString(R.string.doc_store_interior), getString(R.string.doc_store_interior_sub), true));
        docDefs.add(new DocDef("additional_documents",
                getString(R.string.doc_additional), getString(R.string.doc_additional_sub), false));

        LinearLayout container = findViewById(R.id.documentContainer);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (DocDef def : docDefs) {
            View row = inflater.inflate(R.layout.item_document_row, container, false);
            TextView title = row.findViewById(R.id.docTitle);
            TextView sub = row.findViewById(R.id.docSubtitle);
            TextView upload = row.findViewById(R.id.docUploadButton);
            title.setText(def.required ? def.title + " *" : def.title);
            sub.setText(def.sub);
            upload.setOnClickListener(v -> {
                pendingDocKey = def.key;
                docPicker.launch("*/*");
            });
            docSubtitleViews.put(def.key, sub);
            docButtons.put(def.key, upload);
            container.addView(row);
        }
    }

    private void setupDocumentPicker() {
        docPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null || pendingDocKey == null) {
                return;
            }
            docUris.put(pendingDocKey, uri);
            TextView sub = docSubtitleViews.get(pendingDocKey);
            TextView btn = docButtons.get(pendingDocKey);
            String name = queryFileName(uri);
            if (sub != null && name != null) {
                sub.setText(name);
                sub.setTextColor(ContextCompat.getColor(this, R.color.success));
            }
            if (btn != null) {
                btn.setText(R.string.update_btn);
            }
            pendingDocKey = null;
        });
    }

    // ---- Address autocomplete -------------------------------------------

    private void setupAddressAutocomplete() {
        TextView field = findViewById(R.id.etAddressSearch);
        addressPopup = new android.widget.ListPopupWindow(this);
        addressPopup.setAnchorView(field);

        ((android.widget.EditText) field).addTextChangedListener(new TextWatcher() {
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
                // Autocomplete is best-effort; the user can still drop a pin on the map.
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
                Toast.makeText(CreateTraderActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setAddressText(String value) {
        suppressAddressWatcher = true;
        android.widget.EditText field = findViewById(R.id.etAddressSearch);
        field.setText(value);
        field.setSelection(value.length());
        suppressAddressWatcher = false;
    }

    // ---- Map -------------------------------------------------------------

    private void setupMap(Bundle savedInstanceState) {
        traderMap = findViewById(R.id.traderMap);
        traderMap.onCreate(savedInstanceState);
        traderMap.getMapAsync(this);

        final NestedScrollView scroll = findViewById(R.id.step4Scroll);
        traderMap.setOnTouchListener((v, ev) -> {
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
                    new LatLng(-29.8587, 31.0218), 9f)); // South Africa
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

    // ---- Reference data --------------------------------------------------

    private void loadReference() {
        ApiClient.get(this).getReference(new ApiCallback<ReferenceData>() {
            @Override
            public void onSuccess(ReferenceData data) {
                if (data == null) {
                    return;
                }
                if (data.trader_types != null) {
                    traderTypes.addAll(data.trader_types);
                }
                if (data.distributors != null) {
                    distributors.addAll(data.distributors);
                }
                if (data.product_categories != null) {
                    for (ReferenceData.ProductCategory c : data.product_categories) {
                        if (c.parent_id == null) {
                            primaryCategories.add(c);
                        }
                    }
                }
                renderTraderTypes();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CreateTraderActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- Submit ----------------------------------------------------------

    private void submit() {
        Map<String, String> form = new TreeMap<>();
        form.put("first_name", text(R.id.etFirstName));
        form.put("last_name", text(R.id.etLastName));
        form.put("id_number", text(R.id.etIdNumber));
        form.put("passport_number", text(R.id.etPassportNumber));
        form.put("mobile_number", fullPhone(ccpOwner, R.id.etOwnerMobile));
        form.put("email_address", text(R.id.etOwnerEmail));

        form.put("business_name", text(R.id.etBusinessName));
        form.put("trading_name", text(R.id.etTradingName));
        form.put("business_registration_number", text(R.id.etBusinessReg));
        form.put("business_phone_number", fullPhone(ccpBusiness, R.id.etBusinessPhone));
        form.put("business_email", text(R.id.etBusinessEmail));

        form.put("trader_type", selectedTraderTypeSlug == null ? "" : selectedTraderTypeSlug);
        form.put("primary_product_category_ids", joinLongs(selectedCategoryIds));

        form.put("address", text(R.id.etAddressSearch));
        if (pickedLat != null && pickedLng != null) {
            form.put("latitude", String.valueOf(pickedLat));
            form.put("longitude", String.valueOf(pickedLng));
        }

        form.put("preferred_distributor_ids", joinLongs(selectedDistributorIds));
        if (!selectedDistributorIds.isEmpty()) {
            form.put("recommended_distributor", String.valueOf(selectedDistributorIds.get(0)));
        }
        form.put("preferred_delivery_days", TextUtils.join(",", selectedDays));
        if (paymentMethodValue != null) {
            form.put("payment_method", paymentMethodValue);
        }
        form.put("create_wallet", createWalletNow ? "1" : "0");

        List<Http.FilePart> files = new ArrayList<>();
        for (Map.Entry<String, Uri> e : docUris.entrySet()) {
            Http.FilePart part = readFilePart(e.getKey(), e.getValue());
            if (part != null) {
                files.add(part);
            }
        }

        btnContinue.setEnabled(false);
        btnContinue.setText(R.string.registering);
        ApiClient.get(this).createTrader(form, files, new ApiCallback<CreateTraderData>() {
            @Override
            public void onSuccess(CreateTraderData data) {
                Toast.makeText(CreateTraderActivity.this,
                        R.string.trader_created_msg, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String message) {
                btnContinue.setEnabled(true);
                btnContinue.setText(R.string.register_btn);
                Toast.makeText(CreateTraderActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- File helpers ----------------------------------------------------

    @Nullable
    private Http.FilePart readFilePart(String field, Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
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
        try (android.database.Cursor cursor = getContentResolver().query(
                uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    name = cursor.getString(idx);
                }
            }
        } catch (Exception ignored) {
        }
        return name;
    }

    // ---- Small helpers ---------------------------------------------------

    private String text(int id) {
        TextView tv = findViewById(id);
        return tv.getText() == null ? "" : tv.getText().toString().trim();
    }

    private boolean checked(int id) {
        return ((CheckBox) findViewById(id)).isChecked();
    }

    private boolean isValidEmail(String email) {
        return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private String joinLongs(List<Long> values) {
        StringBuilder sb = new StringBuilder();
        for (Long v : values) {
            if (sb.length() > 0) sb.append(',');
            sb.append(v);
        }
        return sb.toString();
    }

    private String resString(String name) {
        int id = getResources().getIdentifier(name, "string", getPackageName());
        return id != 0 ? getString(id) : name;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ---- MapView lifecycle ----------------------------------------------

    @Override protected void onStart() { super.onStart(); if (traderMap != null) traderMap.onStart(); }
    @Override protected void onResume() { super.onResume(); if (traderMap != null) traderMap.onResume(); }
    @Override protected void onPause() { if (traderMap != null) traderMap.onPause(); super.onPause(); }
    @Override protected void onStop() { if (traderMap != null) traderMap.onStop(); super.onStop(); }
    @Override protected void onDestroy() { if (traderMap != null) traderMap.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (traderMap != null) traderMap.onLowMemory(); }

    @Override
    protected void onSaveInstanceState(@Nullable Bundle outState) {
        super.onSaveInstanceState(outState);
        if (traderMap != null && outState != null) {
            traderMap.onSaveInstanceState(outState);
        }
    }
}
