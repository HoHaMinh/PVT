package com.hoaphat.pvt.service;
import com.hoaphat.pvt.model.dto.UserTabOrder;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.IUserTabOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@Service
@Transactional
public class UserTabOrderService {
    @Autowired private IUserTabOrderRepository repository;
    @Autowired private IAccountRepository accountRepository;
    public List<String> getOrderedTabs(String managerUsername) {
        if (managerUsername == null || managerUsername.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return repository.findByManagerUsernameOrderByDisplayOrderAsc(managerUsername).stream()
                .map(UserTabOrder::getAccountName)
                .filter(accountName -> accountName != null && !accountName.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
    public void saveOrder(String username, List<String> tabs) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Không xác định được quản lý đang lưu thứ tự tab.");
        }
        if (tabs == null) {
            throw new IllegalArgumentException("Danh sách thứ tự tab không được để trống.");
        }

        Set<String> uniqueTabs = new LinkedHashSet<>();
        for (String tab : tabs) {
            if (tab == null || tab.trim().isEmpty()) {
                throw new IllegalArgumentException("Danh sách thứ tự tab chứa tài khoản trống.");
            }
            String accountName = tab.trim();
            if (accountRepository.findByAccountName(accountName) == null) {
                throw new IllegalArgumentException("Tài khoản " + accountName + " không còn tồn tại.");
            }
            uniqueTabs.add(accountName);
        }

        List<String> validTabs = new ArrayList<>(uniqueTabs);
        repository.deleteByManagerUsername(username);
        for (int i = 0; i < validTabs.size(); i++) {
            UserTabOrder u = new UserTabOrder();
            u.setManagerUsername(username); u.setAccountName(validTabs.get(i)); u.setDisplayOrder(i);
            repository.save(u);
        }
    }
}
