package com.hoaphat.pvt.repository.account;

import com.hoaphat.pvt.model.account.Account;
import org.springframework.data.repository.CrudRepository;

public interface IAccountRepository extends CrudRepository<Account, String> {
    Account findByAccountName(String accountName);
}
