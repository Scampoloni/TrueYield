const email = Cypress.env('E2E_TEST_EMAIL');
const password = Cypress.env('E2E_TEST_PASSWORD');
const hasCredentials = Boolean(email && password);

const FAKE_PORTFOLIO_ID = 'test-portfolio-id';
const FAKE_HOLDING_ID = 'test-holding-id';

function loginAs(userEmail, userPassword) {
  cy.visit('/login');
  cy.get('#email').type(userEmail);
  cy.get('#password').type(userPassword);
  cy.contains('button', 'Sign In').click();
  cy.location('pathname').should('not.include', '/login');
}

describe('evidence routes: unauthenticated redirects', () => {
  it('holdings detail page redirects to /login', () => {
    cy.visit(`/portfolios/${FAKE_PORTFOLIO_ID}/holdings/${FAKE_HOLDING_ID}`);
    cy.location('pathname').should('eq', '/login');
  });

  it('evidence create page redirects to /login', () => {
    cy.visit(`/portfolios/${FAKE_PORTFOLIO_ID}/holdings/${FAKE_HOLDING_ID}/create`);
    cy.location('pathname').should('eq', '/login');
  });
});

describe('evidence create form', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests', () => {});
    return;
  }

  beforeEach(() => {
    loginAs(email, password);
    cy.visit(`/portfolios/${FAKE_PORTFOLIO_ID}/holdings/${FAKE_HOLDING_ID}/create`);
  });

  it('renders Add Evidence heading', () => {
    cy.contains('Add Evidence').should('be.visible');
  });

  it('renders contentSnippet textarea', () => {
    cy.get('#contentSnippet').should('be.visible');
  });

  it('renders sourceUrl input', () => {
    cy.get('#sourceUrl').should('be.visible');
  });

  it('renders Add Evidence submit button', () => {
    cy.contains('button', 'Add Evidence').should('be.visible');
  });

  it('shows validation error when content snippet is empty', () => {
    cy.contains('button', 'Add Evidence').click();
    cy.contains('Content snippet is required.').should('be.visible');
  });

  it('shows validation error when snippet is whitespace only', () => {
    cy.get('#contentSnippet').type('   ');
    cy.contains('button', 'Add Evidence').click();
    cy.contains('Content snippet is required.').should('be.visible');
  });

  it('cancel button navigates back to holdings page', () => {
    cy.contains('a', '← Cancel').click();
    cy.location('pathname').should(
      'eq',
      `/portfolios/${FAKE_PORTFOLIO_ID}/holdings/${FAKE_HOLDING_ID}`
    );
  });
});
