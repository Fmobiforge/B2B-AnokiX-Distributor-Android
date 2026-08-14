package com.anokix.distributorapp.ui.vas;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.ui.AirtimeActivity;
import com.anokix.distributorapp.ui.VasFormat;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Airtime &amp; VAS › Dynamic services. Builds an exact-value bundle from one or more
 * service lines and provisions it straight onto a number — the path Limes recommends when
 * the fixed catalog cannot express the amount.
 *
 * Units follow the Limes API: VOICE is minutes, SMS a message count, GPA_CREDIT Rand, and
 * DATA/WHATSAPP are <em>bytes</em>. The operator still types GB (as on the web portal) and
 * the conversion happens here, with the byte figure echoed under the field.
 */
public class DynamicServicesSection {

    private static final long BYTES_PER_GB = 1073741824L;

    /** The services the Limes dynamic endpoint accepts, with the unit each one is typed in. */
    private enum Service {
        VOICE("Voice", "VOICE", R.string.vas_amount_minutes, false, true),
        DATA("Data", "DATA", R.string.vas_amount_gb, true, true),
        SMS("SMS", "SMS", R.string.vas_amount_messages, false, true),
        WHATSAPP("WhatsApp", "WHATSAPP", R.string.vas_amount_gb, true, true),
        AIRTIME("Airtime credit", "GPA_CREDIT", R.string.vas_amount_rand, false, false);

        final String label;
        final String code;
        final int amountLabelRes;
        /** True when the typed amount is GB and must be sent to Limes as bytes. */
        final boolean inGigabytes;
        /** True when the unit only makes sense as a whole number (minutes, messages, bytes). */
        final boolean wholeUnits;

        Service(String label, String code, int amountLabelRes, boolean inGigabytes, boolean wholeUnits) {
            this.label = label;
            this.code = code;
            this.amountLabelRes = amountLabelRes;
            this.inGigabytes = inGigabytes;
            this.wholeUnits = wholeUnits;
        }

        static String[] labels() {
            Service[] all = values();
            String[] out = new String[all.length];
            for (int i = 0; i < all.length; i++) out[i] = all[i].label;
            return out;
        }
    }

    /** One bundle line: its inflated row plus the state the request is built from. */
    private class Line {
        final View row;
        final TextView serviceLabel, amountLabel, bytesNote, expiryLabel;
        final EditText amountInput, priceInput;
        Service service = Service.VOICE;
        String expiryDate = "";

        Line(View row) {
            this.row = row;
            serviceLabel = row.findViewById(R.id.lineServiceLabel);
            amountLabel = row.findViewById(R.id.lineAmountLabel);
            bytesNote = row.findViewById(R.id.lineBytesNote);
            expiryLabel = row.findViewById(R.id.lineExpiryLabel);
            amountInput = row.findViewById(R.id.lineAmountInput);
            priceInput = row.findViewById(R.id.linePriceInput);

            VasUi.bindDropdown(activity, row.findViewById(R.id.lineServiceField), serviceLabel,
                    activity.getString(R.string.vas_f_service), Service.labels(), 0, i -> {
                        service = Service.values()[i];
                        applyService();
                    });
            VasUi.bindDateField(activity, row.findViewById(R.id.lineExpiryField), expiryLabel,
                    iso -> expiryDate = iso);

            amountInput.addTextChangedListener(new VasUi.SimpleWatcher(this::applyService));
            priceInput.addTextChangedListener(new VasUi.SimpleWatcher(
                    DynamicServicesSection.this::updateTotal));
            row.findViewById(R.id.lineRemove).setOnClickListener(v -> removeLine(this));
            applyService();
        }

        /** Re-labels the amount field for the chosen service and echoes the byte conversion. */
        void applyService() {
            amountLabel.setText(service.amountLabelRes);
            if (service.inGigabytes) {
                long bytes = bytesValue();
                bytesNote.setVisibility(bytes > 0 ? View.VISIBLE : View.GONE);
                bytesNote.setText(activity.getString(R.string.vas_bytes_note, VasFormat.bytes(bytes)));
            } else {
                bytesNote.setVisibility(View.GONE);
            }
        }

        double amount() {
            return VasFormat.parse(text(amountInput));
        }

        double price() {
            return VasFormat.parse(text(priceInput));
        }

        long bytesValue() {
            return Math.round(amount() * BYTES_PER_GB);
        }

        /**
         * The value in the unit Limes expects: bytes for data, a whole count for minutes and
         * messages, and the exact Rand figure for airtime credit — rounding R10,50 up to R11
         * there would quietly sell the customer something they did not ask for.
         */
        Number apiValue() {
            if (service.inGigabytes) return bytesValue();
            double raw = amount();
            if (!service.wholeUnits && raw != Math.rint(raw)) return raw;
            return Math.round(raw);
        }

