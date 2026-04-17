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
  let portfolioRiskScore: number | null = $state(null);
  let comments: any[] = $state([]);
  let newComment = $state('');
  let submittingComment = $state(false);

  const isAuditor = $derived((page.data.user?.user_roles ?? []).includes('auditor'));

  const STATES = ['DRAFT', 'AI_ANALYZING', 'PENDING_REVIEW', 'UNDER_REVIEW', 'APPROVED', 'REJECTED'];

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
      if (hRes.ok) {
        holdings = await hRes.json();
        const evidencePerHolding = await Promise.all(
          holdings.map((h: any) => fetch(`/api/evidence?holdingId=${h.id}`).then(r => r.ok ? r.json() : []))
        );
        const allEvidence = evidencePerHolding.flat();
        if (allEvidence.length > 0) {
          const avg = allEvidence.reduce((sum: number, e: any) => sum + e.riskScore, 0) / allEvidence.length;
          portfolioRiskScore = Math.round(avg * 10) / 10;
        }
      }

      const cRes = await fetch(`/api/service/auditcomment?auditReportId=${reportId}`);
      if (cRes.ok) comments = await cRes.json();
    } finally {
      loading = false;
    }
  });

  function stateIndex(s: string) { return STATES.indexOf(s); }

  function toggleAccordion(symbol: string) {
    openAccordions[symbol] = !openAccordions[symbol];
  }

  function handleAccordionKeydown(event: KeyboardEvent, symbol: string) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      toggleAccordion(symbol);
    }
  }

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

  async function submitComment() {
    submittingComment = true;
    try {
      const res = await fetch('/api/service/auditcomment', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ auditReportId: reportId, comment: newComment.trim() })
      });
      if (!res.ok) throw new Error();
      const created = await res.json();
      comments = [...comments, created];
      newComment = '';
      showToast('Comment added');
    } catch {
      showToast('Failed to add comment', 'error');
    } finally {
      submittingComment = false;
    }
  }

  function formatDate(dt: string) {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('de-CH', { dateStyle: 'short', timeStyle: 'short' });
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

    {#if portfolioRiskScore !== null}
      <div class="risk-score-banner">
        <div class="risk-score-label">Overall Portfolio Risk Score</div>
        <div class="risk-score-value" style="color:{portfolioRiskScore < 4 ? 'var(--green,#4ade80)' : portfolioRiskScore < 7 ? 'var(--amber,#fbbf24)' : 'var(--red,#f87171)'}">
          {portfolioRiskScore}<span style="font-size:14px;opacity:0.6">/10</span>
        </div>
        <div class="risk-score-bar">
          <div class="risk-score-fill" style="width:{portfolioRiskScore * 10}%;background:{portfolioRiskScore < 4 ? 'var(--green,#4ade80)' : portfolioRiskScore < 7 ? 'var(--amber,#fbbf24)' : 'var(--red,#f87171)'}"></div>
        </div>
      </div>
    {/if}

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
            <div
              class="accordion-header"
              onclick={() => toggleAccordion(h.symbol)}
              onkeydown={(event) => handleAccordionKeydown(event, h.symbol)}
              role="button"
              tabindex="0"
              aria-expanded={openAccordions[h.symbol]}
            >
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
    <div class="table-wrap table-comments" style="margin-bottom:24px;">
      {#if comments.length === 0}
        <div class="empty" style="padding:24px;">
          <div class="e-ttl">No comments yet</div>
          <div class="e-sub">Comments added by the auditor will appear here.</div>
        </div>
      {:else}
        <div style="padding:16px;display:flex;flex-direction:column;gap:12px;">
          {#each comments as c}
            <div class="comment-card">
              <div class="comment-meta">
                <span class="comment-author">{c.auditorId}</span>
                <span class="comment-date">{formatDate(c.createdAt)}</span>
              </div>
              <div class="comment-text">{c.comment}</div>
            </div>
          {/each}
        </div>
      {/if}

      {#if isAuditor && report.auditStatus === 'UNDER_REVIEW'}
        <div class="comment-form">
          <textarea
            class="comment-input"
            bind:value={newComment}
            placeholder="Add your audit comment..."
            rows="3"
          ></textarea>
          <button
            class="btn btn-primary"
            onclick={submitComment}
            disabled={!newComment.trim() || submittingComment}
          >
            {submittingComment ? 'Adding...' : 'Add Comment'}
          </button>
        </div>
      {/if}
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

<style>
  .risk-score-banner {
    background: rgba(255,255,255,0.04);
    border: 1px solid rgba(255,255,255,0.08);
    border-radius: 12px;
    padding: 16px 20px;
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 20px;
  }
  .risk-score-label {
    font-size: 12px;
    color: var(--text-3, #7a90aa);
    flex: 1;
  }
  .risk-score-value {
    font-size: 28px;
    font-weight: 700;
    min-width: 70px;
    text-align: right;
  }
  .risk-score-bar {
    flex: 2;
    height: 6px;
    background: rgba(255,255,255,0.08);
    border-radius: 3px;
    overflow: hidden;
  }
  .risk-score-fill {
    height: 100%;
    border-radius: 3px;
    transition: width 0.4s ease;
  }
  .comment-card {
    background: rgba(255,255,255,0.04);
    border: 1px solid rgba(255,255,255,0.07);
    border-radius: 8px;
    padding: 12px 16px;
  }
  .comment-meta {
    display: flex;
    justify-content: space-between;
    margin-bottom: 6px;
  }
  .comment-author {
    font-size: 12px;
    font-weight: 600;
    color: var(--blue-light, #93c5fd);
  }
  .comment-date {
    font-size: 11px;
    color: var(--text-3, #7a90aa);
  }
  .comment-text {
    font-size: 13px;
    color: var(--text-2, #b4c6de);
    line-height: 1.5;
  }
  .comment-form {
    padding: 16px;
    border-top: 1px solid rgba(255,255,255,0.07);
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .comment-input {
    width: 100%;
    background: rgba(255,255,255,0.05);
    border: 1px solid rgba(255,255,255,0.1);
    border-radius: 8px;
    padding: 10px 12px;
    color: var(--text-1, #fff);
    font-size: 13px;
    resize: vertical;
    font-family: inherit;
    box-sizing: border-box;
  }
  .comment-input:focus {
    outline: none;
    border-color: var(--blue, #3b82f6);
  }
  .comment-input::placeholder {
    color: var(--text-3, #7a90aa);
  }
</style>
