package com.vaadin.tutorial.addressbook.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * The app's one screen as a user sees it: the filter field, the "New contact" button, the contact list and the
 * contact form. Everything is found by role, label or visible text.
 *
 * <p>The contact list is a Vaadin grid: it shows its cells through slots, so a row's text is read from its
 * accessible name and its cells' names (aria snapshot), and rows are ordered by {@code aria-rowindex}, because
 * the grid reuses row elements while it scrolls and their page order is not the screen order.
 */
final class AddressbookPage {

    static final List<String> FIELDS = List.of("First name", "Last name", "Phone", "Email", "Birth date");

    private static final Pattern CELL = Pattern.compile("^\\s*- gridcell(?: \"((?:[^\"\\\\]|\\\\.)*)\")?");

    final Page page;

    AddressbookPage(Page page) {
        this.page = page;
    }

    /** A row of the contact list: its 1-based position in the list and its cell texts. */
    record Row(int position, List<String> cells, Locator locator) {
        /** "First Last". */
        String name() {
            return cells.get(0) + " " + cells.get(1);
        }

        /** The cell texts joined, as a user reads the row: "George White george@white.com". */
        String text() {
            return String.join(" ", cells).replaceAll("\\s+", " ").trim();
        }

        boolean blank() {
            return cells.stream().allMatch(String::isEmpty);
        }
    }

    // ---- page ----

    void open(String baseUrl) {
        page.navigate(baseUrl + "/");
        waitForList();
    }

    void reload() {
        page.reload();
        waitForList();
    }

    private void waitForList() {
        assertThat(grid()).isVisible();
        assertThat(grid().getByRole(AriaRole.GRIDCELL).first()).isVisible();
        settle();
    }

    /** Waits until the app has answered the last user action (no server round trip in progress). */
    void settle() {
        page.waitForTimeout(200);
        page.waitForFunction("() => { const c = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients;"
                + " return !c || !Object.values(c).some(x => x && x.isActive && x.isActive()); }", null,
                new Page.WaitForFunctionOptions().setPollingInterval(100).setTimeout(10_000));
    }

    // ---- filter and buttons ----

    Locator filterField() {
        return page.getByRole(AriaRole.TEXTBOX).and(page.getByPlaceholder("Filter contacts..."));
    }

    /** Replaces the filter text by typing it, as a user does ("" clears the field); waits for the list. */
    void filter(String text) {
        Locator f = filterField();
        f.click();
        f.fill("");
        if (!text.isEmpty()) {
            f.pressSequentially(text, new Locator.PressSequentiallyOptions().setDelay(60));
        }
        page.waitForTimeout(700); // the list follows the text after a short pause in typing
        settle();
    }

