import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    baseUrl: process.env.E2E_BASE_URL || 'http://localhost:5173',
    env: {
      E2E_TEST_EMAIL: process.env.E2E_TEST_EMAIL || '',
      E2E_TEST_PASSWORD: process.env.E2E_TEST_PASSWORD || '',
      E2E_TEST_ROLE: process.env.E2E_TEST_ROLE || ''
    },
    video: false
  }
});
