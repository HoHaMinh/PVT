package com.hoaphat.pvt.service;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.UserTabOrder;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.IUserTabOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTabOrderServiceTest {
    @Mock
    private IUserTabOrderRepository repository;

    @Mock
    private IAccountRepository accountRepository;

    @InjectMocks
    private UserTabOrderService service;

    @Test
    void saveOrderRejectsNullBodyBeforeDeletingOldOrder() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveOrder("Hồ Hà Minh", null));

        assertEquals("Danh sách thứ tự tab không được để trống.", error.getMessage());
        verify(repository, never()).deleteByManagerUsername("Hồ Hà Minh");
    }

    @Test
    void saveOrderRemovesDuplicatesAndKeepsFirstPosition() {
        when(accountRepository.findByAccountName("dtinvn"))
                .thenReturn(new Account("dtinvn", "ignored"));
        when(accountRepository.findByAccountName("dtrongtv"))
                .thenReturn(new Account("dtrongtv", "ignored"));

        service.saveOrder("Hồ Hà Minh", Arrays.asList("dtinvn", "dtrongtv", "dtinvn"));

        ArgumentCaptor<UserTabOrder> captor = ArgumentCaptor.forClass(UserTabOrder.class);
        verify(repository, times(2)).save(captor.capture());
        List<UserTabOrder> saved = captor.getAllValues();
        assertEquals("dtinvn", saved.get(0).getAccountName());
        assertEquals(0, saved.get(0).getDisplayOrder());
        assertEquals("dtrongtv", saved.get(1).getAccountName());
        assertEquals(1, saved.get(1).getDisplayOrder());
    }

    @Test
    void saveOrderRejectsUnknownAccountBeforeDeletingOldOrder() {
        when(accountRepository.findByAccountName("missing")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveOrder("Hồ Hà Minh", Arrays.asList("missing")));

        assertEquals("Tài khoản missing không còn tồn tại.", error.getMessage());
        verify(repository, never()).deleteByManagerUsername("Hồ Hà Minh");
    }
}
