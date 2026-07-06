package com.hoaphat.pvt.service.account;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.account.AccountRole;
import com.hoaphat.pvt.model.account.Role;
import com.hoaphat.pvt.model.dto.UserDisplayDTO;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.account.IAccountRoleRepository;
import com.hoaphat.pvt.repository.account.IRoleRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DateTimeException;
import java.time.MonthDay;
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
    @Autowired
    private IRoleRepository roleRepository;
    @Autowired
    private IMonthEventRepository monthEventRepository;
    @Autowired
    private IResponseEventInformationRepository responseEventInformationRepository;
    @Override
    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Override
    @Transactional
    public void createAccount(String username, String password, String name, String roleName,
                              String birthday) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username không được để trống.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự.");
        }
        if (password.length() > 72) {
            throw new IllegalArgumentException("Mật khẩu không được vượt quá 72 ký tự.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Chức vụ không được để trống.");
        }

        username = username.trim();
        name = name.trim().replaceAll("\\s+", " ");
        birthday = normalizeBirthday(birthday);

        if (accountRepository.findByAccountName(username) != null) {
            throw new IllegalArgumentException("Username đã tồn tại.");
        }

        Role role = roleRepository.findByRoleName(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Chức vụ đã chọn không tồn tại.");
        }

        String encodedPassword = passwordEncoder.encode(password);
        Account account = new Account(username, encodedPassword, name);
        account.setBirthday(birthday);
        accountRepository.save(account);
        AccountRole accountRole = new AccountRole();
        accountRole.setAccount(account);
        accountRole.setRole(role);

        accountRoleRepository.save(accountRole);
    }

    private String normalizeBirthday(String birthday) {
        if (birthday == null || birthday.trim().isEmpty()) {
            return null;
        }
        String[] parts = birthday.trim().split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Ngày sinh phải có định dạng DD/MM và không nhập năm.");
        }
        try {
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            MonthDay.of(month, day);
            return String.format("%02d/%02d", day, month);
        } catch (NumberFormatException | DateTimeException e) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ. Vui lòng nhập theo định dạng DD/MM.");
        }
    }

    @Override
    @Transactional
    public void changePassword(String accountName, String currentPassword, String newPassword) {
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Không xác định được tài khoản đang đăng nhập.");
        }
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại.");
        }
        if (currentPassword.length() > 72) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không được vượt quá 72 ký tự.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        if (newPassword.length() > 72) {
            throw new IllegalArgumentException("Mật khẩu mới không được vượt quá 72 ký tự.");
        }

        Account account = accountRepository.findByAccountName(accountName);
        if (account == null) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản đang đăng nhập.");
        }
        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }
        if (passwordEncoder.matches(newPassword, account.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }

    public List<UserDisplayDTO> getUserDisplayList() {
        List<Account> accounts = accountRepository.findAll();

        Map<String, List<Account>> map = new HashMap<>();

        for (Account acc : accounts) {
            String[] parts = getSafeAccountName(acc).split(" ");
            String lastName = parts[parts.length - 1];

            map.computeIfAbsent(lastName, k -> new ArrayList<>()).add(acc);
        }

        List<UserDisplayDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<Account>> entry : map.entrySet()) {
            List<Account> list = entry.getValue();

            for (Account acc : list) {
                String[] parts = getSafeAccountName(acc).split(" ");

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

    private String getSafeAccountName(Account account) {
        if (account.getName() == null || account.getName().trim().isEmpty()) {
            return account.getAccountName() == null ? "Không rõ" : account.getAccountName();
        }
        return account.getName().trim().replaceAll("\\s+", " ");
    }

    @Transactional
    public void deleteAccountFull(String accountName) {

        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tài khoản cần xóa.");
        }
        accountName = accountName.trim();
        if (accountRepository.findByAccountName(accountName) == null) {
            throw new IllegalArgumentException("Tài khoản cần xóa không còn tồn tại.");
        }

        // Tránh bulk DELETE qua quan hệ lồng nhau vì Hibernate/MySQL có thể tạo câu lệnh lỗi.
        // Lấy rõ từng công việc và xóa response theo khóa chính trước khi xóa MonthEvent.
        List<MonthEvent> assignedEvents = monthEventRepository.findByAccount_AccountName(accountName);
        for (MonthEvent event : assignedEvents) {
            if (event != null && event.getMonthEventId() != null) {
                responseEventInformationRepository.deleteResponseById(event.getMonthEventId());
            }
        }

        // 1. Xóa role trước
        accountRoleRepository.deleteByAccount_AccountName(accountName);

        // 2. Xóa công việc
        monthEventRepository.deleteByAccount_AccountName(accountName);

        // 3. Xóa account
        accountRepository.deleteById(accountName);
    }
}
