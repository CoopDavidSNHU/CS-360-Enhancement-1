package com.zybooks.project2cs_360;

/**
 * This class stores the identity of whoever is currently using the app.
 *
 * Before this enhancement, activities had no reliable way to know who was
 * logged in, LoginActivity started DataDisplayActivity without passing any
 * checks, and DataDisplayActivity defaulted to a user ID of -1. Every
 * account (and every guest) read and wrote the same rows.
 *
 * A single session object now owns that answer. Exactly one session is active
 * at a time, and its created only by a successful login or by a guest login, and
 * is cleared on logout so the next account cannot get the previous
 * users state.
 */
public final class UserSession {

    // User ID used for rows that belong to a temporary guest session
    public static final int GUEST_USER_ID = -100;

    /// This means no one is logged in
    public static final int NO_USER_ID = -1;

    private static UserSession instance;

    private int userId = NO_USER_ID;
    private String username;
    private boolean guest;

    private UserSession() {
        // Created only through getInstance().
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Starts a session for a registered account.
     *
     * userID is the primary key from the user_data table
     * username is the account name, used only for display
     */
    public void startRegisteredSession(int userId, String username) {
        if (userId <= 0) {
            throw new IllegalArgumentException("A registered session needs a real user ID");
        }
        this.userId = userId;
        this.username = username;
        this.guest = false;
    }

    /**
     * Starts a temporary guest session. Guest data is stored under
     * guest_user_id and is deleted when the session ends.
     */
    public void startGuestSession() {
        this.userId = GUEST_USER_ID;
        this.username = "Guest";
        this.guest = true;
    }

    /**
     * Ends the current session. Temporary guest data is removed first so a
     * later guest never sees the previous guest's entries.
     *
     * databaseHelper is used to clear guest rows, could be null if the caller
     * only needs the in memory session cleared
     */
    public void endSession(DatabaseHelper databaseHelper) {
        if (guest && databaseHelper != null) {
            databaseHelper.clearGuestData();
        }
        userId = NO_USER_ID;
        username = null;
        guest = false;
    }

    // True when a registered user or a guest is currently logged in
    public boolean isActive() {
        return userId != NO_USER_ID;
    }

    public boolean isGuest() {
        return guest;
    }

    // True when the session belongs to a real account in user_data
    public boolean isRegisteredUser() {
        return isActive() && !guest;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}