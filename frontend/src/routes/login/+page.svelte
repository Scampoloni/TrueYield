<script lang="ts">
  import { goto, invalidateAll } from '$app/navigation';
  import { onDestroy } from 'svelte';

  type DemoAccount = {
    role: string;
    label: string;
    email: string;
    password: string;
    accent: 'blue' | 'green' | 'amber';
    description: string;
  };

  const demoAccounts: DemoAccount[] = [
    {
      role: 'Fund Manager',
      label: 'Portfolio owner',
      email: 'fund.manager@trueyield.demo',
      password: '***REMOVED_FROM_GIT_HISTORY***',
      accent: 'blue',
      description: 'Creates portfolios and manages holdings.'
    },
    {
      role: 'ESG Auditor',
      label: 'Review and decision',
      email: 'esg.auditor@trueyield.demo',
      password: '***REMOVED_FROM_GIT_HISTORY***',
      accent: 'green',
      description: 'Reviews evidence and closes audit reports.'
    },
    {
      role: 'Compliance Officer',
      label: 'Oversight and reporting',
      email: 'compliance.officer@trueyield.demo',
      password: '***REMOVED_FROM_GIT_HISTORY***',
      accent: 'amber',
      description: 'Views the full compliance overview.'
    }
  ];

  function toErrorText(value: unknown, fallback: string) {
    if (typeof value === 'string' && value.trim()) return value;
    if (value && typeof value === 'object') {
      try {
        return JSON.stringify(value);
      } catch {
        return fallback;
      }
    }
    return fallback;
  }

  let email = $state('');
  let password = $state('');
  let loading = $state(false);
  let error = $state('');
  let copied = $state('');
  let copyResetTimer: ReturnType<typeof setTimeout> | undefined;

  async function copyText(text: string, label: string) {
    try {
      await navigator.clipboard.writeText(text);
      copied = label;
      if (copyResetTimer) clearTimeout(copyResetTimer);
      copyResetTimer = setTimeout(() => {
        copied = '';
      }, 1600);
    } catch {
      error = 'Clipboard access is blocked in this browser.';
    }
  }

  onDestroy(() => {
    if (copyResetTimer) clearTimeout(copyResetTimer);
  });
</script>

<svelte:head>
  <title>Login | TrueYield</title>
</svelte:head>

