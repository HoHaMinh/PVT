package com.hoaphat.pvt.repository.account;

import com.hoaphat.pvt.model.account.Account;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface IAccountRepository extends CrudRepository<Account, String> {
    Account findByAccountName(String accountName);
    @Query("select a from Account a order by a.accountName asc ")
    List<Account> findAll();
}
