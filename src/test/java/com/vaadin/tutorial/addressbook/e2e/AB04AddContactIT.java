package com.vaadin.tutorial.addressbook.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AB04 Add a new contact. Adds Maria Virtanen at the top. Relative check: AB10 runs before and adds Shared
 * Person, so step 3 checks that the contact that was first before the save is now second (George White on a
 * fresh app).
 */
@Order(8)
@DisplayName("AB04 Add a new contact")
class AB04AddContactIT extends E2eTest {

    private static final Map<String, String> MARIA = new LinkedHashMap<>();

    static {
        MARIA.put("First name", "Maria");
        MARIA.put("Last name", "Virtanen");
        MARIA.put("Phone", "+ 358 555 123");
        MARIA.put("Email", "maria@virtanen.fi");
    }

    @Test
    void addContact() {
        AddressbookPage app = newUser();
        AtomicReference<String> firstBefore = new AtomicReference<>(app.firstRow().text());

        step("AB04", 1, "New contact: an empty form, cursor in First name", () -> {
            app.newContact();
            app.assertFormShown();
            AddressbookPage.FIELDS.forEach(label -> assertThat(app.field(label)).hasValue(""));
            assertThat(app.field("First name")).isFocused();
        });

        step("AB04", 2, "enter Maria Virtanen, birth date 3/15/1980: shows 3/15/80 after leaving the field", () -> {
            MARIA.forEach(app::setField);
            app.setField("Birth date", "3/15/1980");
            app.page.keyboard().press("Tab");
            app.settle();
            assertThat(app.field("Birth date")).hasValue("3/15/80");
            app.assertValues(MARIA);
        });

        step("AB04", 3, "Save: saved, the form closes, Maria Virtanen at the top", () -> {
            app.save();
            app.assertNotification("Saved 'Maria Virtanen'.");
            app.assertFormHidden();
            assertEquals("Maria Virtanen maria@virtanen.fi", app.rows().get(0).text());
            assertEquals(firstBefore.get(), app.rows().get(1).text(), "the previous first contact is second");
        });

        step("AB04", 4, "select Maria Virtanen: exactly the values entered", () -> {
            app.assertNoNotification();
            app.select("Maria Virtanen");
            app.assertFormShown();
            app.assertValues(MARIA);
            assertThat(app.field("Birth date")).hasValue("3/15/80");
        });

        step("AB04", 5, "reload, filter \"virtanen\": only Maria Virtanen", () -> {
            app.reload();
            app.filter("virtanen");
            assertEquals(List.of("Maria Virtanen maria@virtanen.fi"), app.rows().stream().map(r -> r.text()).toList());
        });
    }
}
