<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';

  const isComplianceOfficer = $derived(
    (page.data.user?.user_roles ?? []).includes('compliance-officer')
  );

  interface ComplianceOverview {
    totalPortfolios: number;
    totalHoldings: number;
    totalAuditReports: number;
    reportsByStatus: Record<string, number>;
  }

  interface SfdrScore {
    portfolioId: string;
    portfolioName: string;
    classification: 'ARTICLE_9' | 'ARTICLE_8' | 'NON_SFDR' | 'INSUFFICIENT_DATA';
    averageSentiment: number;
    evidenceCount: number;
  }

  interface Portfolio {
    id: string;
    name: string;
    description: string;
    fundManagerId: string;
  }

  interface AuditReport {
    id: string;
    portfolioId: string;
    auditStatus: string;
    aiRiskSummary: string | null;
    auditorId: string | null;
    createdAt: string;
  }

  let activeTab = $state<'overview' | 'portfolios' | 'reports'>('overview');
  let overview: ComplianceOverview | null = $state(null);
  let sfdrScores: SfdrScore[] = $state([]);
  let portfolios: Portfolio[] = $state([]);
  let reports: AuditReport[] = $state([]);
  let loading = $state(true);
  let error = $state('');

  const STATUS_LABELS: Record<string, string> = {
    AI_ANALYZING: 'AI Analyzing',
    PENDING_REVIEW: 'Pending Review',
    UNDER_REVIEW: 'Under Review',
    APPROVED: 'Approved',
    REJECTED: 'Rejected'
  };

  const STATUS_COLORS: Record<string, string> = {
    AI_ANALYZING: 'badge-pending',
    PENDING_REVIEW: 'badge-pending',
    UNDER_REVIEW: 'badge-under-review',
    APPROVED: 'badge-approved',
    REJECTED: 'badge-rejected'
  };

  const SFDR_LABELS: Record<string, string> = {
    ARTICLE_9: 'Art. 9',
    ARTICLE_8: 'Art. 8',
    NON_SFDR: 'Non-SFDR',
    INSUFFICIENT_DATA: 'No Data'
  };

  const SFDR_COLORS: Record<string, string> = {
    ARTICLE_9: 'badge-approved',
    ARTICLE_8: 'badge-under-review',
    NON_SFDR: 'badge-rejected',
    INSUFFICIENT_DATA: 'badge-pending'
  };

  function formatDate(dt: string) {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('de-CH', { dateStyle: 'short', timeStyle: 'short' });
  }

  onMount(async () => {
    if (!isComplianceOfficer) {
      error = 'Access denied. This page requires the compliance-officer role.';
      loading = false;
      return;
    }
    try {
      const [overviewRes, sfdrRes, portfoliosRes, reportsRes] = await Promise.all([
        fetch('/api/compliance/overview', { cache: 'no-store' }),
        fetch('/api/compliance/sfdr', { cache: 'no-store' }),
        fetch('/api/compliance/portfolios', { cache: 'no-store' }),
        fetch('/api/compliance/reports', { cache: 'no-store' })
      ]);
      if (!overviewRes.ok) throw new Error(`HTTP ${overviewRes.status}`);
      overview = await overviewRes.json();
      if (sfdrRes.ok) sfdrScores = await sfdrRes.json();
      if (portfoliosRes.ok) portfolios = await portfoliosRes.json();
      if (reportsRes.ok) reports = await reportsRes.json();
    } catch (e) {
      error = 'Could not load compliance data. Make sure the backend is running.';
    } finally {
      loading = false;
    }
  });
</script>

<div class="topbar">
  <div>
    <div class="pg-ttl">Compliance Overview</div>
    <div class="pg-sub">System-wide ESG audit statistics — read-only view</div>
  </div>
</div>

