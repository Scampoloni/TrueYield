<script lang="ts">
  import { page } from '$app/state';
  import { goto } from '$app/navigation';
  import { showToast } from '$lib/toast';

  const portfolioId = page.params.id;
  const holdingId = page.params.hid;

  let contentSnippet = $state('');
  let sourceUrl = $state('');
  let loading = $state(false);
  let error = $state('');

  async function submit() {
    if (!contentSnippet.trim()) { error = 'Content snippet is required.'; return; }
    loading = true; error = '';
    try {
      const body: any = { holdingId, contentSnippet: contentSnippet.trim() };
      if (sourceUrl.trim()) body.sourceUrl = sourceUrl.trim();
      const res = await fetch('/api/evidence', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      if (!res.ok) throw new Error();
      showToast('Evidence added — AI sentiment analysis complete');
      goto(`/portfolios/${portfolioId}/holdings/${holdingId}`);
    } catch {
      error = 'Failed to add evidence. Please try again.';
    } finally {
      loading = false;
    }
  }
</script>

<div class="topbar">
  <div>
    <div class="pg-ttl">Add Evidence</div>
    <div class="pg-sub">Submit an ESG news snippet for AI sentiment analysis</div>
  </div>
  <a href={`/portfolios/${portfolioId}/holdings/${holdingId}`} class="btn btn-ghost">← Cancel</a>
</div>

<div class="content">
  <div class="form-wrap">
    <div class="form-glass">
      <div class="ft">Evidence details</div>
      <div class="fs">Paste an ESG-relevant news excerpt. The AI will automatically assign a sentiment score (-1.0 to +1.0) and risk rating.</div>

      {#if error}
        <div class="alert-error">{error}</div>
      {/if}

      <div class="field">
        <label for="contentSnippet">News snippet <span>*</span></label>
        <textarea
          id="contentSnippet"
          class="inp"
          rows="5"
          bind:value={contentSnippet}
          placeholder="e.g. Company X was fined for misreporting CO₂ emissions in its ESG disclosure..."
          style="resize:vertical;"
        ></textarea>
      </div>

      <div class="field">
        <label for="sourceUrl">Source URL <span>(optional)</span></label>
        <input
          id="sourceUrl"
          class="inp"
          type="url"
          bind:value={sourceUrl}
          placeholder="e.g. https://ft.com/article/esg-news"
        />
      </div>

      <div class="form-divider"></div>

      <div class="field">
        <label for="holdingIdDisplay">Holding ID</label>
        <div id="holdingIdDisplay" class="id-field">{holdingId}</div>
        <div class="form-hint">This evidence will be linked to the current holding and analysed by AI.</div>
      </div>

      <button class="submit-btn" onclick={submit} disabled={loading}>
        {loading ? 'Analysing...' : 'Add Evidence'}
      </button>
    </div>
  </div>
</div>
