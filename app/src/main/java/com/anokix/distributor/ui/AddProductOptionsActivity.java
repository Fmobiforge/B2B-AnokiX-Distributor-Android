package com.anokix.distributor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.anokix.distributor.R;
import com.anokix.distributor.ui.views.RobotoBoldTextView;
import com.anokix.distributor.ui.views.RobotoTextView;

/**
 * "How would you like to add this product?" chooser, opened from the Products
 * screen's Add Product action (mirrors Create-Product1.jpg).
 *
 * Four options are shown but only "Create New Product" is wired today — the
 * barcode / library options depend on third-party services and arrive later.
 * Continue opens the {@link CreateProductActivity} wizard.
 */
public class AddProductOptionsActivity extends AppCompatActivity {

    private static final int OPTION_CREATE = 3;

    private final View[] cards = new View[4];
    private final ImageView[] radios = new ImageView[4];
    private int selected = OPTION_CREATE;

    // Launch the wizard for result and relay its RESULT_OK back to the Products
    // screen so the list refreshes after a product is created.
    private final ActivityResultLauncher<Intent> wizardLauncher =
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
        buildOptions();

        findViewById(R.id.btnContinue).setOnClickListener(v -> {
            if (selected == OPTION_CREATE) {
                wizardLauncher.launch(new Intent(this, CreateProductActivity.class));
            } else {
                Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildOptions() {
        LinearLayout container = findViewById(R.id.optionsContainer);
        int[] icons = {R.drawable.ic_products, R.drawable.ic_document,
                R.drawable.ic_search, R.drawable.ic_add};
        int[] titles = {R.string.opt_scan_barcode, R.string.opt_enter_barcode,
                R.string.opt_search_library, R.string.opt_create_product};
        int[] subs = {R.string.opt_scan_barcode_sub, R.string.opt_enter_barcode_sub,
                R.string.opt_search_library_sub, R.string.opt_create_product_sub};

        // A single-column list of full-width rows reads cleaner than a 2-col grid
        // (no ragged heights or wrapped titles). Each row is icon · text · radio.
        for (int i = 0; i < 4; i++) {
            final int index = i;
            View card = buildCard(icons[i], titles[i], subs[i], index);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                cardLp.topMargin = dp(12);
            }
            card.setLayoutParams(cardLp);
            card.setOnClickListener(v -> selectOption(index));
            cards[index] = card;
            container.addView(card);
        }
        applySelection();
    }

    private View buildCard(int iconRes, int titleRes, int subRes, int index) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setMinimumHeight(dp(72));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        FrameLayout iconBox = new FrameLayout(this);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconBox.setLayoutParams(boxLp);
        iconBox.setBackgroundResource(R.drawable.bg_circle_purple);

        ImageView icon = new ImageView(this);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(22), dp(22));
        iconLp.gravity = Gravity.CENTER;
        icon.setLayoutParams(iconLp);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.purple_primary));
        iconBox.addView(icon);
        card.addView(iconBox);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textsLp.setMarginStart(dp(14));
        textsLp.setMarginEnd(dp(12));
        texts.setLayoutParams(textsLp);

        RobotoBoldTextView title = new RobotoBoldTextView(this);
        title.setText(titleRes);
        title.setTextSize(15);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        texts.addView(title);

        RobotoTextView sub = new RobotoTextView(this);
        sub.setText(subRes);
        sub.setTextSize(12);
        sub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(3);
        sub.setLayoutParams(subLp);
        texts.addView(sub);

        card.addView(texts);

        ImageView radio = new ImageView(this);
        radio.setLayoutParams(new LinearLayout.LayoutParams(dp(22), dp(22)));
        radio.setImageResource(R.drawable.ic_radio_unselected);
        card.addView(radio);
        radios[index] = radio;

        return card;
    }

    private void selectOption(int index) {
        if (index != OPTION_CREATE) {
            Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
            return;
        }
        selected = index;
        applySelection();
    }

    private void applySelection() {
        for (int i = 0; i < cards.length; i++) {
            boolean isSelected = i == selected;
            cards[i].setBackgroundResource(isSelected
                    ? R.drawable.bg_trader_type_card_selected : R.drawable.bg_trader_type_card);
            radios[i].setImageResource(isSelected
                    ? R.drawable.ic_radio_selected : R.drawable.ic_radio_unselected);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
