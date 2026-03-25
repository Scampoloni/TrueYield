<script lang="ts">
  import { onMount } from 'svelte';
  import ConfirmModal from '$lib/components/ConfirmModal.svelte';
  import { showToast } from '$lib/toast';

  let portfolios: any[] = $state([]);
  let loading = $state(true);
  let deleteTarget: any = $state(null);

  onMount(async () => {
    try {
      const res = await fetch('/api/portfolio');
      portfolios = await res.json();
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
  <a href="/portfolios/create" class="btn btn-primary">+ New Portfolio</a>
</div>

<div class="content">
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
        <a href="/portfolios/create" class="btn btn-primary">+ New Portfolio</a>
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
              <td><span class="badge {statusClass(p.auditStatus)}">{statusLabel(p.auditStatus)}</span></td>
              <td>
                <div class="tbl-acts">
                  <a href="/portfolios/{p.id}" class="xb xb-blue">Holdings →</a>
                  <a href="/portfolios/{p.id}/edit" class="xb xb-ghost">Edit</a>
                  <button class="xb xb-red" onclick={() => deleteTarget = p}>Delete</button>
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
