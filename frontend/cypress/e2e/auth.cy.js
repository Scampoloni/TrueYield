const PROTECTED_ROUTES = ['/', '/portfolios', '/holdings', '/audit', '/account', '/compliance'];

describe('unauthenticated redirects', () => {
  PROTECTED_ROUTES.forEach((route) => {
    it(`unauthenticated: ${route} redirects to /login`, () => {
      cy.visit(route);
      cy.location('pathname').should('eq', '/login');
    });
  });

  it('unauthenticated: /portfolios sub-route redirects to /login', () => {
    cy.visit('/portfolios/some-id');
    cy.location('pathname').should('eq', '/login');
  });

  it('unauthenticated: /audit sub-route redirects to /login', () => {
    cy.visit('/audit/some-id');
    cy.location('pathname').should('eq', '/login');
  });
});

describe('login page', () => {
  it('links to the public academic prototype description', () => {
    cy.visit('/about');
    cy.contains('Academic portfolio prototype').should('be.visible');
    cy.contains('does not certify SFDR').should('be.visible');
  });

  it('renders email and password inputs', () => {
    cy.visit('/login');
    cy.get('#email').should('be.visible');
    cy.get('#password').should('be.visible');
  });

  it('renders Sign In button', () => {
    cy.visit('/login');
    cy.get('[data-testid="sign-in-button"]').should('be.visible');
  });

  it('Sign In button is enabled when page loads', () => {
    cy.visit('/login');
    cy.get('[data-testid="sign-in-button"]').should('be.enabled');
  });

  it('has link to signup page', () => {
    cy.visit('/login');
    cy.contains('a', 'Sign up').should('have.attr', 'href', '/signup');
  });

  it('shows TrueYield brand', () => {
    cy.visit('/login');
    cy.contains('TrueYield').should('be.visible');
  });
});

describe('signup page', () => {
  it('renders create account action', () => {
    cy.visit('/signup');
    cy.contains('button', 'Create Account').should('be.visible');
  });

  it('renders email and password inputs', () => {
    cy.visit('/signup');
    cy.get('#email').should('be.visible');
    cy.get('#password').should('be.visible');
  });
});

const email = Cypress.env('E2E_TEST_EMAIL');
const password = Cypress.env('E2E_TEST_PASSWORD');
const hasCredentials = Boolean(email && password);

function loginAs(userEmail, userPassword) {
  cy.visit('/login');
  cy.get('#email').type(userEmail);
  cy.get('#password').type(userPassword);
  cy.get('[data-testid="sign-in-button"]').click();
  cy.location('pathname').should('not.include', '/login');
}

describe('authenticated flows', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run authenticated tests', () => {});
    return;
  }

  it('login with valid credentials redirects to home', () => {
    loginAs(email, password);
    cy.location('pathname').should('not.include', '/login');
  });

  it('after login: portfolios page is accessible', () => {
    loginAs(email, password);
    cy.visit('/portfolios');
    cy.location('pathname').should('eq', '/portfolios');
    cy.contains('Portfolios').should('be.visible');
  });
});
