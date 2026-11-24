package com.drivesense.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.drivesense.app.R;

/**
 * LoginActivity — handles Admin & Driver authentication and quick role-based login.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private Button loginButton;
    private TextView signUpLink;
    private ProgressBar progressBar;
    private CardView cardAdminProfile, cardTanmayProfile, cardAyanProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            Intent intent = new Intent(this, LandingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        emailField = findViewById(R.id.et_email);
        passwordField = findViewById(R.id.et_password);
        loginButton = findViewById(R.id.btn_login);
        signUpLink = findViewById(R.id.tv_sign_up);
        progressBar = findViewById(R.id.progress_bar);

        cardAdminProfile = findViewById(R.id.card_admin_profile);
        cardTanmayProfile = findViewById(R.id.card_tanmay_profile);
        cardAyanProfile = findViewById(R.id.card_ayan_profile);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // 1. Admin Quick Login
        cardAdminProfile.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean("logged_in", true)
                    .putString("username", "Fleet Admin")
                    .putString("user_email", "admin@drivesense.app")
                    .putString("user_role", HomeActivity.ROLE_ADMIN)
                    .apply();
            startActivity(new Intent(this, AdminFleetDashboardActivity.class));
            finish();
        });

        // 2. Driver Tanmay Quick Login
        cardTanmayProfile.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean("logged_in", true)
                    .putString("username", "Tanmay")
                    .putString("user_email", "tanmay@drivesense.demo")
                    .putString("user_role", HomeActivity.ROLE_DRIVER)
                    .apply();
            startActivity(new Intent(this, DriverDashboardActivity.class));
            finish();
        });

        // 3. Driver Ayan Quick Login
        cardAyanProfile.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean("logged_in", true)
                    .putString("username", "Ayan")
                    .putString("user_email", "ayan@drivesense.demo")
                    .putString("user_role", HomeActivity.ROLE_DRIVER)
                    .apply();
            startActivity(new Intent(this, DriverDashboardActivity.class));
            finish();
        });

        loginButton.setOnClickListener(v -> attemptLogin());
        signUpLink.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
    }

    private void attemptLogin() {
        String input = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(input)) {
            emailField.setError("Email or User ID is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Password is required");
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Check for Admin credentials (ID: admin / admin123)
        if (("admin".equalsIgnoreCase(input) || "admin@drivesense.app".equalsIgnoreCase(input))
                && "admin123".equals(password)) {
            prefs.edit()
                    .putBoolean("logged_in", true)
                    .putString("username", "Fleet Admin")
                    .putString("user_role", HomeActivity.ROLE_ADMIN)
                    .apply();
            Toast.makeText(this, "Logged in as Fleet Admin", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AdminFleetDashboardActivity.class));
            finish();
            return;
        }

        // Check for Driver credentials
        if ("tanmay@drivesense.demo".equalsIgnoreCase(input) || "tanmay".equalsIgnoreCase(input)) {
            prefs.edit()
                    .putBoolean("logged_in", true)
                    .putString("username", "Tanmay")
                    .putString("user_role", HomeActivity.ROLE_DRIVER)
                    .apply();
            Toast.makeText(this, "Logged in as Driver (Tanmay)", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DriverDashboardActivity.class));
            finish();
            return;
        }

        if ("ayan@drivesense.demo".equalsIgnoreCase(input) || "ayan".equalsIgnoreCase(input)) {
            prefs.edit()
                    .putBoolean("logged_in", true)
                    .putString("username", "Ayan")
                    .putString("user_role", HomeActivity.ROLE_DRIVER)
                    .apply();
            Toast.makeText(this, "Logged in as Driver (Ayan)", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DriverDashboardActivity.class));
            finish();
            return;
        }

        // Default Driver Login
        String name = input.contains("@") ? input.split("@")[0] : input;
        prefs.edit()
                .putBoolean("logged_in", true)
                .putString("username", name)
                .putString("user_role", HomeActivity.ROLE_DRIVER)
                .apply();
        Toast.makeText(this, "Logged in as Driver (" + name + ")", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, DriverDashboardActivity.class));
        finish();
    }
}
