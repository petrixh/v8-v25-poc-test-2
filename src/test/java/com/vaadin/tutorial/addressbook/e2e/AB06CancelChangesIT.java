package com.vaadin.tutorial.addressbook.e2e;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AB06 Cancel or abandon changes. Changes no data, and runs before every test that does: its checks are
 * against the seed contacts. Known pre-existing behaviour: BR-16 (Cancel on a new contact with no row selected
 * leaves the form open).
 */
@Order(4)
@DisplayName("AB06 Cancel or abandon changes")
class AB06CancelChangesIT extends E2eTest {

    private static final String SEED_TOP = "George White, Daniel Thompson, Timothy Jones";

    @Test
    void cancelChanges() {
        AddressbookPage app = newUser();

        step("AB06", 1, "change First name, Cancel: \"Cancelled\", form closes, list unchanged", () -> {
            app.select("Timothy Jones");
            app.assertFormShown();
            app.setField("First name", "Changed");
            app.cancel();
            app.assertNotification("Cancelled");
            app.assertFormHidden();
            assertEquals("Timothy Jones", app.names().get(2));
            assertTrue(app.names().stream().noneMatch(n -> n.startsWith("Changed ")));
        });

        step("AB06", 2, "select Timothy Jones again: the change was not kept", () -> {
            app.assertNoNotification();
            app.select("Timothy Jones");
            app.assertFormShown();
            app.assertValues(Map.of("First name", "Timothy", "Last name", "Jones"));
        });

        step("AB06", 3, "change First name, select Peter Wilson without saving: the form shows Peter Wilson", () -> {
            app.setField("First name", "Unsaved");
            app.select("Peter Wilson");
            app.assertFormShown();
            app.assertValues(Map.of("First name", "Peter", "Last name", "Wilson", "Email", "peter@wilson.com"));
        });

        step("AB06", 4, "select Timothy Jones again: the unsaved change was dropped", () -> {
            app.select("Timothy Jones");
            app.assertFormShown();
            assertThat(app.field("First name")).hasValue("Timothy");
            assertEquals("Timothy Jones", app.names().get(2));
        });

        step("AB06", 5, "no row selected, New contact, Temp, Cancel: \"Cancelled\", form stays open (BR-16)", () -> {
            Locator row = app.select("Timothy Jones");
            app.assertSelected(row, false);
            app.assertFormHidden();
            app.newContact();
            app.assertFormShown();
            app.setField("First name", "Temp");
            app.cancel();
            app.assertNotification("Cancelled");
            app.assertFormShown();
            assertThat(app.field("First name")).hasValue("Temp");
            assertEquals(SEED_TOP, app.firstThree());
            assertTrue(app.names().stream().noneMatch(n -> n.startsWith("Temp ")));
        });
    }
}
