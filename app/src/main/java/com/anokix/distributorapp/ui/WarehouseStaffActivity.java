package com.anokix.distributorapp.ui;

import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.StaffData;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Warehouse Staff (New-Design/Warehouse Staff): the people who sign in with their own
 * credentials to pick, pack and handle stock — add them, reset their password, and turn
 * their sign-in on or off.
 */
public class WarehouseStaffActivity extends BaseSecondaryActivity {

    private static final int MIN_PASSWORD = 8;

    private final List<StaffData.Staff> staff = new ArrayList<>();

    private LinearLayout staffList;
    private View staffEmpty;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_warehouse_staff;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.warehouse_staff);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.warehouse_staff_subtitle);
    }

    @Override
    protected void bindViews() {
        staffList = findViewById(R.id.staffList);
        staffEmpty = findViewById(R.id.staffEmpty);
        findViewById(R.id.addStaffButton).setOnClickListener(v -> showAddStaffSheet());
    }

    @Override
    protected void loadData() {
        ApiClient.get(this).getStaff(new ApiCallback<StaffData>() {
            @Override
            public void onSuccess(StaffData data) {
                staff.clear();
                if (data != null && data.staff != null) {
                    staff.addAll(data.staff);
                }
                render();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(WarehouseStaffActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- List ------------------------------------------------------------

    private void render() {
        staffList.removeAllViews();
        staffEmpty.setVisibility(staff.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < staff.size(); i++) {
            staffList.addView(staffRow(inflater, staff.get(i), i > 0));
        }
    }

    private View staffRow(LayoutInflater inflater, StaffData.Staff member, boolean showDivider) {
        View row = inflater.inflate(R.layout.item_warehouse_staff, staffList, false);
        row.findViewById(R.id.staffDivider).setVisibility(showDivider ? View.VISIBLE : View.GONE);

        ((TextView) row.findViewById(R.id.staffName)).setText(member.displayName());
        ((TextView) row.findViewById(R.id.staffEmail)).setText(member.email);
        ((TextView) row.findViewById(R.id.staffMeta)).setText(meta(member));

        boolean active = member.isActive();
        // Disabled people stay listed but read as muted, matching the web row.
        row.findViewById(R.id.staffName).setAlpha(active ? 1f : 0.6f);
        row.findViewById(R.id.staffEmail).setAlpha(active ? 1f : 0.6f);

        TextView access = row.findViewById(R.id.staffAccess);
        access.setText(member.can_sign_in ? R.string.staff_can_sign_in : R.string.staff_disabled);
        styleBadge(access, member.can_sign_in ? R.color.success : R.color.text_secondary,
                member.can_sign_in ? R.drawable.bg_badge_success : R.drawable.bg_badge_muted);

        row.findViewById(R.id.staffPassword).setOnClickListener(v -> showPasswordSheet(member));

        TextView toggle = row.findViewById(R.id.staffToggleAccess);
        toggle.setText(active ? R.string.staff_disable : R.string.staff_enable);
        toggle.setTextColor(ContextCompat.getColor(this,
                active ? R.color.danger : R.color.purple_primary));
        toggle.setOnClickListener(v -> {
            if (active) {
                confirmDisable(member);
            } else {
                updateStatus(member, "active");
            }
        });
        return row;
    }

    /** "Picker · Added 13 Aug 2026". */
    private String meta(StaffData.Staff member) {
        StringBuilder meta = new StringBuilder();
        if (!isBlank(member.job_title)) {
            meta.append(member.job_title);
        }
        String added = formatDate(member.created_at);
        if (!added.isEmpty()) {
            if (meta.length() > 0) {
                meta.append("  ·  ");
            }
            meta.append(getString(R.string.staff_added_on, added));
        }
        return meta.toString();
    }

    /** API timestamps come back as "yyyy-MM-dd HH:mm:ss"; show them as "13 Aug 2026". */
    private String formatDate(String raw) {
        if (isBlank(raw)) {
            return "";
        }
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(raw);
            if (parsed != null) {
                return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(parsed);
            }
        } catch (ParseException ignored) {
            // Unexpected shape — fall through and show the server value as-is.
        }
        return raw;
    }

    // ---- Actions ---------------------------------------------------------

    private void confirmDisable(StaffData.Staff member) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.staff_disable_confirm_title, member.displayName()))
                .setMessage(R.string.staff_disable_confirm_msg)
                .setNegativeButton(R.string.cancel_btn, null)
                .setPositiveButton(R.string.staff_disable, (d, w) -> updateStatus(member, "disabled"))
                .show();
    }

    private void updateStatus(StaffData.Staff member, String status) {
        ApiClient.get(this).updateStaffStatus(member.id, status, new ApiCallback<String>() {
            @Override
            public void onSuccess(String message) {
                toast(message);
                loadData();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(WarehouseStaffActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showPasswordSheet(StaffData.Staff member) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_staff_password, null, false);
        dialog.setContentView(content);

        ((TextView) content.findViewById(R.id.sheetTitle))
                .setText(getString(R.string.staff_password_title, member.displayName()));

        EditText password = content.findViewById(R.id.passwordInput);
        bindPasswordToggle(password, content.findViewById(R.id.togglePassword));

        content.findViewById(R.id.sheetClose).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.sheetCancel).setOnClickListener(v -> dialog.dismiss());

        View submit = content.findViewById(R.id.sheetSubmit);
        submit.setOnClickListener(v -> {
            String value = text(password);
            if (value.length() < MIN_PASSWORD) {
                Toast.makeText(this, R.string.staff_err_password, Toast.LENGTH_SHORT).show();
                password.requestFocus();
                return;
            }
            submit.setEnabled(false);
            ApiClient.get(this).setStaffPassword(member.id, value, new ApiCallback<String>() {
                @Override
                public void onSuccess(String message) {
                    dialog.dismiss();
                    toast(message);
                }

                @Override
                public void onError(String message) {
                    submit.setEnabled(true);
                    Toast.makeText(WarehouseStaffActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private void showAddStaffSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_add_staff, null, false);
        dialog.setContentView(content);

        EditText firstName = content.findViewById(R.id.firstNameInput);
        EditText lastName = content.findViewById(R.id.lastNameInput);
        EditText email = content.findViewById(R.id.emailInput);
        EditText password = content.findViewById(R.id.passwordInput);
        EditText jobTitle = content.findViewById(R.id.jobTitleInput);
        EditText phone = content.findViewById(R.id.phoneInput);
        bindPasswordToggle(password, content.findViewById(R.id.togglePassword));

        content.findViewById(R.id.sheetClose).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.sheetCancel).setOnClickListener(v -> dialog.dismiss());

        View submit = content.findViewById(R.id.sheetSubmit);
        submit.setOnClickListener(v -> {
            if (text(firstName).isEmpty()) {
                Toast.makeText(this, R.string.staff_err_first_name, Toast.LENGTH_SHORT).show();
                firstName.requestFocus();
                return;
            }
            if (text(lastName).isEmpty()) {
                Toast.makeText(this, R.string.staff_err_last_name, Toast.LENGTH_SHORT).show();
                lastName.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(text(email)).matches()) {
                Toast.makeText(this, R.string.staff_err_email, Toast.LENGTH_SHORT).show();
                email.requestFocus();
                return;
            }
            if (text(password).length() < MIN_PASSWORD) {
                Toast.makeText(this, R.string.staff_err_password, Toast.LENGTH_SHORT).show();
                password.requestFocus();
                return;
            }

            submit.setEnabled(false);
            ApiClient.get(this).addStaff(text(firstName), text(lastName), text(email),
                    text(password), text(phone), text(jobTitle), new ApiCallback<String>() {
                        @Override
                        public void onSuccess(String message) {
                            dialog.dismiss();
                            toast(message);
                            loadData();
                        }

                        @Override
                        public void onError(String message) {
                            submit.setEnabled(true);
                            Toast.makeText(WarehouseStaffActivity.this, message,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }

    // ---- helpers ---------------------------------------------------------

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

    private void styleBadge(TextView badge, @ColorRes int color, @DrawableRes int background) {
        badge.setTextColor(ContextCompat.getColor(this, color));
        badge.setBackgroundResource(background);
    }

    private void toast(String message) {
        if (!isBlank(message)) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty() || "null".equals(value);
    }
}
