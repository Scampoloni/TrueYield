<script lang="ts">
  import { page } from '$app/state';

  const portfolioId = page.params.id;
  const holdingId = page.params.hid;

  const evidence = [
    { headline: 'Company announces breakthrough in sustainable fuel production', source: 'Financial Times', date: 'Mar 15, 2026', risk: 15, sentiment: 'Positive' },
    { headline: 'Renewable demand surges as EU tightens emissions standards', source: 'Reuters', date: 'Mar 12, 2026', risk: 25, sentiment: 'Positive' },
    { headline: 'Concerns raised over palm oil sourcing in biofuel production', source: 'Bloomberg', date: 'Mar 10, 2026', risk: 65, sentiment: 'Negative' },
  ];

  function riskClass(r: number) {
    if (r < 30) return 'low';
    if (r < 60) return 'medium';
    return 'high';
  }
  function sentimentClass(s: string) {
    if (s === 'Positive') return 'badge-approved';
    if (s === 'Negative') return 'badge-rejected';
    return 'badge-pending';
  }
</script>

<div class="topbar">
  <div>
    <div class="page-title">Holding {holdingId}</div>
    <div class="page-subtitle">Evidence & Risk Analysis</div>
  </div>
  <a href="/portfolios/{portfolioId}" class="btn btn-ghost">← Back to Portfolio</a>
</div>

<div class="content">
  {#if evidence.length === 0}
    <div class="table-wrap">
      <div class="empty-state">
        <div class="empty-icon">
          <svg width="20" height="20" viewBox="0 0 16 16" fill="none"><circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.3"/><path d="M11 11l3 3" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
        </div>
        <div class="empty-title">No evidence yet</div>
        <div class="empty-desc">Start an AI analysis from the portfolio detail page to generate ESG evidence for this holding.</div>
      </div>
    </div>
  {:else}
    <div class="section-hd">
      <span class="section-title">AI-Generated Evidence</span>
      <span class="section-meta">{evidence.length} items</span>
    </div>
    <div class="evidence-grid">
      {#each evidence as e}
        <div class="evidence-card">
          <div class="evidence-headline">{e.headline}</div>
          <div class="evidence-source">{e.source}</div>
          <div class="evidence-date">{e.date}</div>
          <div class="risk-label">
            <span>Risk Score</span>
            <span style="font-weight:600;color:{e.risk < 30 ? 'var(--green)' : e.risk < 60 ? 'var(--amber)' : 'var(--red)'}">{e.risk}%</span>
          </div>
          <div class="risk-bar">
            <div class="risk-fill {riskClass(e.risk)}" style="width:{e.risk}%"></div>
          </div>
          <span class="badge {sentimentClass(e.sentiment)}">{e.sentiment}</span>
        </div>
      {/each}
    </div>
  {/if}
</div>
