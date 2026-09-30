package com.zybooks.project2cs_360;

/**
 * One row of the weight_data table as an object instead of a display string.
 *
 * The previous version kept weights in an ArrayList stored as
 * "85 kg (2026-10-02)" and got the database ID by splitting that string
 * back apart. Any weight containing a parenthesis, or two entries with the same
 * weight and date, broke that search. Carrying the ID on the object removes the
 * parsing step entirely.
 */
public class WeightRecord {

    // ID assigned by SQLite, will be NEW_RECORD_ID until the row has been inserted
    public static final int NEW_RECORD_ID = -1;

    private final int id;
    private final String weight;
    private final String date;
    private final int userId;

    // Existing record loaded from the database
    public WeightRecord(int id, String weight, String date, int userId) {
        this.id = id;
        this.weight = weight;
        this.date = date;
        this.userId = userId;
    }

    // New record that has not been saved yet
    public WeightRecord(String weight, String date, int userId) {
        this(NEW_RECORD_ID, weight, date, userId);
    }

    public int getId() {
        return id;
    }

    public String getWeight() {
        return weight;
    }

    public String getDate() {
        return date;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isSaved() {
        return id != NEW_RECORD_ID;
    }

    // Text shown in the grid, example: "85 kg (2026-10-02)"
    public String getDisplayText() {
        return weight + " (" + date + ")";
    }

    @Override
    public String toString() {
        // ArrayAdapter calls toString(), so the grid shows the display text.
        return getDisplayText();
    }
}