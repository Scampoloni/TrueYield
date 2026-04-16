<script lang="ts">
  import { page } from '$app/state';
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import ConfirmModal from '$lib/components/ConfirmModal.svelte';
  import { showToast } from '$lib/toast';

  const id = page.params.id;
  const isFundManager = $derived((page.data.user?.user_roles ?? []).includes('fund-manager'));
  let portfolio: any = $state(null);
  let holdings: any[] = $state([]);
  let loading = $state(true);
  let deleteHoldingTarget: any = $state(null);

  onMount(async () => {
    try {
      const res = await fetch(`/api/portfolio/${id}`, { cache: 'no-store' });
      if (!res.ok) { goto('/portfolios'); return; }
      portfolio = await res.json();
      const hRes = await fetch(`/api/holding?portfolioId=${id}`);
      holdings = hRes.ok ? await hRes.json() : [];
    } finally {
      loading = false;
    }
  });

  async function triggerAnalysis() {
    try {
      const res = await fetch('/api/service/auditreport', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ portfolioId: id })
      });

      if (res.ok) {
        showToast('AI analysis started — check Audit Dashboard', 'success');
      } else {
        showToast('Failed to start analysis', 'error');
      }
    } catch {
      showToast('Failed to start analysis', 'error');
    }
  }
</script>

{#if loading}
  <div class="topbar"><div><div class="skeleton" style="width:200px;height:22px;"></div></div></div>
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
      <div class="pg-ttl">{portfolio.name}</div>
      <div class="pg-sub">{portfolio.description || 'No description'}</div>
    </div>
    <div class="btns">
      {#if isFundManager}
        <a href="/portfolios/{id}/edit" class="btn btn-ghost">Edit</a>
        <a href="/portfolios/{id}/holdings/create" class="btn btn-primary">+ Add Holding</a>
        <button class="btn btn-success" onclick={() => triggerAnalysis()}>
          Analyse starten
        </button>
      {/if}
    </div>
  </div>

  <div class="content">
    <div class="sec-head">
      <span class="sec-name">Holdings</span>
      <span class="text-muted" style="font-size:12px;">{holdings.length} position{holdings.length !== 1 ? 's' : ''}</span>
    </div>
    <div class="glass-table">
      {#if holdings.length === 0}
        <div class="empty">
          <div class="e-icon">
            <svg width="24" height="24" viewBox="0 0 16 16" fill="none">
              <path d="M2 4h12M2 8h8M2 12h10" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="e-ttl">No holdings yet</div>
          <div class="e-sub">Add your first holding to this portfolio to begin ESG analysis.</div>
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
                <td><div class="pf-name">{h.symbol}</div></td>
                <td>{h.name || '—'}</td>
                <td><span class="pf-id">{h.isin || '—'}</span></td>
                <td>{h.weightPercent != null ? h.weightPercent + '%' : '—'}</td>
                <td>
                  <div class="tbl-acts">
                    <a href="/portfolios/{id}/holdings/{h.id}" class="xb xb-blue">View Evidence →</a>
                    <button class="xb xb-red" onclick={() => deleteHoldingTarget = h}>Delete</button>
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
    onConfirm={async () => {
      const res = await fetch(`/api/holding/${deleteHoldingTarget.id}`, { method: 'DELETE' });
      if (res.ok) {
        holdings = holdings.filter(h => h.id !== deleteHoldingTarget.id);
        showToast('Holding removed');
      } else {
        showToast('Failed to delete holding', 'error');
      }
      deleteHoldingTarget = null;
    }}
    onCancel={() => deleteHoldingTarget = null}
  />
{/if}
