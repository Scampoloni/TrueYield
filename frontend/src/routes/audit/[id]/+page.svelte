<script lang="ts">
  import { page } from '$app/stores';
  import { onMount } from 'svelte';
  import ConfirmModal from '$lib/components/ConfirmModal.svelte';
  import { showToast } from '$lib/toast';

  const reportId = $page.params.id;
  let report: any = $state(null);
  let loading = $state(true);
  let comment = $state('');
  let submitting = $state(false);
  let showApproveModal = $state(false);
  let showRejectModal = $state(false);
  let openAccordions: Record<string, boolean> = $state({});

  const AUDITOR_ID = 'temp-auditor-123';

  onMount(async () => {
    report = {
      id: reportId,
      reportId: 'AUD-2026-001',
      portfolio: 'Green Energy Portfolio',
      status: 'UNDER_REVIEW',
      aiSummary: 'This portfolio demonstrates strong ESG compliance across renewable energy holdings. Neste Oyj shows positive sentiment with sustainable aviation fuel initiatives, though some concerns exist regarding palm oil sourcing. Orsted maintains excellent renewable energy credentials with minimal risk factors. Overall risk assessment: Low to Moderate.',
      holdings: [
        { symbol: 'NESTE', name: 'Neste Oyj', evidenceCount: 3 },
        { symbol: 'ORSTED', name: 'Orsted A/S', evidenceCount: 2 },
      ],
      comments: [
        { author: 'temp-user-123', date: 'Mar 15, 2026 10:30', text: 'Initial review completed. Risk assessment looks reasonable.' }
      ],
      approvedAt: null
    };
    loading = false;
  });

  const STATES = ['DRAFT', 'AI_ANALYZING', 'PENDING_REVIEW', 'UNDER_REVIEW', 'APPROVED'];

  function stateIndex(s: string) { return STATES.indexOf(s); }

  function statusClass(s: string) {
    if (s === 'APPROVED') return 'approved';
    if (s === 'REJECTED') return 'rejected';
    if (s === 'UNDER_REVIEW') return 'under-review';
    if (s === 'PENDING_REVIEW') return 'pending';
    return 'pending';
  }

  function statusLabel(s: string) { return s.replace(/_/g, ' '); }

  async function assignReport() {
    try {
      await fetch('http://localhost:8080/api/service/auditreport/assign', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ auditReportId: reportId, auditorId: AUDITOR_ID })
      });
      report = { ...report, status: 'UNDER_REVIEW' };
      showToast('Report assigned to you');
    } catch { showToast('Failed to assign report', 'error'); }
  }

  async function approveReport() {
    try {
      await fetch('http://localhost:8080/api/service/auditreport/complete', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ auditReportId: reportId, auditorId: AUDITOR_ID })
      });
      report = { ...report, status: 'APPROVED', approvedAt: new Date().toLocaleDateString('en-US', { month:'short', day:'numeric', year:'numeric' }) };
      showApproveModal = false;
      showToast('Report approved successfully');
    } catch { showToast('Failed to approve report', 'error'); }
  }

  function rejectReport() {
    report = { ...report, status: 'REJECTED' };
    showRejectModal = false;
    showToast('Report rejected');
  }

  async function submitComment() {
    if (!comment.trim()) return;
    submitting = true;
    await new Promise(r => setTimeout(r, 400));
    report = { ...report, comments: [...report.comments, { author: AUDITOR_ID, date: new Date().toLocaleString('en-US', { month:'short', day:'numeric', year:'numeric', hour:'2-digit', minute:'2-digit' }), text: comment }] };
    comment = '';
    submitting = false;
    showToast('Comment added');
  }
</script>

