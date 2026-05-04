<script lang="ts">
  import { onMount } from 'svelte';
  import ConfirmModal from '$lib/components/ConfirmModal.svelte';
  import { showToast } from '$lib/toast';

  import { page } from '$app/state';

  const isFundManager = $derived((page.data.user?.user_roles ?? []).includes('fund-manager'));
  let portfolios: any[] = $state([]);
  let loading = $state(true);
  let deleteTarget: any = $state(null);

  const kpiTotal = $derived(portfolios.length);
  const kpiPending = $derived(portfolios.filter(p => p.auditStatus === 'PENDING_REVIEW' || p.auditStatus === 'AI_ANALYZING').length);
  const kpiApproved = $derived(portfolios.filter(p => p.auditStatus === 'APPROVED').length);

  let sfdrMap: Record<string, string> = $state({});

  function sfdrDotColor(cls: string): string {
    if (cls === 'ARTICLE_9') return '#10b981';
    if (cls === 'ARTICLE_8') return '#f59e0b';
    return '#3d4a5e';
  }

  function sfdrLabel(cls: string): string {
    if (cls === 'ARTICLE_9') return 'Art. 9';
    if (cls === 'ARTICLE_8') return 'Art. 8';
    return '—';
  }

  onMount(async () => {
    try {
      const roles: string[] = page.data.user?.user_roles ?? [];
      const canSeeSfdr = roles.includes('fund-manager') || roles.includes('compliance-officer');
      const [pfRes, sfdrRes] = await Promise.all([
        fetch('/api/portfolio', { cache: 'no-store' }),
        canSeeSfdr ? fetch('/api/compliance/sfdr', { cache: 'no-store' }) : Promise.resolve(null)
      ]);
      portfolios = await pfRes.json();
      if (sfdrRes?.ok) {
        const scores = await sfdrRes.json();
        sfdrMap = Object.fromEntries(scores.map((s: any) => [s.portfolioId, s.classification]));
      }
    } finally {
      loading = false;
    }
  });

  async function confirmDelete() {
    try {
      await fetch(`/api/portfolio/${deleteTarget.id}`, { method: 'DELETE' });
      portfolios = portfolios.filter(p => p.id !== deleteTarget.id);
      showToast('Portfolio deleted successfully');
    } catch {
      showToast('Failed to delete portfolio', 'error');
    } finally {
      deleteTarget = null;
    }
  }

  function statusClass(s: string) {
    if (!s) return 'badge-active';
    if (s.includes('REVIEW')) return 'badge-under-review';
    if (s.includes('PENDING')) return 'badge-pending';
    if (s.includes('APPROVED')) return 'badge-approved';
    if (s.includes('REJECTED')) return 'badge-rejected';
    return 'badge-active';
  }

  function statusLabel(s: string) {
    if (!s) return 'Active';
    return s.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }
</script>

