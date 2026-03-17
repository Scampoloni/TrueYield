<script lang="ts">
  import '../app.css';
  import { page } from '$app/stores';
  import { goto } from '$app/navigation';
  import Toast from '$lib/components/Toast.svelte';
  import type { Snippet } from 'svelte';

  let { children }: { children: Snippet } = $props();
  let role = $state('fund-manager');

  function isActive(path: string) {
    return $page.url.pathname === path || $page.url.pathname.startsWith(path + '/');
  }
</script>

<div class="app-shell">
  <aside class="sidebar">
    <div class="sidebar-logo">
      <div class="sidebar-logo-dot"></div>
      <span class="sidebar-logo-text">TrueYield</span>
    </div>

    <div class="sidebar-section">
      <span class="sidebar-label">Overview</span>
      <a href="/" class="nav-item" class:active={$page.url.pathname === '/'}>
        <svg class="nav-icon" viewBox="0 0 16 16" fill="none">
          <rect x="1.5" y="1.5" width="5.5" height="5.5" rx="1.5" fill="currentColor" opacity="0.9"/>
          <rect x="9" y="1.5" width="5.5" height="5.5" rx="1.5" fill="currentColor" opacity="0.4"/>
          <rect x="1.5" y="9" width="5.5" height="5.5" rx="1.5" fill="currentColor" opacity="0.4"/>
          <rect x="9" y="9" width="5.5" height="5.5" rx="1.5" fill="currentColor" opacity="0.4"/>
        </svg>
        Dashboard
      </a>
    </div>

    {#if role === 'fund-manager'}
    <div class="sidebar-section">
      <span class="sidebar-label">Fund Management</span>
      <a href="/portfolios" class="nav-item" class:active={isActive('/portfolios')}>
        <svg class="nav-icon" viewBox="0 0 16 16" fill="none">
          <rect x="1.5" y="4.5" width="13" height="9" rx="1.5" stroke="currentColor" stroke-width="1.3"/>
          <path d="M5 4.5V3.5a1.5 1.5 0 011.5-1.5h3A1.5 1.5 0 0111 3.5v1" stroke="currentColor" stroke-width="1.3"/>
          <path d="M1.5 7.5h13" stroke="currentColor" stroke-width="1.3"/>
        </svg>
        Portfolios
      </a>
      <a href="/holdings" class="nav-item" class:active={isActive('/holdings')}>
        <svg class="nav-icon" viewBox="0 0 16 16" fill="none">
          <path d="M2 4h12M2 8h8M2 12h10" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
        </svg>
        Holdings
      </a>
    </div>
    {/if}

    {#if role === 'auditor'}
    <div class="sidebar-section">
      <span class="sidebar-label">Compliance</span>
      <a href="/audit" class="nav-item" class:active={isActive('/audit')}>
        <svg class="nav-icon" viewBox="0 0 16 16" fill="none">
          <path d="M8 1.5l1.8 3.7 4.1.5-3 2.9.7 4L8 10.5l-3.6 2.1.7-4-3-2.9 4.1-.5z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/>
        </svg>
        Audit Reports
        <span class="nav-badge">8</span>
      </a>
    </div>
    {/if}

    <div class="sidebar-footer">
      <div style="padding: 0 10px 12px; display: flex; gap: 6px;">
        <button class="btn btn-sm" class:btn-primary={role==='fund-manager'} class:btn-ghost={role!=='fund-manager'} onclick={() => { role='fund-manager'; goto('/'); }}>FM</button>
        <button class="btn btn-sm" class:btn-primary={role==='auditor'} class:btn-ghost={role!=='auditor'} onclick={() => { role='auditor'; goto('/'); }}>AU</button>
      </div>
      <div class="sidebar-user">
        <div class="sidebar-avatar">{role === 'fund-manager' ? 'FM' : 'AU'}</div>
        <div>
          <div class="sidebar-user-name">{role === 'fund-manager' ? 'Fund Manager' : 'ESG Auditor'}</div>
          <div class="sidebar-user-role">temp-user-123</div>
        </div>
      </div>
    </div>
  </aside>

  <main class="main">
    {@render children()}
  </main>
</div>

<Toast />
