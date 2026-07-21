package com.anokix.distributorapp.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
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
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.Http;
import com.anokix.distributorapp.network.dto.AutocompleteData;
import com.anokix.distributorapp.network.dto.CoordinateData;
import com.anokix.distributorapp.network.dto.ReferenceData;
import com.anokix.distributorapp.network.dto.RegisterDistributorData;
import com.anokix.distributorapp.ui.views.FlowLayout;
import com.anokix.distributorapp.ui.views.LogoCropView;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hbb20.CountryCodePicker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Eight-step public distributor registration ("Create Account") wired to
 * POST /api/distributor/register.
 *
 * Steps: 1 Company · 2 Business · 3 Contacts · 4 Location · 5 Documents ·
 *        6 Service · 7 Wallet · 8 Terms.
 *
 * Every step lives inside a {@link ViewFlipper}, so field values survive
 * forward/back navigation. Product categories are loaded from the public
 * {@link ApiClient#getReference} endpoint. On success the user lands on
 * {@link RegisterSubmittedActivity}.
 */
public class RegisterDistributorActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int STEP_COUNT = 8;
    private static final int STEP_LOCATION = 3;
    private static final int STEP_DOCUMENTS = 4;
    private static final int STEP_SERVICE = 5;
    private static final int STEP_WALLET = 6;
    private static final int STEP_TERMS = 7;

    private static final String[] STEP_KEYS = {
            "rstep_company", "rstep_business", "rstep_contacts", "rstep_location",
            "rstep_documents", "rstep_service", "rstep_wallet", "rstep_terms"
    };

    private static final String LOGO_TARGET = "__logo__";

    // Dropdown option sets: display labels (UI) paired with the API codes the web
    // frontend submits (see the Chrome payload dump). Codes confirmed by the dump:
    // sole_proprietor, 1_10, under_1m, absa, cheque, 1_3, regional. The remaining
    // codes follow the same pattern and are pending confirmation.
    private static final String[] BUSINESS_TYPES = {
            "Sole Proprietor", "(Pty) Ltd", "Close Corporation", "Partnership", "Trust", "Other"};
    private static final String[] BUSINESS_TYPE_CODES = {
            "sole_proprietor", "pty_ltd", "close_corporation", "partnership", "trust", "other"};
    private static final String[] EMPLOYEE_RANGES = {"1-10", "11-50", "51-200", "201-500", "500+"};
    private static final String[] EMPLOYEE_RANGE_CODES = {"1_10", "11_50", "51_200", "201_500", "500_plus"};
    private static final String[] TURNOVER_RANGES = {
            "Under R1 million", "R1M-R5M", "R5M-R20M", "R20M-R50M", "Over R50M"};
    private static final String[] TURNOVER_RANGE_CODES = {
            "under_1m", "1m_5m", "5m_20m", "20m_50m", "over_50m"};
    private static final String[] BANKS = {
            "ABSA", "FNB", "Nedbank", "Standard Bank", "Capitec", "Investec", "TymeBank", "Other"};
    private static final String[] BANK_CODES = {
            "absa", "fnb", "nedbank", "standard_bank", "capitec", "investec", "tymebank", "other"};
    private static final String[] ACCOUNT_TYPES = {
            "Cheque / Current", "Savings", "Business", "Transmission"};
    private static final String[] ACCOUNT_TYPE_CODES = {
            "cheque", "savings", "business", "transmission"};
    private static final String[] FLEET_SIZES = {
            "No fleet", "1-3 vehicles", "4-10 vehicles", "11-20 vehicles", "21+ vehicles"};
    private static final String[] FLEET_SIZE_CODES = {
            "no_fleet", "1_3", "4_10", "11_20", "21_plus"};

    // Delivery-day table order matches the design (Sun first).
    private static final String[] DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final boolean[] DAYS_DEFAULT_ON = {false, true, true, true, true, true, false};

    private ViewFlipper flipper;
    private LinearLayout stepperContainer;
    private TextView btnContinue;
    private TextView btnBack;

    private final TextView[] stepCircles = new TextView[STEP_COUNT];
    private final TextView[] stepLabels = new TextView[STEP_COUNT];
    private int currentStep = 0;

    private boolean passwordVisible;
    private boolean confirmVisible;

    // Phone pickers
    private CountryCodePicker ccpCompany;
    private CountryCodePicker ccpMobile;
    private CountryCodePicker ccpPrimary;

    // Dropdown selections (key -> value sent to the API)
    private final Map<String, String> dropdownValues = new LinkedHashMap<>();

    // Coverage
    private String selectedCoverage;
    private LinearLayout coverageLocal, coverageRegional, coverageNational;

    // Reference data / categories
    private final List<ReferenceData.ProductCategory> primaryCategories = new ArrayList<>();
    private final List<Long> selectedCategoryIds = new ArrayList<>();

    // Delivery days
    private final List<CheckBox> dayChecks = new ArrayList<>();
    private final List<EditText> dayOpen = new ArrayList<>();
    private final List<EditText> dayClose = new ArrayList<>();

    // Additional contacts (dynamic rows)
    private final List<View> additionalContacts = new ArrayList<>();
    private LinearLayout additionalContactsContainer;

    // Logo
    private LinearLayout logoPlaceholder;
    private ImageView logoPreview;
    private Bitmap logoBitmap;

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

    // Wallet
    private boolean createWalletNow = true;

    // Wallet › signing-owner identity + residential address (only sent when "Create Wallet Now")
    private static final String[] ID_TYPE_LABELS = {"SA ID Number", "Passport", "Asylum Document"};
    private static final String[] ID_TYPE_CODES = {"ID", "passport", "asylum"};
    private static final String[] GENDER_VALUES = {"Male", "Female", "Other"};
    // Province display names + ISO 3166-2:ZA codes used by the payload (e.g. "ZA-FS").
    private static final String[] PROVINCE_LABELS = {
            "Eastern Cape", "Free State", "Gauteng", "Limpopo", "Mpumalanga",
            "Northern Cape", "Kwa-Zulu Natal", "North West", "Western Cape"};
    private static final String[] PROVINCE_CODES = {
            "ZA-EC", "ZA-FS", "ZA-GP", "ZA-LP", "ZA-MP",
            "ZA-NC", "ZA-KZN", "ZA-NW", "ZA-WC"};

    private String walletIdTypeCode;     // "ID" / "passport" / "asylum"
    private String walletGender;         // "Male" / "Female" / "Other"
    private String walletProvinceCode;   // e.g. "ZA-FS"
    private String walletDobIso;         // yyyy-MM-dd
    private String walletPassportExpIso; // yyyy-MM-dd
    private boolean walletUssd = true;
    private boolean walletInternet = true;
    private boolean walletCrossBorder = false;
    private boolean walletSaIdValid;     // true once a valid 13-digit SA ID is parsed

    private final SimpleDateFormat walletIsoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat walletReadableFmt = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    // Image / file pickers
    private String pendingTarget;          // LOGO_TARGET or a document key
    private Uri pendingCameraUri;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> fileLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    // Map
    private MapView registerMap;
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
        setContentView(R.layout.activity_register_distributor);

        flipper = findViewById(R.id.stepFlipper);
        stepperContainer = findViewById(R.id.stepperContainer);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);
        additionalContactsContainer = findViewById(R.id.additionalContactsContainer);
        logoPlaceholder = findViewById(R.id.logoPlaceholder);
        logoPreview = findViewById(R.id.logoPreview);

        buildStepper();
        buildBenefits();
        buildDocumentRows();
        buildDeliveryDayTable();
        setupCountryPickers();
        setupPasswordToggles();
        setupLogoUpload();
        setupDropdowns();
        setupCoverage();
        setupCategoryField();
        setupContacts();
        setupWalletOptions();
        setupWalletFields();
        setupNavigation();
        setupPickers();
        setupAddressAutocomplete();
        setupMap(savedInstanceState);

        showStep(0);
        loadReference();
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
                if (text(R.id.etCompanyLegalName).isEmpty()) return fail(R.string.err_company_legal_name);
                if (!isValidEmail(text(R.id.etCompanyEmail))) return fail(R.string.err_company_email);
                if (text(R.id.etCompanyPhone).isEmpty()) return fail(R.string.err_company_phone);
                if (text(R.id.etFirstName).isEmpty()) return fail(R.string.err_reg_first_name);
                if (text(R.id.etLastName).isEmpty()) return fail(R.string.err_reg_last_name);
                if (text(R.id.etMobile).isEmpty()) return fail(R.string.err_reg_mobile);
                if (!isValidEmail(text(R.id.etEmail))) return fail(R.string.err_email);
                if (text(R.id.etPassword).length() < 8) return fail(R.string.err_password_length);
                if (!text(R.id.etPassword).equals(text(R.id.etConfirmPassword))) {
                    return fail(R.string.err_password_match);
                }
                return true;
            case 1:
                if (!dropdownValues.containsKey("business_type")) return fail(R.string.err_business_type);
                if (!dropdownValues.containsKey("bank_name")) return fail(R.string.err_bank_name);
                return true;
            case 2:
                if (text(R.id.etPrimaryName).isEmpty()) return fail(R.string.err_primary_contact_name);
                if (text(R.id.etPrimaryMobile).isEmpty()) return fail(R.string.err_primary_contact_mobile);
                return true;
            case STEP_LOCATION:
                if (pickedLat == null || pickedLng == null) return fail(R.string.err_location);
                return true;
            case STEP_DOCUMENTS:
                for (DocDef d : docDefs) {
                    if (d.required && !docUris.containsKey(d.key)) {
                        return fail(R.string.err_required_documents);
                    }
                }
                return true;
            case STEP_SERVICE:
                if (selectedCoverage == null) return fail(R.string.err_coverage);
                return true;
            case STEP_WALLET:
                return validateWallet();
            case STEP_TERMS:
                if (!checked(R.id.cbAccurate) || !checked(R.id.cbTos)
                        || !checked(R.id.cbKyc) || !checked(R.id.cbComms)) {
                    return fail(R.string.err_terms_reg);
                }
                return true;
            default:
                return true;
        }
    }

    /** Wallet step: nothing required when "Activate Later"; full identity + address otherwise. */
    private boolean validateWallet() {
        if (!createWalletNow) {
            return true;
        }
        if (walletIdTypeCode == null) return fail(R.string.err_wallet_id_type);
        String idNumber = text(R.id.etWalletIdNumber);
        if (idNumber.isEmpty()) return fail(R.string.err_wallet_id_number);
        if ("ID".equals(walletIdTypeCode) && !walletSaIdValid) {
            return fail(R.string.err_wallet_sa_id);
        }
        if ("passport".equals(walletIdTypeCode)) {
            if (text(R.id.etWalletPassportCountry).isEmpty()) {
                return fail(R.string.err_wallet_passport_country);
            }
            if (walletPassportExpIso == null) return fail(R.string.err_wallet_passport_exp);
        }
        if (walletDobIso == null) return fail(R.string.err_wallet_dob);
        if (walletGender == null) return fail(R.string.err_wallet_gender);
        if (text(R.id.etWalletStreet).isEmpty()) return fail(R.string.err_wallet_street);
        if (text(R.id.etWalletSuburb).isEmpty()) return fail(R.string.err_wallet_suburb);
        if (text(R.id.etWalletCity).isEmpty()) return fail(R.string.err_wallet_city);
        if (walletProvinceCode == null) return fail(R.string.err_wallet_province);
        if (text(R.id.etWalletPostCode).isEmpty()) return fail(R.string.err_wallet_post_code);
        return true;
    }

    private boolean fail(int msgRes) {
        Toast.makeText(this, msgRes, Toast.LENGTH_SHORT).show();
        return false;
    }

    // ---- Phone pickers / password ---------------------------------------

    private void setupCountryPickers() {
        ccpCompany = findViewById(R.id.ccpCompany);
        ccpMobile = findViewById(R.id.ccpMobile);
        ccpPrimary = findViewById(R.id.ccpPrimary);
        ccpCompany.registerCarrierNumberEditText(findViewById(R.id.etCompanyPhone));
        ccpMobile.registerCarrierNumberEditText(findViewById(R.id.etMobile));
        ccpPrimary.registerCarrierNumberEditText(findViewById(R.id.etPrimaryMobile));
    }

    private String fullPhone(CountryCodePicker ccp, int numberFieldId) {
        return text(numberFieldId).isEmpty() ? "" : ccp.getFullNumberWithPlus();
    }

    private void setupPasswordToggles() {
        EditText pwd = findViewById(R.id.etPassword);
        EditText confirm = findViewById(R.id.etConfirmPassword);
        pwd.setTransformationMethod(PasswordTransformationMethod.getInstance());
        confirm.setTransformationMethod(PasswordTransformationMethod.getInstance());

        ImageButton togglePwd = findViewById(R.id.togglePassword);
        ImageButton toggleConfirm = findViewById(R.id.toggleConfirmPassword);
        togglePwd.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            pwd.setTransformationMethod(passwordVisible
                    ? null : PasswordTransformationMethod.getInstance());
            togglePwd.setImageResource(passwordVisible
                    ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            pwd.setSelection(pwd.getText() != null ? pwd.getText().length() : 0);
        });
        toggleConfirm.setOnClickListener(v -> {
            confirmVisible = !confirmVisible;
            confirm.setTransformationMethod(confirmVisible
                    ? null : PasswordTransformationMethod.getInstance());
            toggleConfirm.setImageResource(confirmVisible
                    ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            confirm.setSelection(confirm.getText() != null ? confirm.getText().length() : 0);
        });
    }

    // ---- Dropdowns -------------------------------------------------------

    private void setupDropdowns() {
        String[] years = buildYears();
        bindDropdown(R.id.fieldBusinessType, R.id.lblBusinessType, "business_type",
                R.string.type_of_business, BUSINESS_TYPES, BUSINESS_TYPE_CODES);
        bindDropdown(R.id.fieldYear, R.id.lblYear, "year_established",
                R.string.year_established, years, years);
        bindDropdown(R.id.fieldEmployees, R.id.lblEmployees, "number_of_employees",
                R.string.number_of_employees, EMPLOYEE_RANGES, EMPLOYEE_RANGE_CODES);
        bindDropdown(R.id.fieldTurnover, R.id.lblTurnover, "annual_turnover",
                R.string.annual_turnover, TURNOVER_RANGES, TURNOVER_RANGE_CODES);
        bindDropdown(R.id.fieldBank, R.id.lblBank, "bank_name",
                R.string.bank_name, BANKS, BANK_CODES);
        bindDropdown(R.id.fieldAccountType, R.id.lblAccountType, "account_type",
                R.string.account_type, ACCOUNT_TYPES, ACCOUNT_TYPE_CODES);
        bindDropdown(R.id.fieldClients, R.id.lblClients, "active_clients_range",
                R.string.number_active_clients, EMPLOYEE_RANGES, EMPLOYEE_RANGE_CODES);
        bindDropdown(R.id.fieldFleet, R.id.lblFleet, "delivery_fleet_size",
                R.string.delivery_fleet_size_label, FLEET_SIZES, FLEET_SIZE_CODES);
    }

    private void bindDropdown(int fieldId, int labelId, String key, int titleRes,
                              String[] options, String[] codes) {
        TextView label = findViewById(labelId);
        findViewById(fieldId).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(titleRes)
                        .setItems(options, (d, which) -> {
                            dropdownValues.put(key, codes[which]); // submit the API code
                            label.setText(options[which]);         // show the display label
                            label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                        })
                        .show());
    }

    private String[] buildYears() {
        int start = 2026;
        int end = 1950;
        String[] years = new String[start - end + 1];
        for (int y = start; y >= end; y--) {
            years[start - y] = String.valueOf(y);
        }
        return years;
    }

    // ---- Coverage --------------------------------------------------------

    private void setupCoverage() {
        coverageLocal = findViewById(R.id.coverageLocal);
        coverageRegional = findViewById(R.id.coverageRegional);
        coverageNational = findViewById(R.id.coverageNational);
        coverageLocal.setOnClickListener(v -> applyCoverage("Local"));
        coverageRegional.setOnClickListener(v -> applyCoverage("Regional"));
        coverageNational.setOnClickListener(v -> applyCoverage("National"));
    }

    private void applyCoverage(String value) {
        selectedCoverage = value;
        styleCoverageCard(coverageLocal, R.id.iconCoverageLocal, R.id.titleCoverageLocal,
                "Local".equals(value));
        styleCoverageCard(coverageRegional, R.id.iconCoverageRegional, R.id.titleCoverageRegional,
                "Regional".equals(value));
        styleCoverageCard(coverageNational, R.id.iconCoverageNational, R.id.titleCoverageNational,
                "National".equals(value));
    }

    private void styleCoverageCard(View card, int iconId, int titleId, boolean selected) {
        card.setBackgroundResource(selected
                ? R.drawable.bg_trader_type_card_selected : R.drawable.bg_trader_type_card);
        int color = ContextCompat.getColor(this,
                selected ? R.color.purple_primary : R.color.text_secondary);
        ((ImageView) card.findViewById(iconId)).setColorFilter(color);
        ((TextView) card.findViewById(titleId)).setTextColor(ContextCompat.getColor(this,
                selected ? R.color.purple_primary : R.color.text_primary));
    }

    // ---- Categories ------------------------------------------------------

    private void setupCategoryField() {
        findViewById(R.id.categoryField).setOnClickListener(v -> showCategoryDialog());
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

    private View buildChip(String label, Runnable onRemove) {
        View chip = LayoutInflater.from(this).inflate(R.layout.item_tag_chip,
                (ViewGroup) findViewById(R.id.categoryChips), false);
        ((TextView) chip.findViewById(R.id.tagText)).setText(label);
        chip.findViewById(R.id.tagRemove).setOnClickListener(v -> onRemove.run());
        return chip;
    }

    // ---- Contacts --------------------------------------------------------

    private void setupContacts() {
        findViewById(R.id.btnAddContact).setOnClickListener(v -> addContactRow());
    }

    private void addContactRow() {
        View row = LayoutInflater.from(this).inflate(
                R.layout.item_register_contact, additionalContactsContainer, false);
        TextView header = row.findViewById(R.id.contactHeader);
        header.setText(getString(R.string.additional_contact, additionalContacts.size() + 1));
        CountryCodePicker ccp = row.findViewById(R.id.contactCcp);
        ccp.registerCarrierNumberEditText(row.findViewById(R.id.contactMobile));
        row.findViewById(R.id.contactRemove).setOnClickListener(v -> {
            additionalContactsContainer.removeView(row);
            additionalContacts.remove(row);
            renumberContacts();
        });
        additionalContactsContainer.addView(row);
        additionalContacts.add(row);
    }

    private void renumberContacts() {
        for (int i = 0; i < additionalContacts.size(); i++) {
            TextView header = additionalContacts.get(i).findViewById(R.id.contactHeader);
            header.setText(getString(R.string.additional_contact, i + 1));
        }
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
        // "Activate Later" hides the whole identity/address panel (design Register7-new4).
        findViewById(R.id.walletInfoPanel).setVisibility(now ? View.VISIBLE : View.GONE);
    }

    // ---- Wallet identity + residential address --------------------------

    private void setupWalletFields() {
        // Default to SA ID Number (matches the design mockup).
        walletIdTypeCode = "ID";
        TextView idTypeLabel = findViewById(R.id.walletIdTypeLabel);
        idTypeLabel.setText(ID_TYPE_LABELS[0]);
        idTypeLabel.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        // ID Type picker.
        findViewById(R.id.walletIdTypeField).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.wallet_id_type)
                        .setItems(ID_TYPE_LABELS, (d, which) -> {
                            walletIdTypeCode = ID_TYPE_CODES[which];
                            TextView label = findViewById(R.id.walletIdTypeLabel);
                            label.setText(ID_TYPE_LABELS[which]);
                            label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                            applyIdType();
                        })
                        .show());

        // Live SA-ID validation + auto DOB/gender derivation.
        ((EditText) findViewById(R.id.etWalletIdNumber)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) {
                if ("ID".equals(walletIdTypeCode)) {
                    validateSaId(e.toString().trim());
                }
            }
        });

        // Date pickers (guarded so SA-ID DOB stays read-only/auto).
        findViewById(R.id.walletDobField).setOnClickListener(v -> {
            if ("ID".equals(walletIdTypeCode)) return; // auto-filled from the ID number
            pickWalletDate(walletDobIso, iso -> {
                walletDobIso = iso;
                setFieldValue(R.id.walletDobLabel, readableDate(iso));
            });
        });
        findViewById(R.id.walletPassportExpField).setOnClickListener(v ->
                pickWalletDate(walletPassportExpIso, iso -> {
                    walletPassportExpIso = iso;
                    setFieldValue(R.id.walletPassportExpLabel, readableDate(iso));
                }));

        // Gender picker (guarded so SA-ID gender stays read-only/auto).
        findViewById(R.id.walletGenderField).setOnClickListener(v -> {
            if ("ID".equals(walletIdTypeCode)) return;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.wallet_gender)
                    .setItems(GENDER_VALUES, (d, which) -> {
                        walletGender = GENDER_VALUES[which];
                        setFieldValue(R.id.walletGenderLabel, walletGender);
                    })
                    .show();
        });

        // Province picker.
        findViewById(R.id.walletProvinceField).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.wallet_province)
                        .setItems(PROVINCE_LABELS, (d, which) -> {
                            walletProvinceCode = PROVINCE_CODES[which];
                            setFieldValue(R.id.walletProvinceLabel, PROVINCE_LABELS[which]);
                        })
                        .show());

        // Wallet-access toggles.
        findViewById(R.id.walletUssdTile).setOnClickListener(v -> {
            walletUssd = !walletUssd;
            renderAccessTile(R.id.walletUssdTile, R.id.walletUssdCheck, walletUssd);
        });
        findViewById(R.id.walletInternetTile).setOnClickListener(v -> {
            walletInternet = !walletInternet;
            renderAccessTile(R.id.walletInternetTile, R.id.walletInternetCheck, walletInternet);
        });
        findViewById(R.id.walletCrossBorderTile).setOnClickListener(v -> {
            walletCrossBorder = !walletCrossBorder;
            renderAccessTile(R.id.walletCrossBorderTile, R.id.walletCrossBorderCheck, walletCrossBorder);
        });

        // Apply the SA-ID default state (dims the derived DOB + gender fields).
        applyIdType();
    }

    /** Reconfigures the identity fields when the ID type changes. */
    private void applyIdType() {
        boolean saId = "ID".equals(walletIdTypeCode);
        boolean passport = "passport".equals(walletIdTypeCode);

        EditText idInput = findViewById(R.id.etWalletIdNumber);
        TextView idLabel = findViewById(R.id.walletIdNumberLabel);
        if (saId) {
            idLabel.setText(R.string.wallet_sa_id_number);
            idInput.setHint(R.string.wallet_hint_sa_id);
            idInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            idInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(13)});
        } else {
            idLabel.setText(passport ? R.string.wallet_passport_number : R.string.wallet_asylum_number);
            idInput.setHint(R.string.wallet_hint_document_number);
            idInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            idInput.setFilters(new android.text.InputFilter[0]);
        }

        findViewById(R.id.walletPassportGroup).setVisibility(passport ? View.VISIBLE : View.GONE);

        // DOB + gender are derived (read-only) for SA ID, manual otherwise.
        setAutoField(R.id.walletDobField);
        setAutoField(R.id.walletGenderField);

        if (saId) {
            validateSaId(idInput.getText().toString().trim());
        } else {
            // Leaving SA ID clears the derived values + validation UI.
            walletSaIdValid = false;
            findViewById(R.id.walletIdError).setVisibility(View.GONE);
            findViewById(R.id.walletIdHint).setVisibility(View.GONE);
            walletDobIso = null;
            walletGender = null;
            resetFieldValue(R.id.walletDobLabel, R.string.wallet_select_date);
            resetFieldValue(R.id.walletGenderLabel, R.string.wallet_select_gender);
        }
    }

    /** Validates a South African ID and, when valid, fills DOB + gender. */
    private void validateSaId(String id) {
        TextView error = findViewById(R.id.walletIdError);
        TextView hint = findViewById(R.id.walletIdHint);
        walletSaIdValid = false;
        walletDobIso = null;
        walletGender = null;

        if (id.isEmpty()) {
            error.setVisibility(View.GONE);
            hint.setVisibility(View.GONE);
            resetFieldValue(R.id.walletDobLabel, R.string.wallet_select_date);
            resetFieldValue(R.id.walletGenderLabel, R.string.wallet_select_gender);
            return;
        }

        if (id.length() != 13 || !id.matches("\\d{13}")) {
            showIdError(R.string.wallet_err_id_length);
            return;
        }
        String dobIso = saIdDob(id);
        if (dobIso == null) {
            showIdError(R.string.wallet_err_id_date);
            return;
        }
        if (!luhnValid(id)) {
            showIdError(R.string.wallet_err_id_checksum);
            return;
        }

        // Valid: derive DOB + gender.
        walletSaIdValid = true;
        walletDobIso = dobIso;
        int seq = Integer.parseInt(id.substring(6, 10));
        walletGender = seq >= 5000 ? "Male" : "Female";
        error.setVisibility(View.GONE);
        hint.setVisibility(View.VISIBLE);
        setFieldValue(R.id.walletDobLabel, readableDate(dobIso));
        setFieldValue(R.id.walletGenderLabel, walletGender);
    }

    private void showIdError(int msgRes) {
        TextView error = findViewById(R.id.walletIdError);
        error.setText(msgRes);
        error.setVisibility(View.VISIBLE);
        findViewById(R.id.walletIdHint).setVisibility(View.GONE);
        resetFieldValue(R.id.walletDobLabel, R.string.wallet_select_date);
        resetFieldValue(R.id.walletGenderLabel, R.string.wallet_select_gender);
    }

    /** Returns yyyy-MM-dd for a valid YYMMDD prefix, or null if the date is invalid. */
    @Nullable
    private String saIdDob(String id) {
        int yy = Integer.parseInt(id.substring(0, 2));
        int mm = Integer.parseInt(id.substring(2, 4));
        int dd = Integer.parseInt(id.substring(4, 6));
        if (mm < 1 || mm > 12 || dd < 1 || dd > 31) return null;
        int currentYy = Calendar.getInstance().get(Calendar.YEAR) % 100;
        int year = yy > currentYy ? 1900 + yy : 2000 + yy;
        Calendar c = Calendar.getInstance();
        c.clear();
        c.setLenient(false);
        c.set(year, mm - 1, dd);
        try {
            c.getTime(); // triggers validation (e.g. rejects 31 Feb)
        } catch (Exception e) {
            return null;
        }
        return walletIsoFmt.format(c.getTime());
    }

    private boolean luhnValid(String num) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = num.length() - 1; i >= 0; i--) {
            int d = num.charAt(i) - '0';
            if (doubleDigit) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    private interface WalletDatePicked { void onPicked(String iso); }

    private void pickWalletDate(@Nullable String currentIso, WalletDatePicked cb) {
        Calendar c = Calendar.getInstance();
        if (currentIso != null) {
            try {
                c.setTime(walletIsoFmt.parse(currentIso));
            } catch (Exception ignored) {
            }
        }
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.clear();
            picked.set(year, month, day);
            cb.onPicked(walletIsoFmt.format(picked.getTime()));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String readableDate(String iso) {
        try {
            return walletReadableFmt.format(walletIsoFmt.parse(iso));
        } catch (Exception e) {
            return iso;
        }
    }

    private void setFieldValue(int labelId, String value) {
        TextView label = findViewById(labelId);
        label.setText(value);
        label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
    }

    private void resetFieldValue(int labelId, int hintRes) {
        TextView label = findViewById(labelId);
        label.setText(hintRes);
        label.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
    }

    /** Dims a dropdown-style field when it is derived/read-only (SA-ID DOB + gender). */
    private void setAutoField(int fieldId) {
        boolean editable = !"ID".equals(walletIdTypeCode);
        View field = findViewById(fieldId);
        field.setClickable(editable);
        field.setAlpha(editable ? 1f : 0.6f);
    }

    private void renderAccessTile(int tileId, int checkId, boolean on) {
        findViewById(tileId).setBackgroundResource(
                on ? R.drawable.bg_wallet_access_on : R.drawable.bg_wallet_access_off);
        ((ImageView) findViewById(checkId)).setImageResource(
                on ? R.drawable.ic_checkbox_on : R.drawable.ic_checkbox_off);
    }

    private void buildBenefits() {
        LinearLayout container = findViewById(R.id.benefitsContainer);
        int[] benefits = {R.string.benefit_faster_payments, R.string.benefit_access_credit,
                R.string.benefit_insurance, R.string.benefit_rewards, R.string.benefit_growth_tools};
        for (int res : benefits) {
            LinearLayout rowView = new LinearLayout(this);
            rowView.setOrientation(LinearLayout.HORIZONTAL);
            rowView.setGravity(Gravity.CENTER_VERTICAL);
            rowView.setPadding(0, dp(6), 0, dp(6));

            ImageView check = new ImageView(this);
            check.setLayoutParams(new LinearLayout.LayoutParams(dp(18), dp(18)));
            check.setImageResource(R.drawable.ic_check_green);
            rowView.addView(check);

            RobotoTextView label = new RobotoTextView(this);
            label.setText(res);
            label.setTextSize(14);
            label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = dp(10);
            label.setLayoutParams(lp);
            rowView.addView(label);

            container.addView(rowView);
        }
    }

    // ---- Delivery days table --------------------------------------------

    private void buildDeliveryDayTable() {
        LinearLayout table = findViewById(R.id.deliveryDaysTable);
        for (int i = 0; i < DAYS.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), dp(4), dp(4), dp(4));

            CheckBox check = new CheckBox(this);
            check.setText(DAYS[i]);
            check.setTextColor(ContextCompat.getColor(this, R.color.purple_primary));
            check.setTextSize(13);
            check.setChecked(DAYS_DEFAULT_ON[i]);
            check.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));
            row.addView(check);

            EditText open = buildTimeInput(DAYS_DEFAULT_ON[i] ? "8" : "");
            EditText close = buildTimeInput(DAYS_DEFAULT_ON[i] ? "18" : "");
            row.addView(open);
            row.addView(close);

            table.addView(row);
            dayChecks.add(check);
            dayOpen.add(open);
            dayClose.add(close);
        }
    }

    private EditText buildTimeInput(String value) {
        EditText et = new EditText(this);
        et.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        et.setBackgroundResource(R.drawable.bg_login_input);
        et.setGravity(Gravity.CENTER);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        et.setTextSize(13);
        et.setPadding(dp(6), dp(8), dp(6), dp(8));
        ((LinearLayout.LayoutParams) et.getLayoutParams()).setMargins(dp(4), 0, dp(4), 0);
        et.setText(value);
        return et;
    }

    // ---- Documents -------------------------------------------------------

    private void buildDocumentRows() {
        docDefs.add(new DocDef("company_registration_certificate",
                getString(R.string.doc_company_reg_cert), null, true));
        docDefs.add(new DocDef("tax_clearance_certificate",
                getString(R.string.doc_tax_clearance), getString(R.string.doc_if_available), false));
        docDefs.add(new DocDef("vat_registration_certificate",
                getString(R.string.doc_vat_reg_cert), getString(R.string.doc_if_available), false));
        docDefs.add(new DocDef("proof_of_address",
                getString(R.string.doc_proof_address_reg),
                getString(R.string.doc_proof_address_reg_sub), true));
        docDefs.add(new DocDef("bank_confirmation_letter",
                getString(R.string.doc_bank_confirmation), null, true));
        docDefs.add(new DocDef("company_logo",
                getString(R.string.doc_company_logo), getString(R.string.doc_recommended), false));

        LinearLayout container = findViewById(R.id.documentContainer);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (DocDef def : docDefs) {
            View row = inflater.inflate(R.layout.item_document_row, container, false);
            TextView title = row.findViewById(R.id.docTitle);
            TextView sub = row.findViewById(R.id.docSubtitle);
            TextView upload = row.findViewById(R.id.docUploadButton);
            title.setText(def.required ? def.title + " *" : def.title);
            if (def.sub == null) {
                sub.setVisibility(View.GONE);
            } else {
                sub.setText(def.sub);
            }
            upload.setOnClickListener(v -> chooseImageSource(def.key, true));
            docSubtitleViews.put(def.key, sub);
            docButtons.put(def.key, upload);
            container.addView(row);
        }
    }

    // ---- Logo ------------------------------------------------------------

    private void setupLogoUpload() {
        findViewById(R.id.logoUploadBox).setOnClickListener(v ->
                chooseImageSource(LOGO_TARGET, false));
    }

    // ---- Image / file source chooser ------------------------------------

    private void setupPickers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    if (success && pendingCameraUri != null) {
                        onImagePicked(pendingCameraUri);
                    }
                });
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) onImagePicked(uri);
                });
        fileLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) onImagePicked(uri);
                });
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        launchCamera();
                    } else {
                        Toast.makeText(this, R.string.camera_permission_denied,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** allowFile = true for documents (PDF allowed); logo is image-only. */
    private void chooseImageSource(String target, boolean allowFile) {
        pendingTarget = target;
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.source_take_photo));
        labels.add(getString(R.string.source_choose_gallery));
        if (allowFile) {
            labels.add(getString(R.string.source_choose_file));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_image_source)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    if (which == 0) {
                        requestCamera();
                    } else if (which == 1) {
                        galleryLauncher.launch("image/*");
                    } else {
                        fileLauncher.launch("*/*");
                    }
                })
                .show();
    }

    private void requestCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File dir = new File(getCacheDir(), "captures");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File file = new File(dir, "capture_" + System.currentTimeMillis() + ".jpg");
            pendingCameraUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", file);
            cameraLauncher.launch(pendingCameraUri);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void onImagePicked(Uri uri) {
        if (LOGO_TARGET.equals(pendingTarget)) {
            showCropDialog(uri);
        } else {
            applyDocument(pendingTarget, uri);
        }
    }

    private void applyDocument(String key, Uri uri) {
        if (key == null) {
            return;
        }
        docUris.put(key, uri);
        TextView sub = docSubtitleViews.get(key);
        TextView btn = docButtons.get(key);
        String name = queryFileName(uri);
        if (sub != null && name != null) {
            sub.setVisibility(View.VISIBLE);
            sub.setText(name);
            sub.setTextColor(ContextCompat.getColor(this, R.color.success));
        }
        if (btn != null) {
            btn.setText(R.string.replace);
        }
    }

    // ---- Logo crop dialog ------------------------------------------------

    private void showCropDialog(Uri uri) {
        Bitmap bitmap = decodeBitmap(uri, 1500);
        if (bitmap == null) {
            Toast.makeText(this, R.string.err_required_documents, Toast.LENGTH_SHORT).show();
            return;
        }
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_crop_logo, null);
        LogoCropView cropView = view.findViewById(R.id.cropView);
        cropView.setBitmap(bitmap);

        android.widget.SeekBar scale = view.findViewById(R.id.cropScale);
        android.widget.SeekBar rotation = view.findViewById(R.id.cropRotation);
        scale.setProgress(50); // 0.5 + 50/100 = 1.0
        rotation.setProgress(0);
        scale.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override public void onProgressChanged(android.widget.SeekBar sb, int p, boolean u) {
                cropView.setUserScale(0.5f + p / 100f);
            }
        });
        rotation.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override public void onProgressChanged(android.widget.SeekBar sb, int p, boolean u) {
                cropView.setRotationDegrees(p);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();

        view.findViewById(R.id.cropClose).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.cropCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.cropUploadNew).setOnClickListener(v -> {
            dialog.dismiss();
            chooseImageSource(LOGO_TARGET, false);
        });
        view.findViewById(R.id.cropOk).setOnClickListener(v -> {
            Bitmap cropped = cropView.getCroppedBitmap();
            if (cropped != null) {
                logoBitmap = cropped;
                logoPreview.setImageBitmap(cropped);
                logoPreview.setVisibility(View.VISIBLE);
                logoPlaceholder.setVisibility(View.GONE);
            }
            dialog.dismiss();
        });
        dialog.show();
    }

    private abstract static class SimpleSeekBarListener
            implements android.widget.SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) { }
        @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) { }
    }

    @Nullable
    private Bitmap decodeBitmap(Uri uri, int maxDim) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            int largest = Math.max(bounds.outWidth, bounds.outHeight);
            while (largest / sample > maxDim) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, opts);
            }
        } catch (Exception e) {
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
                Toast.makeText(RegisterDistributorActivity.this, message, Toast.LENGTH_SHORT).show();
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
        registerMap = findViewById(R.id.registerMap);
        registerMap.onCreate(savedInstanceState);
        registerMap.getMapAsync(this);

        final NestedScrollView scroll = findViewById(R.id.locationScroll);
        registerMap.setOnTouchListener((v, ev) -> {
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
                if (data == null || data.product_categories == null) {
                    return;
                }
                for (ReferenceData.ProductCategory c : data.product_categories) {
                    if (c.parent_id == null) {
                        primaryCategories.add(c);
                    }
                }
            }

            @Override
            public void onError(String message) {
                // Categories are optional; registration can proceed without them.
            }
        });
    }

    // ---- Submit ----------------------------------------------------------

    private void submit() {
        Map<String, String> form = new TreeMap<>();
        form.put("company_legal_name", text(R.id.etCompanyLegalName));
        form.put("trading_name", text(R.id.etTradingName));
        form.put("company_registration_number", text(R.id.etCompanyReg));
        form.put("vat_number", text(R.id.etVat));
        form.put("company_email", text(R.id.etCompanyEmail));
        form.put("company_phone_number", fullPhone(ccpCompany, R.id.etCompanyPhone));

        form.put("first_name", text(R.id.etFirstName));
        form.put("last_name", text(R.id.etLastName));
        form.put("mobile_number", fullPhone(ccpMobile, R.id.etMobile));
        form.put("email", text(R.id.etEmail));
        form.put("password", text(R.id.etPassword));

        form.putAll(dropdownValues);
        form.put("account_holder_name", text(R.id.etAccountHolder));
        form.put("account_number", text(R.id.etAccountNumber));
        form.put("branch_code", text(R.id.etBranchCode));

        form.put("contact_persons", buildContactsJson());

        form.put("address", text(R.id.etAddressSearch));
        if (pickedLat != null && pickedLng != null) {
            form.put("latitude", String.valueOf(pickedLat));
            form.put("longitude", String.valueOf(pickedLng));
        }

        if (selectedCoverage != null) {
            form.put("distribution_coverage", selectedCoverage.toLowerCase(Locale.ROOT)); // local/regional/national
        }
        form.put("service_product_categories", joinLongs(selectedCategoryIds));
        form.put("preferred_delivery_days", buildDeliveryDays());

        // ---- Wallet ----
        form.put("payment_method", "wallet");
        form.put("wallet_activation", createWalletNow ? "now" : "later");
        if (createWalletNow) {
            form.put("wallet_id_type", walletIdTypeCode);
            form.put("wallet_id_number", text(R.id.etWalletIdNumber));
            if ("passport".equals(walletIdTypeCode)) {
                form.put("wallet_passport_country", text(R.id.etWalletPassportCountry));
                form.put("wallet_passport_exp_date", walletPassportExpIso);
            }
            form.put("wallet_dob", walletDobIso);
            form.put("wallet_gender", walletGender);
            form.put("wallet_street_address", text(R.id.etWalletStreet));
            form.put("wallet_suburb", text(R.id.etWalletSuburb));
            form.put("wallet_city", text(R.id.etWalletCity));
            form.put("wallet_province", walletProvinceCode);
            form.put("wallet_post_code", text(R.id.etWalletPostCode));
            form.put("wallet_ussd", walletUssd ? "1" : "0");
            form.put("wallet_internet", walletInternet ? "1" : "0");
            form.put("wallet_cross_border", walletCrossBorder ? "1" : "0");
        }

        List<Http.FilePart> files = new ArrayList<>();
        for (Map.Entry<String, Uri> e : docUris.entrySet()) {
            if ("company_logo".equals(e.getKey()) && logoBitmap != null) {
                continue; // the cropped logo from step 1 takes precedence
            }
            Http.FilePart part = readFilePart(e.getKey(), e.getValue());
            if (part != null) {
                files.add(part);
            }
        }
        if (logoBitmap != null) {
            files.add(logoFilePart());
        }

        btnContinue.setEnabled(false);
        btnContinue.setText(R.string.registering);
        ApiClient.get(this).registerDistributor(form, files,
                new ApiCallback<RegisterDistributorData>() {
                    @Override
                    public void onSuccess(RegisterDistributorData data) {
                        startActivity(new Intent(RegisterDistributorActivity.this,
                                RegisterSubmittedActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        btnContinue.setEnabled(true);
                        btnContinue.setText(R.string.register_btn);
                        Toast.makeText(RegisterDistributorActivity.this, message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String buildContactsJson() {
        JSONArray array = new JSONArray();
        addContactJson(array, text(R.id.etPrimaryName), text(R.id.etPrimaryPosition),
                fullPhone(ccpPrimary, R.id.etPrimaryMobile), text(R.id.etPrimaryEmail));
        for (View row : additionalContacts) {
            String name = editText(row, R.id.contactName);
            if (name.isEmpty()) {
                continue;
            }
            CountryCodePicker ccp = row.findViewById(R.id.contactCcp);
            String mobile = editText(row, R.id.contactMobile).isEmpty()
                    ? "" : ccp.getFullNumberWithPlus();
            addContactJson(array, name, editText(row, R.id.contactPosition),
                    mobile, editText(row, R.id.contactEmail));
        }
        return array.toString();
    }

    private void addContactJson(JSONArray array, String name, String position,
                                String mobile, String email) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("full_name", name);
            obj.put("position", position);
            obj.put("mobile_number", mobile);
            obj.put("email", email);
            array.put(obj);
        } catch (Exception ignored) {
        }
    }

    /**
     * Web payload format: "{dayNum}:{open}.00-{close}.00" entries joined by "/",
     * where dayNum is the ISO weekday (Mon=1 … Sun=7). Example for Mon–Fri 8–18:
     * "1:8.00-18.00/2:8.00-18.00/3:8.00-18.00/4:8.00-18.00/5:8.00-18.00".
     */
    private String buildDeliveryDays() {
        List<String> parts = new ArrayList<>();
        for (int num = 1; num <= 7; num++) {
            int idx = (num == 7) ? 0 : num; // DAYS is Sun-first (0=Sun … 6=Sat)
            if (!dayChecks.get(idx).isChecked()) {
                continue;
            }
            String open = dayOpen.get(idx).getText().toString().trim();
            String close = dayClose.get(idx).getText().toString().trim();
            if (open.isEmpty()) open = "8";
            if (close.isEmpty()) close = "18";
            parts.add(num + ":" + formatHour(open) + "-" + formatHour(close));
        }
        return TextUtils.join("/", parts);
    }

    /** Normalises a whole-hour input ("8") to the web format ("8.00"). */
    private String formatHour(String value) {
        return value.contains(".") ? value : value + ".00";
    }

    // ---- File helpers ----------------------------------------------------

    private Http.FilePart logoFilePart() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        logoBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        return new Http.FilePart("company_logo", "company_logo.jpg", "image/jpeg", out.toByteArray());
    }

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
        if (name == null) {
            name = uri.getLastPathSegment();
        }
        return name;
    }

    // ---- Small helpers ---------------------------------------------------

    private String text(int id) {
        TextView tv = findViewById(id);
        return tv.getText() == null ? "" : tv.getText().toString().trim();
    }

    private String editText(View root, int id) {
        TextView tv = root.findViewById(id);
        return tv == null || tv.getText() == null ? "" : tv.getText().toString().trim();
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

    @Override protected void onStart() { super.onStart(); if (registerMap != null) registerMap.onStart(); }
    @Override protected void onResume() { super.onResume(); if (registerMap != null) registerMap.onResume(); }
    @Override protected void onPause() { if (registerMap != null) registerMap.onPause(); super.onPause(); }
    @Override protected void onStop() { if (registerMap != null) registerMap.onStop(); super.onStop(); }
    @Override protected void onDestroy() { if (registerMap != null) registerMap.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (registerMap != null) registerMap.onLowMemory(); }

    @Override
    protected void onSaveInstanceState(@Nullable Bundle outState) {
        super.onSaveInstanceState(outState);
        if (registerMap != null && outState != null) {
            registerMap.onSaveInstanceState(outState);
        }
    }
}