<div class="topbar">
  <div>
    <div class="pg-ttl">Portfolios</div>
    <div class="pg-sub">
      {loading ? '...' : `${portfolios.length} portfolio${portfolios.length !== 1 ? 's' : ''} total`}
    </div>
  </div>
  {#if isFundManager}
    <a href="/portfolios/create" class="btn btn-primary">+ New Portfolio</a>
  {/if}
</div>

<div class="content">
  {#if isFundManager && !loading}
  <div class="metrics">
    <div class="m-card blue">
      <div class="m-icon-wrap iw-blue">
        <svg viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect x="1.5" y="4.5" width="13" height="9" rx="1.5" stroke="#93c5fd" stroke-width="1.3"/>
          <path d="M1.5 7.5h13" stroke="#93c5fd" stroke-width="1.3"/>
          <path d="M5 10.5h6" stroke="#93c5fd" stroke-width="1.3" stroke-linecap="round"/>
        </svg>
      </div>
      <div class="m-val blue">{kpiTotal}</div>
      <div class="m-lbl">Portfolios total</div>
    </div>
    <div class="m-card amber">
      <div class="m-icon-wrap iw-amber">
        <svg viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="8" cy="8" r="5.5" stroke="#fcd34d" stroke-width="1.3"/>
          <path d="M8 5.5V8.5" stroke="#fcd34d" stroke-width="1.3" stroke-linecap="round"/>
          <circle cx="8" cy="10.5" r="0.7" fill="#fcd34d"/>
        </svg>
      </div>
      <div class="m-val amber">{kpiPending}</div>
      <div class="m-lbl">Pending Review</div>
    </div>
    <div class="m-card green">
      <div class="m-icon-wrap iw-green">
        <svg viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="8" cy="8" r="5.5" stroke="#6ee7b7" stroke-width="1.3"/>
          <path d="M5.5 8l2 2 3-3" stroke="#6ee7b7" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <div class="m-val green">{kpiApproved}</div>
      <div class="m-lbl">Approved</div>
    </div>
  </div>
  {/if}
  <div class="glass-table">
    {#if loading}
      <div class="skeleton-pad">
        <div class="skeleton" style="height:20px;"></div>
        <div class="skeleton" style="height:20px;"></div>
        <div class="skeleton" style="height:20px;"></div>
      </div>
    {:else if portfolios.length === 0}
      <div class="empty">
        <div class="e-icon">
          <svg width="24" height="24" viewBox="0 0 16 16" fill="none">
            <rect x="1.5" y="4.5" width="13" height="9" rx="1.5" stroke="currentColor" stroke-width="1.3"/>
            <path d="M1.5 7.5h13" stroke="currentColor" stroke-width="1.3"/>
          </svg>
        </div>
        <div class="e-ttl">No portfolios yet</div>
        <div class="e-sub">Create your first investment portfolio to get started.</div>
        {#if isFundManager}
          <a href="/portfolios/create" class="btn btn-primary">+ New Portfolio</a>
        {/if}
      </div>
    {:else}
      <table>
        <thead>
          <tr>
            <th>Portfolio Name</th>
            <th>Description</th>
            <th>Manager ID</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {#each portfolios as p}
            <tr>
              <td><div class="pf-name">{p.name}</div></td>
              <td class="pf-desc">{p.description || '—'}</td>
              <td><span class="pf-id">{p.fundManagerId}</span></td>
              <td>
                <div class="status-cell">
                  <span class="badge {statusClass(p.auditStatus)}">{statusLabel(p.auditStatus)}</span>
                  {#if sfdrMap[p.id]}
                    <span class="sfdr-pill">
                      <span class="sfdr-dot" style="background:{sfdrDotColor(sfdrMap[p.id])}"></span>
                      <span class="sfdr-lbl">{sfdrLabel(sfdrMap[p.id])}</span>
                    </span>
                  {/if}
                </div>
              </td>
              <td>
                <div class="tbl-acts">
                  <a href={`/portfolios/${p.id}`} class="xb xb-blue">Holdings →</a>
                  {#if isFundManager}
                    <a href={`/portfolios/${p.id}/edit`} class="xb xb-ghost">Edit</a>
                    <button class="xb xb-red" onclick={() => deleteTarget = p}>Delete</button>
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

{#if deleteTarget}
  <ConfirmModal
    title="Delete Portfolio"
    description="Are you sure you want to delete '{deleteTarget.name}'? This action cannot be undone."
    confirmLabel="Delete"
    danger={true}
    onConfirm={confirmDelete}
    onCancel={() => deleteTarget = null}
  />
{/if}

<style>
  .m-card.blue::before {
    background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.5) 50%, transparent);
  }
  .m-card.amber::before {
    background: linear-gradient(90deg, transparent, rgba(245, 158, 11, 0.5) 50%, transparent);
  }
  .m-card.green::before {
    background: linear-gradient(90deg, transparent, rgba(16, 185, 129, 0.45) 50%, transparent);
  }

  .status-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .sfdr-pill {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  .sfdr-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .sfdr-lbl {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-3);
    white-space: nowrap;
  }
</style>
