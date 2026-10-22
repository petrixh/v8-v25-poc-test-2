package com.vaadin.tutorial.addressbook.e2e;

import com.vaadin.tutorial.addressbook.e2e.AddressbookPage.Row;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AB01 Browse the contact list. Changes no data. */
@Order(1)
@DisplayName("AB01 Browse the contact list")
class AB01BrowseContactsIT extends E2eTest {

    @Test
    void browseContacts() {
        AddressbookPage app = newUser();

        step("AB01", 1, "title, filter, New contact, columns, no form", () -> {
            assertThat(app.page).hasTitle("Addressbook");
            assertThat(app.filterField()).isVisible();
            assertThat(app.button("New contact")).isVisible();
            assertThat(app.columnHeaders()).hasCount(3);
            List<String> headers = List.of("First Name", "Last Name", "Email");
            for (int i = 0; i < headers.size(); i++) {
                assertThat(app.columnHeaders().nth(i))
                        .hasAccessibleName(Pattern.compile("(^|\\s)" + headers.get(i) + "$"));
            }
            app.assertFormHidden();
        });

        step("AB01", 2, "newest first: George White, Daniel Thompson, Timothy Jones", () -> {
            assertEquals("George White, Daniel Thompson, Timothy Jones", app.firstThree());
            assertEquals("George White george@white.com", app.firstRow().text());
        });

        step("AB01", 3, "the last contact is Peter Moore", () -> {
            app.scrollList(600);
            List<Row> rows = app.rows();
            Row last = rows.get(rows.size() - 1);
            assertEquals(app.contactCount(), last.position(), "the end of the list is reached");
            assertEquals("Peter Moore peter@moore.com", last.text());
            assertThat(last.locator()).isInViewport();
        });

        step("AB01", 4, "sort by last name: A to Z, starting with Anderson", () -> {
            app.scrollList(-600);
            app.sortBy("Last Name");
            List<String> lastNames = lastNames(app);
            assertEquals("Anderson", lastNames.get(0));
            assertTrue(ordered(lastNames, 1), "A to Z: " + lastNames);
        });

        step("AB01", 5, "sort again: Z to A, starting with Young", () -> {
            app.sortBy("Last Name");
            List<String> lastNames = lastNames(app);
            assertEquals("Young", lastNames.get(0));
            assertTrue(ordered(lastNames, -1), "Z to A: " + lastNames);
        });
    }

    private static List<String> lastNames(AddressbookPage app) {
        return app.rows().stream().map(r -> r.cells().get(1)).toList();
    }

    private static boolean ordered(List<String> values, int direction) {
        for (int i = 1; i < values.size(); i++) {
            if (direction * values.get(i - 1).compareTo(values.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }
}
