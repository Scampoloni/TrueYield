<script lang="ts">
  import '$lib/styles/design-system.css';
  import { page } from '$app/state';
  import Toast from '$lib/components/Toast.svelte';
  import type { Snippet } from 'svelte';

  let { children }: { children: Snippet } = $props();

  const user = $derived(page.data.user);
  const roles: string[] = $derived(user?.user_roles ?? []);
  const isFundManager = $derived(roles.includes('fund-manager'));
  const isAuditor = $derived(roles.includes('auditor'));
  const isComplianceOfficer = $derived(roles.includes('compliance-officer'));
  const displayName = $derived(user?.name || user?.nickname || user?.email || 'User');
  const displayRole = $derived(
    isFundManager ? 'Fund Manager'
    : isAuditor ? 'ESG Auditor'
    : isComplianceOfficer ? 'Compliance Officer'
    : 'Viewer'
  );
  const initials = $derived(displayName[0].toUpperCase());

  function isActive(path: string) {
    return page.url.pathname === path || page.url.pathname.startsWith(path + '/');
  }
</script>

<!-- Background canvas with ambient orbs -->
<div class="canvas"></div>
<!-- Subtle grid overlay -->
<div class="grid-lines"></div>

<div class="layout">
  <aside class="sidebar">
    <!-- Logo zone -->
    <div class="logo-zone">
      <div class="logo-icon">
        <svg viewBox="0 0 18 18" fill="none">
          <path d="M9 2.5L15.5 6v6L9 15.5 2.5 12V6L9 2.5z" stroke="white" stroke-width="1.7" stroke-linejoin="round"/>
          <circle cx="9" cy="9" r="2.5" fill="white" fill-opacity="0.9"/>
        </svg>
      </div>
      <span class="logo-wordmark">TrueYield</span>
      <div class="status-pill">
        <div class="status-dot"></div>
        <span>LIVE</span>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="nav">
      <div class="nav-group">
        <div class="nav-cat">Overview</div>
        {#if isComplianceOfficer}
          <a href="/compliance" class="nav-a" class:on={isActive('/compliance')}>
            <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="1" y="1" width="6" height="6" rx="1.5"/>
              <rect x="9" y="1" width="6" height="6" rx="1.5"/>
              <rect x="1" y="9" width="6" height="6" rx="1.5"/>
              <rect x="9" y="9" width="6" height="6" rx="1.5"/>
            </svg>
            Dashboard
          </a>
        {:else}
          <a href="/" class="nav-a" class:on={page.url.pathname === '/'}>
            <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="1" y="1" width="6" height="6" rx="1.5"/>
              <rect x="9" y="1" width="6" height="6" rx="1.5"/>
              <rect x="1" y="9" width="6" height="6" rx="1.5"/>
              <rect x="9" y="9" width="6" height="6" rx="1.5"/>
            </svg>
            Dashboard
          </a>
        {/if}
      </div>

      {#if isFundManager}
      <div class="nav-group">
        <div class="nav-cat">Fund Management</div>
        <a href="/portfolios" class="nav-a" class:on={isActive('/portfolios')}>
          <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="2" y="5" width="12" height="9" rx="1.5"/>
            <path d="M5 5V4a3 3 0 016 0v1"/>
          </svg>
          Portfolios
        </a>
        <a href="/holdings" class="nav-a" class:on={isActive('/holdings')}>
          <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M2 4h12M2 8h8M2 12h5"/>
          </svg>
          Holdings
        </a>
      </div>
      {/if}

      {#if isAuditor}
      <div class="nav-group">
        <div class="nav-cat">Compliance</div>
        <a href="/audit" class="nav-a" class:on={isActive('/audit')}>
          <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M8 1.5l1.8 3.7 4.1.5-3 2.9.7 4L8 10.5l-3.6 2.1.7-4-3-2.9 4.1-.5z" stroke-linejoin="round"/>
          </svg>
          Audit Reports
        </a>
      </div>
      {/if}


      <div class="nav-group">
        <div class="nav-cat">Assistant</div>
        <a href="/chat" class="nav-a" class:on={isActive('/chat')}>
          <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M2 3.5a1.5 1.5 0 011.5-1.5h9A1.5 1.5 0 0114 3.5v6A1.5 1.5 0 0112.5 11H8l-3 2v-2H3.5A1.5 1.5 0 012 9.5z"/>
          </svg>
          Chat
        </a>
      </div>

      <div class="nav-group">
        <div class="nav-cat">Settings</div>
        <a href="/account" class="nav-a" class:on={isActive('/account')}>
          <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="8" cy="5" r="3"/>
            <path d="M2 14c0-3.314 2.686-5 6-5s6 1.686 6 5"/>
          </svg>
          Account
        </a>
      </div>
    </nav>

    <!-- User pill at bottom -->
    <div class="sidebar-foot">
      <div class="user-row">
        <div class="u-av">{initials}</div>
        <div>
          <div class="u-nm">{displayName}</div>
          <div class="u-rl">{displayRole}</div>
        </div>
      </div>
    </div>
  </aside>

  <main class="main">
    {@render children()}
  </main>
</div>

<Toast />
