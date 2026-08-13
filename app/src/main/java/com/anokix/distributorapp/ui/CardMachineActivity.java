package com.anokix.distributorapp.ui;

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
import com.anokix.distributorapp.network.dto.PosPaymentsData;
import com.anokix.distributorapp.network.dto.PosTerminalsData;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Card Machine (New-Design/Card-Machine): push an amount to a PayCloud terminal, manage
 * the machines linked to this account, and review every payment taken on them.
 *
 * Card details never reach the app — the customer taps / inserts / swipes on the device.
 */
public class CardMachineActivity extends BaseSecondaryActivity {

    private static final int PER_PAGE = 10;

    private final List<PosTerminalsData.Terminal> terminals = new ArrayList<>();
    private final List<PosTerminalsData.Terminal> activeTerminals = new ArrayList<>();
    private PosTerminalsData.Terminal selectedTerminal;
    private boolean posConfigured = true;

    private int page = 1;
    private int totalPages = 1;
    private int totalPayments;
    /** Kept so payment rows can be re-labelled once the machine names arrive. */
    private PosPaymentsData lastPayments;

    private EditText amountInput, referenceInput;
    private TextView terminalLabel, machinesEmpty, paginationLabel, sendButton;
    private View noMachineBanner, paymentForm, paymentsEmpty, paginationBar;
    private LinearLayout machineList, paymentList;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_card_machine;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.card_machine);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.card_machine_subtitle);
    }

    @Override
    protected void bindViews() {
        amountInput = findViewById(R.id.amountInput);
        referenceInput = findViewById(R.id.referenceInput);
        terminalLabel = findViewById(R.id.terminalLabel);
        machineList = findViewById(R.id.machineList);
        machinesEmpty = findViewById(R.id.machinesEmpty);
        paymentList = findViewById(R.id.paymentList);
        paymentsEmpty = findViewById(R.id.paymentsEmpty);
        paginationBar = findViewById(R.id.paginationBar);
        paginationLabel = findViewById(R.id.paginationLabel);
        noMachineBanner = findViewById(R.id.noMachineBanner);
        paymentForm = findViewById(R.id.paymentForm);
        sendButton = findViewById(R.id.sendButton);

        findViewById(R.id.terminalField).setOnClickListener(v -> showTerminalPicker());
        findViewById(R.id.addMachineButton).setOnClickListener(v -> showAddMachineSheet());
        sendButton.setOnClickListener(v -> sendPayment());

        ImageButton prev = findViewById(R.id.prevPage);
        ImageButton next = findViewById(R.id.nextPage);
        prev.setOnClickListener(v -> {
            if (page > 1) {
                page--;
                loadPayments();
            }
        });
        next.setOnClickListener(v -> {
            if (page < totalPages) {
                page++;
                loadPayments();
            }
        });
    }

    @Override
    protected void loadData() {
        loadTerminals();
        loadPayments();
    }


    // ---- Terminals -------------------------------------------------------

    private void loadTerminals() {
        ApiClient.get(this).getPosTerminals(new ApiCallback<PosTerminalsData>() {
            @Override
            public void onSuccess(PosTerminalsData data) {
                terminals.clear();
                posConfigured = data == null || data.configured;
                if (data != null && data.terminals != null) {
                    terminals.addAll(data.terminals);
                }
                renderTerminals();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CardMachineActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderTerminals() {
        activeTerminals.clear();
        for (PosTerminalsData.Terminal terminal : terminals) {
            if (terminal.is_active) {
                activeTerminals.add(terminal);
            }
        }
        // Keep the current pick when it survived a reload, otherwise take the first machine.
        if (selectedTerminal != null) {
            PosTerminalsData.Terminal match = null;
            for (PosTerminalsData.Terminal terminal : activeTerminals) {
                if (terminal.id == selectedTerminal.id) {
                    match = terminal;
                    break;
                }
            }
            selectedTerminal = match;
        }
        if (selectedTerminal == null && !activeTerminals.isEmpty()) {
            selectedTerminal = activeTerminals.get(0);
        }
        terminalLabel.setText(selectedTerminal == null
                ? getString(R.string.pos_select_machine) : selectedTerminal.displayName());
        terminalLabel.setTextColor(ContextCompat.getColor(this,
                selectedTerminal == null ? R.color.text_secondary : R.color.text_primary));

        // Without an active machine (or without PayCloud switched on) there is nothing to
        // send to, so the form gives way to an explanatory banner.
        boolean canTakePayment = posConfigured && !activeTerminals.isEmpty();
        paymentForm.setVisibility(canTakePayment ? View.VISIBLE : View.GONE);
        noMachineBanner.setVisibility(canTakePayment ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.bannerTitle)).setText(posConfigured
                ? R.string.pos_no_machine_title : R.string.pos_not_configured);
        findViewById(R.id.bannerBody).setVisibility(posConfigured ? View.VISIBLE : View.GONE);

        machineList.removeAllViews();
        machinesEmpty.setVisibility(terminals.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < terminals.size(); i++) {
            machineList.addView(machineRow(inflater, terminals.get(i), i > 0));
        }

        // Payments may have rendered before the machines did — relabel them now.
        if (lastPayments != null) {
            renderPayments(lastPayments);
        }
    }

    private View machineRow(LayoutInflater inflater, PosTerminalsData.Terminal terminal,
                            boolean showDivider) {
        View row = inflater.inflate(R.layout.item_card_machine, machineList, false);
        row.findViewById(R.id.machineDivider).setVisibility(showDivider ? View.VISIBLE : View.GONE);
        ((TextView) row.findViewById(R.id.machineName)).setText(terminal.displayName());
        ((TextView) row.findViewById(R.id.machineSerial)).setText(terminal.terminal_sn);

        // Unlinked machines read as muted so the live one stands out.
        boolean active = terminal.is_active;
        row.findViewById(R.id.machineName).setAlpha(active ? 1f : 0.6f);
        row.findViewById(R.id.machineSerial).setAlpha(active ? 1f : 0.6f);

        TextView status = row.findViewById(R.id.machineStatus);
        status.setText(active ? R.string.active : R.string.pos_removed);
        styleBadge(status, active ? R.color.success : R.color.text_secondary,
                active ? R.drawable.bg_badge_success : R.drawable.bg_badge_muted);

        View remove = row.findViewById(R.id.machineRemove);
        remove.setVisibility(active ? View.VISIBLE : View.GONE);
        remove.setOnClickListener(v -> confirmRemove(terminal));
        return row;
    }

    private void confirmRemove(PosTerminalsData.Terminal terminal) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.pos_remove_confirm_title)
                .setMessage(getString(R.string.pos_remove_confirm_msg, terminal.displayName()))
                .setNegativeButton(R.string.cancel_btn, null)
                .setPositiveButton(R.string.pos_remove, (d, w) -> ApiClient.get(this)
                        .removePosTerminal(terminal.id, new ApiCallback<String>() {
                            @Override
                            public void onSuccess(String message) {
                                toast(message);
                                loadTerminals();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(CardMachineActivity.this, message,
                                        Toast.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }

    private void showAddMachineSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_add_card_machine, null, false);
        dialog.setContentView(content);

        EditText serial = content.findViewById(R.id.serialInput);
        EditText label = content.findViewById(R.id.labelInput);

        content.findViewById(R.id.sheetClose).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.sheetCancel).setOnClickListener(v -> dialog.dismiss());

        View submit = content.findViewById(R.id.sheetSubmit);
        submit.setOnClickListener(v -> {
            String sn = text(serial);
            if (sn.isEmpty()) {
                Toast.makeText(this, R.string.pos_err_serial, Toast.LENGTH_SHORT).show();
                serial.requestFocus();
                return;
            }
            submit.setEnabled(false);
            ApiClient.get(this).addPosTerminal(sn, text(label), new ApiCallback<String>() {
                @Override
                public void onSuccess(String message) {
                    dialog.dismiss();
                    toast(message);
                    loadTerminals();
                }

                @Override
                public void onError(String message) {
                    submit.setEnabled(true);
                    Toast.makeText(CardMachineActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private void showTerminalPicker() {
        if (activeTerminals.isEmpty()) {
            Toast.makeText(this, R.string.pos_err_machine, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[activeTerminals.size()];
        for (int i = 0; i < activeTerminals.size(); i++) {
            names[i] = activeTerminals.get(i).displayName();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.pos_card_machine)
                .setItems(names, (d, which) -> {
                    selectedTerminal = activeTerminals.get(which);
                    terminalLabel.setText(selectedTerminal.displayName());
                    terminalLabel.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                })
                .show();
    }

    // ---- Take a payment --------------------------------------------------

    private void sendPayment() {
        String amount = text(amountInput);
        if (amount.isEmpty() || parseAmount(amount) <= 0) {
            Toast.makeText(this, R.string.pos_err_amount, Toast.LENGTH_SHORT).show();
            amountInput.requestFocus();
            return;
        }
        if (selectedTerminal == null) {
            Toast.makeText(this, R.string.pos_err_machine, Toast.LENGTH_SHORT).show();
            return;
        }

        setSending(true);
        ApiClient.get(this).sendPosPayment(amount, selectedTerminal.id, text(referenceInput),
                new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String message) {
                        setSending(false);
                        toast(message == null || message.isEmpty()
                                ? getString(R.string.pos_sent_to_machine) : message);
                        amountInput.setText("");
                        referenceInput.setText("");
                        page = 1;
                        loadPayments();
                    }

                    @Override
                    public void onError(String message) {
                        setSending(false);
                        // The terminal's own error ("device does not exist") lands here.
                        new AlertDialog.Builder(CardMachineActivity.this)
                                .setTitle(R.string.pos_send_to_machine)
                                .setMessage(message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        page = 1;
                        loadPayments();
                    }
                });
    }

    /** Pushing to a terminal takes a moment — say so instead of freezing the button. */
    private void setSending(boolean sending) {
        sendButton.setEnabled(!sending);
        sendButton.setAlpha(sending ? 0.7f : 1f);
        sendButton.setText(sending ? R.string.pos_sending : R.string.pos_send_to_machine);
    }

    // ---- Payments --------------------------------------------------------

    private void loadPayments() {
        ApiClient.get(this).getPosPayments(page, PER_PAGE, new ApiCallback<PosPaymentsData>() {
            @Override
            public void onSuccess(PosPaymentsData data) {
                renderPayments(data);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CardMachineActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderPayments(PosPaymentsData data) {
        lastPayments = data;
        paymentList.removeAllViews();
        List<PosPaymentsData.Payment> payments =
                data == null || data.payments == null ? new ArrayList<>() : data.payments;

        if (data != null && data.pagination != null) {
            page = Math.max(1, data.pagination.page);
            totalPages = Math.max(1, data.pagination.total_pages);
            totalPayments = data.pagination.total;
        } else {
            totalPages = 1;
            totalPayments = payments.size();
        }

        paymentsEmpty.setVisibility(payments.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < payments.size(); i++) {
            paymentList.addView(paymentRow(inflater, payments.get(i), i > 0));
        }

        boolean paginate = totalPayments > 0;
        paginationBar.setVisibility(paginate ? View.VISIBLE : View.GONE);
        if (paginate) {
            int first = (page - 1) * PER_PAGE + 1;
            int last = Math.min(totalPayments, first + payments.size() - 1);
            paginationLabel.setText(getString(R.string.pos_payment_range, first, last, totalPayments));
            findViewById(R.id.prevPage).setEnabled(page > 1);
            findViewById(R.id.prevPage).setAlpha(page > 1 ? 1f : 0.4f);
            findViewById(R.id.nextPage).setEnabled(page < totalPages);
            findViewById(R.id.nextPage).setAlpha(page < totalPages ? 1f : 0.4f);
        }
    }

    private View paymentRow(LayoutInflater inflater, PosPaymentsData.Payment payment,
                            boolean showDivider) {
        View row = inflater.inflate(R.layout.item_card_payment, paymentList, false);
        row.findViewById(R.id.paymentDivider).setVisibility(showDivider ? View.VISIBLE : View.GONE);

        ((TextView) row.findViewById(R.id.paymentAmount)).setText(formatAmount(payment));
        TextView description = row.findViewById(R.id.paymentDescription);
        description.setText(payment.description);
        description.setVisibility(isBlank(payment.description) ? View.GONE : View.VISIBLE);

        ((TextView) row.findViewById(R.id.paymentMeta)).setText(meta(payment));

        // The status badge is the whole story here — the terminal's raw error text stays
        // out of the list (the send attempt already surfaces it in a dialog).
        TextView status = row.findViewById(R.id.paymentStatus);
        status.setText(statusLabel(payment.status));
        applyStatusStyle(status, payment.status);
        return row;
    }

    /** "12 Aug, 23:01 · Front Machine · Card 1234". */
    private String meta(PosPaymentsData.Payment payment) {
        StringBuilder meta = new StringBuilder(formatWhen(payment));
        String machine = machineName(payment.terminal_sn);
        if (!isBlank(machine)) {
            meta.append("  ·  ").append(machine);
        }
        if (!isBlank(payment.card_no)) {
            meta.append("  ·  ").append(getString(R.string.pos_card_masked, payment.card_no));
        }
        return meta.toString();
    }

    private String machineName(String serial) {
        if (isBlank(serial)) {
            return "";
        }
        for (PosTerminalsData.Terminal terminal : terminals) {
            if (serial.equals(terminal.terminal_sn)) {
                return terminal.displayName();
            }
        }
        return serial;
    }

    private String formatAmount(PosPaymentsData.Payment payment) {
        String symbol = "ZAR".equalsIgnoreCase(payment.currency) || isBlank(payment.currency)
                ? "R" : payment.currency + " ";
        return String.format(Locale.US, "%s%,.2f", symbol, payment.amount);
    }

    /** API timestamps come back as "yyyy-MM-dd HH:mm:ss"; show them as "12 Aug, 23:01". */
    private String formatWhen(PosPaymentsData.Payment payment) {
        String raw = !isBlank(payment.paid_at) ? payment.paid_at : payment.created_at;
        if (isBlank(raw)) {
            return "";
        }
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(raw);
            if (parsed != null) {
                return new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(parsed);
            }
        } catch (ParseException ignored) {
            // Unexpected shape — fall through and show the server value as-is.
        }
        return raw;
    }

    private String statusLabel(String status) {
        if (isBlank(status)) {
            return getString(R.string.pending);
        }
        return status.substring(0, 1).toUpperCase(Locale.US) + status.substring(1);
    }

    private void applyStatusStyle(TextView badge, String status) {
        String value = status == null ? "" : status.toLowerCase(Locale.US);
        if (value.contains("paid") || value.contains("success") || value.contains("complete")) {
            styleBadge(badge, R.color.success, R.drawable.bg_badge_success);
        } else if (value.contains("fail") || value.contains("cancel") || value.contains("declin")) {
            styleBadge(badge, R.color.danger, R.drawable.bg_badge_danger);
        } else {
            styleBadge(badge, R.color.warning, R.drawable.bg_badge_warning);
        }
    }

    private void styleBadge(TextView badge, @ColorRes int color, @DrawableRes int background) {
        badge.setTextColor(ContextCompat.getColor(this, color));
        badge.setBackgroundResource(background);
    }

    // ---- helpers ---------------------------------------------------------

    private double parseAmount(String value) {
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
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
