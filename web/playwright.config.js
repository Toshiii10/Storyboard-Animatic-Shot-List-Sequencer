const { defineConfig } = require('@playwright/test');

module.exports = defineConfig({
  testDir: '.', testMatch: 'boardflow.spec.js', fullyParallel: false, workers: 1,
  timeout: 35000, expect: { timeout: 10000 },
  use: { baseURL: process.env.BOARDFLOW_URL || 'http://127.0.0.1:8081', viewport: {width:1440,height:1000},
    channel: 'chrome', headless: true, screenshot: 'only-on-failure', trace: 'retain-on-failure' },
  reporter: 'list'
});