<div class="content">
  {#if loading}
    <div class="metrics">
      {#each [1, 2, 3] as _}
        <div class="m-card blue">
          <div class="skeleton" style="width:60px;height:40px;margin-bottom:6px;"></div>
          <div class="skeleton" style="width:80px;height:16px;"></div>
        </div>
      {/each}
    </div>
  {:else if error}
    <div class="glass-table">
      <div class="empty">
        <div class="e-icon">⚠</div>
        <div class="e-ttl">Access Error</div>
        <div class="e-sub">{error}</div>
      </div>
    </div>
  {:else if overview}
    <div class="tab-bar">
      <button class="tab-btn" class:active={activeTab === 'overview'} onclick={() => activeTab = 'overview'}>Overview</button>
      <button class="tab-btn" class:active={activeTab === 'portfolios'} onclick={() => activeTab = 'portfolios'}>
        Portfolios
        {#if portfolios.length > 0}<span class="tab-count">{portfolios.length}</span>{/if}
      </button>
      <button class="tab-btn" class:active={activeTab === 'reports'} onclick={() => activeTab = 'reports'}>
        Audit Reports
        {#if reports.length > 0}<span class="tab-count">{reports.length}</span>{/if}
      </button>
    </div>

    {#if activeTab === 'overview'}
      <div class="metrics">
        <div class="m-card blue">
          <div class="m-icon-wrap iw-blue">
            <svg viewBox="0 0 16 16" fill="none">
              <rect x="1.5" y="4.5" width="13" height="9" rx="1.5" stroke="#93c5fd" stroke-width="1.3"/>
              <path d="M5 4.5V3.5a1.5 1.5 0 011.5-1.5h3a1.5 1.5 0 011.5 1.5v1" stroke="#93c5fd" stroke-width="1.3"/>
            </svg>
          </div>
          <div class="m-val blue">{overview.totalPortfolios}</div>
          <div class="m-lbl">Total Portfolios</div>
          <div class="m-trend tr-green">Across all fund managers</div>
        </div>

        <div class="m-card green">
          <div class="m-icon-wrap iw-green">
            <svg viewBox="0 0 16 16" fill="none">
              <path d="M2 4h12M2 8h8M2 12h5" stroke="#6ee7b7" stroke-width="1.3" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="m-val green">{overview.totalHoldings}</div>
          <div class="m-lbl">Total Holdings</div>
          <div class="m-trend tr-green">Across all portfolios</div>
        </div>

        <div class="m-card amber">
          <div class="m-icon-wrap iw-amber">
            <svg viewBox="0 0 16 16" fill="none">
              <path d="M8 2L14 13H2L8 2Z" stroke="#fcd34d" stroke-width="1.3" stroke-linejoin="round"/>
              <path d="M8 6v3" stroke="#fcd34d" stroke-width="1.3" stroke-linecap="round"/>
              <circle cx="8" cy="11" r="0.5" fill="#fcd34d" stroke="#fcd34d"/>
            </svg>
          </div>
          <div class="m-val amber">{overview.totalAuditReports}</div>
          <div class="m-lbl">Total Audit Reports</div>
          <div class="m-trend tr-amber">System-wide</div>
        </div>
      </div>

      <div class="sec-head">
        <span class="sec-name">Reports by Status</span>
      </div>

      <div class="glass-table">
        <table>
          <thead>
            <tr>
              <th>Status</th>
              <th class="text-right">Count</th>
            </tr>
          </thead>
          <tbody>
            {#each Object.entries(overview.reportsByStatus).filter(([, v]) => v > 0) as [status, count]}
              <tr>
                <td>
                  <span class="badge {STATUS_COLORS[status] ?? 'badge-active'}">
                    {STATUS_LABELS[status] ?? status}
                  </span>
                </td>
                <td class="text-right"><strong>{count}</strong></td>
              </tr>
            {:else}
              <tr>
                <td colspan="2">
                  <div class="empty">
                    <div class="e-ttl">No audit reports found</div>
                  </div>
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>

      {#if sfdrScores.length > 0}
        <div class="sec-head" style="margin-top:2rem;">
          <span class="sec-name">SFDR Classification by Portfolio</span>
          <span class="sec-meta" style="font-size:11px;color:var(--text-muted);">Based on AI sentiment scores across Evidence entries</span>
        </div>
        <div class="glass-table">
          <table>
            <thead>
              <tr>
                <th>Portfolio</th>
                <th>SFDR Class</th>
                <th class="text-right">Avg Sentiment</th>
                <th class="text-right">Evidence</th>
              </tr>
            </thead>
            <tbody>
              {#each sfdrScores as s}
                <tr>
                  <td><strong>{s.portfolioName}</strong></td>
                  <td>
                    <span class="badge {SFDR_COLORS[s.classification]}">
                      {SFDR_LABELS[s.classification]}
                    </span>
                  </td>
                  <td class="text-right" style="font-variant-numeric:tabular-nums;">
                    {s.classification === 'INSUFFICIENT_DATA' ? '—' : s.averageSentiment.toFixed(3)}
                  </td>
                  <td class="text-right">{s.evidenceCount}</td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
        <div class="sfdr-legend">
          <span><span class="badge badge-approved">Art. 9</span> avg &gt; 0.3 — sustainable investment objective</span>
          <span><span class="badge badge-under-review">Art. 8</span> avg &gt; −0.1 — promotes ESG characteristics</span>
          <span><span class="badge badge-rejected">Non-SFDR</span> avg ≤ −0.1 — predominant ESG risk signal</span>
        </div>
      {/if}
    {/if}

    {#if activeTab === 'portfolios'}
      <div class="glass-table">
        {#if portfolios.length === 0}
          <div class="empty" style="padding:32px;">
            <div class="e-ttl">No portfolios yet</div>
            <div class="e-sub">Fund managers haven't created any portfolios.</div>
          </div>
        {:else}
          <table>
            <thead>
              <tr>
                <th>Portfolio</th>
                <th>Description</th>
                <th>Fund Manager</th>
                <th class="text-right">SFDR</th>
              </tr>
            </thead>
            <tbody>
              {#each portfolios as p}
                {@const sfdr = sfdrScores.find(s => s.portfolioId === p.id)}
                <tr>
                  <td><strong>{p.name}</strong></td>
                  <td style="color:var(--text-muted,#94a3b8);font-size:12px;">{p.description || '—'}</td>
                  <td style="font-size:12px;color:var(--text-muted,#94a3b8);">{p.fundManagerId}</td>
                  <td class="text-right">
                    {#if sfdr}
                      <span class="badge {SFDR_COLORS[sfdr.classification]}">{SFDR_LABELS[sfdr.classification]}</span>
                    {:else}
                      <span class="badge badge-pending">No Data</span>
                    {/if}
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        {/if}
      </div>
    {/if}

    {#if activeTab === 'reports'}
      <div class="glass-table">
        {#if reports.length === 0}
          <div class="empty" style="padding:32px;">
            <div class="e-ttl">No audit reports yet</div>
            <div class="e-sub">No audit reports have been created.</div>
          </div>
        {:else}
          <table>
            <thead>
              <tr>
                <th>Report ID</th>
                <th>Portfolio</th>
                <th>Status</th>
                <th>Auditor</th>
                <th class="text-right">Created</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {#each reports as r}
                {@const portfolio = portfolios.find(p => p.id === r.portfolioId)}
                <tr>
                  <td style="font-size:11px;color:var(--text-muted,#94a3b8);font-family:monospace;">{r.id.slice(0, 8)}…</td>
                  <td><strong>{portfolio?.name ?? r.portfolioId}</strong></td>
                  <td>
                    <span class="badge {STATUS_COLORS[r.auditStatus] ?? 'badge-pending'}">
                      {STATUS_LABELS[r.auditStatus] ?? r.auditStatus}
                    </span>
                  </td>
                  <td style="font-size:12px;color:var(--text-muted,#94a3b8);">{r.auditorId ?? '—'}</td>
                  <td class="text-right" style="font-size:12px;">{formatDate(r.createdAt)}</td>
                  <td class="text-right">
                    <a href="/audit/{r.id}" class="xb xb-blue">View →</a>
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        {/if}
      </div>
    {/if}
  {/if}
</div>

<style>
  /* Ensure right-aligned columns work on both th and td */
  :global(th.text-right) {
    text-align: right;
  }
  .tab-bar {
    display: flex;
    gap: 4px;
    margin-bottom: 20px;
    border-bottom: 1px solid rgba(255,255,255,0.08);
    padding-bottom: 0;
  }
  .tab-btn {
    background: none;
    border: none;
    color: var(--text-3, #7a90aa);
    font-size: 13px;
    font-weight: 500;
    padding: 8px 16px;
    cursor: pointer;
    border-bottom: 2px solid transparent;
    margin-bottom: -1px;
    transition: color 0.15s, border-color 0.15s;
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .tab-btn:hover {
    color: var(--text-1, #fff);
  }
  .tab-btn.active {
    color: var(--blue-light, #93c5fd);
    border-bottom-color: var(--blue-light, #93c5fd);
  }
  .tab-count {
    background: rgba(147,197,253,0.15);
    color: var(--blue-light, #93c5fd);
    font-size: 10px;
    font-weight: 600;
    padding: 1px 6px;
    border-radius: 10px;
  }
  .sfdr-legend {
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;
    margin-top: 0.75rem;
    font-size: 11px;
    color: var(--text-muted, #94a3b8);
  }
  .sfdr-legend span {
    display: flex;
    align-items: center;
    gap: 0.4rem;
  }
</style>