<div class="login-page">
  <div class="login-shell">
    <section class="hero-panel hero-panel-wide">
      <div class="brand-lockup">
        <div class="brand-mark">
          <div class="brand-dot"></div>
        </div>
        <div>
          <div class="brand-name">TrueYield</div>
          <div class="brand-sub">ESG Compliance Engine</div>
        </div>
      </div>

      <div class="hero-copy hero-copy-compact">
        <div class="eyebrow">Human-in-the-loop compliance</div>
        <h1>Sign in.</h1>
        <p>
          Review portfolios, inspect evidence, and complete approvals in the demo environment.
        </p>
      </div>

      <div class="hero-points hero-points-inline">
        <div class="hero-point">
          <span class="point-bullet"></span>
          Explainability first, no black-box decisions.
        </div>
        <div class="hero-point">
          <span class="point-bullet"></span>
          Role-based access for Fund Managers, Auditors, and Compliance.
        </div>
        <div class="hero-point">
          <span class="point-bullet"></span>
          Audit trail ready for the presentation and the real workflow.
        </div>
      </div>
    </section>

    <div class="column-stack column-login">
      <div class="form-card login-card login-card-compact">
        <div class="card-header">
          <div>
            <div class="card-kicker">Secure login</div>
            <div class="form-card-title">Welcome back</div>
            <div class="form-card-desc">Use a demo role below or sign in with your own account.</div>
          </div>
        </div>

        {#if error}
          <div class="alert-error">{error}</div>
        {/if}

        <div class="form-row">
          <label class="form-label" for="email">Email</label>
          <input id="email" class="form-input" type="email" bind:value={email} placeholder="you@example.com" />
        </div>
        <div class="form-row">
          <label class="form-label" for="password">Password</label>
          <input id="password" class="form-input" type="password" bind:value={password} placeholder="••••••••" />
        </div>

        <button
          class="btn btn-primary btn-block"
          style="margin-top:8px;"
          disabled={loading}
          onclick={async () => {
            loading = true;
            error = '';
            try {
              const res = await fetch('/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
              });
              const data = await res.json();
              if (!res.ok) throw new Error(toErrorText(data?.error, 'Login failed'));
              await invalidateAll();
              await goto('/', { invalidateAll: true });
            } catch (e: any) {
              error = toErrorText(e?.message, 'Login failed');
            } finally {
              loading = false;
            }
          }}
        >
          {loading ? 'Signing in...' : 'Sign In'}
        </button>

        <div class="login-footer">
          <span>No account yet?</span>
          <a href="/signup">Sign up</a>
        </div>
      </div>

    </div>

    <div class="column-stack column-demo">
      <div class="form-card demo-card demo-card-wide">
        <div class="card-header demo-header">
          <div>
            <div class="card-kicker">Demo access</div>
            <div class="form-card-title">Preconfigured roles</div>
            <div class="form-card-desc">Copy email and password directly for each demo role.</div>
          </div>
          {#if copied}
            <div class="copy-toast">Copied {copied}</div>
          {/if}
        </div>

        <div class="demo-list">
          {#each demoAccounts as account}
            <article class={`demo-row ${account.accent}`}>
              <div class="demo-body">
                <div class="demo-topline">
                  <div>
                    <div class="demo-role">{account.role}</div>
                    <div class="demo-label">{account.label}</div>
                  </div>
                  <div class={`role-chip ${account.accent}`}>{account.role}</div>
                </div>

                <div class="demo-meta">{account.description}</div>

                <div class="credential-block">
                  <div class="credential-item">
                    <span>Email</span>
                    <strong>{account.email}</strong>
                  </div>
                  <div class="credential-item">
                    <span>Password</span>
                    <strong>••••••••••••</strong>
                  </div>
                </div>
              </div>

              <div class="demo-actions">
                <button class="mini-btn mini-btn-ghost" type="button" onclick={() => copyText(account.email, `${account.role} email`)}>Copy email</button>
                <button class="mini-btn mini-btn-primary" type="button" onclick={() => copyText(account.password, `${account.role} password`)}>Copy password</button>
              </div>
            </article>
          {/each}
        </div>

        <div class="demo-note">
          Demo users are preconfigured for the presentation. In production, roles are assigned centrally in Auth0.
        </div>
      </div>
    </div>
  </div>
</div>

<style>
  .login-page {
    min-height: 100vh;
    display: flex;
    align-items: flex-start;
    justify-content: center;
    padding: 20px;
    background: var(--bg);
    position: relative;
    overflow-x: hidden;
    overflow-y: auto;
  }

  .login-page::before,
  .login-page::after {
    content: '';
    position: absolute;
    pointer-events: none;
    border-radius: 999px;
    filter: blur(24px);
    opacity: 0.8;
  }

  .login-page::before {
    width: 420px;
    height: 420px;
    top: -160px;
    right: -120px;
    background: radial-gradient(circle, rgba(37, 99, 235, 0.18), transparent 68%);
  }

  .login-page::after {
    width: 480px;
    height: 480px;
    bottom: -220px;
    left: -180px;
    background: radial-gradient(circle, rgba(16, 185, 129, 0.12), transparent 68%);
  }

  .login-shell {
    width: min(1080px, 100%);
    display: grid;
    grid-template-columns: minmax(0, 0.88fr) minmax(0, 1.12fr);
    grid-template-areas:
      'hero hero'
      'login demo';
    gap: 18px;
    position: relative;
    z-index: 1;
  }

  .hero-panel-wide {
    grid-area: hero;
  }

  .column-login {
    grid-area: login;
  }

  .column-demo {
    grid-area: demo;
  }

  .hero-panel {
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.03) 0%, rgba(255, 255, 255, 0.018) 100%);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 24px;
    padding: 28px;
    backdrop-filter: blur(28px);
    -webkit-backdrop-filter: blur(28px);
    position: relative;
    overflow: hidden;
    box-shadow: 0 24px 80px rgba(0, 0, 0, 0.28);
  }

  .hero-panel-wide {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr);
    gap: 22px 24px;
    align-items: center;
    padding: 22px 24px;
  }

  .hero-panel::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), transparent 35%, rgba(16, 185, 129, 0.08));
    pointer-events: none;
  }

  .brand-lockup {
    display: flex;
    align-items: center;
    gap: 14px;
    position: relative;
    z-index: 1;
  }

  .brand-mark {
    width: 44px;
    height: 44px;
    border-radius: 14px;
    background: linear-gradient(135deg, rgba(30, 64, 175, 0.95), rgba(8, 145, 178, 0.75));
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.3), 0 16px 36px rgba(37, 99, 235, 0.28);
  }

  .brand-dot {
    width: 12px;
    height: 12px;
    border-radius: 999px;
    background: #fff;
    box-shadow: 0 0 14px rgba(255, 255, 255, 0.35);
  }

  .brand-name {
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.5px;
    color: var(--text-1);
  }

  .brand-sub {
    font-size: 11px;
    color: var(--text-3);
    letter-spacing: 1px;
    text-transform: uppercase;
    margin-top: 3px;
  }

  .hero-copy {
    position: relative;
    z-index: 1;
    margin-top: 0;
    max-width: 540px;
  }

  .hero-copy-compact h1 {
    max-width: none;
  }

  .eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 5px 12px;
    border-radius: 999px;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 1.2px;
    text-transform: uppercase;
    color: #93c5fd;
    background: rgba(59, 130, 246, 0.09);
    border: 1px solid rgba(59, 130, 246, 0.18);
  }

  .hero-copy h1 {
    margin-top: 16px;
    font-size: clamp(30px, 3.6vw, 46px);
    line-height: 1.03;
    letter-spacing: -2px;
    color: var(--text-1);
    max-width: 8ch;
  }

  .hero-copy p {
    margin-top: 16px;
    font-size: 15px;
    line-height: 1.75;
    color: rgba(180, 198, 222, 0.76);
    max-width: 56ch;
  }

  .hero-points {
    display: grid;
    gap: 12px;
    margin-top: 0;
    position: relative;
    z-index: 1;
  }

  .hero-points-inline {
    grid-column: 2;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
    align-items: start;
  }

  .hero-point {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    color: rgba(226, 232, 240, 0.88);
    font-size: 13px;
    line-height: 1.55;
  }

  .point-bullet {
    width: 9px;
    height: 9px;
    border-radius: 999px;
    margin-top: 5px;
    background: linear-gradient(135deg, #60a5fa, #34d399);
    box-shadow: 0 0 12px rgba(96, 165, 250, 0.32);
    flex-shrink: 0;
  }

  .column-stack {
    display: grid;
    gap: 12px;
    align-content: start;
  }

  .login-card-compact {
    min-height: 100%;
  }

  .demo-card-wide {
    min-height: 100%;
  }

  .login-card,
  .demo-card {
    width: 100%;
    max-width: none;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    margin-bottom: 18px;
  }

  .card-kicker {
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 1.2px;
    text-transform: uppercase;
    color: var(--text-3);
    margin-bottom: 5px;
  }

  .copy-toast {
    flex-shrink: 0;
    padding: 7px 11px;
    border-radius: 999px;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.8px;
    text-transform: uppercase;
    background: rgba(59, 130, 246, 0.1);
    color: #93c5fd;
    border: 1px solid rgba(59, 130, 246, 0.18);
  }

  .copy-toast {
    background: rgba(16, 185, 129, 0.1);
    color: #6ee7b7;
    border-color: rgba(16, 185, 129, 0.22);
  }

  .login-footer {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 6px;
    margin-top: 18px;
    font-size: 12px;
    color: var(--text-3);
  }

  .login-footer a {
    color: var(--blue-light);
    text-decoration: none;
  }

  .login-footer a:hover {
    text-decoration: underline;
  }

  .demo-list {
    display: grid;
    gap: 12px;
  }

  .demo-row {
    display: grid;
    grid-template-columns: minmax(0, 1.4fr) auto;
    gap: 10px;
    padding: 12px 14px;
    border-radius: 14px;
    background: rgba(255, 255, 255, 0.026);
    border: 1px solid rgba(255, 255, 255, 0.07);
  }

  .demo-row.blue {
    box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.05) inset;
  }

  .demo-row.green {
    box-shadow: 0 0 0 1px rgba(16, 185, 129, 0.05) inset;
  }

  .demo-row.amber {
    box-shadow: 0 0 0 1px rgba(245, 158, 11, 0.05) inset;
  }

  .demo-topline {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 6px;
  }

  .demo-role {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-1);
    letter-spacing: -0.2px;
  }

  .demo-label,
  .demo-meta {
    font-size: 12px;
    color: var(--text-3);
    line-height: 1.55;
  }

  .demo-meta {
    margin-bottom: 8px;
  }

  .credential-block {
    display: grid;
    gap: 8px;
  }

  .credential-item {
    display: grid;
    gap: 3px;
  }

  .credential-item span {
    font-size: 9.5px;
    font-weight: 700;
    letter-spacing: 1px;
    text-transform: uppercase;
    color: rgba(148, 163, 184, 0.35);
  }

  .credential-item strong {
    font-size: 12.5px;
    font-family: var(--mono);
    color: var(--text-2);
    overflow-wrap: anywhere;
  }

  .demo-actions {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 120px;
    align-self: center;
  }

  .mini-btn {
    width: 100%;
    border-radius: 9px;
    border: 1px solid;
    padding: 8px 10px;
    font: 600 12px/1 'Outfit', sans-serif;
    cursor: pointer;
    transition: all 0.18s ease;
  }

  .mini-btn-ghost {
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(255, 255, 255, 0.08);
    color: var(--text-2);
  }

  .mini-btn-ghost:hover {
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(255, 255, 255, 0.14);
  }

  .mini-btn-primary {
    background: rgba(59, 130, 246, 0.12);
    border-color: rgba(59, 130, 246, 0.24);
    color: #93c5fd;
  }

  .mini-btn-primary:hover {
    background: rgba(59, 130, 246, 0.18);
    border-color: rgba(59, 130, 246, 0.34);
  }

  .role-chip {
    display: inline-flex;
    align-items: center;
    padding: 4px 10px;
    border-radius: 999px;
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 0.9px;
    text-transform: uppercase;
    border: 1px solid transparent;
    white-space: nowrap;
  }

  .role-chip.blue {
    background: rgba(59, 130, 246, 0.1);
    color: #93c5fd;
    border-color: rgba(59, 130, 246, 0.18);
  }

  .role-chip.green {
    background: rgba(16, 185, 129, 0.1);
    color: #6ee7b7;
    border-color: rgba(16, 185, 129, 0.2);
  }

  .role-chip.amber {
    background: rgba(245, 158, 11, 0.1);
    color: #fcd34d;
    border-color: rgba(245, 158, 11, 0.2);
  }

  .demo-note {
    margin-top: 12px;
    font-size: 11.5px;
    line-height: 1.6;
    color: var(--text-3);
  }

  @media (max-width: 1040px) {
    .login-shell {
      grid-template-columns: 1fr;
      grid-template-areas:
        'hero'
        'login'
        'demo';
      max-width: 760px;
    }

    .hero-panel-wide {
      grid-template-columns: 1fr;
      align-items: start;
    }

    .hero-points-inline {
      grid-column: auto;
      grid-template-columns: 1fr;
    }

    .hero-copy h1 {
      max-width: 14ch;
    }
  }

  @media (max-width: 640px) {
    .login-page {
      padding: 12px;
      align-items: start;
    }

    .hero-panel,
    .form-card {
      padding: 20px;
      border-radius: 16px;
    }

    .hero-copy h1 {
      font-size: 28px;
      letter-spacing: -1.4px;
    }

    .card-header,
    .demo-topline {
      flex-direction: column;
    }

    .hero-panel-wide {
      gap: 14px;
    }

    .demo-row {
      grid-template-columns: 1fr;
    }

    .demo-actions {
      min-width: 0;
      flex-direction: row;
      flex-wrap: wrap;
    }

    .mini-btn {
      width: auto;
      flex: 1 1 120px;
    }

    .card-header,
    .demo-topline,
    .demo-row {
      gap: 10px;
    }
  }
</style>
