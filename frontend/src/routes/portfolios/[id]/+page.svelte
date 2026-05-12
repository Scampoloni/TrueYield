<script lang="ts">
  import { page } from '$app/state';
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import ConfirmModal from '$lib/components/ConfirmModal.svelte';
  import DonutChart from '$lib/components/DonutChart.svelte';
  import { showToast } from '$lib/toast';

  const id = page.params.id;
  const isFundManager = $derived((page.data.user?.user_roles ?? []).includes('fund-manager'));
  let portfolio: any = $state(null);
  let holdings: any[] = $state([]);
  let loading = $state(true);
  let deleteHoldingTarget: any = $state(null);
  let latestAuditReport: any = $state(null);
  let auditComments: any[] = $state([]);

  onMount(async () => {
    try {
      const res = await fetch(`/api/portfolio/${id}`, { cache: 'no-store' });
      if (!res.ok) { goto('/portfolios'); return; }
      portfolio = await res.json();
      const hRes = await fetch(`/api/holding?portfolioId=${id}`);
      holdings = hRes.ok ? await hRes.json() : [];

      const arRes = await fetch(`/api/service/auditreport/latest?portfolioId=${id}`);
      if (arRes.ok) {
        latestAuditReport = await arRes.json();
        if (latestAuditReport?.id) {
          const cRes = await fetch(`/api/service/auditcomment?auditReportId=${latestAuditReport.id}`);
          if (cRes.ok) auditComments = await cRes.json();
        }
      }
    } finally {
      loading = false;
    }
  });

  function formatDate(dt: string) {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('de-CH', { dateStyle: 'short', timeStyle: 'short' });
  }

  async function triggerAnalysis() {
    try {
      const res = await fetch('/api/service/auditreport', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ portfolioId: id })
      });

      if (res.ok) {
        showToast('AI analysis started — check Audit Dashboard', 'success');
      } else {
        showToast('Failed to start analysis', 'error');
      }
    } catch {
      showToast('Failed to start analysis', 'error');
    }
  }
</script>

