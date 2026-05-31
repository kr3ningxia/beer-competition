package com.beercompetition.common.context;

public final class BaseContext {

    private static final ThreadLocal<SessionUser> SESSION_HOLDER = new ThreadLocal<>();

    private BaseContext() {
    }

    public static void setCurrentUser(SessionUser user) {
        SESSION_HOLDER.set(user);
    }

    public static SessionUser getCurrentUser() {
        return SESSION_HOLDER.get();
    }

    public static Long getCurrentId() {
        SessionUser user = SESSION_HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static String getCurrentRole() {
        SessionUser user = SESSION_HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static void clear() {
        SESSION_HOLDER.remove();
    }
}
