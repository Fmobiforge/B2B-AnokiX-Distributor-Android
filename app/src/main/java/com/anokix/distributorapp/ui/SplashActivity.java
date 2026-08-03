package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;

import androidx.appcompat.app.AppCompatActivity;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.messaging.PushManager;
import com.anokix.distributorapp.session.SessionManager;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        // Earliest point in the process: a push can arrive before MainActivity has ever
        // run, and a notification naming a channel that doesn't exist yet is drawn on
        // Firebase's fallback channel instead of ours.
        PushManager.ensureChannel(this);
        initView();
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
        System.exit(0);
    }

    private void initView() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        new Handler().postDelayed(() -> {
            // Already signed in → straight to the app; otherwise show the welcome screen.
            if (SessionManager.get(this).isLoggedIn()) {
                goToMainWithPush();
            } else {
                goToWelcome();
            }
        }, 2000);
    }

    /**
     * Opens the app, carrying any notification tap through to its destination.
     *
     * <p>The server sends a combined {@code notification} + {@code data} message, so while
     * the app is backgrounded or killed the system draws the tray notification and the tap
     * lands <em>here</em> — this is the launcher activity — with the payload as raw extras.
     * Handing them to {@link PushManager#routingIntent} rebuilds the same intent the
     * foreground path produces, so both states navigate identically. Without this a
     * background tap would open the dashboard and silently drop the destination.
     *
     * <p>A tap while signed out is discarded: the destination screens all need a session.
     */
    private void goToMainWithPush() {
        Intent launch = getIntent();
        if (PushManager.hasPushPayload(launch)) {
            startActivity(PushManager.routingIntent(this, PushManager.dataFromIntent(launch)));
            finish();
        } else {
            goToMain();
        }
    }
}
