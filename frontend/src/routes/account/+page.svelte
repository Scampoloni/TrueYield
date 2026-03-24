<script lang="ts">
  import { page } from '$app/stores';

  const user = $derived($page.data.user);
</script>

<div class="container" style="max-width:640px;margin:0 auto;padding:32px 16px;">
  <h1 style="font-size:20px;font-weight:700;margin-bottom:24px;color:var(--text);">My Account</h1>

  {#if user}
    <div class="form-card" style="margin-bottom:16px;">
      <div style="display:flex;align-items:center;gap:16px;margin-bottom:20px;">
        {#if user.picture}
          <img src={user.picture} alt="Avatar" style="width:56px;height:56px;border-radius:50%;border:2px solid var(--border);" />
        {:else}
          <div style="width:56px;height:56px;border-radius:50%;background:var(--surface2);display:flex;align-items:center;justify-content:center;font-size:22px;font-weight:700;color:var(--text2);">
            {(user.name || user.email || '?')[0].toUpperCase()}
          </div>
        {/if}
        <div>
          <div style="font-size:16px;font-weight:600;color:var(--text);">{user.name || user.nickname || 'User'}</div>
          <div style="font-size:13px;color:var(--text3);">{user.email || ''}</div>
        </div>
      </div>

      <div style="display:grid;gap:12px;">
        <div style="display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-bottom:1px solid var(--border);">
          <span style="font-size:13px;color:var(--text3);">User ID</span>
          <span style="font-size:12px;font-family:monospace;color:var(--text2);max-width:260px;overflow:hidden;text-overflow:ellipsis;">{user.sub || '—'}</span>
        </div>
        <div style="display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-bottom:1px solid var(--border);">
          <span style="font-size:13px;color:var(--text3);">Email</span>
          <span style="font-size:13px;color:var(--text2);">{user.email || '—'}</span>
        </div>
        <div style="display:flex;justify-content:space-between;align-items:center;padding:10px 0;">
          <span style="font-size:13px;color:var(--text3);">Roles</span>
          <div style="display:flex;gap:6px;flex-wrap:wrap;justify-content:flex-end;">
            {#if user.user_roles && user.user_roles.length > 0}
              {#each user.user_roles as role}
                <span style="font-size:11px;font-weight:600;letter-spacing:0.5px;padding:3px 8px;border-radius:4px;background:var(--surface2);color:var(--text2);text-transform:uppercase;">
                  {role}
                </span>
              {/each}
            {:else}
              <span style="font-size:13px;color:var(--text3);">No roles assigned</span>
            {/if}
          </div>
        </div>
      </div>
    </div>

    <form method="POST" action="/logout">
      <button type="submit" class="btn" style="color:var(--red);border:1px solid var(--red);background:transparent;width:100%;">
        Sign Out
      </button>
    </form>
  {:else}
    <div class="form-card">
      <p style="color:var(--text3);text-align:center;">Not signed in. <a href="/login" style="color:var(--blue);">Sign in</a></p>
    </div>
  {/if}
</div>
