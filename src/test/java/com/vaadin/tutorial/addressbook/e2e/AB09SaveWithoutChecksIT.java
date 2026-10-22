package com.vaadin.tutorial.addressbook.e2e;

import com.vaadin.tutorial.addressbook.e2e.AddressbookPage.Row;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AB09 Save a contact with missing or malformed values. Known pre-existing behaviour: BR-23 (no field required,
 * email not checked), BR-11 (no delete). Adds a blank contact and No Email at the top; runs last, as the blank
 * contact would sort first by last name in AB01. Relative check: the contact that was first before (George White
 * on a fresh app) is second after the first save and third after the second.
 */
@Order(10)
@DisplayName("AB09 Save a contact with missing or malformed values")
class AB09SaveWithoutChecksIT extends E2eTest {

    @Test
    void saveWithoutChecks() {
        AddressbookPage app = newUser();
        AtomicReference<String> firstBefore = new AtomicReference<>(app.firstRow().text());

        step("AB09", 1, "New contact, Save with nothing entered: \"Saved ' '.\", a blank row at the top", () -> {
            app.newContact();
            app.assertFormShown();
            app.save();
            app.assertNotification("Saved ' '.");
            app.assertFormHidden();
            List<Row> rows = app.rows();
            assertTrue(rows.get(0).blank(), "first row blank: " + rows.get(0).text());
            assertEquals(firstBefore.get(), rows.get(1).text());
        });

        step("AB09", 2, "No Email with email \"not an email\": saved, at the top", () -> {
            app.assertNoNotification();
            app.newContact();
            app.setField("First name", "No");
            app.setField("Last name", "Email");
            app.setField("Email", "not an email");
            app.save();
            app.assertNotification("Saved 'No Email'.");
            assertEquals("No Email not an email", app.rows().get(0).text());
        });

        step("AB09", 3, "select the blank row (second): the form opens with all fields empty", () -> {
            app.assertNoNotification();
            List<Row> rows = app.rows();
            assertEquals("No Email not an email", rows.get(0).text());
            assertTrue(rows.get(1).blank(), "second row blank: " + rows.get(1).text());
            assertEquals(firstBefore.get(), rows.get(2).text());
            app.select(rows.get(1).locator());
            app.assertSelected(rows.get(1).locator(), true);
            app.assertFormShown();
            AddressbookPage.FIELDS.forEach(label -> assertThat(app.field(label)).hasValue(""));
        });
    }
}
