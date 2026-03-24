<script lang="ts">
  import { goto } from '$app/navigation';
  import { showToast } from '$lib/toast';

  let name = $state('');
  let description = $state('');
  let loading = $state(false);
  let error = $state('');

  async function submit() {
    if (!name.trim()) { error = 'Portfolio name is required.'; return; }
    loading = true; error = '';
    try {
      const res = await fetch('/api/portfolio', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, description })
      });
      if (!res.ok) throw new Error('Failed');
      showToast('Portfolio created successfully');
      goto('/portfolios');
    } catch {
      error = 'Failed to create portfolio. Please try again.';
    } finally {
      loading = false;
    }
  }
</script>

<div class="topbar">
  <div>
    <div class="page-title">New Portfolio</div>
    <div class="page-subtitle">Add a new investment fund to your workspace</div>
  </div>
  <a href="/portfolios" class="btn btn-ghost">← Cancel</a>
</div>

<div class="content">
  <div class="form-card">
    <div class="form-card-title">Portfolio details</div>
    <div class="form-card-desc">Create a new investment portfolio. You can add holdings and start an ESG audit after creation.</div>

    {#if error}
      <div class="alert-error">{error}</div>
    {/if}

    <div class="form-row">
      <label class="form-label" for="name">Portfolio name <span>*</span></label>
      <input id="name" class="form-input" type="text" bind:value={name} placeholder="e.g. Clean Energy Europe 2026" />
    </div>
    <div class="form-row">
      <label class="form-label" for="description">Description <span>(optional)</span></label>
      <textarea id="description" class="form-input" bind:value={description} placeholder="Describe the investment strategy and ESG objectives..."></textarea>
    </div>
    <button class="btn btn-primary btn-block" onclick={submit} disabled={loading}>
      {loading ? 'Creating...' : 'Create Portfolio'}
    </button>
  </div>
</div>
