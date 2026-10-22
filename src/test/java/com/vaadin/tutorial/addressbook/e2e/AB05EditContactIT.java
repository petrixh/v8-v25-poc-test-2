package com.vaadin.tutorial.addressbook.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AB05 Edit a contact. Renames Timothy Jones to Timothy Jonesson (phone + 358 555 111). Runs after the tests
 * that check Timothy Jones and before the ones that add or rename contacts above him.
 */
@Order(6)
@DisplayName("AB05 Edit a contact")
class AB05EditContactIT extends E2eTest {

    @Test
    void editContact() {
        AddressbookPage app = newUser();

        step("AB05", 1, "select Timothy Jones: his details", () -> {
            app.select("Timothy Jones");
            app.assertFormShown();
            app.assertValues(Map.of("First name", "Timothy", "Last name", "Jones", "Phone", "+ 358 555 181",
                    "Email", "timothy@jones.com", "Birth date", "1/13/30"));
        });

        step("AB05", 2, "change Last name and Phone, Save: saved, Timothy Jonesson in the same place", () -> {
            app.setField("Last name", "Jonesson");
            app.setField("Phone", "+ 358 555 999");
            app.save();
            app.assertNotification("Saved 'Timothy Jonesson'.");
            app.assertFormHidden();
            assertEquals("Timothy Jonesson timothy@jones.com", app.rows().get(2).text());
        });

        step("AB05", 3, "select Timothy Jonesson: the changes, other fields unchanged", () -> {
            app.assertNoNotification();
            app.select("Timothy Jonesson");
            app.assertValues(Map.of("First name", "Timothy", "Last name", "Jonesson", "Phone", "+ 358 555 999",
                    "Email", "timothy@jones.com", "Birth date", "1/13/30"));
        });

        step("AB05", 4, "change Phone, Enter in the Phone field: saved as with the Save button", () -> {
            app.setField("Phone", "+ 358 555 111");
            app.page.keyboard().press("Enter");
            app.settle();
            app.assertNotification("Saved 'Timothy Jonesson'.");
            app.assertFormHidden();
        });

        step("AB05", 5, "reload, select Timothy Jonesson: phone 111; no new contact", () -> {
            app.reload();
            assertEquals("George White, Daniel Thompson, Timothy Jonesson", app.firstThree());
            app.select("Timothy Jonesson");
            app.assertValues(Map.of("Phone", "+ 358 555 111", "Email", "timothy@jones.com", "Birth date", "1/13/30"));
        });
    }
}
