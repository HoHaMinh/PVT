package com.hoaphat.pvt.repository.event;

import com.hoaphat.pvt.model.dto.UserTabOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IUserTabOrderRepository extends JpaRepository<UserTabOrder, Integer> {

    List<UserTabOrder> findByManagerUsernameOrderByDisplayOrderAsc(String managerUsername);

    void deleteByManagerUsername(String managerUsername);
}