        /** True once this line carries an amount the API would accept. */
        boolean hasAmount() {
            return apiValue().doubleValue() > 0;
        }
    }

    private final AirtimeActivity activity;
    private final LinearLayout linesContainer;
    private final EditText msisdnInput;
    private final TextView totalLabel;
    private final MaterialButton provisionButton;
    private final List<Line> lines = new ArrayList<>();

    public DynamicServicesSection(AirtimeActivity activity, View root) {
        this.activity = activity;

        linesContainer = root.findViewById(R.id.dynamicLinesContainer);
        msisdnInput = root.findViewById(R.id.dynamicMsisdnInput);
        totalLabel = root.findViewById(R.id.dynamicTotal);
        provisionButton = root.findViewById(R.id.dynamicProvisionButton);

        root.findViewById(R.id.dynamicAddService).setOnClickListener(v -> addLine());
        provisionButton.setOnClickListener(v -> provision());

        addLine();
        updateTotal();
    }

    // ---- Lines -----------------------------------------------------------

    private void addLine() {
        View row = LayoutInflater.from(activity)
                .inflate(R.layout.item_vas_service_line, linesContainer, false);
        // Every line is the same layout, so its children share view ids. Left enabled, the
        // hierarchy's save/restore would write one line's text into all of them on rotation;
        // the section rebuilds its lines from scratch anyway.
        row.setSaveFromParentEnabled(false);
        linesContainer.addView(row);
        lines.add(new Line(row));
        updateRemoveVisibility();
        updateTotal();
    }

    private void removeLine(Line line) {
        linesContainer.removeView(line.row);
        lines.remove(line);
        updateRemoveVisibility();
        updateTotal();
    }

    /** A single line cannot be removed — the bundle would then have nothing to provision. */
    private void updateRemoveVisibility() {
        int visibility = lines.size() > 1 ? View.VISIBLE : View.GONE;
        for (Line l : lines) l.row.findViewById(R.id.lineRemove).setVisibility(visibility);
    }

    private void updateTotal() {
        double total = 0;
        for (Line l : lines) total += l.price();
        totalLabel.setText(VasFormat.money(total));
    }

    // ---- Provision -------------------------------------------------------

    private void provision() {
        if (!validate()) return;
        String customerId = activity.actingCustomerId();
        if (customerId.isEmpty()) {
            VasUi.showValidation(activity, R.string.vas_err_no_acting_customer, null);
            return;
        }

        provisionButton.setEnabled(false);
        provisionButton.setText(R.string.processing);
        ApiClient.get(activity).provisionVasDynamicServices(text(msisdnInput), buildServices(),
                customerId, new ApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        resetButton();
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.vas_dynamic_success_title)
                                .setMessage(R.string.vas_dynamic_success_body)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        activity.refreshTransactions();
                    }

                    @Override
                    public void onError(String message) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        resetButton();
                        VasUi.showError(activity, message);
                    }
                });
    }

    /** The {@code services} array of the dynamic-services request; prices go in cents. */
    private List<Map<String, Object>> buildServices() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Line l : lines) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("definitionCode", l.service.code);
            entry.put("value", l.apiValue());
            entry.put("priceInCents", Math.round(l.price() * 100));
            if (!l.expiryDate.isEmpty()) entry.put("expiryDate", l.expiryDate);
            out.add(entry);
        }
        return out;
    }

    private void resetButton() {
        provisionButton.setEnabled(true);
        provisionButton.setText(R.string.vas_provision_services);
    }

    private boolean validate() {
        String msisdn = text(msisdnInput);
        if (msisdn.isEmpty()) {
            VasUi.showValidation(activity, R.string.vas_err_recipient_empty, msisdnInput);
            return false;
        }
        if (!AirtimeActivity.isValidMsisdn(msisdn)) {
            VasUi.showValidation(activity, R.string.vas_err_recipient_invalid, msisdnInput);
            return false;
        }
        if (lines.isEmpty()) {
            VasUi.showValidation(activity, R.string.vas_err_no_services, null);
            return false;
        }
        for (Line l : lines) {
            if (!l.hasAmount()) {
                VasUi.showValidation(activity, R.string.vas_err_service_amount, l.amountInput);
                return false;
            }
            if (l.price() <= 0) {
                VasUi.showValidation(activity, R.string.vas_err_service_price, l.priceInput);
                return false;
            }
        }
        return true;
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
