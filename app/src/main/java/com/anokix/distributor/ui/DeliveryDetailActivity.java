package com.anokix.distributor.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.anokix.distributor.R;
import com.anokix.distributor.model.DeliveryFormat;
import com.anokix.distributor.model.OrderFormat;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.DeliveryDetailData;

/**
 * Delivery detail screen (mirrors the *-Details mocks): delivery/order numbers + status,
 * trader contact, area + delivery time, the line items, the order total, and a single
 * action button (Dispatch Delivery / Mark as Delivered) decided by the raw {@code status}.
 * Fetched by id from {@code api/distributor/deliveries/get-by-id}.
 */
public class DeliveryDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "id";

    private final ApiClient api = ApiClient.get(this);

    private View scroll, progress;
    private TextView actionButton;
    private boolean changed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_detail);

        scroll = findViewById(R.id.detailScroll);
        progress = findViewById(R.id.detailProgress);
        actionButton = findViewById(R.id.actionButton);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        load(getIntent().getStringExtra(EXTRA_ID));
    }

    private void load(String id) {
        progress.setVisibility(View.VISIBLE);
        scroll.setVisibility(View.GONE);
        api.getDeliveryById(id, new ApiCallback<DeliveryDetailData>() {
            @Override
            public void onSuccess(DeliveryDetailData data) {
                progress.setVisibility(View.GONE);
                if (data == null || data.delivery == null) {
                    Toast.makeText(DeliveryDetailActivity.this, R.string.delivery_load_failed,
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                scroll.setVisibility(View.VISIBLE);
                bind(data.delivery);
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
                Toast.makeText(DeliveryDetailActivity.this,
                        message != null ? message : getString(R.string.delivery_load_failed),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bind(DeliveryDetailData.Delivery d) {
        ((TextView) findViewById(R.id.detailDeliveryNumber)).setText(d.delivery_number);
        ((TextView) findViewById(R.id.detailOrderNumber)).setText(d.order_number);

        // Badge uses the computed delivery_status (delivered/delayed/...).
        String badgeStatus = d.delivery_status != null && !d.delivery_status.isEmpty()
                ? d.delivery_status : d.status_key;
        TextView badge = findViewById(R.id.detailStatus);
        badge.setText(DeliveryFormat.label(badgeStatus));
        badge.setTextColor(ContextCompat.getColor(this, DeliveryFormat.colorRes(badgeStatus)));
        badge.setBackgroundResource(DeliveryFormat.badgeBg(badgeStatus));

        ((TextView) findViewById(R.id.detailTraderName)).setText(d.traderName());

        TextView phone = findViewById(R.id.detailTraderPhone);
        final String number = d.trader != null ? d.trader.phone : null;
        if (number == null || number.trim().isEmpty()) {
            phone.setVisibility(View.GONE);
        } else {
            phone.setVisibility(View.VISIBLE);
            phone.setText(number);
            phone.setOnClickListener(v -> dial(number));
        }

        TextView area = findViewById(R.id.detailArea);
        String areaText = d.area != null && !d.area.isEmpty()
                ? d.area : (d.trader != null ? d.trader.location : null);
        area.setText(areaText == null ? "" : areaText);
        ((View) area.getParent()).setVisibility(areaText == null || areaText.isEmpty()
                ? View.GONE : View.VISIBLE);

        TextView at = findViewById(R.id.detailDeliveryAt);
        String atText = DeliveryFormat.deliveryAt(d.delivery_at);
        at.setText(atText);
        ((View) at.getParent()).setVisibility(atText.isEmpty() ? View.GONE : View.VISIBLE);

        buildItems(d);

        ((TextView) findViewById(R.id.detailTotal)).setText(OrderFormat.money(d.total_amount, "R"));

        bindAction(d);
    }

    private void buildItems(DeliveryDetailData.Delivery d) {
        LinearLayout container = findViewById(R.id.itemsContainer);
        container.removeAllViews();
        if (d.items == null) return;
        int density = (int) getResources().getDisplayMetrics().density;
        boolean first = true;
        for (DeliveryDetailData.Item item : d.items) {
            if (!first) {
                View divider = new View(this);
                LinearLayout.LayoutParams dp =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divider.setLayoutParams(dp);
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border));
                container.addView(divider);
            }
            first = false;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(14 * density, 12 * density, 14 * density, 12 * density);

            TextView name = new TextView(this);
            LinearLayout.LayoutParams nameLp =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            name.setLayoutParams(nameLp);
            name.setText(item.name != null ? item.name : "");
            name.setTextSize(13);
            name.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            TextView qty = new TextView(this);
            qty.setText(String.valueOf(item.quantity));
            qty.setTextSize(13);
            qty.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            row.addView(name);
            row.addView(qty);
            container.addView(row);
        }
    }

    /** Show Dispatch / Mark as Delivered based on the raw status; hide for delivered/other. */
    private void bindAction(DeliveryDetailData.Delivery d) {
        final String action = actionFor(d.status);
        if (action == null) {
            actionButton.setVisibility(View.GONE);
            return;
        }
        actionButton.setVisibility(View.VISIBLE);
        actionButton.setText("dispatch".equals(action)
                ? R.string.dispatch_delivery : R.string.mark_as_delivered);
        actionButton.setOnClickListener(v -> performAction(d.id, action));
    }

    private void performAction(long id, String action) {
        actionButton.setEnabled(false);
        api.deliveryAction(action, id, new ApiCallback<DeliveryDetailData>() {
            @Override
            public void onSuccess(DeliveryDetailData data) {
                changed = true;
                setResult(RESULT_OK);
                int msg = "dispatch".equals(action)
                        ? R.string.delivery_dispatched_msg : R.string.delivery_delivered_msg;
                Toast.makeText(DeliveryDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                actionButton.setEnabled(true);
                if (data != null && data.delivery != null) {
                    bind(data.delivery);
                }
            }

            @Override
            public void onError(String message) {
                actionButton.setEnabled(true);
                Toast.makeText(DeliveryDetailActivity.this,
                        message != null ? message : getString(R.string.delivery_load_failed),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Raw status → action: ReadyForDelivery=dispatch, OutForDelivery=deliver, else none. */
    private static String actionFor(String rawStatus) {
        if (rawStatus == null) return null;
        switch (rawStatus) {
            case "ReadyForDelivery": return "dispatch";
            case "OutForDelivery":   return "deliver";
            default:                 return null;
        }
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
