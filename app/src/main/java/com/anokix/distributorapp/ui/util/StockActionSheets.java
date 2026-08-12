package com.anokix.distributorapp.ui.util;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.InventoryPageData;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * The three stock mutations behind the Inventory screen — Adjust Stock, Transfer and
 * Add Stock / Receive — as bottom sheets, mirroring the web dialogs.
 *
 * Each one posts to the API and reports back through {@link OnCompleted} so the caller
 * can reload the inventory page with fresh numbers.
 */
public final class StockActionSheets {

    /** Called after a successful mutation, with the server's confirmation message. */
    public interface OnCompleted {
        void onCompleted(@Nullable String message);
    }

    private StockActionSheets() {
    }

    // ---- Adjust stock ----------------------------------------------------

    public static void showAdjustStock(@NonNull Activity activity,
                                       @NonNull InventoryPageData.Row row,
                                       @NonNull OnCompleted onCompleted) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_adjust_stock, null, false);
        dialog.setContentView(content);

        ((TextView) content.findViewById(R.id.sheetTitle))
                .setText(activity.getString(R.string.adjust_stock_title, row.name));

        EditText quantity = content.findViewById(R.id.adjustQuantity);
        EditText reason = content.findViewById(R.id.adjustReason);
        quantity.setText(String.valueOf(row.units));
        quantity.setSelection(quantity.getText().length());

        content.findViewById(R.id.sheetClose).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.sheetCancel).setOnClickListener(v -> dialog.dismiss());

        View submit = content.findViewById(R.id.sheetSubmit);
        submit.setOnClickListener(v -> {
            Long value = parseQuantity(activity, quantity);
            if (value == null) {
                return;
            }
            submit.setEnabled(false);
            ApiClient.get(activity).setStock(String.valueOf(row.productId), value,
                    text(reason), new ApiCallback<String>() {
                        @Override
                        public void onSuccess(String message) {
                            dialog.dismiss();
                            onCompleted.onCompleted(message);
                        }

                        @Override
                        public void onError(String message) {
                            submit.setEnabled(true);
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }

    // ---- Transfer --------------------------------------------------------

    public static void showTransfer(@NonNull Activity activity,
                                    @NonNull InventoryPageData.Row row,
                                    @NonNull List<InventoryPageData.Warehouse> warehouses,
                                    @NonNull OnCompleted onCompleted) {
        if (warehouses == null || warehouses.size() < 2) {
            Toast.makeText(activity, R.string.err_no_warehouses, Toast.LENGTH_LONG).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_transfer_stock, null, false);
        dialog.setContentView(content);

        ((TextView) content.findViewById(R.id.sheetTitle))
                .setText(activity.getString(R.string.transfer_stock_title, row.name));

        TextView fromLabel = content.findViewById(R.id.transferFromLabel);
        TextView toLabel = content.findViewById(R.id.transferToLabel);
        EditText quantity = content.findViewById(R.id.transferQuantity);
        EditText reason = content.findViewById(R.id.transferReason);

        // Default the source to the warehouse the row already sits in, when we know it.
        final int[] fromIndex = {indexOfWarehouse(warehouses, row.warehouseKey)};
        final int[] toIndex = {-1};
        fromLabel.setText(warehouses.get(fromIndex[0]).name);
        setSelected(activity, fromLabel, true);

        String[] names = warehouseNames(warehouses);
        content.findViewById(R.id.transferFromField).setOnClickListener(v ->
                pick(activity, R.string.transfer_from_hint, names, which -> {
                    fromIndex[0] = which;
                    fromLabel.setText(names[which]);
                    setSelected(activity, fromLabel, true);
                }));
        content.findViewById(R.id.transferToField).setOnClickListener(v ->
                pick(activity, R.string.transfer_to_hint, names, which -> {
                    toIndex[0] = which;
                    toLabel.setText(names[which]);
                    setSelected(activity, toLabel, true);
                }));

        content.findViewById(R.id.sheetClose).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.sheetCancel).setOnClickListener(v -> dialog.dismiss());

        View submit = content.findViewById(R.id.sheetSubmit);
        submit.setOnClickListener(v -> {
            if (toIndex[0] < 0) {
                Toast.makeText(activity, R.string.err_warehouse_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (toIndex[0] == fromIndex[0]) {
                Toast.makeText(activity, R.string.err_transfer_same_warehouse, Toast.LENGTH_SHORT).show();
                return;
            }
            Long value = parseQuantity(activity, quantity);
            if (value == null) {
                return;
            }
            submit.setEnabled(false);
            ApiClient.get(activity).transferStock(String.valueOf(row.productId),
                    warehouses.get(fromIndex[0]).id, warehouses.get(toIndex[0]).id, value,
                    text(reason), new ApiCallback<String>() {
                        @Override
                        public void onSuccess(String message) {
                            dialog.dismiss();
                            onCompleted.onCompleted(message);
                        }

                        @Override
                        public void onError(String message) {
                            submit.setEnabled(true);
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }

    // ---- Receive ---------------------------------------------------------

    public static void showReceive(@NonNull Activity activity,
                                   @NonNull List<InventoryPageData.Warehouse> warehouses,
                                   @NonNull List<InventoryPageData.Row> rows,
                                   @Nullable InventoryPageData.Row preselected,
                                   @NonNull OnCompleted onCompleted) {
        if (warehouses == null || warehouses.isEmpty()) {
            Toast.makeText(activity, R.string.err_no_warehouses, Toast.LENGTH_LONG).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_receive_stock, null, false);
        dialog.setContentView(content);

        TextView warehouseLabel = content.findViewById(R.id.receiveWarehouseLabel);
        TextView productLabel = content.findViewById(R.id.receiveProductLabel);
        TextView unitLabel = content.findViewById(R.id.receiveUnitLabel);
        TextView expiryLabel = content.findViewById(R.id.receiveExpiryLabel);
        TextView hint = content.findViewById(R.id.receiveHint);
        EditText quantity = content.findViewById(R.id.receiveQuantity);
        EditText cost = content.findViewById(R.id.receiveCost);
        EditText batch = content.findViewById(R.id.receiveBatch);

        final int[] warehouseIndex = {0};
        final int[] productIndex = {preselected == null ? -1 : indexOfRow(rows, preselected)};
        // 0 = cases (quantity × units per case), 1 = loose units.
        final int[] unitMode = {0};
        final String[] expiryDate = {""};

        warehouseLabel.setText(warehouses.get(0).name);
        setSelected(activity, warehouseLabel, true);
        if (productIndex[0] >= 0) {
            productLabel.setText(rows.get(productIndex[0]).name);
            setSelected(activity, productLabel, true);
        }

        Runnable updateHint = () -> {
            long entered = parseLong(text(quantity));
            int perCase = productIndex[0] >= 0
                    ? Math.max(1, rows.get(productIndex[0]).unitsPerCase) : 1;
            long units = unitMode[0] == 0 ? entered * perCase : entered;
            hint.setText(entered <= 0 ? "" : activity.getString(R.string.receive_adds_units,
                    String.format(Locale.getDefault(), "%,d", units)));
            cost.setHint(unitMode[0] == 0
                    ? R.string.receive_cost_per_case : R.string.receive_cost_per_unit);
        };
        quantity.addTextChangedListener(new SimpleWatcher(updateHint));

        String[] warehouseNames = warehouseNames(warehouses);
        content.findViewById(R.id.receiveWarehouseField).setOnClickListener(v ->
                pick(activity, R.string.receive_warehouse_hint, warehouseNames, which -> {
                    warehouseIndex[0] = which;
                    warehouseLabel.setText(warehouseNames[which]);
                    setSelected(activity, warehouseLabel, true);
                }));

        String[] productNames = rowNames(rows);
        content.findViewById(R.id.receiveProductField).setOnClickListener(v ->
                pick(activity, R.string.receive_product_hint, productNames, which -> {
                    productIndex[0] = which;
                    productLabel.setText(productNames[which]);
                    setSelected(activity, productLabel, true);
                    updateHint.run();
                }));

        String[] unitOptions = {activity.getString(R.string.receive_cases),
                activity.getString(R.string.receive_units)};
        content.findViewById(R.id.receiveUnitField).setOnClickListener(v ->
                pick(activity, R.string.receive_quantity_hint, unitOptions, which -> {
                    unitMode[0] = which;
                    unitLabel.setText(unitOptions[which]);
                    updateHint.run();
                }));

        content.findViewById(R.id.receiveExpiryField).setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(activity, (picker, year, month, day) -> {
                expiryDate[0] = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                expiryLabel.setText(String.format(Locale.US, "%02d/%02d/%04d", day, month + 1, year));
                setSelected(activity, expiryLabel, true);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        content.findViewById(R.id.sheetClose).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.sheetCancel).setOnClickListener(v -> dialog.dismiss());

        View submit = content.findViewById(R.id.sheetSubmit);
        submit.setOnClickListener(v -> {
            if (productIndex[0] < 0) {
                Toast.makeText(activity, R.string.err_product_required, Toast.LENGTH_SHORT).show();
                return;
            }
            Long entered = parseQuantity(activity, quantity);
            if (entered == null) {
                return;
            }
            InventoryPageData.Row product = rows.get(productIndex[0]);
            int perCase = Math.max(1, product.unitsPerCase);
            long units = unitMode[0] == 0 ? entered * perCase : entered;
            // The API books sellable units, so a per-case cost is spread over the case.
            String unitCost = text(cost);
            if (!unitCost.isEmpty() && unitMode[0] == 0 && perCase > 1) {
                unitCost = String.format(Locale.US, "%.2f", parseDouble(unitCost) / perCase);
            }

            submit.setEnabled(false);
            ApiClient.get(activity).receiveStock(warehouses.get(warehouseIndex[0]).id,
                    String.valueOf(product.productId), units, unitCost, text(batch), expiryDate[0],
                    new ApiCallback<String>() {
                        @Override
                        public void onSuccess(String message) {
                            dialog.dismiss();
                            onCompleted.onCompleted(message);
                        }

                        @Override
                        public void onError(String message) {
                            submit.setEnabled(true);
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }

    // ---- helpers ---------------------------------------------------------

    private interface OnPicked {
        void onPicked(int which);
    }

    private static void pick(Activity activity, int titleRes, String[] options, OnPicked callback) {
        if (options.length == 0) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(titleRes)
                .setItems(options, (d, which) -> callback.onPicked(which))
                .show();
    }

    private static void setSelected(Activity activity, TextView label, boolean selected) {
        label.setTextColor(ContextCompat.getColor(activity,
                selected ? R.color.text_primary : R.color.text_secondary));
    }

    private static String[] warehouseNames(List<InventoryPageData.Warehouse> warehouses) {
        List<String> names = new ArrayList<>();
        for (InventoryPageData.Warehouse w : warehouses) {
            names.add(w.name == null ? "" : w.name);
        }
        return names.toArray(new String[0]);
    }

    private static String[] rowNames(List<InventoryPageData.Row> rows) {
        List<String> names = new ArrayList<>();
        for (InventoryPageData.Row row : rows) {
            names.add(row.name == null ? "" : row.name);
        }
        return names.toArray(new String[0]);
    }

    private static int indexOfWarehouse(List<InventoryPageData.Warehouse> warehouses, String key) {
        if (key != null && !key.isEmpty()) {
            for (int i = 0; i < warehouses.size(); i++) {
                if (key.equals(warehouses.get(i).id)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private static int indexOfRow(List<InventoryPageData.Row> rows, InventoryPageData.Row row) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).productId == row.productId) {
                return i;
            }
        }
        return -1;
    }

    /** Reads a positive quantity, toasting and returning null when the field is unusable. */
    private static Long parseQuantity(Activity activity, EditText field) {
        long value = parseLong(text(field));
        if (value <= 0) {
            Toast.makeText(activity, R.string.err_quantity_required, Toast.LENGTH_SHORT).show();
            field.requestFocus();
            return null;
        }
        return value;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    /** TextWatcher that only cares about the final text. */
    private static class SimpleWatcher implements TextWatcher {
        private final Runnable action;

        SimpleWatcher(Runnable action) {
            this.action = action;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            action.run();
        }
    }
}
