<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';

  let portfolios: any[] = $state([]);
  let totalHoldings = $state(0);
  let pendingAudits = $state(0);
  let loading = $state(true);
  let error = $state('');

  onMount(async () => {
    try {
      const res = await fetch('/api/portfolio', { cache: 'no-store' });
      if (!res.ok) throw new Error('Failed');
      portfolios = await res.json();

      let holdingsCount = 0;
      let pendingCount = 0;
      for (const p of portfolios) {
        const [hRes, aRes] = await Promise.all([
          fetch(`/api/holding?portfolioId=${p.id}`),
          fetch(`/api/service/auditreport/dashboard?portfolioId=${p.id}`)
        ]);
        if (hRes.ok) {
          const hs = await hRes.json();
          holdingsCount += hs.length;
        }
        if (aRes.ok) {
          const agg: any[] = await aRes.json();
          const pending = agg.find((a: any) => a.id === 'PENDING_REVIEW');
          if (pending) pendingCount += parseInt(pending.count || '0');
        }
      }
      totalHoldings = holdingsCount;
      pendingAudits = pendingCount;
    } catch {
      error = 'Could not load portfolios.';
    } finally {
      loading = false;
    }
  });

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
    <div class="pg-ttl">Dashboard</div>
    <div class="pg-sub">{new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</div>
  </div>
  <div class="btns">
    <a href="/portfolios/create" class="btn btn-primary">+ New Portfolio</a>
  </div>
</div>

<div class="content">
  <div class="metrics">
    <div class="m-card blue">
      <div class="m-icon-wrap iw-blue">
        <svg viewBox="0 0 16 16" fill="none">
          <rect x="1.5" y="4.5" width="13" height="9" rx="1.5" stroke="#93c5fd" stroke-width="1.3"/>
          <path d="M5 4.5V3.5a1.5 1.5 0 011.5-1.5h3a1.5 1.5 0 011.5 1.5v1" stroke="#93c5fd" stroke-width="1.3"/>
          <path d="M1.5 7.5h13" stroke="#93c5fd" stroke-width="1.3"/>
        </svg>
      </div>
      {#if loading}
        <div class="skeleton" style="width:60px;height:40px;margin-bottom:6px;"></div>
      {:else}
        <div class="m-val blue">{portfolios.length}</div>
      {/if}
      <div class="m-lbl">Active Portfolios</div>
      <div class="m-trend tr-green">↑ All managed by you</div>
    </div>

    <div class="m-card green">
      <div class="m-icon-wrap iw-green">
        <svg viewBox="0 0 16 16" fill="none">
          <path d="M2 4h12M2 8h8M2 12h10" stroke="#6ee7b7" stroke-width="1.3" stroke-linecap="round"/>
        </svg>
      </div>
      {#if loading}
        <div class="skeleton" style="width:60px;height:40px;margin-bottom:6px;"></div>
      {:else}
        <div class="m-val green">{totalHoldings}</div>
      {/if}
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
      {#if loading}
        <div class="skeleton" style="width:60px;height:40px;margin-bottom:6px;"></div>
      {:else}
        <div class="m-val amber">
          {pendingAudits}
        </div>
      {/if}
      <div class="m-lbl">Pending Audits</div>
      <div class="m-trend tr-amber">Requires attention</div>
    </div>
  </div>

  <div class="sec-head">
    <span class="sec-name">Recent Portfolios</span>
    <a href="/portfolios" class="sec-more">View all →</a>
  </div>

  <div class="glass-table">
    {#if loading}
      <div class="skeleton-pad">
        <div class="skeleton" style="height:20px;"></div>
        <div class="skeleton" style="height:20px;"></div>
        <div class="skeleton" style="height:20px;"></div>
      </div>
    {:else if error}
      <div class="empty">
        <div class="e-icon">⚠</div>
        <div class="e-ttl">Connection Error</div>
        <div class="e-sub">{error} Make sure the backend is running on port 8080.</div>
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
        <a href="/portfolios/create" class="btn btn-primary">+ New Portfolio</a>
      </div>
    {:else}
      <table>
        <thead>
          <tr>
            <th>Portfolio</th>
            <th>Description</th>
            <th>Status</th>
            <th class="text-right">Holdings</th>
          </tr>
        </thead>
        <tbody>
          {#each portfolios.slice(0, 5) as p}
            <tr class="tr-clickable" onclick={() => goto(`/portfolios/${p.id}`)}>
              <td>
                <div class="pf-name">{p.name}</div>
                <div class="pf-id">{p.fundManagerId}</div>
              </td>
              <td class="pf-desc">{p.description || '—'}</td>
              <td><span class="badge {statusClass(p.auditStatus)}">{statusLabel(p.auditStatus)}</span></td>
              <td class="text-right">
                <a href="/portfolios/{p.id}" class="xb xb-blue">View →</a>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    {/if}
  </div>
</div>
