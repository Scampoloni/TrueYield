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
    <div class="pg-ttl">New Portfolio</div>
    <div class="pg-sub">Add a new investment fund to your workspace</div>
  </div>
  <a href="/portfolios" class="btn btn-ghost">← Cancel</a>
</div>

<div class="content">
  <div class="form-wrap">
    <div class="form-glass">
      <div class="ft">Portfolio details</div>
      <div class="fs">Create a new investment portfolio. You can add holdings and start an ESG audit after creation.</div>

      {#if error}
        <div class="alert-error">{error}</div>
      {/if}

      <div class="field">
        <label for="name">Portfolio name <span>*</span></label>
        <input id="name" class="inp" type="text" bind:value={name} placeholder="e.g. Clean Energy Europe 2026" />
      </div>
      <div class="field">
        <label for="description">Description <span>(optional)</span></label>
        <textarea id="description" class="inp" bind:value={description} placeholder="Describe the investment strategy and ESG objectives..."></textarea>
      </div>
      <button class="submit-btn" onclick={submit} disabled={loading}>
        {loading ? 'Creating...' : 'Create Portfolio'}
      </button>
    </div>
  </div>
</div>
