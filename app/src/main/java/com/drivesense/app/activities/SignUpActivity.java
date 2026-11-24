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

import com.drivesense.app.R;
import com.drivesense.app.utils.BackNavigation;
/**
 * SignUpActivity — local account registration.
 */
public class SignUpActivity extends AppCompatActivity {

    private EditText emailField, passwordField, usernameField;
    private Button signUpButton;
    private TextView loginLink;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        BackNavigation.attach(this);

        usernameField = findViewById(R.id.et_username);
        emailField    = findViewById(R.id.et_email);
        passwordField = findViewById(R.id.et_password);
        signUpButton  = findViewById(R.id.btn_signup);
        loginLink     = findViewById(R.id.tv_login);
        progressBar   = findViewById(R.id.progress_bar);

        signUpButton.setOnClickListener(v -> attemptSignUp());
        loginLink.setOnClickListener(v -> finish());
    }

    private void attemptSignUp() {
        String username = usernameField.getText().toString().trim();
        String email    = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(username)) { usernameField.setError("Username is required"); return; }
        if (TextUtils.isEmpty(email))    { emailField.setError("Email is required"); return; }
        if (password.length() < 6)       { passwordField.setError("Minimum 6 characters"); return; }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit()
                .putString("user_id", "local_user")
                .putString("username", username)
                .putString("user_email", email)
                .putString("user_password", password)
                .putBoolean("logged_in", true)
                .apply();

        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
