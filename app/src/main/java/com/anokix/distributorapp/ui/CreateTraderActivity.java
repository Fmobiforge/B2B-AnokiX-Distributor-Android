package com.anokix.distributorapp.ui;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
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
import android.text.InputType;
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
import androidx.core.widget.NestedScrollView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.Http;
import com.anokix.distributorapp.network.dto.AutocompleteData;
import com.anokix.distributorapp.network.dto.BaseInfoData;
import com.anokix.distributorapp.network.dto.CoordinateData;
import com.anokix.distributorapp.network.dto.CreateTraderData;
import com.anokix.distributorapp.network.dto.ReferenceData;
import com.anokix.distributorapp.network.dto.TraderDetailData;
import com.anokix.distributorapp.network.dto.TradersData;
import com.bumptech.glide.Glide;
import com.anokix.distributorapp.ui.views.FlowLayout;
import com.anokix.distributorapp.ui.views.LogoCropView;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;
import com.google.gson.Gson;
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
 * Eight-step trader wizard, wired to /api/distributor/traders/create when adding and
 * /api/distributor/traders/update when editing an existing trader.
 *
 * Steps: 1 Owner · 2 Business · 3 Trader · 4 Location · 5 Verify ·
 *        6 Distributor · 7 Wallet · 8 Terms.
 *
 * All step views live inside a {@link ViewFlipper}, so every field keeps its
 * value when the user navigates back and forth. Lookup data (trader types,
 * product categories) comes from {@link ApiClient#getReference}; the distributor
 * the trader belongs to is this portal's own, read from
 * {@link ApiClient#getBaseInfo}.
 *
 * <p>Edit mode is launched with {@link #editIntent}: the Traders list already holds the
 * full trader record, so the wizard prefills from it rather than re-fetching.
 */
public class CreateTraderActivity extends AppCompatActivity implements OnMapReadyCallback {

    /** JSON-encoded {@link TradersData.Trader} to edit; absent when adding a new one. */
    private static final String EXTRA_TRADER = "extra_trader_json";

    private static final int STEP_COUNT = 8;
    private static final int STEP_TRADER = 2;     // 0-based
    private static final int STEP_LOCATION = 3;
    private static final int STEP_VERIFY = 4;
    private static final int STEP_DISTRIBUTOR = 5;
    private static final int STEP_TERMS = 7;

    private static final int MIN_PASSWORD = 8;

    private static final String[] STEP_KEYS = {
            "step_owner", "step_business", "step_trader", "step_location",
            "step_verify", "step_distributor", "step_wallet", "step_terms"
    };

    /**
     * Delivery days are stored by index with Sunday first ("1:8.00-18.00" is Monday),
     * so the row order here doubles as the value sent to the API.
     */
    private static final String[] DAY_LABELS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    private static final String[] PAYMENT_LABELS = {"Wallet", "EFT", "Cash", "Card"};
    private static final String[] PAYMENT_VALUES = {"wallet", "eft", "cash", "card"};

    private ViewFlipper flipper;
    private LinearLayout stepperContainer;
    private TextView btnContinue;
    private TextView btnBack;

    private final TextView[] stepCircles = new TextView[STEP_COUNT];
    private final TextView[] stepLabels = new TextView[STEP_COUNT];
    private int currentStep = 0;

    /** The trader being edited, or null when registering a new one. */
    @Nullable
    private TradersData.Trader editing;

    // Reference data
    private final List<ReferenceData.TraderType> traderTypes = new ArrayList<>();
    private final List<ReferenceData.ProductCategory> primaryCategories = new ArrayList<>();

    /** This portal's own distributor — the only one a trader can be registered under. */
    private String currentDistributorId = "";

    // Selections
    private String selectedTraderTypeSlug;
    private final List<View> traderTypeCards = new ArrayList<>();
    private final List<Long> selectedCategoryIds = new ArrayList<>();
    private final List<DeliveryDayRow> deliveryDayRows = new ArrayList<>();
    private String paymentMethodValue;
    private boolean createWalletNow = true;

    /** Cropped company logo, JPEG-encoded at submit time. */
    @Nullable
    private Bitmap logoBitmap;
    private ActivityResultLauncher<String> logoPicker;

    /** A day row on step 6: the tick plus its opening and closing time. */
    private static final class DeliveryDayRow {
        final int dayIndex;
        final CheckBox check;
        final TextView open;
        final TextView close;

        DeliveryDayRow(int dayIndex, CheckBox check, TextView open, TextView close) {
            this.dayIndex = dayIndex;
            this.check = check;
            this.open = open;
            this.close = close;
        }

        boolean isSelected() {
            return check.isChecked();
        }

        String openText() {
            return open.getText() == null ? "" : open.getText().toString().trim();
        }

        String closeText() {
            return close.getText() == null ? "" : close.getText().toString().trim();
        }
    }

    // Phone country pickers (hbb20 CCP)
    private CountryCodePicker ccpOwner;
    private CountryCodePicker ccpBusiness;

    // Documents
    private static final class DocDef {
        /** Multipart field name used when uploading. */
        final String key;
        /**
         * Key this document comes back under in the trader payload. The API reads
         * "business_registration_document" but writes "registration_document", so the
         * two names have to be tracked separately.
         */
        final String responseKey;
        final String title, sub;
        final boolean required;

        DocDef(String key, String responseKey, String title, String sub, boolean required) {
            this.key = key;
            this.responseKey = responseKey;
            this.title = title;
            this.sub = sub;
            this.required = required;
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

    /** Opens the wizard on an existing trader; the list already holds the full record. */
    public static Intent editIntent(Context context, TradersData.Trader trader) {
        Intent intent = new Intent(context, CreateTraderActivity.class);
        intent.putExtra(EXTRA_TRADER, new Gson().toJson(trader));
        return intent;
    }

    private boolean isEditing() {
        return editing != null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_trader);

        editing = readTraderExtra();

        flipper = findViewById(R.id.stepFlipper);
        stepperContainer = findViewById(R.id.stepperContainer);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);

        buildStepper();
        buildDeliveryDayRows();
        buildBenefits();
        buildDocumentRows();
        setupCountryPickers();
        setupSelectionFields();
        setupPasswordFields();
        setupLogoUpload();
        setupWalletOptions();
        setupNavigation();
        setupDocumentPicker();
        setupAddressAutocomplete();
        setupMap(savedInstanceState);

        applyMode();
        showStep(0);
        loadReference();
        loadCurrentDistributor();
        loadTraderDetail();
    }

    @Nullable
    private TradersData.Trader readTraderExtra() {
        String json = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_TRADER);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return new Gson().fromJson(json, TradersData.Trader.class);
        } catch (Exception e) {
            return null; // Malformed extra: fall back to the Add flow rather than crashing.
        }
    }

    /** Retitles the screen and swaps the password copy between Add and Edit. */
    private void applyMode() {
        boolean editMode = isEditing();
        ((TextView) findViewById(R.id.screenTitle)).setText(editMode
                ? getString(R.string.edit_trader_title) : getString(R.string.add_new_trader));
        ((TextView) findViewById(R.id.screenSubtitle)).setText(editMode
                ? getString(R.string.edit_trader_subtitle) : getString(R.string.create_trader_subtitle));
        ((TextView) findViewById(R.id.signInBoxMessage)).setText(editMode
                ? R.string.account_sign_in_help_edit : R.string.account_sign_in_help_create);
        ((TextView) findViewById(R.id.passwordHelp)).setText(editMode
                ? R.string.password_help_edit : R.string.password_help_create);
        ((EditText) findViewById(R.id.etPassword)).setHint(editMode
                ? R.string.password_hint_edit : R.string.password_hint_create);
        ((EditText) findViewById(R.id.etConfirmPassword)).setHint(editMode
                ? R.string.confirm_password_hint_edit : R.string.confirm_password_hint_create);
        ((TextView) findViewById(R.id.logoUploadLabel)).setText(editMode
                ? R.string.change_logo : R.string.upload_logo);

        if (editMode) {
            prefillFromTrader(editing);
        }
    }

    // ---- Prefill (edit mode) ---------------------------------------------

    /**
     * Paints the record onto every step. Trader type and categories need the reference
     * lists, so those are re-applied in {@link #loadReference()} once they arrive.
     */
    private void prefillFromTrader(TradersData.Trader trader) {
        setText(R.id.etFirstName, trader.ownerFirstName());
        setText(R.id.etLastName, trader.ownerLastName());
        setText(R.id.etIdNumber, trader.id_number);
        setText(R.id.etPassportNumber, trader.passport_number);
        setPhone(ccpOwner, R.id.etOwnerMobile, trader.mobile_number);
        setText(R.id.etOwnerEmail, trader.email);

        setText(R.id.etBusinessName, trader.business_name);
        setText(R.id.etTradingName, trader.trading_name);
        setText(R.id.etBusinessReg, trader.business_registration_number);
        setPhone(ccpBusiness, R.id.etBusinessPhone, trader.business_phone_number);
        setText(R.id.etBusinessEmail, trader.business_email);

        selectedTraderTypeSlug = trader.traderTypeValue();
        selectedCategoryIds.clear();
        selectedCategoryIds.addAll(trader.categoryIds());

        setText(R.id.etAddressSearch, trader.address);
        if (trader.latitude != null && trader.longitude != null) {
            pickedLat = trader.latitude;
            pickedLng = trader.longitude;
        }

        applyDeliveryDays(TradersData.parseDeliveryDays(trader.preferred_delivery_days));
        applyPaymentMethod(trader.payment_method);

        renderStoredDocuments(trader);
        loadExistingLogo(trader.company_logo_url);

        // An approved trader already accepted the terms; keep them ticked so an edit
        // does not force the distributor to re-consent on the owner's behalf.
        for (int id : new int[]{R.id.cbAccurate, R.id.cbTos, R.id.cbKyc, R.id.cbComms}) {
            ((CheckBox) findViewById(id)).setChecked(true);
        }
    }

    /**
     * Pulls the full record for the trader being edited. The list row has no owner
     * fields, so step 1 stays blank until this lands and fills in the linked user
     * account's name, email and mobile number.
     */
    private void loadTraderDetail() {
        if (!isEditing() || isBlank(editing.id)) {
            return;
        }
        ApiClient.get(this).getTraderById(editing.id, new ApiCallback<TraderDetailData>() {
            @Override
            public void onSuccess(TraderDetailData data) {
                if (data == null) {
                    return;
                }
                if (data.trader != null) {
                    // Detail is the fuller copy, but it lands after the screen is already
                    // interactive — only fill what the list row could not, so a fast
                    // typist's input is never overwritten.
                    if (isBlank(data.trader.id)) {
                        data.trader.id = editing.id;
                    }
                    editing = data.trader;
                    fillGapsFromTrader(editing);
                }
                prefillOwner(data.user);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CreateTraderActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Re-applies the parts of the record the list row could not supply. Text fields are
     * only filled when still empty; the rest (selections, schedule, documents, logo)
     * is repainted because the row carried no version of it to overwrite.
     */
    private void fillGapsFromTrader(TradersData.Trader trader) {
        setTextIfEmpty(R.id.etIdNumber, trader.id_number);
        setTextIfEmpty(R.id.etPassportNumber, trader.passport_number);
        setTextIfEmpty(R.id.etBusinessName, trader.business_name);
        setTextIfEmpty(R.id.etTradingName, trader.trading_name);
        setTextIfEmpty(R.id.etBusinessReg, trader.business_registration_number);
        setTextIfEmpty(R.id.etBusinessEmail, trader.business_email);
        setTextIfEmpty(R.id.etAddressSearch, trader.address);
        if (text(R.id.etBusinessPhone).isEmpty()) {
            setPhone(ccpBusiness, R.id.etBusinessPhone, trader.business_phone_number);
        }

        if (pickedLat == null || pickedLng == null) {
            if (trader.latitude != null && trader.longitude != null) {
                pickedLat = trader.latitude;
                pickedLng = trader.longitude;
                moveMapTo(new LatLng(pickedLat, pickedLng), true);
            }
        }

        selectedTraderTypeSlug = trader.traderTypeValue();
        normaliseTraderTypeSelection();
        applyTraderTypeSelection();

        selectedCategoryIds.clear();
        selectedCategoryIds.addAll(trader.categoryIds());
        renderCategoryChips();

        applyDeliveryDays(TradersData.parseDeliveryDays(trader.preferred_delivery_days));
        applyPaymentMethod(trader.payment_method);
        renderStoredDocuments(trader);
        loadExistingLogo(trader.company_logo_url);
    }

    /** Owner Information (step 1) comes from the linked user account, not the trader row. */
    private void prefillOwner(TraderDetailData.User user) {
        if (user == null) {
            return;
        }
        setTextIfEmpty(R.id.etFirstName, user.first_name);
        setTextIfEmpty(R.id.etLastName, user.last_name);
        setTextIfEmpty(R.id.etOwnerEmail, user.email);
        if (text(R.id.etOwnerMobile).isEmpty()) {
            setPhone(ccpOwner, R.id.etOwnerMobile, user.phone_number);
        }
    }

    /** Documents already uploaded are listed by name; only replacements are re-sent. */
    private void renderStoredDocuments(TradersData.Trader trader) {
        for (DocDef def : docDefs) {
            // Skip anything the user has already replaced in this session.
            if (docUris.containsKey(def.key)) {
                continue;
            }
            String name = trader.documentName(def.responseKey);
            TextView sub = docSubtitleViews.get(def.key);
            TextView button = docButtons.get(def.key);
            if (name.isEmpty() || sub == null) {
                continue;
            }
            sub.setText(getString(R.string.doc_on_file, name));
            sub.setTextColor(ContextCompat.getColor(this, R.color.success));
            if (button != null) {
                button.setText(R.string.replace);
            }
        }
    }

    /** Shows the logo already on file so an edit does not look like it has none. */
    private void loadExistingLogo(String url) {
        if (isBlank(url) || logoBitmap != null) {
            return; // A freshly cropped logo always wins.
        }
        ImageView preview = findViewById(R.id.logoPreview);
        preview.setVisibility(View.VISIBLE);
        findViewById(R.id.logoPlaceholder).setVisibility(View.GONE);
        Glide.with(this).load(url).centerCrop().into(preview);
    }

    /** Ticks the saved days and fills their opening hours ("1:8.00-18.00"). */
    private void applyDeliveryDays(Map<Integer, String> saved) {
        for (DeliveryDayRow row : deliveryDayRows) {
            String hours = saved.get(row.dayIndex);
            row.check.setChecked(hours != null);
            if (hours == null) {
                continue;
            }
            int dash = hours.indexOf('-');
            if (dash > 0) {
                row.open.setText(hours.substring(0, dash).trim());
                row.close.setText(hours.substring(dash + 1).trim());
            }
        }
    }

    private void applyPaymentMethod(String value) {
        for (int i = 0; i < PAYMENT_VALUES.length; i++) {
            if (PAYMENT_VALUES[i].equalsIgnoreCase(value == null ? "" : value.trim())) {
                paymentMethodValue = PAYMENT_VALUES[i];
                TextView label = findViewById(R.id.paymentMethodLabel);
                label.setText(PAYMENT_LABELS[i]);
                label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                return;
            }
        }
    }

    private void setText(int id, String value) {
        if (!isBlank(value)) {
            ((TextView) findViewById(id)).setText(value);
        }
    }

    /** Used by the late-arriving detail response so it never overwrites typed input. */
    private void setTextIfEmpty(int id, String value) {
        if (text(id).isEmpty()) {
            setText(id, value);
        }
    }

    /**
     * Splits a stored MSISDN ("27821234567") back into the country picker and the
     * national part so the field reads the way the user typed it.
     */
    private void setPhone(CountryCodePicker ccp, int fieldId, String stored) {
        if (isBlank(stored)) {
            return;
        }
        String digits = stored.trim();
        if (!digits.startsWith("+")) {
            digits = "+" + digits;
        }
        // Ask the picker to split it; when it cannot, show the raw number as typed.
        try {
            ccp.setFullNumber(digits);
        } catch (Exception e) {
            ((TextView) findViewById(fieldId)).setText(stored);
        }
    }

    /**
     * Resolves the distributor this portal signs in as. Traders created here always
     * belong to it, so step 6 shows it as a fixed chip instead of a picker.
     */
    private void loadCurrentDistributor() {
        ApiClient.get(this).getBaseInfo(new ApiCallback<BaseInfoData>() {
            @Override
            public void onSuccess(BaseInfoData data) {
                BaseInfoData.PortalInfo portal = data == null ? null : data.portal_info;
                if (portal == null) {
                    return;
                }
                if (!isBlank(portal.id)) {
                    currentDistributorId = portal.id;
                }
                // Registering a trader is a legal relationship, so the chip names the
                // distributor's registered entity rather than its trading name.
                String label = !isBlank(portal.company_legal_name)
                        ? portal.company_legal_name : portal.trading_name;
                TextView chip = findViewById(R.id.distributorChip);
                chip.setText(isBlank(label) ? "" : label);
                chip.setVisibility(isBlank(label) ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                // Non-fatal: the trader keeps whatever distributor it already had.
            }
        });
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
        btnContinue.setText(index == STEP_TERMS ? submitLabel() : R.string.continue_btn);
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
                return validatePassword();
            case 1:
                if (text(R.id.etBusinessName).isEmpty()) return fail(R.string.err_business_name);
                return true;
            case STEP_TRADER:
                if (isBlank(selectedTraderTypeSlug)) return fail(R.string.err_trader_type);
                return true;
            case STEP_LOCATION:
                if (pickedLat == null || pickedLng == null) return fail(R.string.err_location);
                return true;
            case STEP_VERIFY:
                for (DocDef d : docDefs) {
                    // When editing, a document already on file satisfies the requirement —
                    // only a replacement is re-uploaded.
                    if (d.required && !docUris.containsKey(d.key) && !hasStoredDocument(d)) {
                        return fail(R.string.err_documents);
                    }
                }
                return true;
            case STEP_DISTRIBUTOR:
                return validateDeliveryDays();
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

    /**
     * A new trader needs a password to sign in with. Editing leaves it blank to keep the
     * current one, so the rules only apply once something has been typed.
     */
    private boolean validatePassword() {
        String password = rawText(R.id.etPassword);
        String confirm = rawText(R.id.etConfirmPassword);
        if (isEditing() && password.isEmpty() && confirm.isEmpty()) {
            return true;
        }
        if (password.length() < MIN_PASSWORD) {
            return fail(R.string.err_password_length);
        }
        if (!password.equals(confirm)) {
            return fail(R.string.err_password_match);
        }
        return true;
    }

    /** Every ticked delivery day needs both an opening and a closing time. */
    private boolean validateDeliveryDays() {
        for (DeliveryDayRow row : deliveryDayRows) {
            if (row.isSelected() && (row.openText().isEmpty() || row.closeText().isEmpty())) {
                return fail(R.string.err_delivery_hours);
            }
        }
        return true;
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
        findViewById(R.id.paymentMethodField).setOnClickListener(v -> showPaymentDialog());
    }

    // ---- Password --------------------------------------------------------

    private void setupPasswordFields() {
        bindPasswordToggle(findViewById(R.id.etPassword), findViewById(R.id.togglePassword));
        bindPasswordToggle(findViewById(R.id.etConfirmPassword),
                findViewById(R.id.toggleConfirmPassword));
    }

    /** Eye button that flips the field between dots and plain text. */
    private void bindPasswordToggle(EditText field, ImageButton toggle) {
        toggle.setOnClickListener(v -> {
            boolean hidden = field.getTransformationMethod() instanceof PasswordTransformationMethod;
            field.setInputType(InputType.TYPE_CLASS_TEXT | (hidden
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            toggle.setImageResource(hidden ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            field.setSelection(field.getText() == null ? 0 : field.getText().length());
        });
    }

    // ---- Company logo ----------------------------------------------------

    private void setupLogoUpload() {
        logoPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                showCropDialog(uri);
            }
        });
        findViewById(R.id.logoUploadBox).setOnClickListener(v -> logoPicker.launch("image/*"));
    }

    /** Scale / rotate / crop the picked image before it becomes company_logo. */
    private void showCropDialog(Uri uri) {
        Bitmap bitmap = decodeBitmap(uri, 1500);
        if (bitmap == null) {
            Toast.makeText(this, R.string.err_documents, Toast.LENGTH_SHORT).show();
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
            logoPicker.launch("image/*");
        });
        view.findViewById(R.id.cropOk).setOnClickListener(v -> {
            Bitmap cropped = cropView.getCroppedBitmap();
            if (cropped != null) {
                logoBitmap = cropped;
                ImageView preview = findViewById(R.id.logoPreview);
                preview.setImageBitmap(cropped);
                preview.setVisibility(View.VISIBLE);
                findViewById(R.id.logoPlaceholder).setVisibility(View.GONE);
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

    /** Decodes a picked image, subsampled so a full-resolution photo cannot blow up memory. */
    @Nullable
    private Bitmap decodeBitmap(Uri uri, int maxDim) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int longest = Math.max(bounds.outWidth, bounds.outHeight);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, longest / Math.max(1, maxDim));
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, options);
            }
        } catch (Exception e) {
            return null;
        }
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

    /**
     * One row per day (16.Trader-Distributor.png): tick the day, then set its opening
     * and closing time. Ticking a fresh day seeds the standard 08:00–18:00 window so the
     * common case needs a single tap.
     */
    private void buildDeliveryDayRows() {
        LinearLayout container = findViewById(R.id.deliveryDaysContainer);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int day = 0; day < DAY_LABELS.length; day++) {
            View row = inflater.inflate(R.layout.item_delivery_day, container, false);
            CheckBox check = row.findViewById(R.id.dayCheck);
            TextView label = row.findViewById(R.id.dayLabel);
            TextView open = row.findViewById(R.id.dayOpen);
            TextView close = row.findViewById(R.id.dayClose);
            label.setText(DAY_LABELS[day]);

            DeliveryDayRow dayRow = new DeliveryDayRow(day, check, open, close);
            check.setOnCheckedChangeListener((button, checked) -> {
                if (checked && dayRow.openText().isEmpty() && dayRow.closeText().isEmpty()) {
                    open.setText("8.00");
                    close.setText("18.00");
                }
                applyDayRowEnabled(dayRow);
            });
            label.setOnClickListener(v -> check.setChecked(!check.isChecked()));
            open.setOnClickListener(v -> pickTime(open));
            close.setOnClickListener(v -> pickTime(close));

            applyDayRowEnabled(dayRow);
            container.addView(row);
            deliveryDayRows.add(dayRow);
        }
    }

    /** Times only make sense on a day that is actually selected. */
    private void applyDayRowEnabled(DeliveryDayRow row) {
        boolean enabled = row.isSelected();
        row.open.setEnabled(enabled);
        row.close.setEnabled(enabled);
        row.open.setAlpha(enabled ? 1f : 0.45f);
        row.close.setAlpha(enabled ? 1f : 0.45f);
    }

    /** Times round-trip as "8.00" / "18.30", which is the format the API stores. */
    private void pickTime(TextView target) {
        String current = target.getText() == null ? "" : target.getText().toString().trim();
        int hour = 8;
        int minute = 0;
        int dot = current.indexOf('.');
        try {
            if (dot > 0) {
                hour = Integer.parseInt(current.substring(0, dot));
                minute = Integer.parseInt(current.substring(dot + 1));
            } else if (!current.isEmpty()) {
                hour = Integer.parseInt(current);
            }
        } catch (NumberFormatException ignored) {
            // Unparsable value — open the picker on the default 08:00.
        }
        new TimePickerDialog(this, (view, pickedHour, pickedMinute) ->
                target.setText(String.format(Locale.US, "%d.%02d", pickedHour, pickedMinute)),
                hour, minute, true).show();
    }

    /** "1:8.00-18.00/3:8.00-18.00" — day index first, Sunday is 0. */
    private String deliveryDaysParam() {
        StringBuilder value = new StringBuilder();
        for (DeliveryDayRow row : deliveryDayRows) {
            if (!row.isSelected() || row.openText().isEmpty() || row.closeText().isEmpty()) {
                continue;
            }
            if (value.length() > 0) {
                value.append('/');
            }
            value.append(row.dayIndex).append(':')
                    .append(row.openText()).append('-').append(row.closeText());
        }
        return value.toString();
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
        docDefs.add(new DocDef("business_registration_document", "registration_document",
                getString(R.string.doc_business_reg), getString(R.string.doc_business_reg_sub), true));
        docDefs.add(new DocDef("owner_id_document", "id_document",
                getString(R.string.doc_owner_id), getString(R.string.doc_owner_id_sub), true));
        docDefs.add(new DocDef("proof_of_address", "proof_of_address",
                getString(R.string.doc_proof_address), getString(R.string.doc_proof_address_sub), true));
        docDefs.add(new DocDef("store_front_photo", "store_front_photo",
                getString(R.string.doc_store_front), getString(R.string.doc_store_front_sub), true));
        docDefs.add(new DocDef("store_interior_photo", "store_interior_photo",
                getString(R.string.doc_store_interior), getString(R.string.doc_store_interior_sub), true));
        docDefs.add(new DocDef("additional_documents", "additional_documents",
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
                if (data.product_categories != null) {
                    for (ReferenceData.ProductCategory c : data.product_categories) {
                        if (c.parent_id == null) {
                            primaryCategories.add(c);
                        }
                    }
                }
                // The saved trader type and categories only become selectable now that
                // the lookup lists have arrived.
                normaliseTraderTypeSelection();
                renderTraderTypes();
                renderCategoryChips();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CreateTraderActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * The API stores trader_type as a numeric id on some records and as a slug on others.
     * Resolve whichever came back to the slug the cards are keyed by.
     */
    private void normaliseTraderTypeSelection() {
        if (isBlank(selectedTraderTypeSlug)) {
            return;
        }
        String saved = selectedTraderTypeSlug.trim();
        for (ReferenceData.TraderType type : traderTypes) {
            if (saved.equalsIgnoreCase(type.slug) || saved.equals(String.valueOf(type.id))) {
                selectedTraderTypeSlug = type.slug;
                return;
            }
        }
    }

    // ---- Submit ----------------------------------------------------------

    private void submit() {
        Map<String, String> form = new TreeMap<>();
        if (isEditing()) {
            form.put("id", editing.id == null ? "" : editing.id);
        }

        form.put("first_name", text(R.id.etFirstName));
        form.put("last_name", text(R.id.etLastName));
        form.put("id_number", text(R.id.etIdNumber));
        form.put("passport_number", text(R.id.etPassportNumber));
        form.put("mobile_number", fullPhone(ccpOwner, R.id.etOwnerMobile));
        form.put("email", text(R.id.etOwnerEmail));

        form.put("business_name", text(R.id.etBusinessName));
        form.put("trading_name", text(R.id.etTradingName));
        form.put("business_registration_number", text(R.id.etBusinessReg));
        form.put("business_phone_number", fullPhone(ccpBusiness, R.id.etBusinessPhone));
        form.put("business_email", text(R.id.etBusinessEmail));

        form.put("trader_type", isBlank(selectedTraderTypeSlug) ? "" : selectedTraderTypeSlug);
        form.put("primary_product_category_ids", joinLongs(selectedCategoryIds));

        form.put("address", text(R.id.etAddressSearch));
        if (pickedLat != null && pickedLng != null) {
            form.put("latitude", String.valueOf(pickedLat));
            form.put("longitude", String.valueOf(pickedLng));
        }

        // This is the distributor portal: the trader belongs to us and nobody else.
        String distributorId = resolveDistributorId();
        if (!distributorId.isEmpty()) {
            form.put("preferred_distributor_ids", distributorId);
        }
        form.put("preferred_delivery_days", deliveryDaysParam());
        if (paymentMethodValue != null) {
            form.put("payment_method", paymentMethodValue);
        }
        // Blank on an edit means "keep the current password", so only send a real value.
        String password = rawText(R.id.etPassword);
        if (!password.isEmpty()) {
            form.put("password", password);
        }
        form.put("create_wallet", createWalletNow ? "1" : "0");

        List<Http.FilePart> files = new ArrayList<>();
        for (Map.Entry<String, Uri> e : docUris.entrySet()) {
            Http.FilePart part = readFilePart(e.getKey(), e.getValue());
            if (part != null) {
                files.add(part);
            }
        }
        if (logoBitmap != null) {
            files.add(logoFilePart());
        }

        btnContinue.setEnabled(false);
        btnContinue.setText(isEditing() ? R.string.saving : R.string.registering);

        ApiCallback<CreateTraderData> callback = new ApiCallback<CreateTraderData>() {
            @Override
            public void onSuccess(CreateTraderData data) {
                Toast.makeText(CreateTraderActivity.this,
                        isEditing() ? R.string.trader_updated_msg : R.string.trader_created_msg,
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String message) {
                btnContinue.setEnabled(true);
                btnContinue.setText(submitLabel());
                Toast.makeText(CreateTraderActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };

        if (isEditing()) {
            ApiClient.get(this).updateTrader(form, files, callback);
        } else {
            ApiClient.get(this).createTrader(form, files, callback);
        }
    }

    private int submitLabel() {
        return isEditing() ? R.string.save_changes_btn : R.string.register_btn;
    }

    /**
     * The signed-in distributor, falling back to whatever the record already carried if
     * base-info has not answered yet — better to leave the link untouched than to clear it.
     */
    private String resolveDistributorId() {
        if (!currentDistributorId.isEmpty()) {
            return currentDistributorId;
        }
        if (isEditing() && !isBlank(editing.preferred_distributor_ids)) {
            return editing.preferred_distributor_ids.trim();
        }
        return "";
    }

    /** JPEG-encodes the cropped logo for the company_logo part. */
    private Http.FilePart logoFilePart() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        logoBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        return new Http.FilePart("company_logo", "company_logo.jpg", "image/jpeg", out.toByteArray());
    }

    /** True when the trader being edited already has a file stored for this document. */
    private boolean hasStoredDocument(DocDef def) {
        return isEditing() && !editing.documentName(def.responseKey).isEmpty();
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

    /** Passwords are taken verbatim — trimming would silently change what the user typed. */
    private String rawText(int id) {
        TextView tv = findViewById(id);
        return tv.getText() == null ? "" : tv.getText().toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty() || "null".equals(value);
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
