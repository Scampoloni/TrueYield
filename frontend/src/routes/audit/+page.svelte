<script lang="ts">
  import { onMount } from 'svelte';

  let reports: any[] = $state([]);
  let loading = $state(true);
  let activeFilter = $state('all');
  let search = $state('');

  onMount(async () => {
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
    <div class="page-title">Audit Dashboard</div>
    <div class="page-subtitle">Manage ESG compliance audits</div>
  </div>
</div>

<div class="content">
  <div class="stats-grid">
    <div class="stat-card amber">
      <div class="stat-icon amber">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path d="M8 2L14 13H2L8 2Z" stroke="#fbbf24" stroke-width="1.3" stroke-linejoin="round"/>
          <path d="M8 6v3" stroke="#fbbf24" stroke-width="1.3" stroke-linecap="round"/>
        </svg>
      </div>
      <div class="stat-value amber">{count('PENDING_REVIEW')}</div>
      <div class="stat-label">Pending Review</div>
      <div class="stat-change warning">Awaiting assignment</div>
    </div>
    <div class="stat-card blue">
      <div class="stat-icon blue">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <circle cx="8" cy="8" r="5.5" stroke="#4f8ef7" stroke-width="1.3"/>
          <path d="M8 5v3l2 2" stroke="#4f8ef7" stroke-width="1.3" stroke-linecap="round"/>
        </svg>
      </div>
      <div class="stat-value blue">{count('UNDER_REVIEW')}</div>
      <div class="stat-label">Under Review</div>
      <div class="stat-change info">In progress</div>
    </div>
    <div class="stat-card green">
      <div class="stat-icon green">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path d="M3 8l3.5 3.5L13 5" stroke="#34d399" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <div class="stat-value green">{count('APPROVED')}</div>
      <div class="stat-label">Approved</div>
      <div class="stat-change positive">Compliance confirmed</div>
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

  <div class="table-wrap">
    {#if loading}
      <div class="skeleton-pad">
        <div class="skeleton" style="height:20px;"></div>
        <div class="skeleton" style="height:20px;"></div>
        <div class="skeleton" style="height:20px;"></div>
      </div>
    {:else if filtered.length === 0}
      <div class="empty-state">
        <div class="empty-icon">
          <svg width="20" height="20" viewBox="0 0 16 16" fill="none">
            <path d="M3 8l3.5 3.5L13 5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="empty-title">No reports found</div>
        <div class="empty-desc">No audit reports match your current filter.</div>
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
              <td><span class="cell-mono">{r.reportId}</span></td>
              <td><div class="cell-primary">{r.portfolio}</div></td>
              <td><span class="cell-mono">{r.auditor}</span></td>
              <td><span class="badge {badgeClass(r.status)}">{badgeLabel(r.status)}</span></td>
              <td>
                {#if r.score}
                  <span class="score {scoreClass(r.score)}">{r.score}%</span>
                {:else}
                  <span class="text-muted">—</span>
                {/if}
              </td>
              <td class="text-muted">{r.date}</td>
              <td><a href="/audit/{r.id}" class="cell-link">Review →</a></td>
            </tr>
          {/each}
        </tbody>
      </table>
    {/if}
  </div>
</div>
