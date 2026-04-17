<script lang="ts">
  import { page } from '$app/state';
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import ConfirmModal from '$lib/components/ConfirmModal.svelte';
  import { showToast } from '$lib/toast';

  const id = page.params.id;
  let name = $state('');
  let description = $state('');
  let loading = $state(true);
  let saving = $state(false);
  let showDeleteModal = $state(false);

  onMount(async () => {
    const res = await fetch(`/api/portfolio/${id}`);
    const p = await res.json();
    name = p.name;
    description = p.description || '';
    loading = false;
  });

  async function save() {
    saving = true;
    try {
      const res = await fetch(`/api/portfolio/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, description })
      });
      if (!res.ok) throw new Error();
      showToast('Portfolio updated successfully');
      goto(`/portfolios/${id}`);
    } catch {
      showToast('Failed to update portfolio', 'error');
    } finally {
      saving = false;
    }
  }

  async function deletePortfolio() {
    try {
      await fetch(`/api/portfolio/${id}`, { method: 'DELETE' });
      showToast('Portfolio deleted');
      goto('/portfolios');
    } catch {
      showToast('Failed to delete portfolio', 'error');
    }
  }
</script>

<div class="topbar">
  <div>
    <div class="pg-ttl">Edit Portfolio</div>
    <div class="pg-sub">Update portfolio details</div>
  </div>
  <a href={`/portfolios/${id}`} class="btn btn-ghost">← Cancel</a>
</div>

<div class="content">
  {#if loading}
    <div class="form-wrap">
      <div class="form-glass">
        <div class="skeleton-pad">
          <div class="skeleton" style="height:40px;"></div>
          <div class="skeleton" style="height:40px;"></div>
          <div class="skeleton" style="height:40px;"></div>
        </div>
      </div>
    </div>
  {:else}
    <div class="form-wrap">
      <div class="form-glass">
        <div class="ft">Portfolio details</div>
        <div class="fs">Update the name and description of this portfolio.</div>
        <div class="field">
          <label for="name">Portfolio name <span>*</span></label>
          <input id="name" class="inp" type="text" bind:value={name} />
        </div>
        <div class="field">
          <label for="description">Description</label>
          <textarea id="description" class="inp" bind:value={description}></textarea>
        </div>
        <button class="submit-btn" onclick={save} disabled={saving}>
          {saving ? 'Saving...' : 'Save Changes'}
        </button>
        <div class="danger-link-wrap">
          <button class="danger-link" onclick={() => showDeleteModal = true}>
            Delete this portfolio
          </button>
        </div>
      </div>
    </div>
  {/if}
</div>

{#if showDeleteModal}
  <ConfirmModal
    title="Delete Portfolio"
    description="This will permanently delete this portfolio and all its holdings. This cannot be undone."
    confirmLabel="Delete permanently"
    danger={true}
    onConfirm={deletePortfolio}
    onCancel={() => showDeleteModal = false}
  />
{/if}
