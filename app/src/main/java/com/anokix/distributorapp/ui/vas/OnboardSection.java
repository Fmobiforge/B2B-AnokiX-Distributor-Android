package com.anokix.distributorapp.ui.vas;

import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.BaseInfoData;
import com.anokix.distributorapp.network.dto.VasOnboardCustomerData;
import com.anokix.distributorapp.network.dto.VasOnboardPreviewData;
import com.anokix.distributorapp.session.SessionManager;
import com.anokix.distributorapp.ui.AirtimeActivity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Airtime &amp; VAS › Onboard customer. Registers an end-customer in the Limes CRM so an
 * account exists before a SIM is assigned. "View request body" previews the exact JSON the
 * server would POST to Limes without sending anything, which is how an operator checks a
 * half-filled form.
 */
public class OnboardSection {

    // Label/value pairs for the Limes system codes. Labels are what the operator reads;
    // the value at the same index is what the API expects.
    private static final String[] TITLE_LABELS = {"Mr", "Mrs", "Ms", "Dr"};
    private static final String[] TITLE_VALUES = {"Mr", "Mrs", "Ms", "Dr"};
    private static final String[] ID_TYPE_LABELS = {"SA ID", "Passport"};
    private static final String[] ID_TYPE_VALUES = {"ID", "PASSPORT"};
    private static final String[] TAX_LABELS = {"VB8 · VAT 15%"};
    private static final String[] TAX_VALUES = {"VB8"};
    private static final String[] PLAN_LABELS = {"STD9 · Standard"};
    private static final String[] PLAN_VALUES = {"STD9"};
    private static final String[] LANG_LABELS = {"English", "Afrikaans"};
    private static final String[] LANG_VALUES = {"en-gb", "af-za"};
    private static final String[] ROLE_LABELS = {"Customer"};
    private static final String[] ROLE_VALUES = {"CUSTOMER"};
    private static final String[] MEDIA_LABELS = {"Email", "SMS", "Post"};
    private static final String[] MEDIA_VALUES = {"EMAIL", "SMS", "POST"};
    private static final String[] COUNTRY_LABELS = {"South Africa"};
    private static final String[] COUNTRY_VALUES = {"South Africa"};

    private final AirtimeActivity activity;

    private final EditText firstName, lastName, idNumber, email, phone;
    private final EditText streetNo, streetName, suburb, city, province, postCode;
    private final com.google.android.material.button.MaterialButton createButton, previewButton;

    private int titleIndex, idTypeIndex, taxIndex, planIndex, langIndex, roleIndex, mediaIndex, countryIndex;

    public OnboardSection(AirtimeActivity activity, View root) {
        this.activity = activity;

        firstName = root.findViewById(R.id.onboardFirstName);
        lastName = root.findViewById(R.id.onboardLastName);
        idNumber = root.findViewById(R.id.onboardIdNumber);
        email = root.findViewById(R.id.onboardEmail);
        phone = root.findViewById(R.id.onboardPhone);
        streetNo = root.findViewById(R.id.onboardStreetNo);
        streetName = root.findViewById(R.id.onboardStreetName);
        suburb = root.findViewById(R.id.onboardSuburb);
        city = root.findViewById(R.id.onboardCity);
        province = root.findViewById(R.id.onboardProvince);
        postCode = root.findViewById(R.id.onboardPostCode);
        createButton = root.findViewById(R.id.onboardCreateButton);
        previewButton = root.findViewById(R.id.onboardPreviewButton);

        // The explainer copy carries <b> markup so the key steps stand out, as on the web.
        TextView hint = root.findViewById(R.id.onboardHint);
        hint.setText(HtmlCompat.fromHtml(activity.getString(R.string.vas_onboard_hint),
                HtmlCompat.FROM_HTML_MODE_COMPACT));

        bindDropdowns(root);
        prefillFromSignedInUser();

        createButton.setOnClickListener(v -> submit());
        previewButton.setOnClickListener(v -> preview());
    }

