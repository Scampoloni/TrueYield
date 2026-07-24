export function loginWithSecrets(emailKey, passwordKey) {
  cy.env([emailKey, passwordKey], { log: false }).then((credentials) => {
    const email = credentials[emailKey];
    const password = credentials[passwordKey];
    expect(email, `${emailKey} is configured`).to.be.a('string').and.not.be.empty;
    expect(password, `${passwordKey} is configured`).to.be.a('string').and.not.be.empty;

    cy.visit('/login');
    cy.get('#email').type(email);
    cy.get('#password').type(password, { log: false });
    cy.get('[data-testid="sign-in-button"]').click();
    cy.location('pathname').should('not.include', '/login');
  });
}

export function loginViaApi(emailKey, passwordKey) {
  cy.clearCookies();
  return cy.env([emailKey, passwordKey], { log: false }).then((credentials) => {
    const email = credentials[emailKey];
    const password = credentials[passwordKey];
    expect(email, `${emailKey} is configured`).to.be.a('string').and.not.be.empty;
    expect(password, `${passwordKey} is configured`).to.be.a('string').and.not.be.empty;

    return cy.request({
      method: 'POST',
      url: '/auth/login',
      body: { email, password },
      log: false
    }).its('status').should('eq', 200);
  });
}
