package com.drivesense.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.drivesense.app.R;
import com.drivesense.app.utils.BackNavigation;
/**
 * LoginActivity — local email/password authentication.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private Button loginButton;
    private TextView signUpLink;
    private ProgressBar progressBar;
    private CardView tanmayProfile;
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

        emailField   = findViewById(R.id.et_email);
        passwordField = findViewById(R.id.et_password);
        loginButton  = findViewById(R.id.btn_login);
        signUpLink   = findViewById(R.id.tv_sign_up);
        progressBar  = findViewById(R.id.progress_bar);
        tanmayProfile = findViewById(R.id.card_tanmay_profile);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.contains("user_email")) {
            prefs.edit()
                    .putString("username", "Tanmay")
                    .putString("user_email", "tanmay@drivesense.demo")
                    .putString("user_password", "demo123")
                    .putString("user_id", "local_tanmay")
                    .apply();
        }
        emailField.setText(prefs.getString("user_email", "tanmay@drivesense.demo"));
        passwordField.setText(prefs.getString("user_password", "demo123"));
        tanmayProfile.setOnClickListener(v -> {
            prefs.edit().putBoolean("logged_in", true).apply();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        loginButton.setOnClickListener(v -> attemptLogin());
        signUpLink.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
    }

    private void attemptLogin() {
        String email    = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(email)) { emailField.setError("Email is required"); return; }
        if (TextUtils.isEmpty(password)) { passwordField.setError("Password is required"); return; }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String savedEmail = prefs.getString("user_email", "");
        String savedPassword = prefs.getString("user_password", "");

        if (!savedEmail.equalsIgnoreCase(email) || !savedPassword.equals(password)) {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show();
            return;
        }

        prefs.edit().putBoolean("logged_in", true).apply();
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
