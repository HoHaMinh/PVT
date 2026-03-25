package com.hoaphat.pvt.service.account;

import com.hoaphat.pvt.model.account.Account;

import java.util.List;

public interface IAccountService {
    List<Account> findAll();
    void createAccount(String username, String password, String name, String roleName);
}
