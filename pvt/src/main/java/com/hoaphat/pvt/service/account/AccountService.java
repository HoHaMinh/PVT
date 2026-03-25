package com.hoaphat.pvt.service.account;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.account.AccountRole;
import com.hoaphat.pvt.model.account.Role;
import com.hoaphat.pvt.model.dto.UserDisplayDTO;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.account.IAccountRoleRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountService implements IAccountService {
    @Autowired
    private IAccountRepository accountRepository;
    @Autowired
    private IAccountRoleRepository accountRoleRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private IMonthEventRepository monthEventRepository;
    @Override
    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Override
    public void createAccount(String username, String password, String name, String roleName) {
        // ❌ check trùng username
        if (accountRepository.findByAccountName(username) != null) {
            throw new RuntimeException("Username đã tồn tại");
        }

        // ✅ encode password
        String encodedPassword = passwordEncoder.encode(password);

        // ✅ lưu account
        Account account = new Account(username, encodedPassword, name);
        accountRepository.save(account);

        // ✅ lấy role từ DB
        Role role = entityManager
                .createQuery("SELECT r FROM Role r WHERE r.roleName = :roleName", Role.class)
                .setParameter("roleName", roleName)
                .getSingleResult();

        // ✅ gán role
        AccountRole accountRole = new AccountRole();
        accountRole.setAccount(account);
        accountRole.setRole(role);

        accountRoleRepository.save(accountRole);
    }

    public List<UserDisplayDTO> getUserDisplayList() {
        List<Account> accounts = accountRepository.findAll();

        Map<String, List<Account>> map = new HashMap<>();

        for (Account acc : accounts) {
            String[] parts = acc.getName().split(" ");
            String lastName = parts[parts.length - 1];

            map.computeIfAbsent(lastName, k -> new ArrayList<>()).add(acc);
        }

        List<UserDisplayDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<Account>> entry : map.entrySet()) {
            List<Account> list = entry.getValue();

            for (Account acc : list) {
                String[] parts = acc.getName().split(" ");

                String display;

                if (list.size() == 1) {
                    display = parts[parts.length - 1];
                } else {
                    display = parts[parts.length - 2] + " " + parts[parts.length - 1];
                }

                result.add(new UserDisplayDTO(acc.getAccountName(), display));
            }
        }

        return result;
    }

    @Transactional
    public void deleteAccountFull(String accountName) {

        // 1. Xóa role trước
        accountRoleRepository.deleteByAccount_AccountName(accountName);

        // 2. Xóa công việc
        monthEventRepository.deleteByAccount_AccountName(accountName);

        // 3. Xóa account
        accountRepository.deleteById(accountName);
    }
}
