package ch.zhaw.trueyield.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioTest {

    // ── Valid case ────────────────────────────────────────────────────────────

    @Test
    void constructor_withValidName_createsPortfolio() {
        Portfolio portfolio = new Portfolio("ESG Global Fund", "manager-001");
        assertEquals("ESG Global Fund", portfolio.getName());
        assertEquals("manager-001", portfolio.getFundManagerId());
    }

    // ── Blank name tests ──────────────────────────────────────────────────────

    @Test
    void constructor_withNullName_throwsRuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new Portfolio(null, "manager-001"));
        assertEquals("Error: Portfolio name must not be blank.", ex.getMessage());
    }

    @Test
    void constructor_withEmptyName_throwsRuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new Portfolio("", "manager-001"));
        assertEquals("Error: Portfolio name must not be blank.", ex.getMessage());
    }

    @Test
    void constructor_withBlankName_throwsRuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new Portfolio("   ", "manager-001"));
        assertEquals("Error: Portfolio name must not be blank.", ex.getMessage());
    }

    // ── Too long name test ────────────────────────────────────────────────────

    @Test
    void constructor_withNameExceeding100Chars_throwsRuntimeException() {
        String longName = "A".repeat(101);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new Portfolio(longName, "manager-001"));
        assertEquals("Error: Portfolio name must not exceed 100 characters.", ex.getMessage());
    }
}
