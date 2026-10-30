package com.hoaphat.pvt.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalReminderUiContractTest {
    @Test
    void personalReminderUsesProminentResponsiveCardsInsteadOfSmallText() throws Exception {
        String template = StreamUtils.copyToString(
                new ClassPathResource("templates/private.html").getInputStream(),
                StandardCharsets.UTF_8);

        assertTrue(template.contains("personal-reminder-item personal-reminder-item--price"));
        assertTrue(template.contains("personal-reminder-item personal-reminder-item--debt"));
        assertTrue(template.contains("personal-reminder-icon"));
        assertFalse(template.contains("small text-danger personal-daily-reminder"));
    }
}