{#if loading}
  <div class="topbar"><div><div class="skeleton" style="width:240px;height:22px;"></div></div></div>
  <div class="content">{#each [1,2,3,4] as _}<div class="skeleton" style="height:16px;margin-bottom:14px;"></div>{/each}</div>
{:else if report}
  <div class="topbar">
    <div>
      <div class="page-title">Audit Report {report.reportId}</div>
      <div class="page-subtitle">{report.portfolio}</div>
    </div>
    <div class="topbar-actions">
      {#if report.status === 'PENDING_REVIEW'}
        <button class="btn btn-primary" onclick={assignReport}>Assign to me</button>
      {:else if report.status === 'UNDER_REVIEW'}
        <button class="btn btn-success" onclick={() => showApproveModal = true}>Approve</button>
        <button class="btn btn-danger" onclick={() => showRejectModal = true}>Reject</button>
      {/if}
    </div>
  </div>

  <div class="content">
    <span class="status-large {statusClass(report.status)}">{statusLabel(report.status)}</span>

    <div class="table-wrap" style="padding:24px 28px;margin-bottom:24px;">
      <div class="timeline">
        {#each STATES as state, i}
          {@const idx = stateIndex(report.status)}
          {@const isDone = i < idx}
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

    <div class="ai-summary">
      <div class="ai-icon">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <circle cx="8" cy="8" r="5.5" stroke="#4f8ef7" stroke-width="1.3"/>
          <path d="M6 8h4M8 6v4" stroke="#4f8ef7" stroke-width="1.3" stroke-linecap="round"/>
        </svg>
      </div>
      <div>
        <div class="ai-summary-title">AI Analysis Summary</div>
        <div class="ai-summary-text">{report.aiSummary}</div>
      </div>
    </div>

    <div class="section-hd" style="margin-bottom:0">
      <span class="section-title">Holdings & Evidence</span>
    </div>
    <div class="table-wrap" style="margin-bottom:24px;">
      {#each report.holdings as h}
        <div class="accordion-item">
          <!-- svelte-ignore a11y_no_static_element_interactions -->
          <div class="accordion-header" onclick={() => openAccordions[h.symbol] = !openAccordions[h.symbol]}>
            <div>
              <div class="accordion-title">{h.symbol}</div>
              <div class="accordion-sub">{h.name}</div>
            </div>
            <div class="accordion-meta">
              <span>{h.evidenceCount} evidence item{h.evidenceCount !== 1 ? 's' : ''}</span>
              <span style="transition:transform 0.2s;display:inline-block;transform:{openAccordions[h.symbol] ? 'rotate(180deg)' : 'rotate(0)'}">&#8595;</span>
            </div>
          </div>
          {#if openAccordions[h.symbol]}
            <div class="accordion-body" style="padding:0 0 16px;color:var(--text2);font-size:12px;">
              Sample evidence: Positive sustainability metrics identified for {h.name}.
            </div>
          {/if}
        </div>
      {/each}
    </div>

    <div class="section-hd">
      <span class="section-title">Comments</span>
    </div>
    <div class="table-wrap" style="padding:20px 24px;">
      {#each report.comments as c}
        <div class="comment">
          <div class="comment-header">
            <span class="comment-author">{c.author}</span>
            <span class="comment-date">{c.date}</span>
          </div>
          <div class="comment-text">{c.text}</div>
        </div>
      {/each}

      {#if report.status === 'APPROVED'}
        <div style="text-align:center;padding:12px 0;font-size:12px;color:var(--green);font-weight:600;">
          Approved on {report.approvedAt}
        </div>
      {:else if report.status === 'REJECTED'}
        <div style="text-align:center;padding:12px 0;font-size:12px;color:var(--red);font-weight:600;">
          Report rejected
        </div>
      {:else}
        <div class="comment-form">
          <textarea class="comment-textarea" bind:value={comment} placeholder="Add a comment..."></textarea>
          <button class="btn btn-primary btn-sm" style="margin-top:10px;" onclick={submitComment} disabled={submitting || !comment.trim()}>
            {submitting ? 'Submitting...' : 'Submit Comment'}
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
