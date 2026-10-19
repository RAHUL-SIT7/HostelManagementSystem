package com.hostelmanagement.model;

public class User {

    private final long userId;
    private final String email;
    private final String role;
    private final String accountStatus;

    public User(long userId, String email, String role, String accountStatus) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.accountStatus = accountStatus;
    }

    public long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getAccountStatus() {
        return accountStatus;
    }
}