    Locator button(String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    void newContact() {
        button("New contact").click();
        settle();
    }

    void save() {
        button("Save").click();
        settle();
    }

    void cancel() {
        button("Cancel").click();
        settle();
    }

    // ---- contact list ----

    Locator grid() {
        return page.getByRole(AriaRole.TREEGRID).or(page.getByRole(AriaRole.GRID)).first();
    }

    Locator columnHeaders() {
        return grid().getByRole(AriaRole.COLUMNHEADER);
    }

    /** Clicks a column header's label: sorts by that column, a second click reverses the order. */
    void sortBy(String header) {
        // The label is slotted into the header cell from outside the grid's table, so it is found on the page.
        page.getByText(header, new Page.GetByTextOptions().setExact(true)).click();
        settle();
    }

    /** Number of contacts in the list (not only the ones on screen). */
    int contactCount() {
        return Integer.parseInt(grid().getAttribute("aria-rowcount")) - 1; // minus the header row
    }

    /** The rows the list has rendered (the ones on screen and a few around them), in list order. */
    List<Row> rows() {
        Map<Integer, Row> byPosition = new TreeMap<>();
        Locator all = grid().getByRole(AriaRole.ROW);
        int count = all.count();
        int contacts = contactCount();
        for (int i = 0; i < count; i++) {
            Locator row = all.nth(i);
            String index = row.getAttribute("aria-rowindex");
            if (index == null || row.getByRole(AriaRole.COLUMNHEADER).count() > 0) {
                continue;
            }
            int position = Integer.parseInt(index) - 1;
            if (position < 1 || position > contacts) {
                continue;
            }
            List<String> cells = new ArrayList<>();
            for (String line : row.ariaSnapshot().split("\n")) {
                Matcher m = CELL.matcher(line);
                if (m.find()) {
                    cells.add(m.group(1) == null ? "" : m.group(1).replaceAll("\\\\(.)", "$1"));
                }
            }
            if (cells.size() >= 2) {
                byPosition.put(position, new Row(position, cells, row));
            }
        }
        return new ArrayList<>(byPosition.values());
    }

    List<String> names() {
        return rows().stream().map(Row::name).toList();
    }

    /** "First Last" of the first three contacts, comma separated. */
    String firstThree() {
        List<String> names = names();
        return String.join(", ", names.subList(0, Math.min(3, names.size())));
    }

    Row firstRow() {
        return rows().get(0);
    }

    /** The row of the contact named "First Last" (its accessible name starts with the name). */
    Locator row(String name) {
        return grid().getByRole(AriaRole.ROW, new Locator.GetByRoleOptions()
                .setName(Pattern.compile("^" + jsQuote(name) + "(\\s|$)")));
    }

    /** Escapes regex characters the way JavaScript regexes accept (Playwright matches names in the browser). */
    private static String jsQuote(String text) {
        return text.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
    }

    Locator select(String name) {
        Locator row = row(name);
        select(row);
        return row;
    }

    void select(Locator row) {
        row.getByRole(AriaRole.GRIDCELL).first().click();
        settle();
    }

    void assertSelected(Locator row, boolean selected) {
        assertThat(row).hasAttribute("aria-selected", String.valueOf(selected));
    }

    /** Scrolls the list with the mouse wheel until it stops moving (dy > 0: down). */
    void scrollList(int dy) {
        BoundingBox box = grid().boundingBox();
        page.mouse().move(box.x + box.width / 2, box.y + box.height / 2);
        String last = "";
        for (int i = 0; i < 40; i++) {
            page.mouse().wheel(0, dy);
            page.waitForTimeout(250);
            settle();
            String now = rows().stream().map(r -> r.position() + " " + r.text()).toList().toString();
            if (now.equals(last)) {
                return;
            }
            last = now;
        }
    }

    // ---- contact form ----

    Locator field(String label) {
        return page.getByLabel(label, new Page.GetByLabelOptions().setExact(true));
    }

    /** Replaces a field's value by typing it; the cursor stays in the field. */
    void setField(String label, String value) {
        Locator f = field(label);
        f.click();
        page.keyboard().press("ControlOrMeta+a");
        page.keyboard().press("Backspace");
        if (!value.isEmpty()) {
            f.pressSequentially(value, new Locator.PressSequentiallyOptions().setDelay(20));
        }
    }

    void assertValues(Map<String, String> values) {
        values.forEach((label, value) -> assertThat(field(label)).hasValue(value));
    }

    void assertFormShown() {
        assertThat(button("Save")).isVisible();
        assertThat(button("Cancel")).isVisible();
        FIELDS.forEach(label -> assertThat(field(label)).isVisible());
    }

    void assertFormHidden() {
        assertThat(button("Save")).isHidden();
        FIELDS.forEach(label -> assertThat(field(label)).isHidden());
    }

    // ---- notifications ----

    Locator notifications() {
        return page.getByRole(AriaRole.ALERT);
    }

    /** Exactly one notification is shown, with this text. */
    void assertNotification(String text) {
        assertThat(notifications()).hasCount(1);
        assertThat(notifications()).hasText(text);
    }

    /** No notification is shown; waits up to 10 s for shown ones to close by themselves. */
    void assertNoNotification() {
        assertThat(notifications()).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(10_000));
    }
}
