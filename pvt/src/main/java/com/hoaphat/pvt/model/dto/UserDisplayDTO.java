package com.hoaphat.pvt.model.dto;

public class UserDisplayDTO {
    private String accountName;
    private String displayName;

    public UserDisplayDTO(String accountName, String displayName) {
        this.accountName = accountName;
        this.displayName = displayName;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getDisplayName() {
        return displayName;
    }
}