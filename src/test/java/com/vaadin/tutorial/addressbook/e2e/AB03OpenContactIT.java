package com.vaadin.tutorial.addressbook.e2e;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** AB03 Open a contact and close it again. Changes no data. */
@Order(3)
@DisplayName("AB03 Open a contact and close it again")
class AB03OpenContactIT extends E2eTest {

    @Test
    void openContact() {
        AddressbookPage app = newUser();

        step("AB03", 1, "select George White: row selected, form with his details, cursor in First name", () -> {
            Locator row = app.select("George White");
            app.assertSelected(row, true);
            app.assertFormShown();
            app.assertValues(Map.of("First name", "George", "Last name", "White", "Phone", "+ 358 555 853",
                    "Email", "george@white.com", "Birth date", "6/11/59"));
            assertThat(app.field("First name")).isFocused();
        });

        step("AB03", 2, "select Peter Wilson: the form shows his details", () -> {
            Locator row = app.select("Peter Wilson");
            app.assertSelected(row, true);
            app.assertFormShown();
            app.assertValues(Map.of("First name", "Peter", "Last name", "Wilson", "Phone", "+ 358 555 633",
                    "Email", "peter@wilson.com", "Birth date", "2/8/80"));
        });

        step("AB03", 3, "select Peter Wilson again: deselected, the form closes", () -> {
            Locator row = app.select("Peter Wilson");
            app.assertSelected(row, false);
            app.assertFormHidden();
        });
    }
}
