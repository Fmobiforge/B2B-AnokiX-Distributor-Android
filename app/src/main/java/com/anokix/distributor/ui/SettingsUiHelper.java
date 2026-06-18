package com.anokix.distributor.ui;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.text.method.PasswordTransformationMethod;

import com.anokix.distributor.R;

import java.util.List;

final class SettingsUiHelper {

    private SettingsUiHelper() {
    }

    static void setText(View view, String value) {
        if (view instanceof EditText) {
            ((EditText) view).setText(value == null ? "" : value);
        }
    }

    static String textOf(View view) {
        if (view instanceof EditText && ((EditText) view).getText() != null) {
            return ((EditText) view).getText().toString().trim();
        }
        return "";
    }

    static String initialsOf(String name, String fallback) {
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 2; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return sb.length() == 0 ? fallback : sb.toString();
    }

    static void setupPasswordToggle(EditText field, ImageButton toggle) {
        toggle.setOnClickListener(v -> {
            boolean visible = field.getTransformationMethod() == null;
            if (visible) {
                field.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggle.setImageResource(R.drawable.ic_visibility);
            } else {
                field.setTransformationMethod(null);
                toggle.setImageResource(R.drawable.ic_visibility_off);
            }
            field.setSelection(field.getText() != null ? field.getText().length() : 0);
        });
    }

    static ArrayAdapter<String> simpleAdapter(BaseSettingsFormActivity activity, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                R.layout.item_spinner, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    static void selectSpinnerItem(Spinner spinner, List<String> options, String value) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    static String selectedItem(Spinner spinner) {
        Object item = spinner.getSelectedItem();
        return item == null ? "" : item.toString();
    }
}
