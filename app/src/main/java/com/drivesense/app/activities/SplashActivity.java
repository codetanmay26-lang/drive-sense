package com.drivesense.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import com.drivesense.app.R;

/**
 * SplashActivity — displayed on app launch.
 * Clears auto-login state and navigates directly to LoginActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            // Clear previous session so Login Screen ALWAYS opens on app launch
            prefs.edit()
                    .putBoolean("logged_in", false)
                    .putString("username", "Tanmay")
                    .putString("user_email", "tanmay@drivesense.demo")
                    .putString("user_password", "demo123")
                    .putString("user_role", HomeActivity.ROLE_DRIVER)
                    .apply();

            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}
