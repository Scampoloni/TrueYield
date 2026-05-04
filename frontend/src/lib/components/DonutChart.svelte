<script lang="ts">
  let { holdings }: { holdings: any[] } = $props();

  const COLORS = [
    '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6',
    '#ec4899', '#06b6d4', '#f97316', '#84cc16', '#6366f1',
    '#14b8a6', '#a855f7'
  ];
  const UNWEIGHTED_COLOR = '#3d4a5e';

  const CX = 100, CY = 100, R_OUT = 80, R_IN = 52;

  function polarToCartesian(cx: number, cy: number, r: number, deg: number) {
    const rad = (deg - 90) * Math.PI / 180;
    return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) };
  }

  function arcPath(startDeg: number, endDeg: number) {
    const os = polarToCartesian(CX, CY, R_OUT, startDeg);
    const oe = polarToCartesian(CX, CY, R_OUT, endDeg);
    const is_ = polarToCartesian(CX, CY, R_IN, startDeg);
    const ie = polarToCartesian(CX, CY, R_IN, endDeg);
    const large = endDeg - startDeg > 180 ? 1 : 0;
    return `M${os.x.toFixed(3)},${os.y.toFixed(3)} A${R_OUT},${R_OUT} 0 ${large} 1 ${oe.x.toFixed(3)},${oe.y.toFixed(3)} L${ie.x.toFixed(3)},${ie.y.toFixed(3)} A${R_IN},${R_IN} 0 ${large} 0 ${is_.x.toFixed(3)},${is_.y.toFixed(3)} Z`;
  }

  const weighted = $derived(holdings.filter(h => h.weightPercent != null && h.weightPercent > 0));
  const unweightedHoldings = $derived(holdings.filter(h => h.weightPercent == null || h.weightPercent <= 0));
  const showChart = $derived(weighted.length >= 2);

  const segmentData = $derived.by(() => {
    const total = weighted.reduce((sum: number, h: any) => sum + h.weightPercent, 0);
    const remaining = Math.max(0, 100 - total);
    const segs: { symbol: string; name: string; percent: number; color: string }[] = weighted.map((h: any, i: number) => ({
      symbol: h.symbol,
      name: h.name || h.symbol,
      percent: h.weightPercent,
      color: COLORS[i % COLORS.length]
    }));
    if (unweightedHoldings.length > 0 && remaining > 0.01) {
      segs.push({
        symbol: 'Ungewichtet',
        name: `${unweightedHoldings.length} Position${unweightedHoldings.length !== 1 ? 'en' : ''}`,
        percent: remaining,
        color: UNWEIGHTED_COLOR
      });
    }
    return segs;
  });

  const chartPaths = $derived.by(() => {
    const total = segmentData.reduce((sum: number, s) => sum + s.percent, 0);
    if (total <= 0) return [];
    let angle = 0;
    return segmentData.map(seg => {
      const sweep = (seg.percent / total) * 360;
      const start = angle;
      const end = angle + sweep;
      angle = end;
      return { ...seg, path: arcPath(start, end - 0.4) };
    });
  });

  let hoveredIdx = $state(-1);
  let tooltipText = $state('');
  let mx = $state(0);
  let my = $state(0);

  function handleContainerMouseMove(e: MouseEvent) {
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    mx = e.clientX - rect.left;
    my = e.clientY - rect.top;
  }

  function handleContainerMouseLeave() {
    hoveredIdx = -1;
  }
</script>

{#if showChart}
<div class="chart-section">
  <div class="chart-sec-head">
    <span class="sec-name">Asset Allocation</span>
  </div>
  <div class="chart-body">
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div
      class="chart-svg-wrap"
      onmousemove={handleContainerMouseMove}
      onmouseleave={handleContainerMouseLeave}
    >
      <svg viewBox="0 0 200 200" class="donut-svg" aria-label="Portfolio allocation donut chart">
        {#each chartPaths as seg, i}
          <path
            d={seg.path}
            fill={seg.color}
            class="donut-seg"
            class:donut-seg--hovered={hoveredIdx === i}
            role="img"
            aria-label="{seg.symbol} {seg.percent.toFixed(1)}%"
            onmouseenter={() => { hoveredIdx = i; tooltipText = `${seg.symbol} · ${seg.percent.toFixed(1)}%`; }}
          />
        {/each}
      </svg>
      {#if hoveredIdx >= 0}
        <div class="seg-tooltip" style="left:{mx + 14}px;top:{my - 10}px">
          {tooltipText}
        </div>
      {/if}
    </div>
    <div class="chart-legend">
      {#each chartPaths as seg, i}
        <div
          class="legend-row"
          class:legend-row--active={hoveredIdx === i}
          onmouseenter={() => hoveredIdx = i}
          onmouseleave={() => hoveredIdx = -1}
          role="presentation"
        >
          <span class="legend-dot" style="background:{seg.color}"></span>
          <span class="legend-sym">{seg.symbol}</span>
          <span class="legend-pct">{seg.percent.toFixed(1)}%</span>
        </div>
      {/each}
    </div>
  </div>
</div>
{/if}

<style>
  .chart-section {
    background: rgba(255, 255, 255, 0.022);
    backdrop-filter: blur(24px);
    -webkit-backdrop-filter: blur(24px);
    border: 1px solid rgba(255, 255, 255, 0.065);
    border-radius: 16px;
    padding: 22px 24px;
    margin-bottom: 24px;
  }

  .chart-sec-head {
    margin-bottom: 18px;
  }

  .chart-body {
    display: flex;
    align-items: center;
    gap: 32px;
    flex-wrap: wrap;
  }

  .chart-svg-wrap {
    position: relative;
    width: 200px;
    height: 200px;
    flex-shrink: 0;
  }

  .donut-svg {
    width: 100%;
    height: 100%;
    overflow: visible;
  }

  .donut-seg {
    cursor: pointer;
    transition: transform 0.18s ease, filter 0.18s ease;
    transform-origin: 100px 100px;
    transform-box: view-box;
  }

  .donut-seg--hovered {
    transform: scale(1.06);
    filter: brightness(1.18);
  }

  .seg-tooltip {
    position: absolute;
    pointer-events: none;
    background: rgba(10, 15, 30, 0.92);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 8px;
    padding: 6px 12px;
    font-size: 12px;
    font-weight: 500;
    color: var(--text-1);
    white-space: nowrap;
    z-index: 10;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  }

  .chart-legend {
    display: flex;
    flex-direction: column;
    gap: 10px;
    flex: 1;
    min-width: 160px;
  }

  .legend-row {
    display: flex;
    align-items: center;
    gap: 9px;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 8px;
    transition: background 0.14s;
    border: 1px solid transparent;
  }

  .legend-row:hover,
  .legend-row--active {
    background: rgba(255, 255, 255, 0.04);
    border-color: rgba(255, 255, 255, 0.07);
  }

  .legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .legend-sym {
    font-size: 12.5px;
    font-weight: 600;
    color: var(--text-1);
    flex: 1;
    font-family: 'JetBrains Mono', monospace;
    letter-spacing: 0.3px;
  }

  .legend-pct {
    font-size: 12px;
    color: var(--text-3);
    font-variant-numeric: tabular-nums;
    min-width: 44px;
    text-align: right;
  }
</style>
