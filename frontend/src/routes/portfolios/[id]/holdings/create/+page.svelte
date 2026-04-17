<script lang="ts">
  import { page } from '$app/state';
  import { goto } from '$app/navigation';
  import { showToast } from '$lib/toast';

  const portfolioId = page.params.id;
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
      const body: any = { portfolioId, symbol: symbol.toUpperCase() };
      if (isin) body.isin = isin;
      if (name) body.name = name;
      if (weightPercent) body.weightPercent = parseFloat(weightPercent);
      const res = await fetch('/api/holding', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      if (!res.ok) throw new Error();
      showToast(`${symbol.toUpperCase()} added to portfolio`);
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
    <div class="pg-ttl">Add Holding</div>
    <div class="pg-sub">Add a new position to this portfolio</div>
  </div>
  <a href={`/portfolios/${portfolioId}`} class="btn btn-ghost">← Cancel</a>
</div>

<div class="content">
  <div class="form-wrap">
    <div class="form-glass">
      <div class="ft">Holding details</div>
      <div class="fs">Add a new stock or fund position. Symbol is required — all other fields are optional.</div>

      {#if error}
        <div class="alert-error">{error}</div>
      {/if}

      <div class="field">
        <label for="symbol">Symbol <span>*</span></label>
        <input id="symbol" class="inp" type="text" bind:value={symbol} placeholder="e.g. AAPL" style="font-family:'JetBrains Mono',monospace;font-size:15px;font-weight:600;letter-spacing:0.5px;" />
      </div>
      <div class="field">
        <label for="holdingName">Company name <span>(optional)</span></label>
        <input id="holdingName" class="inp" type="text" bind:value={name} placeholder="e.g. Apple Inc." />
      </div>
      <div class="frow">
        <div class="field">
          <label for="isin">ISIN <span>(optional)</span></label>
          <input id="isin" class="inp" type="text" bind:value={isin} placeholder="e.g. US0378331005" style="font-family:'JetBrains Mono',monospace;" />
        </div>
        <div class="field">
          <label for="weight">Weight % <span>(optional)</span></label>
          <input id="weight" class="inp" type="number" bind:value={weightPercent} placeholder="e.g. 15.5" min="0" max="100" step="0.1" />
        </div>
      </div>
      <div class="form-divider"></div>
      <div class="field">
        <label for="portfolioIdDisplay">Portfolio ID</label>
        <div id="portfolioIdDisplay" class="id-field">{portfolioId}</div>
        <div class="form-hint">This holding will be assigned to the current portfolio.</div>
      </div>
      <button class="submit-btn" onclick={submit} disabled={loading}>
        {loading ? 'Adding...' : 'Add Holding'}
      </button>
    </div>
  </div>
</div>
