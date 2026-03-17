const BASE = 'http://localhost:8080';

export async function fetchPortfolios() {
  const res = await fetch(`${BASE}/api/portfolio`);
  if (!res.ok) throw new Error('Failed to fetch portfolios');
  return res.json();
}

export async function fetchPortfolio(id: string) {
  const res = await fetch(`${BASE}/api/portfolio/${id}`);
  if (!res.ok) throw new Error('Portfolio not found');
  return res.json();
}

export async function createPortfolio(data: { name: string; description: string; fundManagerId: string }) {
  const res = await fetch(`${BASE}/api/portfolio`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  if (!res.ok) throw new Error('Failed to create portfolio');
  return res.json();
}

export async function updatePortfolio(id: string, data: { name: string; description: string }) {
  const res = await fetch(`${BASE}/api/portfolio/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  if (!res.ok) throw new Error('Failed to update portfolio');
  return res.json();
}

export async function deletePortfolio(id: string) {
  const res = await fetch(`${BASE}/api/portfolio/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Failed to delete portfolio');
}

export async function createHolding(data: { portfolioId: string; symbol: string; isin?: string; name?: string; weightPercent?: number }) {
  const res = await fetch(`${BASE}/api/holding`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  if (!res.ok) throw new Error('Failed to create holding');
  return res.json();
}

export async function fetchDashboard(portfolioId: string) {
  const res = await fetch(`${BASE}/api/service/auditreport/dashboard?portfolioId=${portfolioId}`);
  if (!res.ok) throw new Error('Failed to fetch dashboard');
  return res.json();
}

export async function assignAuditReport(auditReportId: string, auditorId: string) {
  const res = await fetch(`${BASE}/api/service/auditreport/assign`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ auditReportId, auditorId })
  });
  if (!res.ok) throw new Error('Failed to assign report');
  return res.json();
}

export async function completeAuditReport(auditReportId: string, auditorId: string) {
  const res = await fetch(`${BASE}/api/service/auditreport/complete`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ auditReportId, auditorId })
  });
  if (!res.ok) throw new Error('Failed to complete report');
  return res.json();
}
