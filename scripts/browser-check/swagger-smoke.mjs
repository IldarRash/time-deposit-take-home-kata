import assert from 'node:assert/strict';
import { chromium } from 'playwright';

// Run only against the isolated smoke database after its first two accrual cycles.
const origin = 'http://localhost:8081';
const browser = await chromium.launch();
const page = await browser.newPage();
page.setDefaultTimeout(15_000);
const browserErrors = [];
page.on('pageerror', error => browserErrors.push(error.message));
try {
  await page.goto(origin);
  const get = page.locator('#operations-default-listTimeDeposits');
  const post = page.locator('#operations-default-updateTimeDepositBalances');
  await get.waitFor({ state: 'visible' });
  assert.equal(await page.locator('.opblock').count(), 2);
  for (const operation of [get, post]) {
    await operation.locator('.opblock-summary').click();
    await operation.getByRole('button', { name: 'Try it out', exact: true }).click();
  }

  async function execute(operation, method, path) {
    const pending = page.waitForResponse(response =>
      response.url() === origin + path && response.request().method() === method);
    await operation.getByRole('button', { name: 'Execute', exact: true }).click();
    return pending;
  }

  const beforeResponse = await execute(get, 'GET', '/time-deposits');
  assert.equal(beforeResponse.status(), 200);
  const before = await beforeResponse.json();
  assert.deepEqual(before.map(d => d.balance), [1202, 1206.01, 1210.02, 1200, 1200, 1200]);

  const updated = await execute(post, 'POST', '/time-deposits/update-balances');
  assert.equal(updated.status(), 204);
  assert.equal((await updated.body()).length, 0);

  const afterResponse = await execute(get, 'GET', '/time-deposits');
  assert.equal(afterResponse.status(), 200);
  const after = await afterResponse.json();
  assert.deepEqual(after.map(d => d.balance), [1203, 1209.03, 1215.06, 1200, 1200, 1200]);
  assert.deepEqual(after.map(({ balance, ...rest }) => rest), before.map(({ balance, ...rest }) => rest));
  assert.deepEqual(browserErrors, []);
  await page.screenshot({ path: 'java/target/smoke-swagger.png', fullPage: true });
  console.log('PASS: Swagger UI browser Execute GET -> POST 204 -> GET persisted balances');
} finally {
  await browser.close();
}
