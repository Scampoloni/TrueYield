<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';

  let portfolios: any[] = $state([]);
  let loading = $state(true);
  let error = $state('');

  onMount(async () => {
    try {
      const res = await fetch('http://localhost:8080/api/portfolio');
      if (!res.ok) throw new Error('Failed');
      portfolios = await res.json();
    } catch (e) {
      error = 'Could not load portfolios.';
    } finally {
      loading = false;
    }
  });

  function getStatusClass(status: string) {
    if (!status) return 'badge-active';
    const s = status.toLowerCase().replace('_', '-');
    if (s.includes('review')) return 'badge-under-review';
    if (s.includes('pending')) return 'badge-pending';
    if (s.includes('approved')) return 'badge-approved';
    if (s.includes('rejected')) return 'badge-rejected';
    return 'badge-active';
  }

  function getStatusLabel(status: string) {
    if (!status) return 'Active';
    return status.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }
</script>

<div class="topbar">
  <div>
    <div class="page-title">Dashboard</div>
    <div class="page-subtitle">{new Date().toLocaleDateString('en-US', { weekday:'long', year:'numeric', month:'long', day:'numeric' })}</div>
  </div>
  <div class="topbar-actions">
    <button class="btn btn-ghost">Export</button>
    <a href="/portfolios/create" class="btn btn-primary">+ New Portfolio</a>
  </div>
</div>

<div class="content">
  <div class="stats-grid">
    <div class="stat-card blue">
      <div class="stat-icon blue">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <rect x="1.5" y="4.5" width="13" height="9" rx="1.5" stroke="#4f8ef7" stroke-width="1.3"/>
          <path d="M5 4.5V3.5a1.5 1.5 0 011.5-1.5h3a1.5 1.5 0 011.5 1.5v1" stroke="#4f8ef7" stroke-width="1.3"/>
          <path d="M1.5 7.5h13" stroke="#4f8ef7" stroke-width="1.3"/>
        </svg>
      </div>
      {#if loading}
        <div class="skeleton" style="width:40px;height:30px;margin-bottom:6px;"></div>
      {:else}
        <div class="stat-value blue">{portfolios.length}</div>
      {/if}
      <div class="stat-label">Active Portfolios</div>
      <div class="stat-change positive">↑ All managed by you</div>
    </div>

    <div class="stat-card green">
      <div class="stat-icon green">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path d="M2 4h12M2 8h8M2 12h10" stroke="#34d399" stroke-width="1.3" stroke-linecap="round"/>
        </svg>
      </div>
      {#if loading}
        <div class="skeleton" style="width:40px;height:30px;margin-bottom:6px;"></div>
      {:else}
        <div class="stat-value green">{portfolios.reduce((sum, p) => sum + (p.holdings?.length || 0), 0)}</div>
      {/if}
      <div class="stat-label">Total Holdings</div>
      <div class="stat-change positive">↑ Across all portfolios</div>
    </div>

    <div class="stat-card amber">
      <div class="stat-icon amber">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path d="M8 2L14 13H2L8 2Z" stroke="#fbbf24" stroke-width="1.3" stroke-linejoin="round"/>
          <path d="M8 6v3" stroke="#fbbf24" stroke-width="1.3" stroke-linecap="round"/>
          <circle cx="8" cy="11" r="0.5" fill="#fbbf24" stroke="#fbbf24"/>
        </svg>
      </div>
      {#if loading}
        <div class="skeleton" style="width:40px;height:30px;margin-bottom:6px;"></div>
      {:else}
        <div class="stat-value amber">{portfolios.filter(p => p.auditReports?.some((r: any) => r.auditStatus === 'PENDING_REVIEW')).length}</div>
      {/if}
      <div class="stat-label">Pending Audits</div>
      <div class="stat-change warning">Requires attention</div>
    </div>
  </div>

  <div class="section-hd">
    <span class="section-title">Recent Portfolios</span>
    <a href="/portfolios" class="btn btn-ghost btn-sm">View all →</a>
  </div>

  <div class="table-wrap">
    {#if loading}
      <div style="padding: 24px;">
        {#each [1,2,3] as _}
          <div class="skeleton" style="height:20px;margin-bottom:16px;border-radius:6px;"></div>
        {/each}
      </div>
    {:else if error}
      <div class="empty-state">
        <div class="empty-icon">⚠</div>
        <div class="empty-title">Connection Error</div>
        <div class="empty-desc">{error} Make sure the backend is running on port 8080.</div>
      </div>
    {:else if portfolios.length === 0}
      <div class="empty-state">
        <div class="empty-icon">
          <svg width="20" height="20" viewBox="0 0 16 16" fill="none">
            <rect x="1.5" y="4.5" width="13" height="9" rx="1.5" stroke="currentColor" stroke-width="1.3"/>
            <path d="M1.5 7.5h13" stroke="currentColor" stroke-width="1.3"/>
          </svg>
        </div>
        <div class="empty-title">No portfolios yet</div>
        <div class="empty-desc">Create your first investment portfolio to get started.</div>
        <a href="/portfolios/create" class="btn btn-primary">+ New Portfolio</a>
      </div>
    {:else}
      <table>
        <thead>
          <tr>
            <th>Portfolio</th>
            <th>Description</th>
            <th>Status</th>
            <th style="text-align:right">Holdings</th>
          </tr>
        </thead>
        <tbody>
          {#each portfolios.slice(0, 5) as p}
            <tr onclick={() => goto(`/portfolios/${p.id}`)} style="cursor:pointer">
              <td>
                <div class="cell-primary">{p.name}</div>
                <div class="cell-sub">{p.fundManagerId}</div>
              </td>
              <td>{p.description || '—'}</td>
              <td><span class="badge {getStatusClass(p.auditStatus)}">{getStatusLabel(p.auditStatus)}</span></td>
              <td style="text-align:right">
                <a href="/portfolios/{p.id}" class="cell-link">View →</a>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    {/if}
  </div>
</div>
