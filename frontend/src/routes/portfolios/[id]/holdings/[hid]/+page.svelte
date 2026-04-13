<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';

  const portfolioId = page.params.id;
  const holdingId = page.params.hid;

  let evidence: any[] = $state([]);
  let loading = $state(true);

  onMount(async () => {
    try {
      const res = await fetch(`/api/evidence?holdingId=${holdingId}`, { cache: 'no-store' });
      if (res.ok) {
        evidence = await res.json();
      }
    } finally {
      loading = false;
    }
  });

  function riskPercent(score: number): number {
    return Math.round((1 - (score + 1) / 2) * 100);
  }

  function riskClass(score: number): string {
    if (score > 0.3) return 'low';
    if (score > -0.3) return 'medium';
    return 'high';
  }

  function sentimentLabel(score: number): string {
    if (score > 0.3) return 'Positive';
    if (score > -0.3) return 'Neutral';
    return 'Negative';
  }

  function sentimentClass(score: number): string {
    if (score > 0.3) return 'badge-approved';
    if (score > -0.3) return 'badge-pending';
    if (score <= -0.3) return 'badge-rejected';
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

{#if loading}
  <div class="content">
    <div class="skeleton-pad">
      <div class="skeleton" style="height:16px;"></div>
    </div>
  </div>
{:else}
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
          <div class="evidence-headline">{e.contentSnippet}</div>
          <div class="evidence-source">{e.sourceUrl}</div>
          <div class="evidence-date">{e.publishedAt}</div>
          <div class="risk-label">
            <span>Risk Score</span>
            <span style="font-weight:600;color:{riskPercent(e.aiSentimentScore ?? 0) < 30 ? 'var(--green)' : riskPercent(e.aiSentimentScore ?? 0) < 60 ? 'var(--amber)' : 'var(--red)'}">{riskPercent(e.aiSentimentScore ?? 0)}%</span>
          </div>
          <div class="risk-bar">
            <div class="risk-fill {riskClass(e.aiSentimentScore ?? 0)}" style="width:{riskPercent(e.aiSentimentScore ?? 0)}%"></div>
          </div>
          <span class="badge {sentimentClass(e.aiSentimentScore ?? 0)}">{sentimentLabel(e.aiSentimentScore ?? 0)}</span>
        </div>
      {/each}
    </div>
  {/if}
</div>
{/if}
