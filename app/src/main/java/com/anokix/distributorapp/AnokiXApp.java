package com.anokix.distributorapp;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Application entry point. Locks the app to light (day) mode so the UI renders
 * identically regardless of the phone's system dark/light appearance setting.
 * The palette is a fixed light design (see res/values/colors.xml), so night
 * resources are never appropriate.
 */
public class AnokiXApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