    /**
     * Opens the form on the signed-in operator's own details, which is what the web portal
     * does: most first onboards are the distributor registering themselves, and everything here
     * stays editable for the customers that follow.
     *
     * <p>The mobile number is only known once login or the profile screen has cached it, so an
     * older session falls back to one {@code base-info} read rather than leaving the field blank.
     */
    private void prefillFromSignedInUser() {
        SessionManager session = SessionManager.get(activity);
        setIfEmpty(firstName, session.getFirstName());
        setIfEmpty(lastName, session.getLastName());
        setIfEmpty(email, session.getEmail());
        setIfEmpty(phone, session.getPhone());
        if (!text(phone).isEmpty()) return;

        ApiClient.get(activity).getBaseInfo(new ApiCallback<BaseInfoData>() {
            @Override
            public void onSuccess(BaseInfoData data) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (data == null || data.user == null) return;
                session.setPhone(data.user.phone_number);
                setIfEmpty(firstName, data.user.first_name);
                setIfEmpty(lastName, data.user.last_name);
                setIfEmpty(email, data.user.email);
                setIfEmpty(phone, data.user.phone_number);
            }

            @Override
            public void onError(String message) {
                // A prefill is a convenience — the operator can still type the details.
            }
        });
    }

    /** Writes {@code value} only into a field the operator has not typed into. */
    private void setIfEmpty(EditText field, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (!text(field).isEmpty()) return;
        field.setText(value.trim());
    }

    private void bindDropdowns(View root) {
        VasUi.bindDropdown(activity, root.findViewById(R.id.onboardTitleField),
                root.findViewById(R.id.onboardTitleLabel),
                activity.getString(R.string.vas_f_title), TITLE_LABELS, 0, i -> titleIndex = i);

        TextView idTypeLabel = root.findViewById(R.id.onboardIdTypeLabel);
        VasUi.bindDropdown(activity, root.findViewById(R.id.onboardIdTypeField), idTypeLabel,
                activity.getString(R.string.vas_f_id_type), ID_TYPE_LABELS, 0, i -> {
                    idTypeIndex = i;
                    applyIdTypeToInput();
                });
        applyIdTypeToInput();

        VasUi.bindDropdown(activity, root.findViewById(R.id.onboardTaxField),
                root.findViewById(R.id.onboardTaxLabel),
                activity.getString(R.string.vas_f_tax_scheme), TAX_LABELS, 0, i -> taxIndex = i);

        VasUi.bindDropdown(activity, root.findViewById(R.id.onboardPlanField),
                root.findViewById(R.id.onboardPlanLabel),
                activity.getString(R.string.vas_f_collection_plan), PLAN_LABELS, 0, i -> planIndex = i);

        VasUi.bindDropdown(activity, root.findViewById(R.id.onboardLanguageField),
                root.findViewById(R.id.onboardLanguageLabel),
                activity.getString(R.string.vas_f_bill_language), LANG_LABELS, 0, i -> langIndex = i);

        VasUi.bindDropdown(activity, root.findViewById(R.id.onboardRoleField),
                root.findViewById(R.id.onboardRoleLabel),
                activity.getString(R.string.vas_f_contact_role), ROLE_LABELS, 0, i -> roleIndex = i);

        VasUi.bindDropdown(activity, root.findViewById(R.id.onboardMediaField),
                root.findViewById(R.id.onboardMediaLabel),
                activity.getString(R.string.vas_f_bill_media), MEDIA_LABELS, 0, i -> mediaIndex = i);

        VasUi.bindDropdown(activity, root.findViewById(R.id.onboardCountryField),
                root.findViewById(R.id.onboardCountryLabel),
                activity.getString(R.string.vas_f_country), COUNTRY_LABELS, 0, i -> countryIndex = i);
    }

    /** A SA ID is 13 digits; a passport is alphanumeric — so the keyboard follows the ID type. */
    private void applyIdTypeToInput() {
        boolean saId = "ID".equals(ID_TYPE_VALUES[idTypeIndex]);
        idNumber.setInputType(saId
                ? InputType.TYPE_CLASS_NUMBER
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        idNumber.setHint(saId
                ? activity.getString(R.string.vas_f_id_number_hint)
                : activity.getString(R.string.vas_f_passport_hint));
    }

    // ---- Request ---------------------------------------------------------

    /**
     * The onboard form as the flat field map both {@code onboard-customer} and its preview
     * take. The optional passport fields are always sent blank — the server omits them.
     */
    private Map<String, String> buildForm() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("title", TITLE_VALUES[titleIndex]);
        form.put("firstname", text(firstName));
        form.put("lastname", text(lastName));
        form.put("id_type", ID_TYPE_VALUES[idTypeIndex]);
        form.put("id_number", text(idNumber));
        form.put("id_issuer", "");
        form.put("id_issued_date", "");
        form.put("id_expiry_date", "");
        form.put("email", text(email));
        form.put("tax_scheme", TAX_VALUES[taxIndex]);
        form.put("collection_plan", PLAN_VALUES[planIndex]);
        form.put("bill_language", LANG_VALUES[langIndex]);
        form.put("contact_role", ROLE_VALUES[roleIndex]);
        form.put("bill_media", MEDIA_VALUES[mediaIndex]);
        form.put("country", COUNTRY_VALUES[countryIndex]);
        form.put("phone", text(phone));
        form.put("street_no", text(streetNo));
        form.put("street_name", text(streetName));
        form.put("suburb", text(suburb));
        form.put("city", text(city));
        form.put("province", text(province));
        form.put("post_code", text(postCode));
        return form;
    }

    private void submit() {
        if (!validate()) return;

        createButton.setEnabled(false);
        createButton.setText(R.string.processing);
        ApiClient.get(activity).onboardVasCustomer(buildForm(),
                new ApiCallback<VasOnboardCustomerData>() {
                    @Override
                    public void onSuccess(VasOnboardCustomerData data) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        resetButton();
                        boolean existed = data != null && data.customer != null
                                && data.customer.already_exists;
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.vas_onboard_success_title)
                                .setMessage(existed
                                        ? R.string.vas_onboard_exists_body
                                        : R.string.vas_onboard_success_body)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        activity.onCustomerOnboarded();
                    }

                    @Override
                    public void onError(String message) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        resetButton();
                        VasUi.showError(activity, message);
                    }
                });
    }

    private void preview() {
        previewButton.setEnabled(false);
        // The preview runs no validation on purpose — seeing a half-filled body is the point.
        ApiClient.get(activity).previewVasOnboard(buildForm(),
                new ApiCallback<VasOnboardPreviewData>() {
                    @Override
                    public void onSuccess(VasOnboardPreviewData data) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        previewButton.setEnabled(true);
                        VasUi.showLongMessage(activity,
                                activity.getString(R.string.vas_request_body_title),
                                data == null ? "" : data.render());
                    }

                    @Override
                    public void onError(String message) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        previewButton.setEnabled(true);
                        VasUi.showError(activity, message);
                    }
                });
    }

    private void resetButton() {
        createButton.setEnabled(true);
        createButton.setText(R.string.vas_create_customer);
    }

    private boolean validate() {
        if (text(firstName).isEmpty()) {
            VasUi.showValidation(activity, R.string.vas_err_firstname, firstName);
            return false;
        }
        if (text(lastName).isEmpty()) {
            VasUi.showValidation(activity, R.string.vas_err_lastname, lastName);
            return false;
        }
        if (text(idNumber).isEmpty()) {
            VasUi.showValidation(activity, R.string.vas_err_id_number, idNumber);
            return false;
        }
        String mail = text(email);
        if (mail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            VasUi.showValidation(activity, R.string.vas_err_email, email);
            return false;
        }
        String msisdn = text(phone);
        if (msisdn.isEmpty()) {
            VasUi.showValidation(activity, R.string.vas_err_recipient_empty, phone);
            return false;
        }
        if (!AirtimeActivity.isValidMsisdn(msisdn)) {
            VasUi.showValidation(activity, R.string.vas_err_recipient_invalid, phone);
            return false;
        }
        return true;
    }

    private String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