{#if loading}
  <div class="topbar"><div><div class="skeleton" style="width:200px;height:22px;"></div></div></div>
  <div class="content">
    <div class="skeleton-pad">
      <div class="skeleton" style="height:16px;"></div>
      <div class="skeleton" style="height:16px;"></div>
      <div class="skeleton" style="height:16px;"></div>
    </div>
  </div>
{:else if portfolio}
  <div class="topbar">
    <div>
      <div class="pg-ttl">{portfolio.name}</div>
      <div class="pg-sub">{portfolio.description || 'No description'}</div>
    </div>
    <div class="btns">
      {#if isFundManager}
        <a href={`/portfolios/${id}/edit`} class="btn btn-ghost">Edit</a>
        <a href={`/portfolios/${id}/holdings/create`} class="btn btn-primary">+ Add Holding</a>
        <button class="btn btn-success" onclick={() => triggerAnalysis()}>
          Analyse starten
        </button>
      {/if}
    </div>
  </div>

  <div class="content">
    {#if latestAuditReport && (latestAuditReport.auditStatus === 'REJECTED' || latestAuditReport.auditStatus === 'APPROVED')}
      {@const isRejected = latestAuditReport.auditStatus === 'REJECTED'}
      <div class="audit-feedback-banner" class:rejected={isRejected} class:approved={!isRejected}>
        <div class="afb-header">
          <div class="afb-icon">
            {#if isRejected}
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <circle cx="8" cy="8" r="5.5" stroke="#f87171" stroke-width="1.3"/>
                <path d="M5.5 5.5l5 5M10.5 5.5l-5 5" stroke="#f87171" stroke-width="1.3" stroke-linecap="round"/>
              </svg>
            {:else}
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <circle cx="8" cy="8" r="5.5" stroke="#4ade80" stroke-width="1.3"/>
                <path d="M5 8l2 2 4-4" stroke="#4ade80" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            {/if}
          </div>
          <span class="afb-title">Audit {isRejected ? 'Rejected' : 'Approved'}</span>
          <a href={`/audit/${latestAuditReport.id}`} class="xb xb-blue" style="margin-left:auto">View Full Report →</a>
        </div>

        {#if latestAuditReport.aiRiskScore != null}
          {@const score = latestAuditReport.aiRiskScore}
          {@const scoreColor = score <= 3 ? '#4ade80' : score <= 6 ? '#fbbf24' : '#f87171'}
          <div class="afb-score-row">
            <span class="afb-score-lbl">AI Risk Score:</span>
            <span class="afb-score-val" style="color:{scoreColor}">{score}/10</span>
            <div class="afb-score-bar">
              <div class="afb-score-fill" style="width:{score * 10}%;background:{scoreColor}"></div>
            </div>
          </div>
          {#if latestAuditReport.aiRiskRationale}
            <div class="afb-rationale">{latestAuditReport.aiRiskRationale}</div>
          {/if}
        {/if}

        {#if auditComments.length > 0}
          <div class="afb-comments-lbl">Auditor Comments:</div>
          {#each auditComments as c}
            <div class="afb-comment">
              <span class="afb-comment-author">{c.auditorId}</span>
              <span class="afb-comment-date">{formatDate(c.createdAt)}</span>
              <div class="afb-comment-text">{c.comment}</div>
            </div>
          {/each}
        {/if}
      </div>
    {/if}

    <DonutChart {holdings} />
    <div class="sec-head">
      <span class="sec-name">Holdings</span>
      <span class="text-muted" style="font-size:12px;">{holdings.length} position{holdings.length !== 1 ? 's' : ''}</span>
    </div>
    <div class="glass-table">
      {#if holdings.length === 0}
        <div class="empty">
          <div class="e-icon">
            <svg width="24" height="24" viewBox="0 0 16 16" fill="none">
              <path d="M2 4h12M2 8h8M2 12h10" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="e-ttl">No holdings yet</div>
          <div class="e-sub">Add your first holding to this portfolio to begin ESG analysis.</div>
          <a href={`/portfolios/${id}/holdings/create`} class="btn btn-primary">+ Add Holding</a>
        </div>
      {:else}
        <table>
          <thead>
            <tr>
              <th>Symbol</th>
              <th>Company</th>
              <th>ISIN</th>
              <th>Weight %</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {#each holdings as h}
              <tr>
                <td><div class="pf-name">{h.symbol}</div></td>
                <td>{h.name || '—'}</td>
                <td><span class="pf-id">{h.isin || '—'}</span></td>
                <td>{h.weightPercent != null ? h.weightPercent + '%' : '—'}</td>
                <td>
                  <div class="tbl-acts">
                    <a href={`/portfolios/${id}/holdings/${h.id}`} class="xb xb-blue">View Evidence →</a>
                    {#if isFundManager}
                      <button class="xb xb-red" onclick={() => deleteHoldingTarget = h}>Delete</button>
                    {/if}
                  </div>
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      {/if}
    </div>
  </div>
{/if}

{#if deleteHoldingTarget}
  <ConfirmModal
    title="Delete Holding"
    description="Remove {deleteHoldingTarget.symbol} from this portfolio? This cannot be undone."
    confirmLabel="Delete"
    danger={true}
    onConfirm={async () => {
      const res = await fetch(`/api/holding/${deleteHoldingTarget.id}`, { method: 'DELETE' });
      if (res.ok) {
        holdings = holdings.filter(h => h.id !== deleteHoldingTarget.id);
        showToast('Holding removed');
      } else {
        showToast('Failed to delete holding', 'error');
      }
      deleteHoldingTarget = null;
    }}
    onCancel={() => deleteHoldingTarget = null}
  />
{/if}

<style>
  .audit-feedback-banner {
    border-radius: 12px;
    padding: 16px 20px;
    margin-bottom: 20px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    border: 1px solid;
  }
  .audit-feedback-banner.rejected {
    background: rgba(248, 113, 113, 0.06);
    border-color: rgba(248, 113, 113, 0.25);
  }
  .audit-feedback-banner.approved {
    background: rgba(74, 222, 128, 0.06);
    border-color: rgba(74, 222, 128, 0.25);
  }
  .afb-header {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .afb-icon { display: flex; align-items: center; }
  .afb-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-1, #fff);
  }
  .afb-score-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .afb-score-lbl {
    font-size: 12px;
    color: var(--text-3, #7a90aa);
    white-space: nowrap;
  }
  .afb-score-val {
    font-size: 20px;
    font-weight: 700;
    min-width: 50px;
  }
  .afb-score-bar {
    flex: 1;
    height: 5px;
    background: rgba(255,255,255,0.08);
    border-radius: 3px;
    overflow: hidden;
  }
  .afb-score-fill {
    height: 100%;
    border-radius: 3px;
    transition: width 0.4s ease;
  }
  .afb-rationale {
    font-size: 12px;
    color: var(--text-2, #b4c6de);
    line-height: 1.5;
  }
  .afb-comments-lbl {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-3, #7a90aa);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-top: 4px;
  }
  .afb-comment {
    background: rgba(255,255,255,0.03);
    border: 1px solid rgba(255,255,255,0.07);
    border-radius: 8px;
    padding: 8px 12px;
  }
  .afb-comment-author {
    font-size: 11px;
    font-weight: 600;
    color: var(--blue-light, #93c5fd);
    margin-right: 8px;
  }
  .afb-comment-date {
    font-size: 11px;
    color: var(--text-3, #7a90aa);
  }
  .afb-comment-text {
    font-size: 12px;
    color: var(--text-2, #b4c6de);
    margin-top: 4px;
    line-height: 1.5;
  }
</style>
