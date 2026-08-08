package com.hoaphat.pvt.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseSummaryUiContractTest {

    @Test
    void privateSummaryRowUsesWarningBackgroundAndBrandTitleInBothRenderPaths() throws IOException {
        String template = resource("/templates/private.html");

        assertFalse(template.contains("table-primary align-middle response-summary-row"));
        assertTrue(count(template, "warning align-middle response-summary-row") >= 2);
        assertTrue(count(template, "response-summary-title") >= 2);
        assertTrue(template.contains("@media (prefers-reduced-motion: reduce)"));
    }

    @Test
    void responseSummaryTemplateProtectsCheckboxAndMobileLayout() throws IOException {
        String template = resource("/templates/responseSummary.html");

        assertTrue(template.contains(".summary-task-checkbox"));
        assertTrue(template.contains("flex: 0 0 1.125rem"));
        assertTrue(template.contains(".summary-toolbar h3"));
        assertTrue(template.contains("height: auto"));
        assertTrue(template.contains("summary-mobile-action-label"));
        assertTrue(template.contains("summary-back-button"));
    }

    @Test
    void bulkDeadlineDialogSupportsQuickShiftAndOneSharedManualDeadline() throws IOException {
        String template = resource("/templates/responseSummary.html");

        assertTrue(template.contains("name=\"deadlineMode\""));
        assertTrue(template.contains("value=\"SHIFT\""));
        assertTrue(template.contains("value=\"MANUAL_DEADLINE\""));
        assertTrue(template.contains("name=\"manualDeadline\""));
        assertTrue(template.contains("type=\"datetime-local\""));
        assertTrue(template.contains("manualDeadlineSection"));
        assertTrue(template.contains("renderDeadlinePreview"));
    }

    private String resource(String path) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Missing classpath resource: " + path);
            }
            try (Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
                return scanner.hasNext() ? scanner.next() : "";
            }
        }
    }

    private int count(String value, String needle) {
        int occurrences = 0;
        int from = 0;
        while ((from = value.indexOf(needle, from)) >= 0) {
            occurrences++;
            from += needle.length();
        }
        return occurrences;
    }
}
