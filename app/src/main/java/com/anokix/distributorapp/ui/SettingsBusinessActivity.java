package com.anokix.distributorapp.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.Http;
import com.anokix.distributorapp.network.dto.AutocompleteData;
import com.anokix.distributorapp.network.dto.BusinessProfileData;
import com.anokix.distributorapp.network.dto.CoordinateData;
import com.anokix.distributorapp.network.dto.ReferenceData;
import com.anokix.distributorapp.network.dto.WalletData;
import com.anokix.distributorapp.ui.views.FlowLayout;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Settings → Business: a 7-step editor (Company · Business · Contacts · Location · Documents ·
 * Service · Wallet) pre-filled from GET api/distributor/business/profile. Every step has its own
 * Save Changes button that posts just that step's fields to api/distributor/business/profile/update
 * (multipart). Wallet is read-only and comes from GET api/common/wallet. Mirrors the public
 * Create Account flow.
 */
public class SettingsBusinessActivity extends BaseSecondaryActivity implements OnMapReadyCallback {

    private static final int STEP_COUNT = 7;
    private static final String[] STEP_KEYS = {
            "rstep_company", "rstep_business", "rstep_contacts", "rstep_location",
            "rstep_documents", "rstep_service", "rstep_wallet"};

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
    private static final String[] ACCOUNT_TYPES = {"Cheque / Current", "Savings", "Business", "Transmission"};
    private static final String[] ACCOUNT_TYPE_CODES = {"cheque", "savings", "business", "transmission"};

    private static final String[] DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    private static final String[] DOC_KEYS = {
            "company_registration_certificate", "tax_clearance_certificate",
            "vat_registration_certificate", "proof_of_address", "bank_confirmation_letter"};

    private ViewFlipper flipper;
    private LinearLayout stepperContainer;
    private final TextView[] stepCircles = new TextView[STEP_COUNT];
    private final TextView[] stepLabels = new TextView[STEP_COUNT];
    private int currentStep = 0;

    private final Map<String, String> dropdownValues = new LinkedHashMap<>();

    private String selectedCoverage;
    private final List<ReferenceData.ProductCategory> primaryCategories = new ArrayList<>();
    private final List<Long> selectedCategoryIds = new ArrayList<>();

    private final List<CheckBox> dayChecks = new ArrayList<>();
    private final List<EditText> dayOpen1 = new ArrayList<>();
    private final List<EditText> dayClose1 = new ArrayList<>();
    private final List<EditText> dayOpen2 = new ArrayList<>();
    private final List<EditText> dayClose2 = new ArrayList<>();

    private final List<View> additionalContacts = new ArrayList<>();
    private LinearLayout additionalContactsContainer;
    private com.hbb20.CountryCodePicker ccpCompany;
    private com.hbb20.CountryCodePicker ccpPrimary;

    private Uri logoUri;
    private final Map<String, Uri> docUris = new LinkedHashMap<>();
    private final Map<String, TextView> docSubtitles = new LinkedHashMap<>();

    private String pendingTarget;          // "__logo__" or a document key
    private ActivityResultLauncher<String> pickLauncher;

    private MapView businessMap;
    private GoogleMap googleMap;
    private Marker marker;
    private Double pickedLat, pickedLng;

    private final Handler autocompleteHandler = new Handler(Looper.getMainLooper());
    private Runnable autocompleteRunnable;
    private boolean suppressAddressWatcher;
    private android.widget.ListPopupWindow addressPopup;
    private final List<AutocompleteData.Prediction> predictions = new ArrayList<>();

