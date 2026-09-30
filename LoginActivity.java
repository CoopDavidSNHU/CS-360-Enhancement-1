package com.zybooks.project2cs_360;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * This is how users enter the app. It now also is responsible for establishing who is using the app
 * before the weight screen opens, instead of opening that screen anonymously
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private Button buttonLogin, buttonCreateAccount, buttonGuestLogin, buttonSmsPermissions;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonCreateAccount = findViewById(R.id.buttonCreateAccount);
        buttonGuestLogin = findViewById(R.id.guestLogin);
        buttonSmsPermissions = findViewById(R.id.buttonSmsPermissions);
        databaseHelper = new DatabaseHelper(this);

        buttonLogin.setOnClickListener(v -> loginUser());
        buttonCreateAccount.setOnClickListener(v -> registerUser());
        buttonGuestLogin.setOnClickListener(v -> loginAsGuest());
        buttonSmsPermissions.setOnClickListener(v -> openSmsPermissionsActivity());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reaching the login screen means no one is logged in. Clearing here
        // covers logout, so the next person to sign
        // in cannot get the previous session.
        UserSession.getInstance().endSession(databaseHelper);
        editTextPassword.setText("");
    }

    // Registered login: validate, look up the ID, open the session
    private void loginUser() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter a username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = databaseHelper.getUserIdForCredentials(username, password);
        if (userId == UserSession.NO_USER_ID) {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            return;
        }

        UserSession.getInstance().startRegisteredSession(userId, username);
        openDataDisplayActivity();
    }

    // Guest login: creates a temporary session that is never tied to an account
    private void loginAsGuest() {
        UserSession.getInstance().startGuestSession();

        // Start clean so one guest never sees a previous guest's entries.
        databaseHelper.clearGuestData();

        Toast.makeText(this, "Guest session started. Entries are not saved.",
                Toast.LENGTH_SHORT).show();
        openDataDisplayActivity();
    }

    private void registerUser() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter a username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (databaseHelper.registerUser(username, password)) {
            Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Registration failed. That username may be taken.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void openDataDisplayActivity() {
        startActivity(new Intent(this, DataDisplayActivity.class));
    }

    private void openSmsPermissionsActivity() {
        startActivity(new Intent(this, SmsPermissionsActivity.class));
    }
}