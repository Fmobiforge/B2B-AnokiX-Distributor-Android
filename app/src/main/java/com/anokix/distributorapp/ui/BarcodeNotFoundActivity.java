package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.HcRequestData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * "Barcode not found in Product Library". Offers Create New Product (with the barcode
 * prefilled) and — when the Hirt &amp; Carter service answered — a request asking
 * Hirt &amp; Carter to add the product to their catalogue.
 */
public class BarcodeNotFoundActivity extends AppCompatActivity {

    /** Units offered by the portal's request form. */
    private static final String[] REQUEST_UNITS = {"g", "kg", "ml", "l", "ea", "pack", "case", "m"};

    private String barcode;

    private final ActivityResultLauncher<Intent> createLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    setResult(RESULT_OK);
                    finish();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_not_found);

        barcode = getIntent().getStringExtra(AddProductFlow.EXTRA_BARCODE);
        boolean canRequest = getIntent().getBooleanExtra(AddProductFlow.EXTRA_CAN_REQUEST, false);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        AddProductFlow.clipToOutline(findViewById(R.id.panel));

        findViewById(R.id.btnCreate).setOnClickListener(v -> {
            Intent i = new Intent(this, CreateProductActivity.class);
            i.putExtra(CreateProductActivity.EXTRA_BARCODE, barcode);
            createLauncher.launch(i);
        });

        View request = findViewById(R.id.btnRequest);
        request.setVisibility(canRequest ? View.VISIBLE : View.GONE);
        request.setOnClickListener(v -> showRequestDialog());

        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            setResult(AddProductFlow.RESULT_CLEAR_BARCODE);
            finish();
        });
    }

    // ---- Request product data from Hirt & Carter ---------------------------

    private void showRequestDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_hc_request, null, false);
        EditText etGtin = content.findViewById(R.id.etGtin);
        EditText etBrand = content.findViewById(R.id.etBrand);
        EditText etName = content.findViewById(R.id.etProductName);
        EditText etSize = content.findViewById(R.id.etSize);
        TextView unitField = content.findViewById(R.id.unitField);
        EditText etSku = content.findViewById(R.id.etSku);
        EditText etNote = content.findViewById(R.id.etNote);
        TextView error = content.findViewById(R.id.requestError);
        View form = content.findViewById(R.id.requestForm);
        View resultBox = content.findViewById(R.id.requestResult);
        TextView resultTitle = content.findViewById(R.id.resultTitle);
        TextView resultMessage = content.findViewById(R.id.resultMessage);

        etGtin.setText(barcode);
        final String[] unit = {null};
        unitField.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.request_unit)
                .setItems(REQUEST_UNITS, (d, which) -> {
                    unit[0] = REQUEST_UNITS[which];
                    unitField.setText(unit[0]);
                    unitField.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                    unitField.setBackgroundResource(R.drawable.bg_login_input);
                })
                .show());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.request_hc_data)
                .setView(content)
                .setNegativeButton(R.string.cancel_btn, null)
                .setPositiveButton(R.string.request_send, null)
                .create();

        // Replace the auto-dismissing positive button: validate, send, then switch the dialog
        // to its result state with a single Close button.
        final boolean[] sent = {false};
        final boolean[] sending = {false};
        dialog.setOnShowListener(d -> {
            Button send = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            send.setOnClickListener(v -> {
                if (sent[0]) {
                    dialog.dismiss();
                    return;
                }
                if (sending[0]) return;
                error.setVisibility(View.GONE);
                boolean ok = require(etBrand, R.string.err_request_brand)
                        & require(etName, R.string.err_request_name)
                        & require(etSize, R.string.err_request_size);
                if (unit[0] == null) {
                    unitField.setBackgroundResource(R.drawable.bg_hc_input_invalid);
                    unitField.setError(getString(R.string.err_request_unit));
                    ok = false;
                }
                if (!ok) return;

                Map<String, String> body = new LinkedHashMap<>();
                body.put("gtin", barcode == null ? "" : barcode.trim());
                body.put("brand", text(etBrand));
                body.put("product_name", text(etName));
                body.put("size", text(etSize));
                body.put("uom", unit[0]);
                if (!text(etSku).isEmpty()) body.put("sku", text(etSku));
                if (!text(etNote).isEmpty()) body.put("note", text(etNote));

                sending[0] = true;
                send.setEnabled(false);
                ApiClient.get(this).requestHcProduct(body, new ApiCallback<HcRequestData>() {
                    @Override
                    public void onSuccess(HcRequestData data) {
                        if (isFinishing() || isDestroyed()) return;
                        sending[0] = false;
                        sent[0] = true;
                        boolean already = isAlreadyRequested(data);
                        resultTitle.setText(already ? R.string.request_duplicate_title : R.string.request_sent_title);
                        resultMessage.setText(already ? R.string.request_duplicate_msg : R.string.request_sent_msg);
                        form.setVisibility(View.GONE);
                        resultBox.setVisibility(View.VISIBLE);
                        send.setEnabled(true);
                        send.setText(R.string.request_close);
                        cancel.setVisibility(View.GONE);
                        dialog.setCancelable(true);
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) return;
                        sending[0] = false;
                        send.setEnabled(true);
                        error.setText(AddProductFlow.isBlank(message)
                                ? getString(R.string.err_request_failed) : message);
                        error.setVisibility(View.VISIBLE);
                    }
                });
            });
        });
        dialog.show();
    }

    /**
     * The API flags repeats with {@code already_requested}; like the portal, a request
     * created more than two minutes ago is also an earlier one being returned.
     */
    private static boolean isAlreadyRequested(@Nullable HcRequestData data) {
        if (data == null) return false;
        if (Boolean.TRUE.equals(data.already_requested)) return true;
        if (data.request == null || AddProductFlow.isBlank(data.request.created_at)) return false;
        try {
            Date created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .parse(data.request.created_at.replace('T', ' '));
            return created != null && System.currentTimeMillis() - created.getTime() > 120_000L;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean require(EditText field, int errorRes) {
        if (text(field).isEmpty()) {
            field.setError(getString(errorRes));
            return false;
        }
        return true;
    }

    private static String text(TextView v) {
        return v.getText() == null ? "" : v.getText().toString().trim();
    }
}
