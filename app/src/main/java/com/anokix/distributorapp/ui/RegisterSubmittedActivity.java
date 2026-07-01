package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;

/**
 * Confirmation screen shown after a successful distributor registration.
 * Mirrors Register_submitted.png: a success check, a "What happens next?"
 * timeline, the estimated approval window, and a Back to Login action.
 */
public class RegisterSubmittedActivity extends AppCompatActivity {

    private static final int[] NEXT_ICONS = {
            R.drawable.ic_document, R.drawable.ic_shield,
            R.drawable.ic_person, R.drawable.ic_wallet};
    private static final int[] NEXT_LABELS = {
            R.string.next_review_docs, R.string.next_kyc,
            R.string.next_distributor_approval, R.string.next_wallet_activation};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_submitted);

        buildNextSteps();
        findViewById(R.id.btnBackToLogin).setOnClickListener(v -> backToLogin());
    }

    private void buildNextSteps() {
        LinearLayout container = findViewById(R.id.nextStepsContainer);
        for (int i = 0; i < NEXT_LABELS.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));

            // Icon tile with a numbered badge.
            android.widget.FrameLayout tile = new android.widget.FrameLayout(this);
            tile.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
            tile.setBackgroundResource(R.drawable.bg_next_icon);

            ImageView icon = new ImageView(this);
            android.widget.FrameLayout.LayoutParams iconLp =
                    new android.widget.FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER);
            icon.setLayoutParams(iconLp);
            icon.setImageResource(NEXT_ICONS[i]);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.purple_primary));
            tile.addView(icon);

            RobotoBoldTextView badge = new RobotoBoldTextView(this);
            android.widget.FrameLayout.LayoutParams badgeLp =
                    new android.widget.FrameLayout.LayoutParams(dp(18), dp(18),
                            Gravity.TOP | Gravity.END);
            badge.setLayoutParams(badgeLp);
            badge.setGravity(Gravity.CENTER);
            badge.setText(String.valueOf(i + 1));
            badge.setTextSize(9);
            badge.setTextColor(0xFFFFFFFF);
            badge.setBackgroundResource(R.drawable.bg_step_circle_active);
            tile.addView(badge);

            row.addView(tile);

            RobotoBoldTextView label = new RobotoBoldTextView(this);
            label.setText(NEXT_LABELS[i]);
            label.setTextSize(12);
            label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = dp(14);
            label.setLayoutParams(lp);
            row.addView(label);

            container.addView(row);
        }
    }

    private void backToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        backToLogin();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
