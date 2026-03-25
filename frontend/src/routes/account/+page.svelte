<script lang="ts">
  import { page } from '$app/stores';

  const user = $derived($page.data.user);
</script>

<div class="topbar">
  <div>
    <div class="pg-ttl">Account</div>
    <div class="pg-sub">Your profile and access settings</div>
  </div>
</div>

<div class="content">
  {#if user}
    <div class="acct-glass">
      <div style="display:flex;align-items:center;gap:16px;margin-bottom:20px;">
        {#if user.picture}
          <img src={user.picture} alt="Avatar" class="a-av" style="object-fit:cover;" />
        {:else}
          <div class="a-av">{(user.name || user.email || '?')[0].toUpperCase()}</div>
        {/if}
        <div>
          <div class="a-nm">{user.name || user.nickname || 'User'}</div>
          <div class="a-em">{user.email || ''}</div>
        </div>
      </div>

      <div class="a-div"></div>

      <div class="a-row">
        <span class="a-key">User ID</span>
        <span class="a-v" style="max-width:220px;overflow:hidden;text-overflow:ellipsis;">{user.sub || '—'}</span>
      </div>
      <div class="a-row">
        <span class="a-key">Email</span>
        <span class="a-v">{user.email || '—'}</span>
      </div>
      <div class="a-row">
        <span class="a-key">Roles</span>
        <div style="display:flex;gap:6px;flex-wrap:wrap;justify-content:flex-end;">
          {#if user.user_roles && user.user_roles.length > 0}
            {#each user.user_roles as role}
              <span class="role-chip">{role}</span>
            {/each}
          {:else}
            <span class="a-v">No roles assigned</span>
          {/if}
        </div>
      </div>

      <form method="POST" action="/logout">
        <button type="submit" class="so-btn">Sign Out</button>
      </form>
    </div>
  {:else}
    <div class="acct-glass">
      <p style="color:var(--text-3);text-align:center;font-size:13px;">
        Not signed in. <a href="/login" style="color:var(--blue-light);">Sign in</a>
      </p>
    </div>
  {/if}
</div>
