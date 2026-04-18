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

  let overview: ComplianceOverview | null = $state(null);
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

  onMount(async () => {
    if (!isComplianceOfficer) {
      error = 'Access denied. This page requires the compliance-officer role.';
      loading = false;
      return;
    }
    try {
      const res = await fetch('/api/compliance/overview', { cache: 'no-store' });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      overview = await res.json();
    } catch (e) {
      error = 'Could not load compliance overview. Make sure the backend is running.';
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
        <div class="m-trend tr-green">↑ Across all fund managers</div>
      </div>

      <div class="m-card green">
        <div class="m-icon-wrap iw-green">
          <svg viewBox="0 0 16 16" fill="none">
            <path d="M2 4h12M2 8h8M2 12h10" stroke="#6ee7b7" stroke-width="1.3" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="m-val green">{overview.totalHoldings}</div>
        <div class="m-lbl">Total Holdings</div>
        <div class="m-trend tr-green">↑ Across all portfolios</div>
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
  {/if}
</div>
