// CI smoke check for the Addressbook app (see ci/README.md).
// Opens the contact view by its route (""), waits for the contact grid to have rows, fails on console errors, page errors
// or any HTTP response >= 400, and writes a screenshot.
// Usage: node ci/smoke.cjs [baseUrl] [outDir]   (defaults: http://localhost:8080, target/smoke)
// Exit 0 = pass, 1 = fail.
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const base = (process.argv[2] || 'http://localhost:8080').replace(/\/+$/, '');
const out = process.argv[3] || 'target/smoke';
const TIMEOUT = 30000;

(async () => {
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch();
  const failures = [];
  try {
    // Use the real Chrome UA, not "HeadlessChrome", so CI sends what users' browsers send
    // (the old Vaadin 8 app chose client code from it; kept so old and new runs match).
    const probe = await browser.newPage();
    const userAgent = (await probe.evaluate(() => navigator.userAgent)).replace('HeadlessChrome', 'Chrome');
    await probe.close();
    const context = await browser.newContext({ userAgent, viewport: { width: 1400, height: 900 } });
    const page = await context.newPage();
    page.on('pageerror', e => failures.push('page error: ' + e.message.split('\n')[0]));
    page.on('console', m => { if (m.type() === 'error') failures.push('console error: ' + m.text().split('\n')[0]); });
    page.on('response', r => { if (r.status() >= 400) failures.push('HTTP ' + r.status() + ' ' + r.url()); });

    const response = await page.goto(base + '/', { timeout: TIMEOUT });
    if (!response) failures.push('no response for ' + base + '/');
    // The contact grid is filled with demo data on start: wait until it shows at least one row.
    // Grid body rows live in the vaadin-grid shadow DOM; Playwright's CSS selectors pierce it.
    const rowSelector = 'vaadin-grid #items tr[part~="body-row"]';
    await page.locator(rowSelector).first().waitFor({ state: 'attached', timeout: TIMEOUT });
    const rows = await page.locator(rowSelector).count();
    // Let late requests and client errors arrive before judging.
    await page.waitForTimeout(2000);
    const shot = path.join(out, 'smoke.png');
    await page.screenshot({ path: shot, fullPage: true });
    console.log(`grid rows: ${rows}; screenshot: ${shot}`);
    await context.close();
  } catch (e) {
    failures.push('error: ' + e.message.split('\n')[0]);
  } finally {
    await browser.close();
  }
  fs.writeFileSync(path.join(out, 'smoke.json'), JSON.stringify({ base, pass: failures.length === 0, failures }, null, 2));
  if (failures.length) {
    failures.forEach(f => console.error('FAIL ' + f));
    process.exit(1);
  }
  console.log('PASS smoke ' + base);
})().catch(e => { console.error('SMOKE ERROR ' + e.message.split('\n')[0]); process.exit(1); });
