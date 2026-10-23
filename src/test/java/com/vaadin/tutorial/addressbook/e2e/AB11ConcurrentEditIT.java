package com.vaadin.tutorial.addressbook.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AB11 Two users edit the same contact: two browser sessions on one app. Known pre-existing behaviour: BR-07
 * (one shared list), BR-36 (the last save wins, with no warning).
 *
 * <p>Leaves Daniel Thompson with phone + 358 555 000 and his name and place unchanged, so the later tests'
 * checks of names and order still hold. Runs before AB10, which renames him.
 */
@Order(5)
@DisplayName("AB11 Two users edit the same contact")
class AB11ConcurrentEditIT extends E2eTest {

    @Test
    void concurrentEdit() {
        AddressbookPage a = newUser();
        AddressbookPage b = newUser();

        step("AB11", 1, "user A renames Daniel Thompson to Thompsonne and saves", () -> {
            a.select("Daniel Thompson");
            a.setField("Last name", "Thompsonne");
            a.save();
            a.assertNotification("Saved 'Daniel Thompsonne'.");
            assertTrue(a.names().contains("Daniel Thompsonne"), a.names().toString());
            assertFalse(a.names().contains("Daniel Thompson"), a.names().toString());
        });

        step("AB11", 2, "user B, not reloaded, selects Daniel Thompson: the old details", () -> {
            b.page.bringToFront();
            b.select("Daniel Thompson");
            b.assertFormShown();
            b.assertValues(Map.of("First name", "Daniel", "Last name", "Thompson", "Phone", "+ 358 555 265"));
        });

        step("AB11", 3, "user B changes the phone and saves: the usual message, no warning", () -> {
            b.setField("Phone", "+ 358 555 000");
            b.save();
            b.assertNotification("Saved 'Daniel Thompson'.");
            assertTrue(b.names().contains("Daniel Thompson"), b.names().toString());
        });

        step("AB11", 4, "user A reloads: Thompson again, phone 000; user A's change lost, no warning (BR-36)", () -> {
            a.page.bringToFront();
            a.reload();
            a.assertNoNotification();
            assertFalse(a.names().contains("Daniel Thompsonne"), a.names().toString());
            a.select("Daniel Thompson");
            a.assertValues(Map.of("Last name", "Thompson", "Phone", "+ 358 555 000"));
            // User B sees nothing but its own save message, if that is still shown.
            int shown = b.notifications().count();
            assertTrue(shown == 0 || (shown == 1 && b.notifications().innerText().equals("Saved 'Daniel Thompson'.")),
                    "user B: " + b.notifications().allInnerTexts());
        });
    }
}
