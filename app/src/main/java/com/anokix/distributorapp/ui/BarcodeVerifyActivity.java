package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.HcLookupData;
import com.anokix.distributorapp.ui.views.RobotoTextView;
import com.google.gson.JsonObject;

/**
 * "Scan or Enter Barcode" — the portal's barcode verification step.
 *
 * A valid 8–14 digit barcode is checked in two places, as on the portal:
 *  1. the distributor's own catalogue (get-by-barcode) → {@link ProductFoundActivity};
 *  2. otherwise the Hirt &amp; Carter catalogue (external-lookup) →
 *     {@link HcProductActivity} when found, {@link BarcodeNotFoundActivity} when not.
 *
 * A barcode scanner that types into the field (keyboard wedge) works like typing.
 */
public class BarcodeVerifyActivity extends AppCompatActivity {

    private static final int MAX_DIGITS = 14;

    private EditText etBarcode;
    private View barcodeShell;
    private View validIcon;
    private View verifyLabel;
    private View verifyProgress;
    private View btnVerify;
    private boolean busy;

    private final ActivityResultLauncher<Intent> foundLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    done();
                } else if (result.getResultCode() == AddProductFlow.RESULT_CLEAR_BARCODE) {
                    clearBarcode();
                }
            });

    private final ActivityResultLauncher<Intent> hcProductLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    done();
                } else if (result.getResultCode() == AddProductFlow.RESULT_REJECTED) {
                    // "Not this product": the catalogue match was wrong, so treat the barcode
                    // as unknown — but without offering a data request for it.
                    openNotFound(currentBarcode(), false);
                }
            });

    private final ActivityResultLauncher<Intent> notFoundLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    done();
                } else if (result.getResultCode() == AddProductFlow.RESULT_CLEAR_BARCODE) {
                    clearBarcode();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_verify);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());

        etBarcode = findViewById(R.id.etBarcode);
        barcodeShell = findViewById(R.id.barcodeShell);
        validIcon = findViewById(R.id.validIcon);
        verifyLabel = findViewById(R.id.verifyLabel);
        verifyProgress = findViewById(R.id.verifyProgress);
        btnVerify = findViewById(R.id.btnVerify);

        etBarcode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Pasted values can carry spaces or dashes; keep digits only.
                String clean = AddProductFlow.digits(s.toString(), MAX_DIGITS);
                if (!clean.equals(s.toString())) {
                    etBarcode.setText(clean);
                    etBarcode.setSelection(clean.length());
                    return;
                }
                renderValidity();
            }
        });
        etBarcode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                verify();
                return true;
            }
            return false;
        });
        btnVerify.setOnClickListener(v -> verify());

        buildHowItWorks();

        String initial = AddProductFlow.digits(getIntent().getStringExtra(AddProductFlow.EXTRA_BARCODE), MAX_DIGITS);
        etBarcode.setText(initial);
        etBarcode.setSelection(initial.length());
        renderValidity();
        etBarcode.requestFocus();
    }

    private String currentBarcode() {
        return etBarcode.getText() == null ? "" : etBarcode.getText().toString().trim();
    }

    private void clearBarcode() {
        etBarcode.setText("");
        renderValidity();
    }

    /** Neutral while empty, green with a tick when valid, red otherwise. */
    private void renderValidity() {
        String value = currentBarcode();
        boolean valid = AddProductFlow.isBarcode(value);
        if (value.isEmpty()) {
            barcodeShell.setBackgroundResource(R.drawable.bg_hc_input);
        } else {
            barcodeShell.setBackgroundResource(valid ? R.drawable.bg_hc_input_valid : R.drawable.bg_hc_input_invalid);
        }
        validIcon.setVisibility(valid ? View.VISIBLE : View.GONE);
    }

    // ---- Lookup ------------------------------------------------------------

    private void verify() {
        if (busy) return;
        final String barcode = currentBarcode();
        if (!AddProductFlow.isBarcode(barcode)) {
            AddProductFlow.toast(this, R.string.err_invalid_barcode);
            return;
        }
        setBusy(true);
        ApiClient.get(this).getProductByBarcode(barcode, new ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject product) {
                if (isFinishing() || isDestroyed()) return;
                if (product != null) {
                    setBusy(false);
                    Intent i = new Intent(BarcodeVerifyActivity.this, ProductFoundActivity.class);
                    i.putExtra(AddProductFlow.EXTRA_PRODUCT_JSON, product.toString());
                    foundLauncher.launch(i);
                } else {
                    lookUpExternally(barcode);
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                AddProductFlow.toast(BarcodeVerifyActivity.this,
                        AddProductFlow.isBlank(message) ? getString(R.string.err_verify_product) : message);
            }
        });
    }

    /** Not in the distributor's catalogue — try Hirt &amp; Carter. */
    private void lookUpExternally(String barcode) {
        ApiClient.get(this).hcLookup(barcode, new ApiCallback<HcLookupData>() {
            @Override
            public void onSuccess(HcLookupData data) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                boolean available = data != null && data.available;
                if (available && data.found && data.product != null) {
                    Intent i = new Intent(BarcodeVerifyActivity.this, HcProductActivity.class);
                    i.putExtra(AddProductFlow.EXTRA_HC_PRODUCT, data.product);
                    i.putExtra(AddProductFlow.EXTRA_BARCODE, barcode);
                    hcProductLauncher.launch(i);
                } else {
                    openNotFound(barcode, available);
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                AddProductFlow.toast(BarcodeVerifyActivity.this,
                        getString(R.string.catalogue_lookup_unavailable));
                openNotFound(barcode, false);
            }
        });
    }

    private void openNotFound(String barcode, boolean canRequest) {
        Intent i = new Intent(this, BarcodeNotFoundActivity.class);
        i.putExtra(AddProductFlow.EXTRA_BARCODE, barcode);
        i.putExtra(AddProductFlow.EXTRA_CAN_REQUEST, canRequest);
        notFoundLauncher.launch(i);
    }

    private void setBusy(boolean value) {
        busy = value;
        btnVerify.setEnabled(!value);
        verifyLabel.setVisibility(value ? View.INVISIBLE : View.VISIBLE);
        verifyProgress.setVisibility(value ? View.VISIBLE : View.GONE);
    }

    private void done() {
        setResult(RESULT_OK);
        finish();
    }

    // ---- How it works --------------------------------------------------------

    private void buildHowItWorks() {
        LinearLayout box = findViewById(R.id.howItWorks);
        int[] icons = {R.drawable.ic_search, R.drawable.ic_add_circle_outline, R.drawable.ic_document};
        int[] texts = {R.string.how_check_library, R.string.how_add_catalogue, R.string.how_create_new};
        for (int i = 0; i < icons.length; i++) {
            LinearLayout step = new LinearLayout(this);
            step.setOrientation(LinearLayout.HORIZONTAL);
            step.setGravity(Gravity.CENTER_VERTICAL);

            FrameLayout iconWrap = new FrameLayout(this);
            iconWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
            iconWrap.setBackgroundResource(R.drawable.bg_hc_howto_icon);
            ImageView icon = new ImageView(this);
            FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(20), dp(20));
            iconLp.gravity = Gravity.CENTER;
            icon.setLayoutParams(iconLp);
            icon.setImageResource(icons[i]);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.hc_slate));
            iconWrap.addView(icon);
            step.addView(iconWrap);

            RobotoTextView text = new RobotoTextView(this);
            text.setText(texts[i]);
            text.setTextSize(12);
            text.setLineSpacing(0, 1.2f);
            text.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textLp.setMarginStart(dp(10));
            text.setLayoutParams(textLp);
            step.addView(text);
            box.addView(step);

            if (i < icons.length - 1) {
                ImageView arrow = new ImageView(this);
                LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(dp(14), dp(14));
                arrowLp.gravity = Gravity.CENTER_HORIZONTAL;
                arrowLp.topMargin = dp(8);
                arrowLp.bottomMargin = dp(8);
                arrow.setLayoutParams(arrowLp);
                arrow.setImageResource(R.drawable.ic_arrow_down);
                box.addView(arrow);
            }
        }
    }

    private int dp(int v) {
        return AddProductFlow.dp(this, v);
    }
}
