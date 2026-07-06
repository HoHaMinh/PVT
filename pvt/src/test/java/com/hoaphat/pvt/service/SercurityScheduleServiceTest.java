package com.hoaphat.pvt.service;

import com.hoaphat.pvt.model.event.SercuritySchedule;
import com.hoaphat.pvt.repository.event.ISercurityScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SercurityScheduleServiceTest {
    @Mock
    private ISercurityScheduleRepository repository;

    @InjectMocks
    private SercurityScheduleService service;

    @Test
    void getAllRemovesNullAndWhitespaceOnlyMessages() {
        SercuritySchedule empty = new SercuritySchedule(1, null, " ", "", null);
        SercuritySchedule content = new SercuritySchedule(2, "Nhắc lịch", null, "Kiểm tra hồ sơ", " ");
        when(repository.findAll()).thenReturn(Arrays.asList(null, empty, content));

        List<SercuritySchedule> result = service.getAll();

        assertEquals(1, result.size());
        assertSame(content, result.get(0));
    }
}
