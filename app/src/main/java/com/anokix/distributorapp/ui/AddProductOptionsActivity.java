package com.anokix.distributorapp.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.HcLibraryData;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;

/**
 * "How would you like to add this product?" — the portal's add-method chooser
 * (/products/create), opened from the Products screen's Add Product action.
 *
 * Methods: Enter or Scan Barcode (default) · Create New Product · Hirt &amp; Carter Library.
 * The library card only appears once a one-row library request confirms the integration is
 * available; until then a placeholder card holds its place. While the first catalogue sync is
 * still running the card is shown disabled, and once ready it carries the product count.
 */
public class AddProductOptionsActivity extends AppCompatActivity {

    private static final String METHOD_BARCODE = "manual";
    private static final String METHOD_CREATE = "create_new";
    private static final String METHOD_LIBRARY = "hc_library";

    private LinearLayout container;
    private String selected = METHOD_BARCODE;

    // Library card state: loading → (hidden | shown); shown cards are ready or still preparing.
    private boolean libraryLoading = true;
    private boolean libraryShown;
    private boolean libraryReady;
    private int libraryTotal;

    // Every downstream screen ends with RESULT_OK once a product is saved; pass it on to the
    // Products screen so its list refreshes, and close the chooser.
    private final ActivityResultLauncher<Intent> flowLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    setResult(RESULT_OK);
                    finish();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product_options);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        container = findViewById(R.id.optionsContainer);
        findViewById(R.id.btnContinue).setOnClickListener(v -> onContinue());

        renderOptions();
        loadLibraryInfo();
    }

    /** One-row library request: tells us whether to show the card, its readiness and size. */
    private void loadLibraryInfo() {
        ApiClient.get(this).getHcLibrary("", "", false, 1, 1, new ApiCallback<HcLibraryData>() {
            @Override
            public void onSuccess(HcLibraryData data) {
                if (isFinishing() || isDestroyed()) return;
                libraryLoading = false;
                if (data != null && data.available) {
                    libraryShown = true;
                    libraryReady = data.library_ready;
                    libraryTotal = data.total;
                }
                renderOptions();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                libraryLoading = false;   // the card simply stays hidden, as on the portal
                renderOptions();
            }
        });
    }

    private void onContinue() {
        switch (selected) {
            case METHOD_CREATE:
                flowLauncher.launch(new Intent(this, CreateProductActivity.class));
                break;
            case METHOD_LIBRARY:
                flowLauncher.launch(new Intent(this, HcLibraryActivity.class));
                break;
            default:
                flowLauncher.launch(new Intent(this, BarcodeVerifyActivity.class));
                break;
        }
    }

    // ---- Cards -------------------------------------------------------------

    private void renderOptions() {
        container.removeAllViews();
        addCard(METHOD_BARCODE, R.drawable.ic_barcode, getString(R.string.opt_barcode),
                getString(R.string.opt_barcode_sub), null, false);
        addCard(METHOD_CREATE, R.drawable.ic_add_circle_outline, getString(R.string.opt_create_product),
                getString(R.string.opt_create_product_sub), null, false);
        if (libraryShown) {
            boolean disabled = !libraryReady;
            String badge = null;
            if (libraryReady) {
                badge = libraryTotal > 0
                        ? getString(R.string.hc_badge_products, AddProductFlow.count(libraryTotal))
                        : getString(R.string.hc_badge_new);
            }
            addCard(METHOD_LIBRARY, R.drawable.ic_library_book, getString(R.string.opt_hc_library),
                    getString(disabled ? R.string.opt_hc_library_preparing : R.string.opt_hc_library_sub),
                    badge, disabled);
        } else if (libraryLoading) {
            container.addView(buildSkeletonCard(), cardParams());
        }
    }

    private void addCard(String key, int iconRes, String title, String description,
                         @Nullable String badge, boolean disabled) {
        boolean isSelected = !disabled && key.equals(selected);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackgroundResource(disabled ? R.drawable.bg_hc_card_muted
                : isSelected ? R.drawable.bg_hc_card_selected : R.drawable.bg_hc_card);
        if (disabled) {
            card.setAlpha(0.6f);
        } else {
            card.setOnClickListener(v -> {
                selected = key;
                renderOptions();
            });
        }

        FrameLayout iconBox = new FrameLayout(this);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(dp(52), dp(52)));
        iconBox.setBackgroundResource(isSelected ? R.drawable.bg_hc_icon_selected : R.drawable.bg_hc_icon);
        ImageView icon = new ImageView(this);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(26), dp(26));
        iconLp.gravity = Gravity.CENTER;
        icon.setLayoutParams(iconLp);
        icon.setImageResource(iconRes);
        icon.setColorFilter(color(isSelected ? R.color.hc_violet : R.color.hc_slate));
        iconBox.addView(icon);
        card.addView(iconBox);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bodyLp.setMarginStart(dp(16));
        body.setLayoutParams(bodyLp);

        RobotoBoldTextView titleView = new RobotoBoldTextView(this);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(color(isSelected ? R.color.hc_violet : R.color.text_primary));
        body.addView(titleView);

        if (badge != null) {
            RobotoBoldTextView badgeView = new RobotoBoldTextView(this);
            badgeView.setText(badge);
            badgeView.setTextSize(11);
            badgeView.setTextColor(color(R.color.hc_violet_dark));
            badgeView.setBackgroundResource(R.drawable.bg_hc_badge);
            badgeView.setPadding(dp(8), dp(1), dp(8), dp(1));
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            badgeLp.topMargin = dp(4);
            badgeView.setLayoutParams(badgeLp);
            body.addView(badgeView);
        }

        RobotoTextView descView = new RobotoTextView(this);
        descView.setText(description);
        descView.setTextSize(13);
        descView.setLineSpacing(0, 1.2f);
        descView.setTextColor(color(R.color.text_secondary));
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descLp.topMargin = dp(4);
        descView.setLayoutParams(descLp);
        body.addView(descView);

        card.addView(body);
        container.addView(card, cardParams());
    }

    /** Placeholder for the library card while its availability is being checked. */
    private View buildSkeletonCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackgroundResource(R.drawable.bg_hc_card);
        card.setContentDescription(getString(R.string.opt_hc_library));

        View icon = new View(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(52), dp(52)));
        icon.setBackgroundResource(R.drawable.bg_hc_icon);
        card.addView(icon);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bodyLp.setMarginStart(dp(16));
        body.setLayoutParams(bodyLp);
        body.addView(skeletonBar(0.62f, 16));
        body.addView(skeletonBar(0.34f, 16));
        body.addView(skeletonBar(0.92f, 11));
        body.addView(skeletonBar(0.64f, 11));
        card.addView(body);

        ObjectAnimator pulse = ObjectAnimator.ofFloat(card, View.ALPHA, 1f, 0.5f);
        pulse.setDuration(700);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        card.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                pulse.start();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                pulse.cancel();
            }
        });
        return card;
    }

    /** A grey bar {@code widthFraction} of the card body wide. */
    private View skeletonBar(float widthFraction, int heightDp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp));
        rowLp.bottomMargin = dp(8);
        row.setLayoutParams(rowLp);
        row.setWeightSum(1f);

        View bar = new View(this);
        bar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, widthFraction));
        bar.setBackgroundResource(R.drawable.bg_hc_thumb_well);
        row.addView(bar);
        return row;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (container.getChildCount() > 0) {
            lp.topMargin = dp(14);
        }
        return lp;
    }

    private int color(int res) {
        return ContextCompat.getColor(this, res);
    }

    private int dp(int v) {
        return AddProductFlow.dp(this, v);
    }
}
