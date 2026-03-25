package com.hoaphat.pvt.service;

import com.hoaphat.pvt.model.dto.UserTabOrder;
import com.hoaphat.pvt.repository.event.IUserTabOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserTabOrderService {
    @Autowired
    private IUserTabOrderRepository repository;

    public List<String> getOrderedTabs(String managerUsername) {
        return repository.findByManagerUsernameOrderByDisplayOrderAsc(managerUsername)
                .stream()
                .map(UserTabOrder::getAccountName)
                .collect(Collectors.toList());
    }

    public void saveOrder(String username, List<String> tabs) {

        // Xóa thứ tự cũ
        repository.deleteByManagerUsername(username);

        // Lưu thứ tự mới
        for (int i = 0; i < tabs.size(); i++) {
            UserTabOrder u = new UserTabOrder();
            u.setManagerUsername(username);
            u.setAccountName(tabs.get(i));
            u.setDisplayOrder(i);
            repository.save(u);
        }
    }
}
