package com.vaadin.tutorial.addressbook.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Base of the end-to-end tests: one Chromium per test class, one browser context (a separate session, as a
 * separate user) per {@link #newUser()}.
 *
 * <p>The tests drive an app that is already running, in production mode, at the URL in the system property
 * {@code app.url} (for example {@code mvn verify -Pe2e -Dapp.url=http://localhost:8080}). They never start the
 * app. The app keeps its contacts in memory and all sessions share them, so it must be restarted before a run:
 * the tests expect the seed contacts. The test classes run in a fixed order (their {@code @Order}); tests that
 * change data come after the ones whose checks they would break, and where an earlier test's change shows,
 * the check is relative (see each class). One app copy per run; the classes do not run in parallel.
 */
abstract class E2eTest {

    private static Playwright playwright;
    private static Browser browser;
    private static String userAgent;

    protected static String baseUrl;

    private final List<BrowserContext> contexts = new ArrayList<>();
    private final List<Page> pages = new ArrayList<>();

    @BeforeAll
    static void launchBrowser() {
        baseUrl = System.getProperty("app.url", "").trim().replaceAll("/+$", "");
        if (baseUrl.isEmpty() || baseUrl.startsWith("${")) {
            throw new IllegalStateException("Set the URL of the running app: -Dapp.url=http://localhost:8080");
        }
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
        // Chromium headless says "HeadlessChrome" in its user agent; send what users' browsers send, as
        // ci/smoke.cjs and the workflow runs do.
        try (BrowserContext probe = browser.newContext()) {
            userAgent = probe.newPage().evaluate("() => navigator.userAgent").toString()
                    .replace("HeadlessChrome", "Chrome");
        }
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @AfterEach
    void closeContexts() {
        contexts.forEach(BrowserContext::close);
        contexts.clear();
        pages.clear();
    }

    /** A new user: a fresh browser session (en-US, UTC, 1400 x 900) with the app open on its contact list. */
    protected AddressbookPage newUser() {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1400, 900)
                .setLocale("en-US")
                .setTimezoneId("UTC")
                .setUserAgent(userAgent));
        context.setDefaultTimeout(15_000);
        contexts.add(context);
        Page page = context.newPage();
        pages.add(page);
        AddressbookPage app = new AddressbookPage(page);
        app.open(baseUrl);
        return app;
    }

    /**
     * Runs one workflow step. A failure names the step, and a screenshot of every open page goes to
     * {@code target/e2e-failures/} (CI uploads it on failure only).
     */
    protected void step(String workflow, int number, String what, Runnable body) {
        try {
            body.run();
        } catch (AssertionError | RuntimeException e) {
            String name = workflow + "-step" + number;
            for (int i = 0; i < pages.size(); i++) {
                try {
                    Path dir = Path.of("target", "e2e-failures");
                    Files.createDirectories(dir);
                    pages.get(i).screenshot(new Page.ScreenshotOptions()
                            .setPath(dir.resolve(name + "-user" + (char) ('a' + i) + ".png")).setFullPage(true));
                } catch (Exception ignored) {
                    // Diagnostics only; the step's own failure is what is reported.
                }
            }
            throw new AssertionError(workflow + " step " + number + " (" + what + "): " + e.getMessage(), e);
        }
    }
}
