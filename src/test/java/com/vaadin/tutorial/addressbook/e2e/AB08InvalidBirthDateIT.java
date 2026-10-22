package com.vaadin.tutorial.addressbook.e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * AB08 Try to save an invalid birth date. Known pre-existing behaviour: BR-13 (a failed save does nothing and
 * shows no message). Adds Bad Date at the top. Relative check: earlier tests add contacts, so steps 2 and 4
 * compare with the contact that was first before (George White on a fresh app).
 */
@Order(9)
@DisplayName("AB08 Try to save an invalid birth date")
class AB08InvalidBirthDateIT extends E2eTest {

    @Test
    void invalidBirthDate() {
        AddressbookPage app = newUser();
        AtomicReference<String> firstBefore = new AtomicReference<>(app.firstRow().text());

        step("AB08", 1, "New contact, Bad Date, birth date \"notadate\", leave the field: flagged as invalid", () -> {
            app.newContact();
            app.assertFormShown();
            app.setField("First name", "Bad");
            app.setField("Last name", "Date");
            app.setField("Birth date", "notadate");
            app.page.keyboard().press("Tab");
            app.settle();
            assertThat(app.field("Birth date")).hasValue("notadate");
            assertThat(app.field("Birth date")).hasAttribute("aria-invalid", "true");
        });

        step("AB08", 2, "Save: nothing happens, no message, nothing added (BR-13)", () -> {
            app.save();
            app.page.waitForTimeout(1500); // time for a notification to show, if one were coming
            assertThat(app.notifications()).hasCount(0);
            app.assertFormShown();
            app.assertValues(Map.of("First name", "Bad", "Last name", "Date", "Birth date", "notadate"));
            assertEquals(firstBefore.get(), app.firstRow().text());
            assertFalse(app.names().contains("Bad Date"), app.names().toString());
        });

        step("AB08", 3, "birth date 12/24/1985, leave the field: the flag goes, shows 12/24/85", () -> {
            app.setField("Birth date", "12/24/1985");
            app.page.keyboard().press("Tab");
            app.settle();
            assertThat(app.field("Birth date")).not().hasAttribute("aria-invalid", "true");
            assertThat(app.field("Birth date")).hasValue("12/24/85");
        });

        step("AB08", 4, "Save: saved, the form closes, Bad Date at the top with an empty email", () -> {
            app.save();
            app.assertNotification("Saved 'Bad Date'.");
            app.assertFormHidden();
            assertEquals(List.of("Bad", "Date", ""), app.rows().get(0).cells());
            assertEquals(firstBefore.get(), app.rows().get(1).text(), "only one contact was added");
        });

        step("AB08", 5, "select Bad Date: 12/24/85, Phone and Email empty; stored as 24 December 1985", () -> {
            app.assertNoNotification();
            app.select("Bad Date");
            app.assertFormShown();
            app.assertValues(Map.of("Birth date", "12/24/85", "Phone", "", "Email", ""));
            // The calendar opens on the stored date (1985, not 2085), with that day selected. Keyboard: Arrow Down.
            app.field("Birth date").click();
            app.page.keyboard().press("ArrowDown");
            // Calendar days are named "<day> <month> <year>, <weekday>"; the contact list's selected row is not.
            assertThat(app.page.getByRole(AriaRole.GRIDCELL, new Page.GetByRoleOptions().setSelected(true)
                    .setName(Pattern.compile("^\\d{1,2} \\w+ \\d{4}, \\w+$"))))
                    .hasAccessibleName("24 December 1985, Tuesday");
            app.page.keyboard().press("Escape");
            app.settle();
        });
    }
}