    private WalletData wallet;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_settings_business;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.settings_business_title);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        businessMap = findViewById(R.id.businessMap);
        businessMap.onCreate(savedInstanceState);
        businessMap.getMapAsync(this);
        final NestedScrollView scroll = findViewById(R.id.locationScroll);
        businessMap.setOnTouchListener((v, ev) -> {
            int action = ev.getActionMasked();
            scroll.requestDisallowInterceptTouchEvent(
                    action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE);
            return false;
        });
    }

    @Override
    protected void bindViews() {
        flipper = findViewById(R.id.stepFlipper);
        stepperContainer = findViewById(R.id.stepperContainer);
        additionalContactsContainer = findViewById(R.id.additionalContactsContainer);

        ccpCompany = findViewById(R.id.ccpCompany);
        ccpPrimary = findViewById(R.id.ccpPrimary);
        ccpCompany.registerCarrierNumberEditText(findViewById(R.id.etCompanyPhone));
        ccpPrimary.registerCarrierNumberEditText(findViewById(R.id.etPrimaryMobile));

        buildStepper();
        buildDeliveryDayTable();
        buildDocumentRows();
        findViewById(R.id.walletRefresh).setOnClickListener(v -> loadWallet(true));
        loadWallet(false);
        setupDropdowns();
        setupCoverage();
        bindStepFooter(R.id.footerCompany, this::saveCompany);
        bindStepFooter(R.id.footerBusiness, this::saveBusiness);
        bindStepFooter(R.id.footerContacts, this::saveContacts);
        bindStepFooter(R.id.footerLocation, this::saveLocation);
        bindStepFooter(R.id.footerDocuments, this::saveDocuments);
        bindStepFooter(R.id.footerService, this::saveService);

        findViewById(R.id.btnAddContact).setOnClickListener(v -> addContactRow(null));
        findViewById(R.id.categoryField).setOnClickListener(v -> showCategoryDialog());
        findViewById(R.id.companyLogoBox).setOnClickListener(v -> pickImage("__logo__"));

        pickLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) onPicked(uri);
                });

        setupAddressAutocomplete();
        showStep(0);
    }

    @Override
    protected void loadData() {
        loadReference();
        ApiClient.get(this).getBusinessProfile(new ApiCallback<BusinessProfileData>() {
            @Override
            public void onSuccess(BusinessProfileData data) {
                // bindProfile calls Glide.with(this); skip if the activity is already gone.
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (data != null && data.profile != null) {
                    bindProfile(data.profile);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SettingsBusinessActivity.this,
                        message != null ? message : getString(R.string.biz_load_failed),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- Stepper ---------------------------------------------------------

    private void buildStepper() {
        for (int i = 0; i < STEP_COUNT; i++) {
            if (i > 0) {
                View connector = new View(this);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(18), dp(1));
                clp.setMargins(dp(6), 0, dp(6), 0);
                connector.setLayoutParams(clp);
                connector.setBackgroundColor(ContextCompat.getColor(this, R.color.border));
                stepperContainer.addView(connector);
            }
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            final int index = i;
            item.setOnClickListener(v -> showStep(index));

            RobotoBoldTextView circle = new RobotoBoldTextView(this);
            circle.setLayoutParams(new LinearLayout.LayoutParams(dp(26), dp(26)));
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

    private void showStep(int index) {
        currentStep = index;
        flipper.setDisplayedChild(index);
        for (int i = 0; i < STEP_COUNT; i++) {
            boolean active = i == index;
            stepCircles[i].setBackgroundResource(active
                    ? R.drawable.bg_step_circle_active : R.drawable.bg_step_circle_inactive);
            stepCircles[i].setTextColor(active ? 0xFFFFFFFF
                    : ContextCompat.getColor(this, R.color.text_secondary));
            stepLabels[i].setTextColor(ContextCompat.getColor(this,
                    active ? R.color.purple_primary : R.color.text_secondary));
            stepLabels[i].setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void bindStepFooter(int footerId, Runnable onSave) {
        View footer = findViewById(footerId);
        footer.findViewById(R.id.btnStepSave).setOnClickListener(v -> onSave.run());
        footer.findViewById(R.id.btnStepReset).setOnClickListener(v -> loadData());
    }

    // ---- Populate from profile ------------------------------------------

    private void bindProfile(BusinessProfileData.Profile p) {
        setText(R.id.etCompanyLegalName, p.company_legal_name);
        setText(R.id.etTradingName, p.trading_name);
        setText(R.id.etRegNumber, p.company_registration_number);
        setText(R.id.etVatNumber, p.vat_number);
        setText(R.id.etCompanyEmail, p.company_email);
        if (p.company_phone_number != null && !p.company_phone_number.trim().isEmpty()) {
            ccpCompany.setFullNumber(p.company_phone_number.trim());
        }
        if (p.company_logo_url != null && !p.company_logo_url.isEmpty()) {
            ImageView preview = findViewById(R.id.companyLogoPreview);
            preview.setImageTintList(null);
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            com.bumptech.glide.Glide.with(this).load(p.company_logo_url).into(preview);
        }

        selectDropdown(R.id.lblBusinessType, "business_type", p.business_type,
                BUSINESS_TYPES, BUSINESS_TYPE_CODES);
        selectYear(p.business_established_year);
        selectDropdown(R.id.lblEmployees, "employee_count", p.employee_count,
                EMPLOYEE_RANGES, EMPLOYEE_RANGE_CODES);
        selectDropdown(R.id.lblTurnover, "annual_turnover", p.annual_turnover,
                TURNOVER_RANGES, TURNOVER_RANGE_CODES);
        selectDropdown(R.id.lblBank, "bank_name", p.bank_name, BANKS, BANK_CODES);
        setText(R.id.etAccountHolder, p.account_holder_name);
        setText(R.id.etAccountNumber, p.account_number);
        selectDropdown(R.id.lblAccountType, "account_type", p.account_type,
                ACCOUNT_TYPES, ACCOUNT_TYPE_CODES);
        setText(R.id.etBranchCode, p.branch_code);

        bindPrimaryContact(p.contact_persons);

        setAddressText(p.address);
        pickedLat = p.latitude;
        pickedLng = p.longitude;
        if (googleMap != null && pickedLat != null && pickedLng != null) {
            moveMapTo(new LatLng(pickedLat, pickedLng), true);
        }

        bindExistingDocuments(p.documents);

        if (p.distribution_coverage != null) {
            applyCoverage(capitalize(p.distribution_coverage));
        }
        selectedCategoryIds.clear();
        if (p.primary_product_category_ids != null) {
            for (String part : p.primary_product_category_ids.split(",")) {
                part = part.trim();
                if (!part.isEmpty()) {
                    try { selectedCategoryIds.add(Long.parseLong(part)); } catch (Exception ignored) { }
                }
            }
        }
        renderCategoryChips();
        setText(R.id.etActiveClients, p.active_clients_count);
        setText(R.id.etFleetSize, p.delivery_fleet_size);
        applyDeliveryDays(p.preferred_delivery_days);
    }

    private void bindPrimaryContact(List<BusinessProfileData.Contact> contacts) {
        additionalContacts.clear();
        additionalContactsContainer.removeAllViews();
        if (contacts == null || contacts.isEmpty()) return;
        BusinessProfileData.Contact first = contacts.get(0);
        setText(R.id.etPrimaryName, first.full_name);
        setText(R.id.etPrimaryPosition, first.position);
        if (first.mobile_number != null && !first.mobile_number.trim().isEmpty()) {
            ccpPrimary.setFullNumber(first.mobile_number.trim());
        }
        setText(R.id.etPrimaryEmail, first.email_address);
        for (int i = 1; i < contacts.size(); i++) {
            addContactRow(contacts.get(i));
        }
    }

    // ---- Dropdowns -------------------------------------------------------

    private void setupDropdowns() {
        bindDropdown(R.id.fieldBusinessType, R.id.lblBusinessType, "business_type",
                R.string.type_of_business, BUSINESS_TYPES, BUSINESS_TYPE_CODES);
        String[] years = buildYears();
        bindDropdown(R.id.fieldYear, R.id.lblYear, "business_established_year",
                R.string.year_established, years, years);
        bindDropdown(R.id.fieldEmployees, R.id.lblEmployees, "employee_count",
                R.string.number_of_employees, EMPLOYEE_RANGES, EMPLOYEE_RANGE_CODES);
        bindDropdown(R.id.fieldTurnover, R.id.lblTurnover, "annual_turnover",
                R.string.annual_turnover, TURNOVER_RANGES, TURNOVER_RANGE_CODES);
        bindDropdown(R.id.fieldBank, R.id.lblBank, "bank_name",
                R.string.bank_name, BANKS, BANK_CODES);
        bindDropdown(R.id.fieldAccountType, R.id.lblAccountType, "account_type",
                R.string.account_type, ACCOUNT_TYPES, ACCOUNT_TYPE_CODES);
    }

    private void bindDropdown(int fieldId, int labelId, String key, int titleRes,
                              String[] options, String[] codes) {
        TextView label = findViewById(labelId);
        findViewById(fieldId).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(titleRes)
                        .setItems(options, (d, which) -> {
                            dropdownValues.put(key, codes[which]);
                            label.setText(options[which]);
                            label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                        })
                        .show());
    }

    private void selectDropdown(int labelId, String key, String code,
                                String[] options, String[] codes) {
        if (code == null || code.isEmpty()) return;
        for (int i = 0; i < codes.length; i++) {
            if (code.equalsIgnoreCase(codes[i])) {
                dropdownValues.put(key, codes[i]);
                TextView label = findViewById(labelId);
                label.setText(options[i]);
                label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                return;
            }
        }
    }

    private void selectYear(String year) {
        if (year == null || year.isEmpty()) return;
        dropdownValues.put("business_established_year", year);
        TextView label = findViewById(R.id.lblYear);
        label.setText(year);
        label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
    }

    private String[] buildYears() {
        int start = 2026, end = 1950;
        String[] years = new String[start - end + 1];
        for (int y = start; y >= end; y--) years[start - y] = String.valueOf(y);
        return years;
    }

    // ---- Coverage / categories ------------------------------------------

    private void setupCoverage() {
        findViewById(R.id.coverageLocal).setOnClickListener(v -> applyCoverage("Local"));
        findViewById(R.id.coverageRegional).setOnClickListener(v -> applyCoverage("Regional"));
        findViewById(R.id.coverageNational).setOnClickListener(v -> applyCoverage("National"));
    }

    private void applyCoverage(String value) {
        selectedCoverage = value;
        styleCoverage(R.id.coverageLocal, R.id.iconCoverageLocal, R.id.titleCoverageLocal, "Local".equals(value));
        styleCoverage(R.id.coverageRegional, R.id.iconCoverageRegional, R.id.titleCoverageRegional, "Regional".equals(value));
        styleCoverage(R.id.coverageNational, R.id.iconCoverageNational, R.id.titleCoverageNational, "National".equals(value));
    }

    private void styleCoverage(int cardId, int iconId, int titleId, boolean selected) {
        findViewById(cardId).setBackgroundResource(selected
                ? R.drawable.bg_trader_type_card_selected : R.drawable.bg_trader_type_card);
        int color = ContextCompat.getColor(this, selected ? R.color.purple_primary : R.color.text_secondary);
        ((ImageView) findViewById(iconId)).setColorFilter(color);
        ((TextView) findViewById(titleId)).setTextColor(ContextCompat.getColor(this,
                selected ? R.color.purple_primary : R.color.text_primary));
    }

    private void loadReference() {
        ApiClient.get(this).getReference(new ApiCallback<ReferenceData>() {
            @Override
            public void onSuccess(ReferenceData data) {
                primaryCategories.clear();
                if (data != null && data.product_categories != null) {
                    for (ReferenceData.ProductCategory c : data.product_categories) {
                        if (c.parent_id == null) primaryCategories.add(c);
                    }
                }
                renderCategoryChips();
            }

            @Override
            public void onError(String message) { }
        });
    }

    private void showCategoryDialog() {
        if (primaryCategories.isEmpty()) return;
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
        findViewById(R.id.categoryHint).setVisibility(
                selectedCategoryIds.isEmpty() ? View.VISIBLE : View.GONE);
        for (ReferenceData.ProductCategory cat : primaryCategories) {
            if (!selectedCategoryIds.contains(cat.id)) continue;
            View chip = LayoutInflater.from(this).inflate(R.layout.item_tag_chip, chips, false);
            ((TextView) chip.findViewById(R.id.tagText)).setText(cat.title);
            chip.findViewById(R.id.tagRemove).setOnClickListener(v -> {
                selectedCategoryIds.remove(cat.id);
                renderCategoryChips();
            });
            chips.addView(chip);
        }
    }

    // ---- Contacts --------------------------------------------------------

    private void addContactRow(@Nullable BusinessProfileData.Contact prefill) {
        View row = LayoutInflater.from(this).inflate(
                R.layout.item_register_contact, additionalContactsContainer, false);
        TextView header = row.findViewById(R.id.contactHeader);
        header.setText(getString(R.string.additional_contact, additionalContacts.size() + 1));
        com.hbb20.CountryCodePicker ccp = row.findViewById(R.id.contactCcp);
        ccp.registerCarrierNumberEditText(row.findViewById(R.id.contactMobile));
        if (prefill != null) {
            ((TextView) row.findViewById(R.id.contactName)).setText(safe(prefill.full_name));
            ((TextView) row.findViewById(R.id.contactPosition)).setText(safe(prefill.position));
            ((TextView) row.findViewById(R.id.contactEmail)).setText(safe(prefill.email_address));
            if (prefill.mobile_number != null && !prefill.mobile_number.isEmpty()) {
                ccp.setFullNumber(prefill.mobile_number);
            }
        }
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
            ((TextView) additionalContacts.get(i).findViewById(R.id.contactHeader))
                    .setText(getString(R.string.additional_contact, i + 1));
        }
    }

    private String buildContactsJson() {
        JSONArray array = new JSONArray();
        String primaryMobile = text(R.id.etPrimaryMobile).isEmpty()
                ? "" : ccpPrimary.getFullNumberWithPlus();
        addContactJson(array, text(R.id.etPrimaryName), text(R.id.etPrimaryPosition),
                primaryMobile, text(R.id.etPrimaryEmail));
        for (View row : additionalContacts) {
            String name = editText(row, R.id.contactName);
            if (name.isEmpty()) continue;
            com.hbb20.CountryCodePicker ccp = row.findViewById(R.id.contactCcp);
            String mobile = editText(row, R.id.contactMobile).isEmpty() ? "" : ccp.getFullNumberWithPlus();
            addContactJson(array, name, editText(row, R.id.contactPosition), mobile,
                    editText(row, R.id.contactEmail));
        }
        return array.toString();
    }

    private void addContactJson(JSONArray array, String name, String position, String mobile, String email) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("full_name", name);
            obj.put("position", position);
            obj.put("mobile_number", mobile);
            obj.put("email_address", email);
            array.put(obj);
        } catch (Exception ignored) { }
    }

    // ---- Delivery days ---------------------------------------------------

    private void buildDeliveryDayTable() {
        LinearLayout table = findViewById(R.id.deliveryDaysTable);
        for (int i = 0; i < DAYS.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), dp(4), dp(4), dp(4));

            CheckBox check = new CheckBox(this);
            check.setText(DAYS[i]);
            check.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            check.setTextSize(12);
            check.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));
            row.addView(check);

            EditText o1 = timeInput(), c1 = timeInput(), o2 = timeInput(), c2 = timeInput();
            row.addView(o1); row.addView(c1); row.addView(o2); row.addView(c2);

            table.addView(row);
            dayChecks.add(check);
            dayOpen1.add(o1); dayClose1.add(c1); dayOpen2.add(o2); dayClose2.add(c2);
        }
    }

    private EditText timeInput() {
        EditText et = new EditText(this);
        et.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        et.setBackgroundResource(R.drawable.bg_login_input);
        et.setGravity(Gravity.CENTER);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        et.setTextSize(12);
        et.setPadding(dp(4), dp(8), dp(4), dp(8));
        ((LinearLayout.LayoutParams) et.getLayoutParams()).setMargins(dp(3), 0, dp(3), 0);
        return et;
    }

    /** Parse "1:8.00-12,15-22/2:8.00-22,-" into the table (ISO Mon=1 … Sun=7). */
    private void applyDeliveryDays(String value) {
        for (int i = 0; i < DAYS.length; i++) {
            dayChecks.get(i).setChecked(false);
            dayOpen1.get(i).setText(""); dayClose1.get(i).setText("");
            dayOpen2.get(i).setText(""); dayClose2.get(i).setText("");
        }
        if (value == null || value.trim().isEmpty()) return;
        for (String part : value.split("/")) {
            String[] dayBody = part.split(":", 2);
            if (dayBody.length < 2) continue;
            int num;
            try { num = Integer.parseInt(dayBody[0].trim()); } catch (Exception e) { continue; }
            int idx = (num == 7) ? 0 : num;   // Sun-first table
            if (idx < 0 || idx >= DAYS.length) continue;
            dayChecks.get(idx).setChecked(true);
            String[] ranges = dayBody[1].split(",");
            applyRange(ranges.length > 0 ? ranges[0] : "", dayOpen1.get(idx), dayClose1.get(idx));
            applyRange(ranges.length > 1 ? ranges[1] : "", dayOpen2.get(idx), dayClose2.get(idx));
        }
    }

    private void applyRange(String range, EditText open, EditText close) {
        if (range == null) return;
        range = range.trim();
        if (range.isEmpty() || range.equals("-")) return;
        String[] oc = range.split("-");
        if (oc.length >= 1) open.setText(stripDecimal(oc[0].trim()));
        if (oc.length >= 2) close.setText(stripDecimal(oc[1].trim()));
    }

    private String buildDeliveryDays() {
        List<String> parts = new ArrayList<>();
        for (int num = 1; num <= 7; num++) {
            int idx = (num == 7) ? 0 : num;
            if (!dayChecks.get(idx).isChecked()) continue;
            String o1 = dayOpen1.get(idx).getText().toString().trim();
            String c1 = dayClose1.get(idx).getText().toString().trim();
            String o2 = dayOpen2.get(idx).getText().toString().trim();
            String c2 = dayClose2.get(idx).getText().toString().trim();
            StringBuilder body = new StringBuilder();
            if (!o1.isEmpty()) body.append(formatHour(o1)).append("-").append(formatHour(c1.isEmpty() ? "18" : c1));
            if (!o2.isEmpty()) {
                body.append(",").append(formatHour(o2)).append("-").append(formatHour(c2.isEmpty() ? "18" : c2));
            }
            if (body.length() == 0) body.append("8.00-18.00");
            parts.add(num + ":" + body);
        }
        return TextUtils.join("/", parts);
    }

    private String formatHour(String value) {
        return value.contains(".") ? value : value + ".00";
    }

    private String stripDecimal(String value) {
        return value.endsWith(".00") ? value.substring(0, value.length() - 3) : value;
    }

    // ---- Documents -------------------------------------------------------

    private void buildDocumentRows() {
        int[] titles = {R.string.doc_company_reg_cert, R.string.doc_tax_clearance,
                R.string.doc_vat_reg_cert, R.string.doc_proof_address_reg, R.string.doc_bank_confirmation};
        boolean[] required = {true, false, false, true, true};
        LinearLayout container = findViewById(R.id.documentContainer);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < DOC_KEYS.length; i++) {
            final String key = DOC_KEYS[i];
            View row = inflater.inflate(R.layout.item_document_row, container, false);
            TextView title = row.findViewById(R.id.docTitle);
            TextView sub = row.findViewById(R.id.docSubtitle);
            TextView upload = row.findViewById(R.id.docUploadButton);
            title.setText(required[i] ? getString(titles[i]) + " *" : getString(titles[i]));
            sub.setVisibility(View.GONE);
            upload.setText(R.string.upload);
            upload.setOnClickListener(v -> pickImage(key));
            docSubtitles.put(key, sub);
            container.addView(row);
        }
    }

    private void bindExistingDocuments(BusinessProfileData.Documents docs) {
        if (docs == null) return;
        showDoc("company_registration_certificate", docs.company_registration_certificate);
        showDoc("tax_clearance_certificate", docs.tax_clearance_certificate);
        showDoc("vat_registration_certificate", docs.vat_registration_certificate);
        showDoc("proof_of_address", docs.proof_of_address);
        showDoc("bank_confirmation_letter", docs.bank_confirmation_letter);
    }

    private void showDoc(String key, List<BusinessProfileData.DocFile> files) {
        if (files == null || files.isEmpty()) return;
        TextView sub = docSubtitles.get(key);
        if (sub != null) {
            sub.setVisibility(View.VISIBLE);
            sub.setText(safe(files.get(0).original_name));
            sub.setTextColor(ContextCompat.getColor(this, R.color.success));
        }
    }

    // ---- Image / file picking -------------------------------------------

    private void pickImage(String target) {
        pendingTarget = target;
        pickLauncher.launch("__logo__".equals(target) ? "image/*" : "*/*");
    }

    private void onPicked(Uri uri) {
        if ("__logo__".equals(pendingTarget)) {
            logoUri = uri;
            ImageView preview = findViewById(R.id.companyLogoPreview);
            preview.setImageTintList(null);          // drop the purple placeholder tint
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            preview.setImageURI(uri);
        } else if (pendingTarget != null) {
            docUris.put(pendingTarget, uri);
            TextView sub = docSubtitles.get(pendingTarget);
            String name = queryFileName(uri);
            if (sub != null) {
                sub.setVisibility(View.VISIBLE);
                sub.setText(name != null ? name : getString(R.string.replace));
                sub.setTextColor(ContextCompat.getColor(this, R.color.success));
            }
        }
    }

    // ---- Per-step save ---------------------------------------------------

    private void saveCompany() {
        if (text(R.id.etCompanyLegalName).isEmpty()) { toast(R.string.err_business_name); return; }
        Map<String, String> form = new TreeMap<>();
        form.put("company_legal_name", text(R.id.etCompanyLegalName));
        form.put("trading_name", text(R.id.etTradingName));
        form.put("company_registration_number", text(R.id.etRegNumber));
        form.put("vat_number", text(R.id.etVatNumber));
        form.put("company_email", text(R.id.etCompanyEmail));
        form.put("company_phone_number",
                text(R.id.etCompanyPhone).isEmpty() ? "" : ccpCompany.getFullNumberWithPlus());
        List<Http.FilePart> files = new ArrayList<>();
        if (logoUri != null) {
            Http.FilePart logo = readFilePart("company_logo", logoUri);
            if (logo != null) files.add(logo);
        }
        submitStep(R.id.footerCompany, form, files);
    }

    private void saveBusiness() {
        Map<String, String> form = new TreeMap<>();
        putDropdown(form, "business_type");
        putDropdown(form, "business_established_year");
        putDropdown(form, "employee_count");
        putDropdown(form, "annual_turnover");
        putDropdown(form, "bank_name");
        form.put("account_holder_name", text(R.id.etAccountHolder));
        form.put("account_number", text(R.id.etAccountNumber));
        putDropdown(form, "account_type");
        form.put("branch_code", text(R.id.etBranchCode));
        submitStep(R.id.footerBusiness, form, new ArrayList<>());
    }

    private void saveContacts() {
        if (text(R.id.etPrimaryName).isEmpty()) { toast(R.string.err_primary_contact_name); return; }
        Map<String, String> form = new TreeMap<>();
        form.put("contact_persons", buildContactsJson());
        submitStep(R.id.footerContacts, form, new ArrayList<>());
    }

    private void saveLocation() {
        if (pickedLat == null || pickedLng == null) { toast(R.string.err_location); return; }
        Map<String, String> form = new TreeMap<>();
        form.put("address", text(R.id.etAddressSearch));
        form.put("latitude", String.valueOf(pickedLat));
        form.put("longitude", String.valueOf(pickedLng));
        submitStep(R.id.footerLocation, form, new ArrayList<>());
    }

    private void saveDocuments() {
        List<Http.FilePart> files = new ArrayList<>();
        for (Map.Entry<String, Uri> e : docUris.entrySet()) {
            Http.FilePart part = readFilePart(e.getKey(), e.getValue());
            if (part != null) files.add(part);
        }
        if (files.isEmpty()) { toast(R.string.biz_step_saved); return; }
        submitStep(R.id.footerDocuments, new TreeMap<>(), files);
    }

    private void saveService() {
        Map<String, String> form = new TreeMap<>();
        if (selectedCoverage != null) {
            form.put("distribution_coverage", selectedCoverage.toLowerCase(Locale.ROOT));
        }
        form.put("primary_product_category_ids", joinLongs(selectedCategoryIds));
        form.put("active_clients_count", text(R.id.etActiveClients));
        form.put("delivery_fleet_size", text(R.id.etFleetSize));
        form.put("preferred_delivery_days", buildDeliveryDays());
        submitStep(R.id.footerService, form, new ArrayList<>());
    }

    private void putDropdown(Map<String, String> form, String key) {
        if (dropdownValues.containsKey(key)) form.put(key, dropdownValues.get(key));
    }

    private void submitStep(int footerId, Map<String, String> form, List<Http.FilePart> files) {
        View save = findViewById(footerId).findViewById(R.id.btnStepSave);
        save.setEnabled(false);
        ApiClient.get(this).updateBusinessProfile(form, files, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                save.setEnabled(true);
                Toast.makeText(SettingsBusinessActivity.this, R.string.biz_step_saved,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                save.setEnabled(true);
                Toast.makeText(SettingsBusinessActivity.this,
                        message != null ? message : getString(R.string.error_generic),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- Address autocomplete + map -------------------------------------

    private void setupAddressAutocomplete() {
        EditText field = findViewById(R.id.etAddressSearch);
        addressPopup = new android.widget.ListPopupWindow(this);
        addressPopup.setAnchorView(field);
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (suppressAddressWatcher) return;
                String keyword = s.toString().trim();
                if (autocompleteRunnable != null) autocompleteHandler.removeCallbacks(autocompleteRunnable);
                if (keyword.length() < 3) { addressPopup.dismiss(); return; }
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

            @Override public void onError(String message) { }
        });
    }

    private void showPredictions() {
        if (predictions.isEmpty()) { addressPopup.dismiss(); return; }
        List<String> labels = new ArrayList<>();
        for (AutocompleteData.Prediction p : predictions) labels.add(p.description);
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
                if (data == null) return;
                pickedLat = data.latitude;
                pickedLng = data.longitude;
                if (data.formatted_address != null && !data.formatted_address.isEmpty()) {
                    setAddressText(data.formatted_address);
                }
                moveMapTo(new LatLng(data.latitude, data.longitude), true);
            }

            @Override public void onError(String message) { }
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
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-29.8587, 31.0218), 9f));
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
                List<Address> results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (results != null && !results.isEmpty() && results.get(0).getMaxAddressLineIndex() >= 0) {
                    final String line = results.get(0).getAddressLine(0);
                    runOnUiThread(() -> setAddressText(line));
                }
            } catch (Exception ignored) { }
        }).start();
    }

    // ---- Wallet ----------------------------------------------------------

    /** GET api/common/wallet — balances, IMB account details and the recent ledger. */
    private void loadWallet(boolean spin) {
        if (spin) {
            View icon = findViewById(R.id.walletRefreshIcon);
            if (icon != null) icon.animate().rotationBy(360f).setDuration(600).start();
        }
        ApiClient.get(this).getWallet(new ApiCallback<WalletData>() {
            @Override
            public void onSuccess(WalletData data) {
                if (isFinishing() || data == null) return;
                wallet = data;
                bindWallet();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || wallet != null) return;   // keep what we already show
                setStatusPill(false, getString(R.string.biz_wallet_load_failed));
            }
        });
    }

    private void bindWallet() {
        boolean active = "active".equalsIgnoreCase(wallet.status);
        setStatusPill(active, active
                ? getString(R.string.biz_wallet_status_active)
                : getString(R.string.biz_wallet_status_inactive));

        TextView account = findViewById(R.id.walletAccountNumber);
        account.setText(TextUtils.isEmpty(wallet.account_number)
                ? getString(R.string.wallet_placeholder) : wallet.account_number);

        TextView activated = findViewById(R.id.walletActivatedAt);
        if (TextUtils.isEmpty(wallet.activated_at)) {
            activated.setVisibility(View.GONE);
        } else {
            activated.setVisibility(View.VISIBLE);
            activated.setText(getString(R.string.biz_wallet_activated, wallet.activated_at));
        }

        WalletData.Balance balance = wallet.balance;
        ((TextView) findViewById(R.id.walletAvailable))
                .setText(money(balance == null ? 0 : balance.available));
        ((TextView) findViewById(R.id.walletCurrent))
                .setText(money(balance == null ? 0 : balance.current));
        ((TextView) findViewById(R.id.walletPending))
                .setText(money(balance == null ? 0 : balance.pending));

        buildTransactions(balance == null ? null : balance.transactions);
    }

    private void setStatusPill(boolean active, String label) {
        TextView pill = findViewById(R.id.walletStatusPill);
        pill.setText(label);
        pill.setBackgroundResource(active ? R.drawable.bg_badge_success : R.drawable.bg_badge_warning);
        pill.setTextColor(ContextCompat.getColor(this, active ? R.color.success : R.color.warning));
        pill.setCompoundDrawablesRelativeWithIntrinsicBounds(
                active ? R.drawable.ic_check_badge_green : 0, 0, 0, 0);
    }

    private void buildTransactions(@Nullable List<WalletData.Transaction> transactions) {
        LinearLayout container = findViewById(R.id.walletTransactions);
        View empty = findViewById(R.id.walletTransactionsEmpty);
        container.removeAllViews();
        if (transactions == null || transactions.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            return;
        }
        empty.setVisibility(View.GONE);
        for (WalletData.Transaction tx : transactions) {
            container.addView(buildTransactionRow(tx));
        }
    }

    private View buildTransactionRow(WalletData.Transaction tx) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));

        RobotoTextView date = new RobotoTextView(this);
        date.setText(walletDate(tx.date));
        date.setTextSize(13);
        date.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        date.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(date);

        if (tx.pending) {
            RobotoTextView flag = new RobotoTextView(this);
            flag.setText(R.string.biz_wallet_pending_flag);
            flag.setTextSize(11);
            flag.setTextColor(ContextCompat.getColor(this, R.color.warning));
            flag.setBackgroundResource(R.drawable.bg_badge_warning);
            flag.setPadding(dp(6), dp(2), dp(6), dp(2));
            LinearLayout.LayoutParams flagLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            flagLp.rightMargin = dp(8);
            flag.setLayoutParams(flagLp);
            row.addView(flag);
        }

        double amount = parseAmount(tx.amount);
        RobotoBoldTextView value = new RobotoBoldTextView(this);
        value.setText(money(amount));
        value.setTextSize(13);
        value.setTextColor(ContextCompat.getColor(this,
                amount < 0 ? R.color.danger : R.color.text_primary));
        row.addView(value);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View divider = new View(this);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border_light));
        wrapper.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        return wrapper;
    }

    private double parseAmount(String amount) {
        try {
            return Double.parseDouble(amount);
        } catch (Exception ignored) {
            return 0d;
        }
    }

    /** ZA money: {@code R 40 168,50} — space thousands, comma decimals, sign kept. */
    private String money(double value) {
        String formatted = String.format(Locale.US, "%,.2f", Math.abs(value))
                .replace(',', ' ').replace('.', ',');
        return (value < 0 ? "R -" : "R ") + formatted;
    }

    /** ISO-8601 ledger timestamp → "15 Jul 2026, 15:58"; falls back to the raw value. */
    private String walletDate(String iso) {
        if (TextUtils.isEmpty(iso)) return "";
        try {
            java.text.SimpleDateFormat in =
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            java.text.SimpleDateFormat out =
                    new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);
            return out.format(in.parse(iso));
        } catch (Exception ignored) {
            return iso;
        }
    }

    // ---- File / small helpers -------------------------------------------

    @Nullable
    private Http.FilePart readFilePart(String field, Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
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
        } catch (Exception ignored) { }
        return name != null ? name : uri.getLastPathSegment();
    }

    private void setText(int id, String value) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(value == null ? "" : value);
    }

    private String text(int id) {
        TextView tv = findViewById(id);
        return tv == null || tv.getText() == null ? "" : tv.getText().toString().trim();
    }

    private String editText(View root, int id) {
        TextView tv = root.findViewById(id);
        return tv == null || tv.getText() == null ? "" : tv.getText().toString().trim();
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
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

    // ---- Map lifecycle ---------------------------------------------------

    @Override protected void onStart() { super.onStart(); if (businessMap != null) businessMap.onStart(); }
    @Override protected void onResume() { super.onResume(); if (businessMap != null) businessMap.onResume(); }
    @Override protected void onPause() { if (businessMap != null) businessMap.onPause(); super.onPause(); }
    @Override protected void onStop() { if (businessMap != null) businessMap.onStop(); super.onStop(); }
    @Override protected void onDestroy() { if (businessMap != null) businessMap.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (businessMap != null) businessMap.onLowMemory(); }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (businessMap != null) businessMap.onSaveInstanceState(outState);
    }
}
