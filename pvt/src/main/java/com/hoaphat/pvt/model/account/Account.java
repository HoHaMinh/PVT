package com.hoaphat.pvt.model.account;

import javax.persistence.*;

@Entity
public class Account {
    @Id
    @Column(nullable = false)
    private String accountName;

    @Column(nullable = false, length = 128)
    private String password;

    public Account() {
    }

    public Account(String accountName, String password) {
        this.accountName = accountName;
        this.password = password;
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
}
