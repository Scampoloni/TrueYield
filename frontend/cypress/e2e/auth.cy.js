import { loginWithSecrets } from '../support/credentials.js';

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

  it('explains that demo access is invitation-only', () => {
    cy.visit('/login');
    cy.contains('Demo access is invitation-only').should('be.visible');
    cy.contains('a', 'Sign up').should('not.exist');
  });

  it('shows TrueYield brand', () => {
    cy.visit('/login');
    cy.contains('TrueYield').should('be.visible');
  });
});

describe('signup page', () => {
  it('redirects to login when self-registration is disabled', () => {
    cy.visit('/signup');
    cy.location('pathname').should('eq', '/login');
    cy.contains('Demo access is invitation-only').should('be.visible');
  });

  it('rejects direct signup requests', () => {
    cy.request({
      method: 'POST',
      url: '/auth/signup',
      body: { email: 'unassigned@example.com', password: 'NotARealPassword1!' },
      failOnStatusCode: false
    }).then(({ status, body }) => {
      expect(status).to.eq(403);
      expect(body.error).to.contain('invitation-only');
    });
  });
});

const hasCredentials = Cypress.expose('hasLegacyCredentials');

describe('authenticated flows', () => {
  if (!hasCredentials) {
    it.skip('Set E2E_TEST_EMAIL and E2E_TEST_PASSWORD to run authenticated tests', () => {});
    return;
  }

  it('login with valid credentials redirects to home', () => {
    loginWithSecrets('E2E_TEST_EMAIL', 'E2E_TEST_PASSWORD');
    cy.location('pathname').should('not.include', '/login');
  });

  it('after login: portfolios page is accessible', () => {
    loginWithSecrets('E2E_TEST_EMAIL', 'E2E_TEST_PASSWORD');
    cy.visit('/portfolios');
    cy.location('pathname').should('eq', '/portfolios');
    cy.contains('Portfolios').should('be.visible');
  });
});
