import { defineConfig } from 'cypress';

export default defineConfig({
  allowCypressEnv: false,
  expose: {
    hasLegacyCredentials: Boolean(
      process.env.E2E_TEST_EMAIL && process.env.E2E_TEST_PASSWORD
    ),
    legacyRole: process.env.E2E_TEST_ROLE || '',
    hasRoleCredentials: Boolean(
      process.env.E2E_FUND_MANAGER_EMAIL &&
      process.env.E2E_AUDITOR_EMAIL &&
      process.env.E2E_COMPLIANCE_EMAIL &&
      process.env.E2E_DEMO_PASSWORD
    )
  },
  e2e: {
    baseUrl: process.env.E2E_BASE_URL || 'http://localhost:5173',
    env: {
      E2E_TEST_EMAIL: process.env.E2E_TEST_EMAIL || '',
      E2E_TEST_PASSWORD: process.env.E2E_TEST_PASSWORD || '',
      E2E_TEST_ROLE: process.env.E2E_TEST_ROLE || '',
      E2E_FUND_MANAGER_EMAIL: process.env.E2E_FUND_MANAGER_EMAIL || '',
      E2E_AUDITOR_EMAIL: process.env.E2E_AUDITOR_EMAIL || '',
      E2E_COMPLIANCE_EMAIL: process.env.E2E_COMPLIANCE_EMAIL || '',
      E2E_DEMO_PASSWORD: process.env.E2E_DEMO_PASSWORD || ''
    },
    video: false
  }
});
