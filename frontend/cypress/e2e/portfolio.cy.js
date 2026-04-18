const email = Cypress.env('E2E_TEST_EMAIL');
const password = Cypress.env('E2E_TEST_PASSWORD');
const role = Cypress.env('E2E_TEST_ROLE');
const hasCredentials = Boolean(email && password);

function loginAs(userEmail, userPassword) {
  cy.visit('/login');
  cy.get('#email').type(userEmail);
  cy.get('#password').type(userPassword);
  cy.contains('button', 'Sign In').click();
  cy.location('pathname').should('not.include', '/login');
}

describe('portfolio create form', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests', () => {});
    return;
  }

  beforeEach(() => {
    loginAs(email, password);
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
});

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
    const isFundManager = (email && email.includes('manager')) || role === 'fund-manager';
    if (isFundManager) {
      cy.contains('a', 'New Portfolio').should('be.visible');
    }
  });
});

describe('holdings page', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run these tests', () => {});
    return;
  }

  it('holdings page renders after login', () => {
    loginAs(email, password);
    cy.visit('/holdings');
    cy.location('pathname').should('eq', '/holdings');
    cy.contains('Holdings').should('be.visible');
  });
});
