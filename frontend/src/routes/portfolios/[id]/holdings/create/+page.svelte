<script lang="ts">
  import { page } from '$app/stores';
  import { goto } from '$app/navigation';
  import { showToast } from '$lib/toast';

  const portfolioId = $page.params.id;
  let symbol = $state('');
  let isin = $state('');
  let name = $state('');
  let weightPercent = $state('');
  let loading = $state(false);
  let error = $state('');

  async function submit() {
    if (!symbol.trim()) { error = 'Symbol is required.'; return; }
    loading = true; error = '';
    try {
      const body: any = { portfolioId, symbol };
      if (isin) body.isin = isin;
      if (name) body.name = name;
      if (weightPercent) body.weightPercent = parseFloat(weightPercent);
      const res = await fetch('http://localhost:8080/api/holding', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      if (!res.ok) throw new Error();
      showToast(`${symbol} added to portfolio`);
      goto(`/portfolios/${portfolioId}`);
    } catch {
      error = 'Failed to add holding. Check that the portfolio ID is valid.';
    } finally {
      loading = false;
    }
  }
</script>

<div class="topbar">
  <div>
    <div class="page-title">Add Holding</div>
    <div class="page-subtitle">Add a new position to this portfolio</div>
  </div>
  <a href="/portfolios/{portfolioId}" class="btn btn-ghost">← Cancel</a>
</div>

<div class="content">
  <div class="form-card">
    <div class="form-card-title">Holding details</div>
    <div class="form-card-desc">Add a new stock or fund position. Symbol is required — all other fields are optional.</div>

    {#if error}
      <div style="background:var(--red-dim);border:1px solid rgba(239,68,68,0.2);border-radius:9px;padding:12px 14px;font-size:12px;color:var(--red);margin-bottom:20px;">{error}</div>
    {/if}

    <div class="form-row">
      <label class="form-label">Symbol <span>*</span></label>
      <input class="form-input" type="text" bind:value={symbol} placeholder="e.g. AAPL" style="font-family:var(--mono);font-size:15px;font-weight:600;letter-spacing:0.5px;" />
    </div>
    <div class="form-row">
      <label class="form-label">Company name <span>(optional)</span></label>
      <input class="form-input" type="text" bind:value={name} placeholder="e.g. Apple Inc." />
    </div>
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
      <div class="form-row">
        <label class="form-label">ISIN <span>(optional)</span></label>
        <input class="form-input" type="text" bind:value={isin} placeholder="e.g. US0378331005" style="font-family:var(--mono)" />
      </div>
      <div class="form-row">
        <label class="form-label">Weight % <span>(optional)</span></label>
        <input class="form-input" type="number" bind:value={weightPercent} placeholder="e.g. 15.5" min="0" max="100" step="0.1" />
      </div>
    </div>
    <div class="form-divider"></div>
    <div class="form-row">
      <label class="form-label">Portfolio ID</label>
      <div class="form-readonly">{portfolioId}</div>
      <div class="form-hint">This holding will be assigned to the current portfolio.</div>
    </div>
    <button class="btn btn-primary" style="width:100%;justify-content:center;padding:11px;" onclick={submit} disabled={loading}>
      {loading ? 'Adding...' : 'Add Holding'}
    </button>
  </div>
</div>
