package com.anokix.distributor.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.anokix.distributor.R;
import com.anokix.distributor.model.OrderFormat;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.OrderDetailData;
import com.anokix.distributor.network.dto.OrdersData;
import com.bumptech.glide.Glide;

import java.util.Locale;

/**
 * Order detail / tracking screen (mirrors the *-Details design mocks): an order header,
 * a vertical status timeline (Order Placed → Accepted → Picking → Packing → Ready for
 * Delivery → Out for Delivery → Delivered, or a cancelled note), the trader details, and
 * the ordered items with a subtotal/total. Fetched by id from
 * {@code api/distributor/orders?id=}.
 */
public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "id";

    private static final String[] STAGE_KEYS = {
            "pending", "accepted", "picking", "packing", "ready_for_delivery",
            "out_for_delivery", "delivered"
    };
    private static final String[] STAGE_LABELS = {
            "Order Placed", "Accepted", "Picking", "Packing", "Ready for Delivery",
            "Out for Delivery", "Delivered"
    };

    private static final String[] LOGO_COLORS = {
            "#EC4899", "#7C3AED", "#2563EB", "#0891B2", "#16A34A", "#EA580C", "#D97706"
    };

    private final ApiClient api = ApiClient.get(this);

    private View scroll, progress;
    private final String currency = "R";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        scroll = findViewById(R.id.detailScroll);
        progress = findViewById(R.id.detailProgress);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        load(getIntent().getStringExtra(EXTRA_ID));
    }

    private void load(String id) {
        progress.setVisibility(View.VISIBLE);
        scroll.setVisibility(View.GONE);
        api.getOrderById(id, new ApiCallback<OrderDetailData>() {
            @Override
            public void onSuccess(OrderDetailData data) {
                progress.setVisibility(View.GONE);
                if (data == null || data.order == null) {
                    Toast.makeText(OrderDetailActivity.this, R.string.order_load_failed,
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                scroll.setVisibility(View.VISIBLE);
                bind(data.order);
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
                Toast.makeText(OrderDetailActivity.this,
                        message != null ? message : getString(R.string.order_load_failed),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bind(OrdersData.Order o) {
        String name = o.traderName();

        TextView logo = findViewById(R.id.detailLogo);
        logo.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.US));
        ViewCompat.setBackgroundTintList(logo,
                ColorStateList.valueOf(Color.parseColor(logoColor(o.trader_id))));

        ((TextView) findViewById(R.id.detailOrderNumber)).setText(o.order_number);
        ((TextView) findViewById(R.id.detailTrader)).setText(name);
        ((TextView) findViewById(R.id.detailAmount)).setText(OrderFormat.money(o.total_amount, currency));
        ((TextView) findViewById(R.id.detailItemsCount))
                .setText(getString(R.string.items_count, o.item_count));

        TextView statusBadge = findViewById(R.id.detailStatus);
        int color = OrderFormat.statusColor(o.statusKey());
        statusBadge.setText(OrderFormat.humanize(o.statusKey()));
        statusBadge.setTextColor(color);
        ViewCompat.setBackgroundTintList(statusBadge,
                ColorStateList.valueOf(Color.argb(28, Color.red(color), Color.green(color), Color.blue(color))));

        bindTrader(o);
        buildTimeline(o);
        buildItems(o);
        bindNotes(o);
    }

    private void bindTrader(OrdersData.Order o) {
        ((TextView) findViewById(R.id.detailTraderName)).setText(o.traderName());
        TextView address = findViewById(R.id.detailTraderAddress);
        TextView phone = findViewById(R.id.detailTraderPhone);
        if (o.trader != null) {
            address.setText(safe(o.trader.location));
            address.setVisibility(isEmpty(o.trader.location) ? View.GONE : View.VISIBLE);
            final String number = o.trader.phone;
            phone.setText(safe(number));
            phone.setVisibility(isEmpty(number) ? View.GONE : View.VISIBLE);
            phone.setOnClickListener(isEmpty(number) ? null : v -> dialPhone(number));
        } else {
            address.setVisibility(View.GONE);
            phone.setVisibility(View.GONE);
        }
    }

    /** Open the dialer pre-filled with the trader's number (ACTION_DIAL needs no permission). */
    private void dialPhone(String number) {
        if (isEmpty(number)) return;
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number.trim())));
        } catch (Exception e) {
            Toast.makeText(this, R.string.order_call_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void bindNotes(OrdersData.Order o) {
        TextView notes = findViewById(R.id.detailNotes);
        if (isEmpty(o.notes)) {
            notes.setVisibility(View.GONE);
        } else {
            notes.setVisibility(View.VISIBLE);
            notes.setText(getString(R.string.order_note_prefix, o.notes));
        }
    }

    // ---- Timeline --------------------------------------------------------

    private void buildTimeline(OrdersData.Order o) {
        LinearLayout timeline = findViewById(R.id.timeline);
        timeline.removeAllViews();

        String statusKey = o.statusKey();
        boolean cancelled = "cancelled".equalsIgnoreCase(statusKey)
                || "failed".equalsIgnoreCase(statusKey);
        int currentIndex = indexOf(statusKey);
        int density = (int) getResources().getDisplayMetrics().density;

        if (cancelled) {
            // Order Placed (done) then a red terminal node.
            addStageRow(timeline, STAGE_LABELS[0], stageSubtitle(0, o), null, true, false, false, true, density);
            boolean failed = "failed".equalsIgnoreCase(statusKey);
            addTerminalRow(timeline,
                    failed ? getString(R.string.order_failed_stage) : getString(R.string.order_cancelled_stage),
                    failed ? getString(R.string.order_failed_note) : getString(R.string.order_cancelled_note),
                    density);
            return;
        }

        for (int i = 0; i < STAGE_LABELS.length; i++) {
            boolean done = i < currentIndex;
            boolean current = i == currentIndex;
            boolean terminalDone = current && "delivered".equals(statusKey);
            boolean last = i == STAGE_LABELS.length - 1;
            // The active (non-terminal) stage gets a purple descriptive caption, matching the mocks.
            String caption = (current && !terminalDone) ? stageCaption(i) : null;
            addStageRow(timeline, STAGE_LABELS[i], stageSubtitle(i, o), caption,
                    done || terminalDone, current && !terminalDone, !last, done, density);
        }
    }

    /** Purple status description shown under the current stage (null where the mocks show none). */
    private String stageCaption(int stageIndex) {
        switch (stageIndex) {
            case 1: return getString(R.string.order_stage_caption_accepted);
            case 2: return getString(R.string.order_stage_caption_picking);
            case 3: return getString(R.string.order_stage_caption_packing);
            case 4: return getString(R.string.order_stage_caption_ready);
            case 5: return getString(R.string.order_stage_caption_out_for_delivery);
            default: return null;  // Order Placed shows only the date; Delivered shows only the date.
        }
    }

    private void addStageRow(LinearLayout timeline, String label, String sub, String caption,
                             boolean done, boolean current, boolean hasLine, boolean lineDone,
                             int density) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams leftLp =
                new LinearLayout.LayoutParams(24 * density, LinearLayout.LayoutParams.WRAP_CONTENT);
        leftLp.setMarginEnd(14 * density);
        left.setLayoutParams(leftLp);

        left.addView(buildCircle(done, current, false, density));
        if (hasLine) {
            View line = new View(this);
            line.setLayoutParams(new LinearLayout.LayoutParams(2 * density, 30 * density));
            line.setBackgroundColor(lineDone ? 0xFF16A34A : 0xFFE2E8F0);
            left.addView(line);
        }

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setPadding(0, density, 0, 0);

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextSize(12);
        boolean reached = done || current;
        title.setTextColor(reached ? Color.parseColor("#0F172A") : Color.parseColor("#94A3B8"));
        if (current) title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        right.addView(title);

        if (sub != null) {
            TextView date = new TextView(this);
            date.setText(sub);
            date.setTextSize(11);
            date.setTextColor(Color.parseColor("#64748B"));
            date.setPadding(0, density, 0, 0);
            right.addView(date);
        }

        if (caption != null) {
            TextView captionView = new TextView(this);
            captionView.setText(caption);
            captionView.setTextSize(12);
            captionView.setTextColor(Color.parseColor("#7C3AED"));
            captionView.setPadding(0, density, 0, 0);
            right.addView(captionView);
        }

        row.addView(left);
        row.addView(right);
        timeline.addView(row);
    }

    /** Red terminal node used for cancelled / failed orders. */
    private void addTerminalRow(LinearLayout timeline, String label, String note, int density) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams leftLp =
                new LinearLayout.LayoutParams(24 * density, LinearLayout.LayoutParams.WRAP_CONTENT);
        leftLp.setMarginEnd(14 * density);
        left.setLayoutParams(leftLp);

        View circle = new View(this);
        circle.setLayoutParams(new LinearLayout.LayoutParams(24 * density, 24 * density));
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(0xFFDC2626);
        circle.setBackground(dot);
        left.addView(circle);

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setPadding(0, density, 0, 0);

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextSize(14);
        title.setTextColor(Color.parseColor("#0F172A"));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        right.addView(title);

        TextView noteView = new TextView(this);
        noteView.setText(note);
        noteView.setTextSize(12);
        noteView.setTextColor(0xFFDC2626);
        noteView.setPadding(0, density, 0, 0);
        right.addView(noteView);

        row.addView(left);
        row.addView(right);
        timeline.addView(row);
    }

    /** Date/time text shown under a stage, or null if none. */
    private String stageSubtitle(int stageIndex, OrdersData.Order o) {
        switch (stageIndex) {
            case 0:
                return OrderFormat.createdAt(o.created_at);
            case 5: {
                String date = OrderFormat.deliveryDate(o.delivery_date);
                if (date.isEmpty()) return null;
                String window = timeWindow(o);
                return date + (window.isEmpty() ? "" : ", " + window);
            }
            case 6: {
                String date = OrderFormat.deliveryDate(o.delivery_date);
                if (date.isEmpty()) return null;
                String window = timeWindow(o);
                return date + (window.isEmpty() ? "" : ", " + window);
            }
            default:
                return null;
        }
    }

    private String timeWindow(OrdersData.Order o) {
        String start = OrderFormat.trimSeconds(o.delivery_start_time);
        String end = OrderFormat.trimSeconds(o.delivery_end_time);
        if (start.isEmpty() && end.isEmpty()) return "";
        return start + "–" + end;
    }

    private View buildCircle(boolean done, boolean current, boolean unused, int density) {
        int size = 24 * density;
        if (done) {
            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new LinearLayout.LayoutParams(size, size));
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(0xFF16A34A);
            iv.setBackground(bg);
            int pad = 5 * density;
            iv.setPadding(pad, pad, pad, pad);
            iv.setImageResource(R.drawable.ic_check_white);
            iv.setColorFilter(Color.WHITE);
            return iv;
        }
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(Color.WHITE);
        ring.setStroke((current ? 3 : 2) * density, current ? 0xFF7C3AED : 0xFFCBD5E1);
        v.setBackground(ring);
        return v;
    }

    // ---- Items -----------------------------------------------------------

    private void buildItems(OrdersData.Order o) {
        LinearLayout container = findViewById(R.id.itemsContainer);
        container.removeAllViews();
        int count = o.items != null ? o.items.size() : 0;
        ((TextView) findViewById(R.id.itemsHeader)).setText(getString(R.string.items_header, count));

        ((TextView) findViewById(R.id.detailSubtotal))
                .setText(OrderFormat.money(o.subtotal > 0 ? o.subtotal : o.total_amount, currency));
        ((TextView) findViewById(R.id.detailTotal)).setText(OrderFormat.money(o.total_amount, currency));

        if (o.items == null) return;
        LayoutInflater inflater = LayoutInflater.from(this);
        for (OrdersData.Item item : o.items) {
            View row = inflater.inflate(R.layout.item_order_detail_product, container, false);
            ImageView image = row.findViewById(R.id.itemImage);
            ((TextView) row.findViewById(R.id.itemQty)).setText("x" + item.quantity);
            ((TextView) row.findViewById(R.id.itemName)).setText(item.name != null ? item.name : "");
            ((TextView) row.findViewById(R.id.itemPrice)).setText(
                    item.quantity + " × " + OrderFormat.money(item.unit_price, currency));
            ((TextView) row.findViewById(R.id.itemLineTotal))
                    .setText(OrderFormat.money(item.line_total, currency));
            if (item.image_url != null && !item.image_url.isEmpty()) {
                Glide.with(image).load(item.image_url).centerCrop().into(image);
            }
            container.addView(row);
        }
    }

    // ---- Helpers ---------------------------------------------------------

    private int indexOf(String status) {
        for (int i = 0; i < STAGE_KEYS.length; i++) {
            if (STAGE_KEYS[i].equalsIgnoreCase(status)) return i;
        }
        return 0;
    }

    private static String logoColor(long traderId) {
        return LOGO_COLORS[(int) (Math.abs(traderId) % LOGO_COLORS.length)];
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
