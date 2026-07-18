package com.anokix.distributorapp.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.dto.LoginData;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;

/**
 * Shown when {@code POST api/common/login} returns {@code pending_approval=true}: the
 * distributor account has been registered but is still awaiting approval, so no token is
 * issued. Mirrors the web portal "Application Under Review" screen (see
 * newDesign/Login/Screenshot_6.png), adapted to the app's light theme. Every action here
 * (back link, button, back press) returns to {@link LoginActivity}.
 */
public class PendingApprovalActivity extends AppCompatActivity {

    private static final String EX_FIRST_NAME = "first_name";
    private static final String EX_BUSINESS = "business_name";
    private static final String EX_REFERENCE = "reference";
    private static final String EX_SUBMITTED = "submitted_at";
    private static final String EX_WALLET_ACTIVE = "wallet_active";

    private static final int[] NEXT_STEPS = {
            R.string.pending_next_1, R.string.pending_next_2, R.string.pending_next_3};

    /** Pack the fields the screen needs out of the login response. */
    public static Intent newIntent(Context context, LoginData data) {
        Intent intent = new Intent(context, PendingApprovalActivity.class);
        if (data.user != null) {
            intent.putExtra(EX_FIRST_NAME, data.user.first_name);
        }
        intent.putExtra(EX_BUSINESS, data.business_name);
        intent.putExtra(EX_REFERENCE, data.reference);
        intent.putExtra(EX_SUBMITTED, data.submitted_at);
        intent.putExtra(EX_WALLET_ACTIVE, isWalletActive(data.wallet));
        return intent;
    }

    /** A wallet counts as active only once it has been activated on the backend. */
    private static boolean isWalletActive(LoginData.Wallet wallet) {
        if (wallet == null) {
            return false;
        }
        boolean activated = !TextUtils.isEmpty(wallet.activated_at);
        boolean statusActive = "active".equalsIgnoreCase(wallet.status);
        return activated || statusActive;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approval);

        Intent i = getIntent();
        bindSubtitle(i.getStringExtra(EX_FIRST_NAME));
        setValue(R.id.valueBusiness, i.getStringExtra(EX_BUSINESS));
        setValue(R.id.valueReference, i.getStringExtra(EX_REFERENCE));
        setValue(R.id.valueSubmitted, i.getStringExtra(EX_SUBMITTED));
        bindWallet(i.getBooleanExtra(EX_WALLET_ACTIVE, false));
        buildNextSteps();

        findViewById(R.id.backLink).setOnClickListener(v -> backToLogin());
        findViewById(R.id.btnBackToLogin).setOnClickListener(v -> backToLogin());
        findViewById(R.id.supportEmail).setOnClickListener(v -> emailSupport());
    }

    private void bindSubtitle(String firstName) {
        TextView subtitle = findViewById(R.id.subtitle);
        if (TextUtils.isEmpty(firstName)) {
            subtitle.setText(R.string.pending_subtitle_no_name);
        } else {
            subtitle.setText(getString(R.string.pending_subtitle, firstName));
        }
    }

    private void setValue(int viewId, String value) {
        TextView view = findViewById(viewId);
        view.setText(TextUtils.isEmpty(value) ? "—" : value);
    }

    private void bindWallet(boolean active) {
        TextView pill = findViewById(R.id.valueWallet);
        if (active) {
            pill.setText(R.string.pending_wallet_active);
            pill.setBackgroundResource(R.drawable.bg_badge_success);
            pill.setTextColor(ContextCompat.getColor(this, R.color.success));
            findViewById(R.id.walletNote).setVisibility(android.view.View.GONE);
        } else {
            pill.setText(R.string.pending_wallet_not_activated);
            pill.setBackgroundResource(R.drawable.bg_pill_neutral);
            pill.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    /** Numbered "1. 2. 3." rows describing what happens after approval. */
    private void buildNextSteps() {
        LinearLayout container = findViewById(R.id.nextStepsContainer);
        for (int step = 0; step < NEXT_STEPS.length; step++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(7), 0, dp(7));

            RobotoBoldTextView number = new RobotoBoldTextView(this);
            number.setText((step + 1) + ".");
            number.setTextSize(14);
            number.setTextColor(ContextCompat.getColor(this, R.color.purple_primary));
            LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(dp(22),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            number.setLayoutParams(numLp);
            row.addView(number);

            RobotoTextView label = new RobotoTextView(this);
            label.setText(NEXT_STEPS[step]);
            label.setTextSize(14);
            label.setLineSpacing(dp(3), 1f);
            label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            label.setLayoutParams(lp);
            row.addView(label);

            container.addView(row);
        }
    }

    private void emailSupport() {
        String email = getString(R.string.support_email);
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_subject));
        try {
            startActivity(intent);
        } catch (Exception ignored) {
            // No email client available — nothing actionable to show.
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
