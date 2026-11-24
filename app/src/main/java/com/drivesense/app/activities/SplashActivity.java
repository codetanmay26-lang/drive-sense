package com.drivesense.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import com.drivesense.app.R;
/**
 * SplashActivity — displayed on app launch for 2 seconds.
 * Navigates to LoginActivity or HomeActivity based on local saved login state.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (!prefs.contains("username")) {
                prefs.edit()
                        .putString("username", "Tanmay")
                        .putString("user_email", "tanmay@drivesense.demo")
                        .putString("user_password", "demo123")
                        .putString("user_id", "local_tanmay")
                        .putBoolean("logged_in", true)
                        .apply();
            }
            Intent intent;
            if (!prefs.getBoolean("landing_seen", false)) {
                intent = new Intent(SplashActivity.this, LandingActivity.class);
            } else {
                boolean loggedIn = prefs.getBoolean("logged_in", true);
                intent = new Intent(SplashActivity.this,
                        loggedIn ? HomeActivity.class : LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}
