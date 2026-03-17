<script lang="ts">
  import { goto } from '$app/navigation';
  import { showToast } from '$lib/toast';

  let name = $state('');
  let description = $state('');
  let fundManagerId = $state('temp-user-123');
  let loading = $state(false);
  let error = $state('');

  async function submit() {
    if (!name.trim()) { error = 'Portfolio name is required.'; return; }
    loading = true; error = '';
    try {
      const res = await fetch('http://localhost:8080/api/portfolio', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, description, fundManagerId })
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
      <div style="background:var(--red-dim);border:1px solid rgba(239,68,68,0.2);border-radius:9px;padding:12px 14px;font-size:12px;color:var(--red);margin-bottom:20px;">{error}</div>
    {/if}

    <div class="form-row">
      <label class="form-label">Portfolio name <span>*</span></label>
      <input class="form-input" type="text" bind:value={name} placeholder="e.g. Clean Energy Europe 2026" />
    </div>
    <div class="form-row">
      <label class="form-label">Description <span>(optional)</span></label>
      <textarea class="form-input" bind:value={description} placeholder="Describe the investment strategy and ESG objectives..."></textarea>
    </div>
    <div class="form-divider"></div>
    <div class="form-row">
      <label class="form-label">Fund Manager ID <span>*</span></label>
      <input class="form-input" type="text" bind:value={fundManagerId} />
      <div class="form-hint">This will be automatically set to your Auth0 user ID after authentication is configured.</div>
    </div>
    <button class="btn btn-primary" style="width:100%;justify-content:center;padding:11px;" onclick={submit} disabled={loading}>
      {loading ? 'Creating...' : 'Create Portfolio'}
    </button>
  </div>
</div>
