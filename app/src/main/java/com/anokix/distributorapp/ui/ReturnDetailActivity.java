package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.OrderFormat;
import com.anokix.distributorapp.model.ReturnFormat;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.ReturnDetailData;

/**
 * Return detail screen (mirrors the Returns-Item-Details mock): GRN number + status, trader
 * contact, reason + submitted date, trader note, the line items (item / qty / line total),
 * the issued credit note, the decision note, and — while pending — Approve / Reject buttons.
 * Fetched by id from {@code api/distributor/grns/get-by-id}.
 */
public class ReturnDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "id";

    private final ApiClient api = ApiClient.get(this);

    private View scroll, progress, actionRow;
    private TextView approveButton, rejectButton;
    private boolean changed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_detail);

        scroll = findViewById(R.id.detailScroll);
        progress = findViewById(R.id.detailProgress);
        actionRow = findViewById(R.id.actionRow);
        approveButton = findViewById(R.id.approveButton);
        rejectButton = findViewById(R.id.rejectButton);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        load(getIntent().getStringExtra(EXTRA_ID));
    }

    private void load(String id) {
        progress.setVisibility(View.VISIBLE);
        scroll.setVisibility(View.GONE);
        api.getReturnById(id, new ApiCallback<ReturnDetailData>() {
            @Override
            public void onSuccess(ReturnDetailData data) {
                progress.setVisibility(View.GONE);
                if (data == null || data.aReturn == null) {
                    Toast.makeText(ReturnDetailActivity.this, R.string.return_load_failed,
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                scroll.setVisibility(View.VISIBLE);
                bind(data.aReturn);
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
                Toast.makeText(ReturnDetailActivity.this,
                        message != null ? message : getString(R.string.return_load_failed),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bind(ReturnDetailData.Return r) {
        ((TextView) findViewById(R.id.detailReturnNumber)).setText(r.grn_number);

        TextView badge = findViewById(R.id.detailStatus);
        badge.setText(ReturnFormat.label(r.status));
        badge.setTextColor(ContextCompat.getColor(this, ReturnFormat.colorRes(r.status)));
        badge.setBackgroundResource(ReturnFormat.badgeBg(r.status));

        ((TextView) findViewById(R.id.detailTraderName)).setText(r.traderName());

        TextView phone = findViewById(R.id.detailTraderPhone);
        final String number = r.trader_phone;
        if (number == null || number.trim().isEmpty()) {
            phone.setVisibility(View.GONE);
        } else {
            phone.setVisibility(View.VISIBLE);
            phone.setText(number);
            phone.setOnClickListener(v -> dial(number));
        }

        TextView reason = findViewById(R.id.detailReason);
        reason.setText(r.reason_label != null && !r.reason_label.isEmpty()
                ? r.reason_label : ReturnFormat.label(r.reason));
        ((TextView) findViewById(R.id.detailSubmitted)).setText(ReturnFormat.date(r.created_at));

        // Trader note
        View noteCard = findViewById(R.id.traderNoteCard);
        if (r.note != null && !r.note.trim().isEmpty()) {
            noteCard.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.detailNote)).setText(r.note);
        } else {
            noteCard.setVisibility(View.GONE);
        }

        buildItems(r);

        // Credit note (issued)
        View creditCard = findViewById(R.id.creditCard);
        if (r.credit_note_number != null && !r.credit_note_number.trim().isEmpty()) {
            creditCard.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.detailCreditNumber)).setText(r.credit_note_number);
            ((TextView) findViewById(R.id.detailCreditSub)).setText(
                    getString(R.string.credited_to_trader, OrderFormat.money(r.credit_note_total, "R")));
        } else {
            creditCard.setVisibility(View.GONE);
        }

        // Decision note
        View decisionCard = findViewById(R.id.decisionCard);
        if (r.decision_note != null && !r.decision_note.trim().isEmpty()) {
            decisionCard.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.detailDecision)).setText(r.decision_note);
        } else {
            decisionCard.setVisibility(View.GONE);
        }

        bindActions(r);
    }

    private void buildItems(ReturnDetailData.Return r) {
        LinearLayout container = findViewById(R.id.itemsContainer);
        container.removeAllViews();
        if (r.items == null) return;
        int density = (int) getResources().getDisplayMetrics().density;
        boolean first = true;
        for (ReturnDetailData.Item item : r.items) {
            if (!first) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border));
                container.addView(divider);
            }
            first = false;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(14 * density, 12 * density, 14 * density, 12 * density);

            TextView name = new TextView(this);
            name.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            name.setText(item.name != null ? item.name : "");
            name.setTextSize(13);
            name.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            TextView qty = new TextView(this);
            qty.setLayoutParams(new LinearLayout.LayoutParams(48 * density,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            qty.setGravity(Gravity.END);
            qty.setText(String.valueOf(item.quantity));
            qty.setTextSize(13);
            qty.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            TextView line = new TextView(this);
            line.setLayoutParams(new LinearLayout.LayoutParams(80 * density,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            line.setGravity(Gravity.END);
            line.setText(OrderFormat.money(item.line_total, "R"));
            line.setTextSize(13);
            line.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            row.addView(name);
            row.addView(qty);
            row.addView(line);
            container.addView(row);
        }
    }

    /** Approve / Reject only while the return is pending. */
    private void bindActions(ReturnDetailData.Return r) {
        boolean pending = r.status != null && "pending".equalsIgnoreCase(r.status.trim());
        if (!pending) {
            actionRow.setVisibility(View.GONE);
            return;
        }
        actionRow.setVisibility(View.VISIBLE);
        approveButton.setOnClickListener(v -> promptAction(r.id, "approve"));
        rejectButton.setOnClickListener(v -> promptAction(r.id, "reject"));
    }

    private void promptAction(long id, String action) {
        final EditText input = new EditText(this);
        input.setHint(R.string.decision_note_hint);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        boolean approve = "approve".equals(action);
        new AlertDialog.Builder(this)
                .setTitle(approve ? R.string.approve_return : R.string.reject_return)
                .setView(input)
                .setPositiveButton(approve ? R.string.approve : R.string.reject,
                        (d, w) -> performAction(id, action, input.getText().toString().trim()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performAction(long id, String action, String note) {
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);
        api.returnAction(action, id, note, new ApiCallback<ReturnDetailData>() {
            @Override
            public void onSuccess(ReturnDetailData data) {
                changed = true;
                setResult(RESULT_OK);
                int msg = "approve".equals(action)
                        ? R.string.return_approved_msg : R.string.return_rejected_msg;
                Toast.makeText(ReturnDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                approveButton.setEnabled(true);
                rejectButton.setEnabled(true);
                if (data != null && data.aReturn != null) {
                    bind(data.aReturn);
                }
            }

            @Override
            public void onError(String message) {
                approveButton.setEnabled(true);
                rejectButton.setEnabled(true);
                Toast.makeText(ReturnDetailActivity.this,
                        message != null ? message : getString(R.string.return_action_failed),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void dial(String number) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number.trim())));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void finish() {
        if (changed) setResult(RESULT_OK);
        super.finish();
    }
}
