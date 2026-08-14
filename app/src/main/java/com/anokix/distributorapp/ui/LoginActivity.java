package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.LoginData;
import com.anokix.distributorapp.session.SessionManager;
import com.anokix.distributorapp.ui.views.RobotoEditText;

public class LoginActivity extends AppCompatActivity {

    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        RobotoEditText emailInput = findViewById(R.id.emailInput);
        RobotoEditText passwordInput = findViewById(R.id.passwordInput);
        passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());

        TextView loginButton = findViewById(R.id.loginButton);
        TextView tabEmail = findViewById(R.id.tabEmail);
        TextView tabPhone = findViewById(R.id.tabPhone);
        TextView forgotPassword = findViewById(R.id.forgotPassword);
        ImageButton togglePassword = findViewById(R.id.togglePassword);
        LinearLayout joinButton = findViewById(R.id.joinButton);
        LinearLayout emailForm = findViewById(R.id.emailForm);

        selectEmailTab(tabEmail, tabPhone);

        tabEmail.setOnClickListener(v -> {
            selectEmailTab(tabEmail, tabPhone);
            emailForm.setVisibility(View.VISIBLE);
        });

        tabPhone.setOnClickListener(v -> {
            selectPhoneTab(tabEmail, tabPhone);
            Toast.makeText(this, "Phone login coming soon", Toast.LENGTH_SHORT).show();
        });

        togglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                passwordInput.setTransformationMethod(null);
                togglePassword.setImageResource(R.drawable.ic_visibility_off);
            } else {
                passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePassword.setImageResource(R.drawable.ic_visibility);
            }
            passwordInput.setSelection(passwordInput.getText() != null ? passwordInput.getText().length() : 0);
        });

        forgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }
            doLogin(loginButton, email, password);
        });

        joinButton.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterDistributorActivity.class)));
    }

    private void doLogin(TextView loginButton, String email, String password) {
        loginButton.setEnabled(false);
        loginButton.setText(R.string.signing_in);

        ApiClient.get(this).login(email, password, new ApiCallback<LoginData>() {
            @Override
            public void onSuccess(LoginData data) {
                // Account awaiting approval: no token is issued — show the review screen
                // instead of proceeding into the app.
                if (data != null && data.pending_approval) {
                    startActivity(PendingApprovalActivity.newIntent(LoginActivity.this, data));
                    loginButton.setEnabled(true);
                    loginButton.setText(R.string.login_button);
                    return;
                }
                SessionManager session = SessionManager.get(LoginActivity.this);
                session.setToken(data.token);
                if (data.user != null) {
                    session.setUser(data.user.first_name, data.user.last_name,
                            data.user.email, data.user.role_label);
                    session.setPhone(data.user.phone_number);
                }
                if (data.portal_info != null) {
                    session.setCompany(data.portal_info.company_name,
                            data.portal_info.company_logo_url);
                }
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(String message) {
                loginButton.setEnabled(true);
                loginButton.setText(R.string.login_button);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void selectEmailTab(TextView tabEmail, TextView tabPhone) {
        tabEmail.setBackgroundResource(R.drawable.bg_login_tab_selected);
        tabEmail.setTextColor(getResources().getColor(R.color.purple_primary));
        tabPhone.setBackgroundResource(R.drawable.bg_login_tab_unselected);
        tabPhone.setTextColor(getResources().getColor(R.color.text_secondary));
    }

    private void selectPhoneTab(TextView tabEmail, TextView tabPhone) {
        tabPhone.setBackgroundResource(R.drawable.bg_login_tab_selected);
        tabPhone.setTextColor(getResources().getColor(R.color.purple_primary));
        tabEmail.setBackgroundResource(R.drawable.bg_login_tab_unselected);
        tabEmail.setTextColor(getResources().getColor(R.color.text_secondary));
    }
}
