package com.vaadin.tutorial.addressbook.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AB02 Filter the contact list. Changes no data. Known pre-existing behaviour: BR-18 (hidden fields match). */
@Order(2)
@DisplayName("AB02 Filter the contact list")
class AB02FilterContactsIT extends E2eTest {

    private static final List<String> SMITHS =
            List.of("Alice Smith", "Dan Smith", "Daniel Smith", "Lisa Smith", "Mike Smith");

    @Test
    void filterContacts() {
        AddressbookPage app = newUser();
        AtomicReference<List<String>> fullList = new AtomicReference<>();

        step("AB02", 1, "\"smith\": the 5 Smiths", () -> {
            app.filter("smith");
            assertEquals(5, app.contactCount());
            assertEquals(SMITHS, app.names().stream().sorted().toList());
        });

        step("AB02", 2, "\"SMITH\": the same 5 (case ignored)", () -> {
            app.filter("SMITH");
            assertEquals(5, app.contactCount());
            assertEquals(SMITHS, app.names().stream().sorted().toList());
        });

        step("AB02", 3, "\"853\": only George White (phone matches, not shown)", () -> {
            app.filter("853");
            assertEquals(List.of("George White"), app.names());
        });

        step("AB02", 4, "\"zzz\": empty list, no message", () -> {
            app.filter("zzz");
            assertEquals(0, app.contactCount());
            assertEquals(List.of(), app.rows());
            app.assertNoNotification();
        });

        step("AB02", 5, "cleared: the full list is back", () -> {
            app.filter("");
            fullList.set(app.names());
            assertEquals("George White, Daniel Thompson, Timothy Jones", app.firstThree());
        });

        step("AB02", 6, "\"contact\": every contact is still listed (BR-18)", () -> {
            app.filter("contact");
            assertEquals("George White", app.names().get(0));
            assertEquals(fullList.get(), app.names());
        });

        step("AB02", 7, "open George White, then \"dan\": the form closes, only matches listed", () -> {
            app.filter("");
            app.select("George White");
            app.assertFormShown();
            app.filter("dan");
            app.assertFormHidden();
            List<String> names = app.names();
            assertTrue(names.contains("Dan Robinson"), names.toString());
            assertTrue(names.contains("Daniel Thompson"), names.toString());
            app.rows().forEach(r -> assertTrue(r.text().toLowerCase().contains("dan"), r.text()));
        });
    }
}
