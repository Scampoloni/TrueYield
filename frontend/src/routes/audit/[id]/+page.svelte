<script lang="ts">
  import { page } from '$app/state';
  import { onMount } from 'svelte';
  import ConfirmModal from '$lib/components/ConfirmModal.svelte';
  import { showToast } from '$lib/toast';

  const reportId = page.params.id;
  let report: any = $state(null);
  let portfolioName = $state('');
  let holdings: any[] = $state([]);
  let loading = $state(true);
  let showApproveModal = $state(false);
  let showRejectModal = $state(false);
  let openAccordions: Record<string, boolean> = $state({});

  const STATES = ['DRAFT', 'AI_ANALYZING', 'PENDING_REVIEW', 'UNDER_REVIEW', 'APPROVED'];

  onMount(async () => {
    try {
      const rRes = await fetch(`/api/service/auditreport/${reportId}`);
      if (!rRes.ok) { loading = false; return; }
      report = await rRes.json();

      const [pRes, hRes] = await Promise.all([
        fetch(`/api/portfolio/${report.portfolioId}`),
        fetch(`/api/holding?portfolioId=${report.portfolioId}`)
      ]);
      if (pRes.ok) { const p = await pRes.json(); portfolioName = p.name; }
      if (hRes.ok) holdings = await hRes.json();
    } finally {
      loading = false;
    }
  });

  function stateIndex(s: string) { return STATES.indexOf(s); }

  function statusClass(s: string) {
    if (s === 'APPROVED')     return 'approved';
    if (s === 'REJECTED')     return 'rejected';
    if (s === 'UNDER_REVIEW') return 'under-review';
    return 'pending';
  }

  function statusLabel(s: string) { return s.replace(/_/g, ' '); }

  const auditorId = () => page.data.user?.sub ?? '';

  async function assignReport() {
    try {
      const res = await fetch('/api/service/auditreport/assign', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ auditReportId: reportId, auditorId: auditorId() })
      });
      if (!res.ok) throw new Error();
      report = { ...report, auditStatus: 'UNDER_REVIEW', auditorId: auditorId() };
      showToast('Report assigned to you');
    } catch {
      showToast('Failed to assign report', 'error');
    }
  }

  async function approveReport() {
    try {
      const res = await fetch('/api/service/auditreport/complete', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ auditReportId: reportId, auditorId: auditorId() })
      });
      if (!res.ok) throw new Error();
      report = { ...report, auditStatus: 'APPROVED' };
      showApproveModal = false;
      showToast('Report approved successfully');
    } catch {
      showToast('Failed to approve report', 'error');
    }
  }

  async function rejectReport() {
    try {
      const res = await fetch('/api/service/auditreport/reject', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ auditReportId: reportId, auditorId: auditorId() })
      });
      if (!res.ok) throw new Error();
      report = { ...report, auditStatus: 'REJECTED' };
      showRejectModal = false;
      showToast('Report rejected');
    } catch {
      showToast('Failed to reject report', 'error');
    }
  }
</script>

