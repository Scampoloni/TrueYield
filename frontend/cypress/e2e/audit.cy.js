import { loginWithSecrets } from '../support/credentials.js';

const hasCredentials = Cypress.expose('hasLegacyCredentials');

const FAKE_AUDIT_ID = 'test-audit-id';

// ── Unauthenticated redirects ────────────────────────────────────────────────

describe('audit routes: unauthenticated redirects', () => {
  it('audit dashboard redirects to /login', () => {
    cy.visit('/audit');
    cy.location('pathname').should('eq', '/login');
  });

  it('audit detail page redirects to /login', () => {
    cy.visit(`/audit/${FAKE_AUDIT_ID}`);
    cy.location('pathname').should('eq', '/login');
  });

  it('account page redirects to /login', () => {
    cy.visit('/account');
    cy.location('pathname').should('eq', '/login');
  });
});

// ── Audit dashboard ──────────────────────────────────────────────────────────

describe('audit dashboard', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests', () => {});
    return;
  }

  beforeEach(() => {
    loginWithSecrets('E2E_TEST_EMAIL', 'E2E_TEST_PASSWORD');
    cy.visit('/audit');
  });

  it('renders Audit Dashboard heading', () => {
    cy.contains('Audit Dashboard').should('be.visible');
  });

  it('renders Pending Review metric card', () => {
    cy.contains('Pending Review').should('be.visible');
  });

  it('renders Under Review metric card', () => {
    cy.contains('Under Review').should('be.visible');
  });

  it('renders Approved metric card', () => {
    cy.contains('Approved').should('be.visible');
  });

  it('renders filter tabs (All / Pending / Under Review)', () => {
    cy.contains('button', 'All').should('be.visible');
    cy.contains('button', 'Pending').should('be.visible');
    cy.contains('button', 'Under Review').should('be.visible');
  });

  it('renders search input', () => {
    cy.get('input[placeholder*="Search by report ID" i]').should('be.visible');
  });

  it('filter tab "Pending" is clickable and stays on /audit', () => {
    cy.contains('button', 'Pending').first().click();
    cy.location('pathname').should('eq', '/audit');
  });

  it('filter tab "Under Review" is clickable and stays on /audit', () => {
    cy.contains('button', 'Under Review').first().click();
    cy.location('pathname').should('eq', '/audit');
  });

  it('filter tab "All" resets view and stays on /audit', () => {
    cy.contains('button', 'Pending').first().click();
    cy.contains('button', 'All').first().click();
    cy.location('pathname').should('eq', '/audit');
  });

  it('search input accepts text', () => {
    cy.get('input[placeholder*="Search by report ID" i]').type('test-123');
    cy.get('input[placeholder*="Search by report ID" i]').should('have.value', 'test-123');
  });

  it('page does not crash on load', () => {
    cy.location('pathname').should('eq', '/audit');
    cy.get('body').should('be.visible');
  });
});

// ── Account page ─────────────────────────────────────────────────────────────

describe('account page', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests', () => {});
    return;
  }

  it('account page renders after login', () => {
    loginWithSecrets('E2E_TEST_EMAIL', 'E2E_TEST_PASSWORD');
    cy.visit('/account');
    cy.location('pathname').should('eq', '/account');
    cy.get('body').should('be.visible');
  });
});
