package com.anokix.distributor.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.anokix.distributor.R;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.ui.views.RobotoEditText;

/**
 * Dedicated "Forgot password" screen reached from the login form. Collects an email and
 * calls POST /api/common/forgot-password (portal_type=distributor). The backend always
 * returns a generic success, so on success we simply confirm the reset link was sent.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private RobotoEditText emailInput;
    private TextView submitButton;
    private View formContainer;
    private View successContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInput = findViewById(R.id.emailInput);
        submitButton = findViewById(R.id.submitButton);
        formContainer = findViewById(R.id.formContainer);
        successContainer = findViewById(R.id.successContainer);
        ImageButton close = findViewById(R.id.btnClose);
        TextView backToLogin = findViewById(R.id.backToLoginLink);
        TextView doneButton = findViewById(R.id.doneButton);

        close.setOnClickListener(v -> finish());
        backToLogin.setOnClickListener(v -> finish());
        doneButton.setOnClickListener(v -> finish());
        submitButton.setOnClickListener(v -> submit());

        emailInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
    }

    private void submit() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        if (email.isEmpty()) {
            Toast.makeText(this, R.string.enter_email_prompt, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.enter_valid_email, Toast.LENGTH_SHORT).show();
            return;
        }

        submitButton.setEnabled(false);
        submitButton.setText(R.string.sending);

        ApiClient.get(this).forgotPassword(email, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                showSuccess(email);
            }

            @Override
            public void onError(String message) {
                submitButton.setEnabled(true);
                submitButton.setText(R.string.send_reset_link);
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showSuccess(String email) {
        ((TextView) findViewById(R.id.successMessage))
                .setText(getString(R.string.reset_link_sent_message, email));
        formContainer.setVisibility(View.GONE);
        successContainer.setVisibility(View.VISIBLE);
    }
}
