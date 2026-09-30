package com.zybooks.project2cs_360;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Weight tracking screen.
 *
 * It no longer guesses at an identity from an intent extra that was never set.
 * It asks the session who is logged in, loads only that persons records, and
 * deletes by the ID carried on each WeightRecord.
 */
public class DataDisplayActivity extends AppCompatActivity {

    private GridView gridViewData;
    private Button buttonAddData, buttonDelete, buttonLogout;
    private TextView textViewSessionStatus;

    private DatabaseHelper databaseHelper;
    private UserSession session;

    private final List<WeightRecord> weightRecords = new ArrayList<>();
    private ArrayAdapter<WeightRecord> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_display);

        databaseHelper = new DatabaseHelper(this);
        session = UserSession.getInstance();

        // No session means this screen was reached without logging in.
        if (!session.isActive()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        gridViewData = findViewById(R.id.gridViewData);
        buttonAddData = findViewById(R.id.buttonAddData);
        buttonDelete = findViewById(R.id.buttonDelete);

        // Optional views: this is null safe so the activity still runs before the
        // layout is updated.
        buttonLogout = findViewById(R.id.buttonLogout);
        textViewSessionStatus = findViewById(R.id.textViewSessionStatus);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, weightRecords);
        gridViewData.setAdapter(adapter);

        buttonAddData.setOnClickListener(v -> showAddDataDialog());
        buttonDelete.setOnClickListener(v -> showDeleteDialog());
        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> logout());
        }

        showSessionStatus();
        loadRecords();
    }

    private void showSessionStatus() {
        if (textViewSessionStatus == null) {
            return;
        }
        if (session.isGuest()) {
            textViewSessionStatus.setText("Guest session - entries are cleared at logout");
        } else {
            textViewSessionStatus.setText("Signed in as " + session.getUsername());
        }
    }

    // Loads only the records that belong to the active session
    private void loadRecords() {
        weightRecords.clear();
        weightRecords.addAll(databaseHelper.getWeightRecords(session.getUserId()));
        adapter.notifyDataSetChanged();
    }

    private void showAddDataDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Weight Data");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText inputWeight = new EditText(this);
        inputWeight.setHint("Weight (e.g., 85 kg)");
        final EditText inputDate = new EditText(this);
        inputDate.setHint("Date (e.g., 2026-10-02)");
        layout.addView(inputWeight);
        layout.addView(inputDate);
        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String weight = inputWeight.getText().toString().trim();
            String date = inputDate.getText().toString().trim();

            if (weight.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please enter both weight and date",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // The record is tied to whoever is in the session right now.
            WeightRecord newRecord = new WeightRecord(weight, date, session.getUserId());
            WeightRecord saved = databaseHelper.insertWeightRecord(newRecord);

            if (saved == null) {
                Toast.makeText(this, "Could not save that entry", Toast.LENGTH_SHORT).show();
            } else {
                loadRecords();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteDialog() {
        if (weightRecords.isEmpty()) {
            Toast.makeText(this, "No data to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] displayItems = new String[weightRecords.size()];
        for (int i = 0; i < weightRecords.size(); i++) {
            displayItems[i] = weightRecords.get(i).getDisplayText();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Weight Data");
        builder.setItems(displayItems, (dialog, which) -> {
            // The selected object already knows its database ID; nothing is
            // parsed back out of the displayed text.
            WeightRecord selected = weightRecords.get(which);
            boolean deleted = databaseHelper.deleteWeightRecord(
                    selected.getId(), session.getUserId());

            Toast.makeText(this, deleted ? "Data deleted" : "Could not delete that entry",
                    Toast.LENGTH_SHORT).show();
            loadRecords();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Ends the session (while also clearing guest data) and returns to the login screen
    private void logout() {
        session.endSession(databaseHelper);
        finish();
    }
}