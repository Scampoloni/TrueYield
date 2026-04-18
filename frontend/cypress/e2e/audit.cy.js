const email = Cypress.env('E2E_TEST_EMAIL');
const password = Cypress.env('E2E_TEST_PASSWORD');
const hasCredentials = Boolean(email && password);

function loginAs(userEmail, userPassword) {
  cy.visit('/login');
  cy.get('#email').type(userEmail);
  cy.get('#password').type(userPassword);
  cy.contains('button', 'Sign In').click();
  cy.location('pathname').should('not.include', '/login');
}

describe('audit dashboard', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests', () => {});
    return;
  }

  beforeEach(() => {
    loginAs(email, password);
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

  it('renders filter tabs', () => {
    cy.contains('button', 'All').should('be.visible');
    cy.contains('button', 'Pending').should('be.visible');
    cy.contains('button', 'Under Review').should('be.visible');
  });

  it('renders search input', () => {
    cy.get('input[placeholder*="Search by report ID" i]').should('be.visible');
  });

  it('filter tab "Pending" is clickable and stays on page', () => {
    cy.contains('button', 'Pending').first().click();
    cy.location('pathname').should('eq', '/audit');
  });
});

describe('account page', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests', () => {});
    return;
  }

  it('account page renders after login', () => {
    loginAs(email, password);
    cy.visit('/account');
    cy.location('pathname').should('eq', '/account');
  });
});
