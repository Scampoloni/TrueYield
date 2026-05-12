<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { showToast } from '$lib/toast';

  const portfolioId = page.params.id;
  const holdingId = page.params.hid;
  const isFundManager = $derived((page.data.user?.user_roles ?? []).includes('fund-manager'));
  const canAddEvidence = $derived((page.data.user?.user_roles ?? [])
    .some((r: string) => r === 'auditor' || r === 'compliance-officer'));
  const returnTo = $derived(page.url.searchParams.get('returnTo') ?? `/portfolios/${portfolioId}`);

  let evidence: any[] = $state([]);
  let loading = $state(true);
  let deleting = $state<string | null>(null);

  onMount(async () => {
    try {
      const res = await fetch(`/api/evidence?holdingId=${holdingId}`, { cache: 'no-store' });
      if (res.ok) {
        const items = await res.json();
        evidence = items.sort((a: any, b: any) => b.riskScore - a.riskScore);
      }
    } finally {
      loading = false;
    }
  });

  async function deleteEvidence(id: string) {
    if (!confirm('Delete this evidence entry?')) return;
    deleting = id;
    try {
      const res = await fetch(`/api/evidence/${id}`, { method: 'DELETE' });
      if (res.ok || res.status === 204) {
        evidence = evidence.filter(e => e.id !== id);
      }
    } finally {
      deleting = null;
    }
  }

  function riskClass(score: number): string {
    if (score > 0.3) return 'low';
    if (score > -0.3) return 'medium';
    return 'high';
  }

  function confidence(e: any): 'HIGH' | 'MEDIUM' | 'LOW' {
    const isPremium = /reuters|bloomberg|financial times|wsj|the guardian/i.test(e.sourceName ?? '');
    const mag = Math.abs(1 - (e.riskScore ?? 5) / 5);
    if (isPremium && mag >= 0.3) return 'HIGH';
    if (isPremium || mag >= 0.2) return 'MEDIUM';
    return 'LOW';
  }

  let fetchingNews = $state(false);
  let fetchNewsStatus = $state('');

  async function fetchNews() {
    fetchingNews = true;
    fetchNewsStatus = '';
    try {
      // Check which providers are configured first
      const statusRes = await fetch('/api/holding/news-provider-status');
      if (statusRes.ok) {
        const providerStatus: Record<string, boolean> = await statusRes.json();
        const configuredProviders = Object.entries(providerStatus)
          .filter(([, ok]) => ok)
          .map(([name]) => name);
        if (configuredProviders.length === 0) {
          showToast('No news providers configured — add API keys to the backend .env', 'error');
          return;
        }
      }

      const initialCount = evidence.length;
      const ingestRes = await fetch(`/api/holding/${holdingId}/ingest-news`, { method: 'POST' });
      if (!ingestRes.ok) {
        showToast(`News ingestion failed (HTTP ${ingestRes.status})`, 'error');
        return;
      }

      fetchNewsStatus = 'Analysing articles…';
      const deadline = Date.now() + 25000;
      let found = false;
      while (Date.now() < deadline) {
        await new Promise(r => setTimeout(r, 2000));
        const res = await fetch(`/api/evidence?holdingId=${holdingId}`, { cache: 'no-store' });
        if (!res.ok) {
          showToast(`Evidence fetch failed (HTTP ${res.status})`, 'error');
          break;
        }
        const items = await res.json();
        if (items.length > initialCount) {
          evidence = items.sort((a: any, b: any) => b.riskScore - a.riskScore);
          showToast(`${items.length - initialCount} new article(s) added`);
          found = true;
          break;
        }
      }
      if (!found) {
        fetchNewsStatus = '';
        showToast('No new articles found — providers may be rate-limited or holding name not recognised', 'error');
      }
    } finally {
      fetchingNews = false;
      fetchNewsStatus = '';
    }
  }
</script>

