package com.hoaphat.pvt.repository.account;

import com.hoaphat.pvt.model.account.Role;
import org.springframework.data.repository.CrudRepository;

public interface IRoleRepository extends CrudRepository<Role, Integer> {
    Role findByRoleName(String roleName);
}
