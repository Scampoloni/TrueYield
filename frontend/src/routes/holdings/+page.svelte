<script lang="ts">
  import { onMount } from 'svelte';

  let holdings: { symbol: string; name: string; isin: string; weightPercent: number | null; portfolioId: string; portfolioName: string; id: string }[] = $state([]);
  let loading = $state(true);
  let error = $state('');

  onMount(async () => {
    try {
      const res = await fetch('/api/portfolio');
      if (!res.ok) throw new Error('Failed to fetch portfolios');
      const portfolios: any[] = await res.json();
      const all: typeof holdings = [];
      for (const p of portfolios) {
        if (p.holdings && Array.isArray(p.holdings)) {
          for (const h of p.holdings) {
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
    <div class="page-title">Holdings</div>
    <div class="page-subtitle">{loading ? '...' : `${holdings.length} position${holdings.length !== 1 ? 's' : ''} across your portfolios`}</div>
  </div>
</div>

<div class="content">
  <div class="table-wrap">
    {#if loading}
      <div style="padding:24px">
        {#each [1,2,3,4] as _}
          <div class="skeleton" style="height:20px;margin-bottom:16px;"></div>
        {/each}
      </div>
    {:else if error}
      <div class="empty-state">
        <div class="empty-icon">
          <svg width="20" height="20" viewBox="0 0 16 16" fill="none"><path d="M8 2L14 13H2L8 2Z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/></svg>
        </div>
        <div class="empty-title">Connection Error</div>
        <div class="empty-desc">{error}</div>
      </div>
    {:else if holdings.length === 0}
      <div class="empty-state">
        <div class="empty-icon">
          <svg width="20" height="20" viewBox="0 0 16 16" fill="none"><path d="M2 4h12M2 8h8M2 12h10" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
        </div>
        <div class="empty-title">No holdings yet</div>
        <div class="empty-desc">Add holdings to your portfolios to see them listed here.</div>
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
              <td><div class="cell-primary">{h.symbol}</div></td>
              <td>{h.name || '—'}</td>
              <td><span class="cell-mono">{h.isin || '—'}</span></td>
              <td>{h.weightPercent != null ? h.weightPercent + '%' : '—'}</td>
              <td><span class="badge badge-active">{h.portfolioName}</span></td>
              <td>
                <a href="/portfolios/{h.portfolioId}/holdings/{h.id}" class="cell-link">View Evidence →</a>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    {/if}
  </div>
</div>
