<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import ConfirmModal from '$lib/components/ConfirmModal.svelte';
  import { showToast } from '$lib/toast';

  let portfolios: any[] = $state([]);
  let loading = $state(true);
  let deleteTarget: any = $state(null);

  onMount(async () => {
    try {
      const res = await fetch('http://localhost:8080/api/portfolio');
      portfolios = await res.json();
    } finally {
      loading = false;
    }
  });

  async function confirmDelete() {
    try {
      await fetch(`http://localhost:8080/api/portfolio/${deleteTarget.id}`, { method: 'DELETE' });
      portfolios = portfolios.filter(p => p.id !== deleteTarget.id);
      showToast('Portfolio deleted successfully');
    } catch {
      showToast('Failed to delete portfolio', 'error');
    } finally {
      deleteTarget = null;
    }
  }

  function getStatusClass(s: string) {
    if (!s) return 'badge-active';
    const v = s.toLowerCase();
    if (v.includes('review')) return 'badge-under-review';
    if (v.includes('pending')) return 'badge-pending';
    if (v.includes('approved')) return 'badge-approved';
    if (v.includes('rejected')) return 'badge-rejected';
    return 'badge-active';
  }
  function getStatusLabel(s: string) {
    if (!s) return 'Active';
    return s.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }
</script>

<div class="topbar">
  <div>
    <div class="page-title">Portfolios</div>
    <div class="page-subtitle">{loading ? '...' : `${portfolios.length} portfolio${portfolios.length !== 1 ? 's' : ''} total`}</div>
  </div>
  <a href="/portfolios/create" class="btn btn-primary">+ New Portfolio</a>
</div>

<div class="content">
  <div class="table-wrap">
    {#if loading}
      <div style="padding:24px">{#each [1,2,3] as _}<div class="skeleton" style="height:20px;margin-bottom:16px;"></div>{/each}</div>
    {:else if portfolios.length === 0}
      <div class="empty-state">
        <div class="empty-icon"><svg width="20" height="20" viewBox="0 0 16 16" fill="none"><rect x="1.5" y="4.5" width="13" height="9" rx="1.5" stroke="currentColor" stroke-width="1.3"/><path d="M1.5 7.5h13" stroke="currentColor" stroke-width="1.3"/></svg></div>
        <div class="empty-title">No portfolios yet</div>
        <div class="empty-desc">Create your first investment portfolio to get started.</div>
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
              <td>
                <div class="cell-primary">{p.name}</div>
              </td>
              <td>{p.description || '—'}</td>
              <td><span class="cell-mono">{p.fundManagerId}</span></td>
              <td><span class="badge {getStatusClass(p.auditStatus)}">{getStatusLabel(p.auditStatus)}</span></td>
              <td>
                <div style="display:flex;gap:8px;justify-content:flex-end;align-items:center;">
                  <a href="/portfolios/{p.id}" class="cell-link">Holdings →</a>
                  <a href="/portfolios/{p.id}/edit" class="btn btn-ghost btn-sm">Edit</a>
                  <button class="btn btn-danger btn-sm" onclick={() => deleteTarget = p}>Delete</button>
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
