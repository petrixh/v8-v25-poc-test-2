package com.vaadin.tutorial.addressbook.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AB10 See another user's changes: two browser sessions on one app. Known pre-existing behaviour: BR-07 (one
 * shared list), BR-35 (a session's list changes only on its own save, filter change or reload).
 *
 * <p>Relative check: AB05 runs before and renames the third contact (Timothy Jones), so step 1 checks that both
 * users see the same first three, starting George White, Daniel Thompson, and step 3 compares with that. Adds
 * Shared Person at the top and renames Daniel Thompson to Thompsonne; it is the first test that adds a contact.
 */
@Order(7)
@DisplayName("AB10 See another user's changes")
class AB10OtherUsersChangesIT extends E2eTest {

    private static final String AFTER_FIRST_THREE = "Shared Person, George White, Daniel Thompsonne";

    @Test
    void otherUsersChanges() {
        AddressbookPage a = newUser();
        AddressbookPage b = newUser();
        AtomicReference<String> before = new AtomicReference<>();

        step("AB10", 1, "both users open the app: the same list, starting George White, Daniel Thompson", () -> {
            before.set(a.firstThree());
            assertTrue(before.get().startsWith("George White, Daniel Thompson, "), before.get());
            assertEquals(before.get(), b.firstThree());
        });

        step("AB10", 2, "user A renames Daniel Thompson and adds Shared Person", () -> {
            a.select("Daniel Thompson");
            a.setField("Last name", "Thompsonne");
            a.save();
            a.assertNotification("Saved 'Daniel Thompsonne'.");
            a.assertNoNotification();
            a.newContact();
            a.setField("First name", "Shared");
            a.setField("Last name", "Person");
            a.save();
            a.assertNotification("Saved 'Shared Person'.");
            assertEquals(AFTER_FIRST_THREE, a.firstThree());
        });

        step("AB10", 3, "user B, not reloaded: the list as before, no Shared Person (BR-35)", () -> {
            b.page.bringToFront();
            assertEquals(before.get(), b.firstThree());
            assertFalse(b.names().contains("Shared Person"), b.names().toString());
        });

        step("AB10", 4, "user B filters \"shared\": Shared Person is listed", () -> {
            b.filter("shared");
            assertTrue(b.names().contains("Shared Person"), b.names().toString());
        });

        step("AB10", 5, "user B clears the filter: the list starts Shared Person, George White, Daniel Thompsonne", () -> {
            b.filter("");
            assertEquals(AFTER_FIRST_THREE, b.firstThree());
        });

        step("AB10", 6, "user B reloads: the same list as user A's", () -> {
            b.reload();
            assertEquals(a.names(), b.names());
            assertEquals(AFTER_FIRST_THREE, b.firstThree());
        });
    }
}
