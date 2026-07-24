import { loginViaApi } from '../support/credentials.js';

const hasRoleCredentials = Cypress.expose('hasRoleCredentials');

describe('role-based audit lifecycle', () => {
  if (!hasRoleCredentials) {
    it.skip('Role credentials are required for the full-stack lifecycle', () => {});
    return;
  }

  it('moves a portfolio from fund-manager submission to auditor approval and compliance oversight', () => {
    const runId = `${Date.now()}-${Cypress._.random(1000, 9999)}`;
    const portfolioName = `E2E ESG Portfolio ${runId}`;
    let portfolioId;
    let holdingId;
    let auditReportId;

    loginViaApi('E2E_FUND_MANAGER_EMAIL', 'E2E_DEMO_PASSWORD');

    cy.request('POST', '/api/portfolio', {
      name: portfolioName,
      description: 'Automated role-lifecycle verification'
    }).then(({ status, body }) => {
      expect(status).to.eq(201);
      expect(body.name).to.eq(portfolioName);
      portfolioId = body.id;
    });

    cy.then(() =>
      cy.request('POST', '/api/holding', {
        portfolioId,
        symbol: 'E2E',
        name: 'Lifecycle Test Holding',
        isin: 'CH0000000001',
        weightPercent: 100
      })
    ).then(({ status, body }) => {
      expect(status).to.eq(201);
      expect(body.portfolioId).to.eq(portfolioId);
      holdingId = body.id;
    });

    cy.then(() =>
      cy.request('POST', '/api/service/auditreport', { portfolioId })
    ).then(({ status, body }) => {
      expect(status).to.eq(201);
      expect(body.auditStatus).to.eq('PENDING_REVIEW');
      auditReportId = body.id;
    });

    loginViaApi('E2E_AUDITOR_EMAIL', 'E2E_DEMO_PASSWORD');

    cy.request({
      method: 'POST',
      url: '/api/portfolio',
      body: { name: 'Forbidden auditor portfolio' },
      failOnStatusCode: false
    }).its('status').should('eq', 403);

    cy.request('/api/service/auditreport/auditor-queue').then(({ status, body }) => {
      expect(status).to.eq(200);
      expect(body.some((report) => report.id === auditReportId)).to.eq(true);
    });

    cy.then(() =>
      cy.request('PUT', '/api/service/auditreport/assign', { auditReportId })
    ).its('body.auditStatus').should('eq', 'UNDER_REVIEW');

    cy.then(() =>
      cy.request('POST', '/api/evidence', {
        holdingId,
        sourceUrl: 'https://example.com/e2e-evidence',
        contentSnippet: 'Independent evidence documents an ESG control improvement.'
      })
    ).then(({ status, body }) => {
      expect(status).to.eq(201);
      expect(body.holdingId).to.eq(holdingId);
    });

    cy.then(() =>
      cy.request('POST', '/api/service/auditcomment', {
        auditReportId,
        comment: 'Evidence reviewed by the automated auditor lifecycle.'
      })
    ).its('status').should('eq', 201);

    cy.then(() =>
      cy.request('PUT', '/api/service/auditreport/complete', { auditReportId })
    ).its('body.auditStatus').should('eq', 'APPROVED');

    loginViaApi('E2E_COMPLIANCE_EMAIL', 'E2E_DEMO_PASSWORD');

    cy.request('/api/compliance/overview').then(({ status, body }) => {
      expect(status).to.eq(200);
      expect(body.totalPortfolios).to.be.greaterThan(0);
      expect(body.reportsByStatus.APPROVED).to.be.greaterThan(0);
    });

    cy.request('/api/compliance/reports').then(({ status, body }) => {
      expect(status).to.eq(200);
      const report = body.find((candidate) => candidate.id === auditReportId);
      expect(report).to.exist;
      expect(report.portfolioId).to.eq(portfolioId);
      expect(report.auditStatus).to.eq('APPROVED');
    });

    cy.request({
      method: 'POST',
      url: '/api/portfolio',
      body: { name: 'Forbidden compliance portfolio' },
      failOnStatusCode: false
    }).its('status').should('eq', 403);
  });
});
