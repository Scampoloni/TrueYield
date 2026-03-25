<script lang="ts">
  import { onMount } from 'svelte';

  let holdings: { symbol: string; name: string; isin: string; weightPercent: number | null; portfolioId: string; portfolioName: string; id: string }[] = $state([]);
  let loading = $state(true);
  let error = $state('');

  onMount(async () => {
    try {
      const res = await fetch('/api/portfolio', { cache: 'no-store' });
      if (!res.ok) throw new Error('Failed to fetch portfolios');
      const portfolios: any[] = await res.json();
      const all: typeof holdings = [];
      for (const p of portfolios) {
        const hRes = await fetch(`/api/holding?portfolioId=${p.id}`);
        if (!hRes.ok) continue;
        const hs: any[] = await hRes.json();
        for (const h of hs) {
          all.push({
            symbol: h.symbol,
            name: h.name || '',
            isin: h.isin || '',
            weightPercent: h.weightPercent ?? null,
            portfolioId: p.id,
            portfolioName: p.name,
            id: h.id
          });
        }
      }
      holdings = all;
    } catch {
      error = 'Could not load holdings. Make sure the backend is running on port 8080.';
    } finally {
      loading = false;
    }
  });
</script>

<div class="topbar">
  <div>
    <div class="pg-ttl">Holdings</div>
    <div class="pg-sub">{loading ? '...' : `${holdings.length} position${holdings.length !== 1 ? 's' : ''} across your portfolios`}</div>
  </div>
</div>

<div class="content">
  <div class="glass-table">
    {#if loading}
      <div class="skeleton-pad">
        {#each [1,2,3,4] as _}
          <div class="skeleton" style="height:20px;"></div>
        {/each}
      </div>
    {:else if error}
      <div class="empty">
        <div class="e-icon">
          <svg width="24" height="24" viewBox="0 0 16 16" fill="none"><path d="M8 2L14 13H2L8 2Z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/></svg>
        </div>
        <div class="e-ttl">Connection Error</div>
        <div class="e-sub">{error}</div>
      </div>
    {:else if holdings.length === 0}
      <div class="empty">
        <div class="e-icon">
          <svg width="24" height="24" viewBox="0 0 16 16" fill="none"><path d="M2 4h12M2 8h8M2 12h10" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
        </div>
        <div class="e-ttl">No holdings yet</div>
        <div class="e-sub">Add holdings to your portfolios to see them listed here.</div>
        <a href="/portfolios" class="btn btn-primary">Go to Portfolios</a>
      </div>
    {:else}
      <table>
        <thead>
          <tr>
            <th>Symbol</th>
            <th>Company</th>
            <th>ISIN</th>
            <th>Weight %</th>
            <th>Portfolio</th>
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
              <td><span class="badge badge-active">{h.portfolioName}</span></td>
              <td>
                <a href="/portfolios/{h.portfolioId}/holdings/{h.id}" class="xb xb-blue">View Evidence →</a>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    {/if}
  </div>
</div>
