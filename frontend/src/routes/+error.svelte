<script lang="ts">
  import { page } from '$app/stores';
</script>

<div class="topbar">
  <div><div class="page-title">Error</div></div>
</div>
<div class="content">
  <div class="empty-state" style="padding:80px 20px;">
    <div style="font-size:64px;font-weight:700;color:var(--text3);letter-spacing:-4px;margin-bottom:16px;">
      {$page.status}
    </div>
    <div class="empty-title">
      {$page.status === 404 ? 'Page Not Found' : $page.status === 403 ? 'Access Denied' : 'Something went wrong'}
    </div>
    <div class="empty-desc">
      {$page.status === 404 ? "The page you're looking for doesn't exist." : $page.status === 403 ? "You don't have permission to view this resource." : $page.error?.message || 'An unexpected error occurred.'}
    </div>
    <a href="/" class="btn btn-ghost">← Go back home</a>
  </div>
</div>
