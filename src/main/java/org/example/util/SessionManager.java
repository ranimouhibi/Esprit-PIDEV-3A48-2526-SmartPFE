package org.example.util;

import org.example.dao.UserDAO;
import org.example.model.User;

public class SessionManager {
    private static User currentUser;

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }
    public static boolean isLoggedIn() { return currentUser != null; }

    /** Full logout: clears session, removes remember token from DB and disk. */
    public static void logout() {
        if (currentUser != null) {
            try { new UserDAO().clearRememberToken(currentUser.getId()); } catch (Exception ignored) {}
        }
        LocalSessionStore.clearSession();
        currentUser = null;
    }
}
