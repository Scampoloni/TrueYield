<script lang="ts">
  import { page } from '$app/state';
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import ConfirmModal from '$lib/components/ConfirmModal.svelte';
  import { showToast } from '$lib/toast';

  const id = page.params.id;
  let portfolio: any = $state(null);
  let holdings: any[] = $state([]);
  let loading = $state(true);
  let deleteHoldingTarget: any = $state(null);

  onMount(async () => {
    try {
      const res = await fetch(`/api/portfolio/${id}`);
      if (!res.ok) { goto('/portfolios'); return; }
      portfolio = await res.json();
      holdings = portfolio.holdings || [];
    } finally {
      loading = false;
    }
  });
</script>

{#if loading}
  <div class="topbar"><div><div class="skeleton" style="width:200px;height:20px;"></div></div></div>
  <div class="content">
    <div class="skeleton-pad">
      <div class="skeleton" style="height:16px;"></div>
      <div class="skeleton" style="height:16px;"></div>
      <div class="skeleton" style="height:16px;"></div>
    </div>
  </div>
{:else if portfolio}
  <div class="topbar">
    <div>
      <div class="page-title">{portfolio.name}</div>
      <div class="page-subtitle">{portfolio.description || 'No description'}</div>
    </div>
    <div class="topbar-actions">
      <a href="/portfolios/{id}/edit" class="btn btn-ghost">Edit</a>
      <a href="/portfolios/{id}/holdings/create" class="btn btn-primary">+ Add Holding</a>
      <button class="btn btn-amber" onclick={() => showToast('AI analysis triggered — report will appear in Audit Dashboard', 'success')}>
        Analyse starten
      </button>
    </div>
  </div>

  <div class="content">
    <div class="section-hd">
      <span class="section-title">Holdings</span>
      <span class="section-meta">{holdings.length} position{holdings.length !== 1 ? 's' : ''}</span>
    </div>
    <div class="table-wrap">
      {#if holdings.length === 0}
        <div class="empty-state">
          <div class="empty-icon">
            <svg width="20" height="20" viewBox="0 0 16 16" fill="none">
              <path d="M2 4h12M2 8h8M2 12h10" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="empty-title">No holdings yet</div>
          <div class="empty-desc">Add your first holding to this portfolio to begin ESG analysis.</div>
          <a href="/portfolios/{id}/holdings/create" class="btn btn-primary">+ Add Holding</a>
        </div>
      {:else}
        <table>
          <thead>
            <tr>
              <th>Symbol</th>
              <th>Company</th>
              <th>ISIN</th>
              <th>Weight %</th>
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
                <td>
                  <div class="row-actions">
                    <a href="/portfolios/{id}/holdings/{h.id}" class="cell-link">View Evidence →</a>
                    <button class="btn btn-danger btn-sm" onclick={() => deleteHoldingTarget = h}>Delete</button>
                  </div>
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      {/if}
    </div>
  </div>
{/if}

{#if deleteHoldingTarget}
  <ConfirmModal
    title="Delete Holding"
    description="Remove {deleteHoldingTarget.symbol} from this portfolio? This cannot be undone."
    confirmLabel="Delete"
    danger={true}
    onConfirm={() => {
      holdings = holdings.filter(h => h.id !== deleteHoldingTarget.id);
      showToast('Holding removed');
      deleteHoldingTarget = null;
    }}
    onCancel={() => deleteHoldingTarget = null}
  />
{/if}
