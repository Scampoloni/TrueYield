import { loginWithSecrets } from '../support/credentials.js';

const hasCredentials = Cypress.expose('hasLegacyCredentials');
const role = Cypress.expose('legacyRole');

const FAKE_PORTFOLIO_ID = 'test-portfolio-id';
const FAKE_HOLDING_ID = 'test-holding-id';

// ── Unauthenticated redirects ────────────────────────────────────────────────

describe('portfolio routes: unauthenticated redirects', () => {
  it('portfolio create page redirects to /login', () => {
    cy.visit('/portfolios/create');
    cy.location('pathname').should('eq', '/login');
  });

  it('portfolio edit page redirects to /login', () => {
    cy.visit(`/portfolios/${FAKE_PORTFOLIO_ID}/edit`);
    cy.location('pathname').should('eq', '/login');
  });

  it('holdings create page redirects to /login', () => {
    cy.visit(`/portfolios/${FAKE_PORTFOLIO_ID}/holdings/create`);
    cy.location('pathname').should('eq', '/login');
  });
});

// ── Portfolio create form ────────────────────────────────────────────────────

describe('portfolio create form', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests', () => {});
    return;
  }

  beforeEach(() => {
    loginWithSecrets('E2E_TEST_EMAIL', 'E2E_TEST_PASSWORD');
    cy.visit('/portfolios/create');
  });

  it('renders portfolio name and description inputs', () => {
    cy.get('#name').should('be.visible');
    cy.get('#description').should('be.visible');
  });

  it('renders Create Portfolio submit button', () => {
    cy.contains('button', 'Create Portfolio').should('be.visible');
  });

  it('shows validation error when name is empty', () => {
    cy.contains('button', 'Create Portfolio').click();
    cy.contains('Portfolio name is required.').should('be.visible');
  });

  it('shows validation error when name is whitespace only', () => {
    cy.get('#name').type('   ');
    cy.contains('button', 'Create Portfolio').click();
    cy.contains('Portfolio name is required.').should('be.visible');
  });

  it('cancel button navigates back to portfolios list', () => {
    cy.contains('a', '← Cancel').click();
    cy.location('pathname').should('eq', '/portfolios');
  });

  it('name input accepts valid text', () => {
    cy.get('#name').type('Test ESG Portfolio');
    cy.get('#name').should('have.value', 'Test ESG Portfolio');
  });

  it('description input accepts text', () => {
    cy.get('#description').type('My test description');
    cy.get('#description').should('have.value', 'My test description');
  });
});

// ── Portfolios list ──────────────────────────────────────────────────────────

describe('portfolios list page', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests', () => {});
    return;
  }

  beforeEach(() => {
    loginAs(email, password);
    cy.visit('/portfolios');
  });

  it('renders Portfolios page heading', () => {
    cy.contains('Portfolios').should('be.visible');
  });

  it('fund-manager role sees New Portfolio button', () => {
    const isFundManager = role === 'fund-manager';
    if (isFundManager) {
      cy.contains('a', 'New Portfolio').should('be.visible');
    }
  });

  it('page does not crash on load', () => {
    cy.location('pathname').should('eq', '/portfolios');
    cy.get('body').should('be.visible');
  });
});

// ── Holdings create form ─────────────────────────────────────────────────────

describe('holdings create form', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests', () => {});
    return;
  }

  beforeEach(() => {
    loginWithSecrets('E2E_TEST_EMAIL', 'E2E_TEST_PASSWORD');
    cy.visit(`/portfolios/${FAKE_PORTFOLIO_ID}/holdings/create`);
  });

  it('renders Add Holding heading', () => {
    cy.contains('Add Holding').should('be.visible');
  });

  it('renders symbol input (required)', () => {
    cy.get('#symbol').should('be.visible');
  });

  it('renders company name input', () => {
    cy.get('#holdingName').should('be.visible');
  });

  it('renders ISIN input', () => {
    cy.get('#isin').should('be.visible');
  });

  it('renders weight percent input', () => {
    cy.get('#weight').should('be.visible');
  });

  it('renders Add Holding submit button', () => {
    cy.contains('button', 'Add Holding').should('be.visible');
  });

  it('shows validation error when symbol is empty', () => {
    cy.contains('button', 'Add Holding').click();
    cy.contains('Symbol is required.').should('be.visible');
  });

  it('shows validation error when symbol is whitespace only', () => {
    cy.get('#symbol').type('   ');
    cy.contains('button', 'Add Holding').click();
    cy.contains('Symbol is required.').should('be.visible');
  });

  it('cancel button navigates back to portfolio detail', () => {
    cy.contains('a', '← Cancel').click();
    cy.location('pathname').should('eq', `/portfolios/${FAKE_PORTFOLIO_ID}`);
  });

  it('symbol input accepts text', () => {
    cy.get('#symbol').type('AAPL');
    cy.get('#symbol').should('have.value', 'AAPL');
  });
});

// ── Holdings page ────────────────────────────────────────────────────────────

describe('holdings page', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests', () => {});
    return;
  }

  it('holdings page renders after login', () => {
    loginWithSecrets('E2E_TEST_EMAIL', 'E2E_TEST_PASSWORD');
    cy.visit('/holdings');
    cy.location('pathname').should('eq', '/holdings');
    cy.contains('Holdings').should('be.visible');
  });
});
