package com.hoaphat.pvt.model.account;

import javax.persistence.*;

@Entity
public class Account {
    @Id
    @Column(nullable = false)
    private String accountName;

    @Column(nullable = false, length = 128)
    private String password;
    @Column(name = "name")
    private String name;

    public Account() {
    }

    public Account(String accountName, String password) {
        this.accountName = accountName;
        this.password = password;
    }

    public Account(String accountName, String password, String name) {
        this.accountName = accountName;
        this.password = password;
        this.name = name;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        String name = this.name; // ✅ dùng tên thật

        if (name == null || name.trim().isEmpty()) {
            return this.accountName; // fallback nếu thiếu
        }

        String[] parts = name.trim().split(" ");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + " " + parts[parts.length - 1];
        }
        return parts[parts.length - 1];
    }
}
