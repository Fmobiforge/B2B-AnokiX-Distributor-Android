package com.anokix.distributorapp.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.anokix.distributorapp.R;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }


    public static void setFullScreen(Activity activity){
        Window w = activity.getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }
    public void setTransparentStatusBar(Activity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
    public void hideKeyBoard(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }
    public void showMessages(int resID){
        Toast.makeText(this, getResources().getText(resID), Toast.LENGTH_LONG).show();
    }
    public void showMessages(String str){
        try {
            /*Toast toast = new Toast(this);
            View toast_view = LayoutInflater.from(this).inflate(R.layout.layout_toast, null);
            TextView tvMessage = toast_view.findViewById(R.id.tvMessage);
            tvMessage.setText(str);
            toast.setView(toast_view);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();*/
            Toast.makeText(this, str, Toast.LENGTH_LONG).show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void showLoadingDialog() {
        try {

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void hideLoadingDialog() {
        try {

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    protected void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    protected void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    protected void goToWelcome() {
        startActivity(new Intent(this, WelcomeActivity.class));
        finish();
    }

    protected ProgressDialog mProgress;

    public void showProgressDialog() {
        if (mProgress.isShowing())
            return;

        mProgress.show();
        mProgress.setContentView(R.layout.dialog_loading);
    }

    public void hideProgressDialog() {
        if (mProgress.isShowing())
            mProgress.dismiss();
    }

    // Remove EditText Keyboard
    public void hideKeyboard(EditText et) {
        if (et != null) {
            InputMethodManager imm = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
            }
        }
    }

    public void hideKeyboard() {
        InputMethodManager imm = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            imm = (InputMethodManager) BaseActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
            View view = BaseActivity.this.getCurrentFocus();
            if (view == null) {
                view = new View(BaseActivity.this);
            }
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /****** CHECK NETWORK CONNECTION *******/
    public static boolean isOnline(Context conn) {
        ConnectivityManager cm = (ConnectivityManager) conn.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }

        return false;
    }
}
