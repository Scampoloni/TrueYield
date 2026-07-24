<script lang="ts">
  import { goto, invalidateAll } from '$app/navigation';

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

  let name = $state('');
  let email = $state('');
  let password = $state('');
  let loading = $state(false);
  let error = $state('');
</script>

<svelte:head>
  <title>Create account | TrueYield</title>
</svelte:head>

<div style="display:flex;align-items:center;justify-content:center;min-height:100vh;background:var(--bg);">
  <div class="form-card" style="width:100%;max-width:400px;">
    <div style="text-align:center;margin-bottom:32px;">
      <div style="display:flex;align-items:center;justify-content:center;gap:9px;margin-bottom:8px;">
        <div style="width:10px;height:10px;border-radius:50%;background:var(--green);box-shadow:0 0 10px rgba(52,211,153,0.5);"></div>
        <span style="font-size:22px;font-weight:700;letter-spacing:-0.5px;color:var(--text);">TrueYield</span>
      </div>
      <div style="font-size:11px;color:var(--text3);letter-spacing:1px;text-transform:uppercase;">ESG Compliance Engine</div>
    </div>

    {#if error}
      <div class="alert-error" style="margin-bottom:16px;">{error}</div>
    {/if}

    <div class="form-row">
      <label class="form-label" for="name">Name</label>
      <input id="name" class="form-input" type="text" bind:value={name} placeholder="Your name" />
    </div>
    <div class="form-row">
      <label class="form-label" for="email">Email</label>
      <input id="email" class="form-input" type="email" bind:value={email} placeholder="you@example.com" />
    </div>
    <div class="form-row">
      <label class="form-label" for="password">Password</label>
      <input id="password" class="form-input" type="password" bind:value={password} placeholder="••••••••" />
      <div class="form-hint">At least 8 chars and 3 of 4 types: lowercase, uppercase, number, special char.</div>
    </div>

    <button
      class="btn btn-primary btn-block"
      style="margin-top:8px;margin-bottom:16px;"
      disabled={loading}
      onclick={async () => {
        loading = true; error = '';
        try {
          const res = await fetch('/auth/signup', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password })
          });
          const data = await res.json();
          if (!res.ok) throw new Error(toErrorText(data?.error, 'Signup failed'));
          await invalidateAll();
          await goto('/', { invalidateAll: true });
        } catch (e: any) {
          error = toErrorText(e?.message, 'Signup failed');
        } finally {
          loading = false;
        }
      }}
    >
      {loading ? 'Creating account...' : 'Create Account'}
    </button>

    <div style="text-align:center;font-size:12px;color:var(--text3);">
      Already have an account? <a href="/login" style="color:var(--blue);">Sign in</a>
    </div>
  </div>
</div>