{#if loading}
  <div class="topbar"><div><div class="skeleton" style="width:240px;height:22px;"></div></div></div>
  <div class="content">
    <div class="skeleton-pad">
      <div class="skeleton" style="height:16px;"></div>
      <div class="skeleton" style="height:16px;"></div>
      <div class="skeleton" style="height:16px;"></div>
      <div class="skeleton" style="height:16px;"></div>
    </div>
  </div>
{:else if report}
  <div class="topbar">
    <div>
      <div class="page-title">Audit Report</div>
      <div class="page-subtitle">{portfolioName || report.portfolioId}</div>
    </div>
    <div class="topbar-actions">
      {#if report.auditStatus === 'PENDING_REVIEW'}
        <button class="btn btn-primary" onclick={assignReport}>Assign to me</button>
      {:else if report.auditStatus === 'UNDER_REVIEW'}
        <button class="btn btn-success" onclick={() => showApproveModal = true}>Approve</button>
        <button class="btn btn-danger"  onclick={() => showRejectModal = true}>Reject</button>
      {/if}
    </div>
  </div>

  <div class="content">
    <span class="status-large {statusClass(report.auditStatus)}">{statusLabel(report.auditStatus)}</span>

    <div class="table-wrap table-section">
      <div class="timeline">
        {#each STATES as state, i}
          {@const idx       = stateIndex(report.auditStatus)}
          {@const isDone    = i < idx}
          {@const isCurrent = i === idx}
          <div class="timeline-step">
            <div class="timeline-dot" class:done={isDone} class:current={isCurrent}></div>
            <div class="timeline-label" class:done={isDone} class:current={isCurrent}>
              {state.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())}
            </div>
          </div>
          {#if i < STATES.length - 1}
            <div class="timeline-line" class:done={i < idx} class:current={i === idx - 1}></div>
          {/if}
        {/each}
      </div>
    </div>

    {#if report.aiRiskSummary}
      <div class="ai-summary">
        <div class="ai-icon">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <circle cx="8" cy="8" r="5.5" stroke="#4f8ef7" stroke-width="1.3"/>
            <path d="M6 8h4M8 6v4" stroke="#4f8ef7" stroke-width="1.3" stroke-linecap="round"/>
          </svg>
        </div>
        <div>
          <div class="ai-summary-title">AI Analysis Summary</div>
          <div class="ai-summary-text">{report.aiRiskSummary}</div>
        </div>
      </div>
    {/if}

    <div class="section-hd">
      <span class="section-title">Holdings & Evidence</span>
    </div>
    <div class="table-wrap" style="margin-bottom:24px;">
      {#if holdings.length === 0}
        <div class="empty" style="padding:24px;">
          <div class="e-ttl">No holdings</div>
          <div class="e-sub">No holdings found for this portfolio.</div>
        </div>
      {:else}
        {#each holdings as h}
          <div class="accordion-item">
            <!-- svelte-ignore a11y_no_static_element_interactions -->
            <div class="accordion-header" onclick={() => openAccordions[h.symbol] = !openAccordions[h.symbol]}>
              <div>
                <div class="accordion-title">{h.symbol}</div>
                <div class="accordion-sub">{h.name || '—'}</div>
              </div>
              <div class="accordion-meta">
                <a href="/portfolios/{report.portfolioId}/holdings/{h.id}" class="xb xb-blue" onclick={(e) => e.stopPropagation()}>View Evidence →</a>
                <span style="display:inline-block;transition:transform 0.2s;transform:{openAccordions[h.symbol] ? 'rotate(180deg)' : 'rotate(0deg)'}">&#8595;</span>
              </div>
            </div>
            {#if openAccordions[h.symbol]}
              <div class="accordion-body">
                ISIN: {h.isin || '—'} · Weight: {h.weightPercent != null ? h.weightPercent + '%' : '—'}
              </div>
            {/if}
          </div>
        {/each}
      {/if}
    </div>

    <div class="section-hd">
      <span class="section-title">Comments</span>
    </div>
    <div class="table-wrap table-comments">
      <div class="empty" style="padding:24px;">
        <div class="e-ttl">Comments coming soon</div>
        <div class="e-sub">Comment functionality is planned for a future release.</div>
      </div>
    </div>
  </div>
{/if}

{#if showApproveModal}
  <ConfirmModal
    title="Approve Report"
    description="Confirm ESG compliance for this portfolio? This action is final and cannot be reversed."
    confirmLabel="Approve"
    onConfirm={approveReport}
    onCancel={() => showApproveModal = false}
  />
{/if}

{#if showRejectModal}
  <ConfirmModal
    title="Reject Report"
    description="Reject this audit report? The portfolio will be flagged as non-compliant."
    confirmLabel="Reject"
    danger={true}
    onConfirm={rejectReport}
    onCancel={() => showRejectModal = false}
  />
{/if}
