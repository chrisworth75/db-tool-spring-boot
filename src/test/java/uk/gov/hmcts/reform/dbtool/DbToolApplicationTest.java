package uk.gov.hmcts.reform.dbtool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DbToolApplicationTest {

    @Test
    void testMainMethod() {
        // Just verify the class can be instantiated and main exists
        assertDoesNotThrow(() -> new DbToolApplication(null, null));
    }

    @Test
    void testApplicationContext() {
        // Verify the application class structure
        assertNotNull(DbToolApplication.class);
        assertDoesNotThrow(() -> DbToolApplication.class.getDeclaredMethod("main", String[].class));
        assertDoesNotThrow(() -> DbToolApplication.class.getDeclaredMethod("commandLineRunner"));
    }
}
