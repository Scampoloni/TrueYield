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

  // Autocomplete state
  interface Suggestion { symbol: string; name: string; exchange: string; type: string; }
  let suggestions: Suggestion[] = $state([]);
  let showDropdown = $state(false);
  let searchLoading = $state(false);
  let debounceTimer: ReturnType<typeof setTimeout> | null = null;
  let selectedIndex = $state(-1);

  function onSymbolInput() {
    selectedIndex = -1;
    if (debounceTimer) clearTimeout(debounceTimer);
    const q = symbol.trim();
    if (q.length < 1) { suggestions = []; showDropdown = false; return; }
    debounceTimer = setTimeout(() => fetchSuggestions(q), 280);
  }

  async function fetchSuggestions(q: string) {
    searchLoading = true;
    try {
      const res = await fetch(`/api/holding/autocomplete?q=${encodeURIComponent(q)}`);
      if (res.ok) {
        suggestions = await res.json();
        showDropdown = suggestions.length > 0;
      }
    } catch {
      suggestions = [];
    } finally {
      searchLoading = false;
    }
  }

  function selectSuggestion(s: Suggestion) {
    symbol = s.symbol;
    name = s.name;
    showDropdown = false;
    suggestions = [];
  }

  function onSymbolKeydown(e: KeyboardEvent) {
    if (!showDropdown || suggestions.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      selectedIndex = Math.min(selectedIndex + 1, suggestions.length - 1);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      selectedIndex = Math.max(selectedIndex - 1, -1);
    } else if (e.key === 'Enter' && selectedIndex >= 0) {
      e.preventDefault();
      selectSuggestion(suggestions[selectedIndex]);
    } else if (e.key === 'Escape') {
      showDropdown = false;
    }
  }

  function onSymbolBlur() {
    // Small delay so click on dropdown item fires first
    setTimeout(() => { showDropdown = false; }, 150);
  }

  async function submit() {
    if (!symbol.trim()) { error = 'Symbol is required.'; return; }
    loading = true; error = '';
    try {
      const body: any = { portfolioId, symbol: symbol.trim().toUpperCase() };
      if (isin) body.isin = isin;
      if (name) body.name = name;
      if (weightPercent) body.weightPercent = parseFloat(weightPercent);
      const res = await fetch('/api/holding', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      if (!res.ok) throw new Error();
      showToast(`${symbol.trim().toUpperCase()} added to portfolio`);
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
      <div class="fs">Type a ticker symbol to auto-fill the company name. ISIN and weight are optional.</div>

      {#if error}
        <div class="alert-error">{error}</div>
      {/if}

      <div class="field">
        <label for="symbol">Symbol <span>*</span></label>
        <div class="autocomplete-wrap">
          <div class="inp-row">
            <input
              id="symbol"
              class="inp"
              type="text"
              bind:value={symbol}
              oninput={onSymbolInput}
              onkeydown={onSymbolKeydown}
              onblur={onSymbolBlur}
              placeholder="e.g. AAPL"
              autocomplete="off"
              style="font-family:'JetBrains Mono',monospace;font-size:15px;font-weight:600;letter-spacing:0.5px;"
            />
            {#if searchLoading}
              <span class="search-spinner"></span>
            {/if}
          </div>
          {#if showDropdown && suggestions.length > 0}
            <div class="dropdown" role="listbox">
              {#each suggestions as s, i}
                <!-- svelte-ignore a11y_click_events_have_key_events -->
                <div
                  class="dropdown-item"
                  class:selected={i === selectedIndex}
                  role="option"
                  aria-selected={i === selectedIndex}
                  onmousedown={() => selectSuggestion(s)}
                >
                  <span class="dropdown-symbol">{s.symbol}</span>
                  <span class="dropdown-name">{s.name}</span>
                  <span class="dropdown-exchange">{s.exchange}</span>
                </div>
              {/each}
            </div>
          {/if}
        </div>
      </div>

      <div class="field">
        <label for="holdingName">Company name <span>(optional)</span></label>
        <input id="holdingName" class="inp" type="text" bind:value={name} placeholder="Auto-filled from symbol search" />
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

<style>
  .autocomplete-wrap {
    position: relative;
  }
  .inp-row {
    position: relative;
    display: flex;
    align-items: center;
  }
  .inp-row .inp {
    width: 100%;
    padding-right: 32px;
  }
  .search-spinner {
    position: absolute;
    right: 10px;
    width: 14px;
    height: 14px;
    border: 2px solid rgba(147,197,253,0.3);
    border-top-color: #93c5fd;
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
    pointer-events: none;
  }
  @keyframes spin { to { transform: rotate(360deg); } }
  .dropdown {
    position: absolute;
    top: calc(100% + 4px);
    left: 0;
    right: 0;
    background: #1a2332;
    border: 1px solid rgba(255,255,255,0.12);
    border-radius: 8px;
    box-shadow: 0 8px 24px rgba(0,0,0,0.4);
    z-index: 50;
    overflow: hidden;
    max-height: 260px;
    overflow-y: auto;
  }
  .dropdown-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 9px 14px;
    cursor: pointer;
    transition: background 0.1s;
  }
  .dropdown-item:hover,
  .dropdown-item.selected {
    background: rgba(59,130,246,0.15);
  }
  .dropdown-symbol {
    font-family: 'JetBrains Mono', monospace;
    font-size: 13px;
    font-weight: 700;
    color: #93c5fd;
    min-width: 60px;
  }
  .dropdown-name {
    font-size: 13px;
    color: var(--text-1, #e2e8f0);
    flex: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .dropdown-exchange {
    font-size: 10px;
    color: var(--text-3, #7a90aa);
    background: rgba(255,255,255,0.06);
    border-radius: 3px;
    padding: 1px 5px;
    white-space: nowrap;
  }
</style>
