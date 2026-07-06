package com.hoaphat.pvt.service.account;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.account.Role;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.account.IAccountRoleRepository;
import com.hoaphat.pvt.repository.account.IRoleRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private IAccountRepository accountRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private IAccountRoleRepository accountRoleRepository;

    @Mock
    private IMonthEventRepository monthEventRepository;

    @Mock
    private IResponseEventInformationRepository responseEventInformationRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void changePasswordUpdatesEncodedPasswordWhenCurrentPasswordIsCorrect() {
        Account account = new Account("dtinvn", "old-hash", "Đinh Văn Tín");
        when(accountRepository.findByAccountName("dtinvn")).thenReturn(account);
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        accountService.changePassword("dtinvn", "old-password", "new-password");

        assertEquals("new-hash", account.getPassword());
        verify(accountRepository).save(account);
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        Account account = new Account("dtinvn", "old-hash", "Đinh Văn Tín");
        when(accountRepository.findByAccountName("dtinvn")).thenReturn(account);
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> accountService.changePassword("dtinvn", "wrong-password", "new-password"));

        assertEquals("Mật khẩu hiện tại không đúng.", error.getMessage());
        verify(accountRepository, never()).save(account);
    }

    @Test
    void changePasswordRejectsShortNewPassword() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> accountService.changePassword("dtinvn", "old-password", "12345"));

        assertEquals("Mật khẩu mới phải có ít nhất 6 ký tự.", error.getMessage());
        verify(accountRepository, never()).findByAccountName("dtinvn");
    }

    @Test
    void createAccountRejectsMissingDisplayNameBeforeWriting() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount("new.user", "123456", null, "ROLE_EMPLOYEE", null));

        assertEquals("Họ và tên không được để trống.", error.getMessage());
        verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account.class));
    }

    @Test
    void createAccountRejectsMissingRoleBeforeWritingAccount() {
        when(accountRepository.findByAccountName("new.user")).thenReturn(null);
        when(roleRepository.findByRoleName("ROLE_EMPLOYEE")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount(
                        "new.user", "123456", "Nguyễn Văn Mới", "ROLE_EMPLOYEE", null));

        assertEquals("Chức vụ đã chọn không tồn tại.", error.getMessage());
        verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account.class));
        verify(accountRoleRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createAccountSavesAccountAndExistingRole() {
        Role role = new Role();
        role.setRoleName("ROLE_EMPLOYEE");
        when(accountRepository.findByAccountName("new.user")).thenReturn(null);
        when(roleRepository.findByRoleName("ROLE_EMPLOYEE")).thenReturn(role);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");

        accountService.createAccount(" new.user ", "123456", " Nguyễn  Văn  Mới ",
                "ROLE_EMPLOYEE", "8/3");

        verify(accountRepository).save(org.mockito.ArgumentMatchers.argThat(account ->
                "new.user".equals(account.getAccountName())
                        && "Nguyễn Văn Mới".equals(account.getName())
                        && "08/03".equals(account.getBirthday())
                        && "encoded".equals(account.getPassword())));
        verify(accountRoleRepository).save(org.mockito.ArgumentMatchers.argThat(accountRole ->
                accountRole.getRole() == role));
    }

    @Test
    void createAccountRejectsImpossibleBirthdayBeforeWriting() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount("new.user", "123456", "Nguyễn Văn Mới",
                        "ROLE_EMPLOYEE", "31/02"));

        assertEquals("Ngày sinh không hợp lệ. Vui lòng nhập theo định dạng DD/MM.",
                error.getMessage());
        verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account.class));
    }

    @Test
    void createAccountAcceptsLeapDayWithoutAYear() {
        Role role = new Role();
        role.setRoleName("ROLE_EMPLOYEE");
        when(accountRepository.findByAccountName("leap.user")).thenReturn(null);
        when(roleRepository.findByRoleName("ROLE_EMPLOYEE")).thenReturn(role);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");

        accountService.createAccount("leap.user", "123456", "Người Sinh Nhật",
                "ROLE_EMPLOYEE", "29/02");

        verify(accountRepository).save(org.mockito.ArgumentMatchers.argThat(account ->
                "29/02".equals(account.getBirthday())));
    }

    @Test
    void deleteAccountRemovesEveryResponseBeforeTasksAndAccount() {
        Account account = new Account("old.user", "ignored", "Nhân viên cũ");
        MonthEvent first = new MonthEvent();
        first.setMonthEventId(501);
        MonthEvent second = new MonthEvent();
        second.setMonthEventId(502);
        when(accountRepository.findByAccountName("old.user")).thenReturn(account);
        when(monthEventRepository.findByAccount_AccountName("old.user"))
                .thenReturn(Arrays.asList(first, second));

        accountService.deleteAccountFull("old.user");

        InOrder order = inOrder(responseEventInformationRepository, accountRoleRepository,
                monthEventRepository, accountRepository);
        order.verify(responseEventInformationRepository).deleteResponseById(501);
        order.verify(responseEventInformationRepository).deleteResponseById(502);
        order.verify(accountRoleRepository).deleteByAccount_AccountName("old.user");
        order.verify(monthEventRepository).deleteByAccount_AccountName("old.user");
        order.verify(accountRepository).deleteById("old.user");
    }
}
