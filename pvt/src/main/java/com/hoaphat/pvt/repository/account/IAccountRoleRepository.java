package com.hoaphat.pvt.repository.account;


import com.hoaphat.pvt.model.account.AccountRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface IAccountRoleRepository extends CrudRepository<AccountRole, Integer> {
    @Query("select ar.role.roleName from AccountRole ar where ar.account.accountName = :accountName")
    List<String> findAllRoleByUser(String accountName);

    void deleteByAccount_AccountName(String accountName);
}