<div class="topbar">
  <div>
    <div class="page-title">Holding {holdingId}</div>
    <div class="page-subtitle">Evidence & Risk Analysis</div>
  </div>
  <div style="display:flex;gap:0.5rem;">
    {#if isFundManager}
      <button onclick={fetchNews} disabled={fetchingNews} class="btn btn-ghost" style="border: 1px solid rgba(255,255,255,0.1);">
        {fetchingNews ? (fetchNewsStatus || 'Fetching…') : 'Fetch AI News'}
      </button>
    {/if}
    {#if canAddEvidence}
      <a href={`/portfolios/${portfolioId}/holdings/${holdingId}/create`} class="btn btn-primary">+ Add Evidence</a>
    {/if}
    <a href={returnTo} class="btn btn-ghost">← Back</a>
  </div>
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
        <div class="empty-desc">Add ESG news snippets via "+ Add Evidence". The AI will automatically analyse the sentiment and assign a risk score.</div>
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
          <div style="display:flex; justify-content:space-between; align-items:flex-start; gap: 8px;">
            <div class="evidence-headline">{e.headline}</div>
            <div style="display:flex;gap:5px;align-items:flex-start;flex-shrink:0;">
              <span class="conf-badge conf-{confidence(e).toLowerCase()}">{confidence(e)}</span>
              <span class="badge-source">{e.sourceName || 'Web'}</span>
            </div>
          </div>
          <div class="evidence-source">{e.summary}</div>
          <div class="evidence-date">{e.createdAt}</div>
          {#if e.sourceUrl}
            <a class="evidence-link" href={e.sourceUrl} target="_blank" rel="noopener noreferrer">
              <svg width="11" height="11" viewBox="0 0 12 12" fill="none" style="vertical-align:middle;margin-right:3px"><path d="M5 2H2a1 1 0 00-1 1v7a1 1 0 001 1h7a1 1 0 001-1V7M8 1h3m0 0v3m0-3L5 7" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>
              Read article
            </a>
          {/if}
          <div class="risk-label">
            <span>Risk Score</span>
            <span style="font-weight:600;color:{e.riskScore < 3 ? 'var(--green)' : e.riskScore < 6 ? 'var(--amber)' : 'var(--red)'}">{e.riskScore}/10</span>
          </div>
          <div class="risk-bar">
            <div class="risk-fill {riskClass((5 - e.riskScore) / 5)}" style="width:{Math.round((e.riskScore / 10) * 100)}%"></div>
          </div>
          <div style="display:flex;align-items:center;justify-content:space-between;margin-top:6px;">
            <span class="badge {e.sentiment === 'POSITIVE' ? 'badge-approved' : e.sentiment === 'NEUTRAL' ? 'badge-pending' : 'badge-rejected'}">{e.sentiment}</span>
            {#if isFundManager}
              <button
                class="btn-delete"
                disabled={deleting === e.id}
                onclick={() => deleteEvidence(e.id)}
                aria-label="Delete evidence"
              >
                {deleting === e.id ? '…' : '🗑'}
              </button>
            {/if}
          </div>
        </div>
      {/each}
    </div>
  {/if}
</div>
{/if}

<style>
  .conf-badge {
    font-size: 10px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    border-radius: 4px;
    padding: 2px 6px;
    border: 1px solid;
    white-space: nowrap;
    flex-shrink: 0;
    line-height: 1.6;
  }
  .conf-high {
    background: rgba(16, 185, 129, 0.15);
    color: #10b981;
    border-color: rgba(16, 185, 129, 0.3);
  }
  .conf-medium {
    background: rgba(245, 158, 11, 0.15);
    color: #f59e0b;
    border-color: rgba(245, 158, 11, 0.3);
  }
  .conf-low {
    background: rgba(239, 68, 68, 0.15);
    color: #ef4444;
    border-color: rgba(239, 68, 68, 0.3);
  }

  .evidence-link {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    margin-top: 6px;
    padding: 3px 8px;
    border-radius: 4px;
    background: rgba(59,130,246,0.12);
    border: 1px solid rgba(59,130,246,0.25);
    color: #93c5fd;
    text-decoration: none;
    font-size: 11px;
    font-weight: 500;
    transition: background 0.15s;
  }
  .evidence-link:hover {
    background: rgba(59,130,246,0.22);
  }
  .btn-delete {
    background: none;
    border: none;
    cursor: pointer;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
    opacity: 0.6;
    transition: opacity 0.15s, background 0.15s;
  }
  .btn-delete:hover:not(:disabled) {
    opacity: 1;
    background: rgba(239,68,68,0.15);
  }
  .btn-delete:disabled {
    cursor: not-allowed;
  }
</style>
