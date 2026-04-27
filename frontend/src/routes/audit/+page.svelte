<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';

  const roles = $derived(page.data.user?.user_roles ?? []);
  const isAuditor = $derived(roles.includes('auditor'));

  let reports: any[] = $state([]);
  let loading = $state(true);
  let activeFilter = $state('all');
  let search = $state('');

  onMount(async () => {
    if (isAuditor) {
      loading = false;
      return;
    }
    try {
      const portfolioRes = await fetch('/api/portfolio');
      const portfolios: any[] = portfolioRes.ok ? await portfolioRes.json() : [];
      const rows: any[] = [];
      for (const p of portfolios) {
        const dashRes = await fetch(`/api/service/auditreport/dashboard?portfolioId=${p.id}`);
        if (!dashRes.ok) continue;
        const agg: any[] = await dashRes.json();
        for (const a of agg) {
          for (const itemId of (a.itemIds || [])) {
            rows.push({
              id: itemId,
              reportId: itemId.slice(-8).toUpperCase(),
              portfolio: p.name,
              auditor: '—',
              status: a.id,
              score: null,
              date: '—'
            });
          }
        }
      }
      reports = rows;
    } finally {
      loading = false;
    }
  });

  let filtered = $derived(reports.filter(r => {
    const matchFilter =
      activeFilter === 'all' ||
      (activeFilter === 'pending'      && r.status === 'PENDING_REVIEW') ||
      (activeFilter === 'under-review' && r.status === 'UNDER_REVIEW') ||
      (activeFilter === 'approved'     && r.status === 'APPROVED') ||
      (activeFilter === 'rejected'     && r.status === 'REJECTED');
    const matchSearch = !search ||
      r.reportId.toLowerCase().includes(search.toLowerCase()) ||
      r.portfolio.toLowerCase().includes(search.toLowerCase());
    return matchFilter && matchSearch;
  }));

  function count(status: string) { return reports.filter(r => r.status === status).length; }

  function badgeClass(s: string) {
    if (s === 'APPROVED')      return 'badge-approved';
    if (s === 'REJECTED')      return 'badge-rejected';
    if (s === 'UNDER_REVIEW')  return 'badge-under-review';
    if (s === 'PENDING_REVIEW') return 'badge-pending';
    return 'badge-draft';
  }

  function badgeLabel(s: string) {
    return s.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }

  function scoreClass(n: number) {
    if (n >= 80) return 'high';
    if (n >= 60) return 'medium';
    return 'low';
  }
</script>

<div class="topbar">
  <div>
    <div class="pg-ttl">Audit Dashboard</div>
    <div class="pg-sub">Manage ESG compliance audits</div>
  </div>
</div>

<div class="content">
  <div class="metrics">
    <div class="m-card amber">
      <div class="m-icon-wrap iw-amber">
        <svg viewBox="0 0 16 16" fill="none">
          <path d="M8 2L14 13H2L8 2Z" stroke="#fcd34d" stroke-width="1.3" stroke-linejoin="round"/>
          <path d="M8 6v3" stroke="#fcd34d" stroke-width="1.3" stroke-linecap="round"/>
        </svg>
      </div>
      <div class="m-val amber">{count('PENDING_REVIEW')}</div>
      <div class="m-lbl">Pending Review</div>
      <div class="m-trend tr-amber">Awaiting assignment</div>
    </div>
    <div class="m-card blue">
      <div class="m-icon-wrap iw-blue">
        <svg viewBox="0 0 16 16" fill="none">
          <circle cx="8" cy="8" r="5.5" stroke="#93c5fd" stroke-width="1.3"/>
          <path d="M8 5v3l2 2" stroke="#93c5fd" stroke-width="1.3" stroke-linecap="round"/>
        </svg>
      </div>
      <div class="m-val blue">{count('UNDER_REVIEW')}</div>
      <div class="m-lbl">Under Review</div>
      <div class="m-trend" style="color:#93c5fd;">In progress</div>
    </div>
    <div class="m-card green">
      <div class="m-icon-wrap iw-green">
        <svg viewBox="0 0 16 16" fill="none">
          <path d="M3 8l3.5 3.5L13 5" stroke="#6ee7b7" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <div class="m-val green">{count('APPROVED')}</div>
      <div class="m-lbl">Approved</div>
      <div class="m-trend tr-green">Compliance confirmed</div>
    </div>
  </div>

  <div class="search-wrap">
    <svg class="search-icon" width="14" height="14" viewBox="0 0 16 16" fill="none">
      <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.3"/>
      <path d="M11 11l3 3" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
    </svg>
    <input class="search-bar" type="text" bind:value={search} placeholder="Search by report ID or portfolio name..." />
  </div>

  <div class="filter-tabs">
    {#each [['all','All',reports.length],['pending','Pending',count('PENDING_REVIEW')],['under-review','Under Review',count('UNDER_REVIEW')],['approved','Approved',count('APPROVED')],['rejected','Rejected',count('REJECTED')]] as [key, label, n]}
      <button class="filter-tab" class:active={activeFilter === key} onclick={() => activeFilter = String(key)}>
        {label} <span class="filter-tab-count">{n}</span>
      </button>
    {/each}
  </div>

  <div class="glass-table">
    {#if loading}
      <div class="skeleton-pad">
        <div class="skeleton" style="height:20px;"></div>
        <div class="skeleton" style="height:20px;"></div>
        <div class="skeleton" style="height:20px;"></div>
      </div>
    {:else if isAuditor}
      <div class="empty">
        <div class="e-icon">
          <svg width="24" height="24" viewBox="0 0 16 16" fill="none">
            <path d="M3 8l3.5 3.5L13 5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="e-ttl">Assignment queue</div>
        <div class="e-sub">Audit reports are assigned to you directly. Open a report link to begin your review.</div>
      </div>
    {:else if filtered.length === 0}
      <div class="empty">
        <div class="e-icon">
          <svg width="24" height="24" viewBox="0 0 16 16" fill="none">
            <path d="M3 8l3.5 3.5L13 5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="e-ttl">No reports found</div>
        <div class="e-sub">No audit reports match your current filter.</div>
      </div>
    {:else}
      <table>
        <thead>
          <tr>
            <th>Report ID</th>
            <th>Portfolio</th>
            <th>Auditor</th>
            <th>Status</th>
            <th>Score</th>
            <th>Date</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {#each filtered as r}
            <tr>
              <td><span class="pf-id">{r.reportId}</span></td>
              <td><div class="pf-name">{r.portfolio}</div></td>
              <td><span class="pf-id">{r.auditor}</span></td>
              <td><span class="badge {badgeClass(r.status)}">{badgeLabel(r.status)}</span></td>
              <td>
                {#if r.score}
                  <span class="score {scoreClass(r.score)}">{r.score}%</span>
                {:else}
                  <span class="text-muted">—</span>
                {/if}
              </td>
              <td class="text-muted">{r.date}</td>
              <td><a href={`/audit/${r.id}`} class="xb xb-blue">Review →</a></td>
            </tr>
          {/each}
        </tbody>
      </table>
    {/if}
  </div>
</div>
