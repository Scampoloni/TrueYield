<script lang="ts">
  import { toasts } from '$lib/toast';
</script>

<div class="toast-container">
  {#each $toasts as toast (toast.id)}
    <div class="toast {toast.type}">
      <div class="toast-dot"></div>
      {toast.message}
    </div>
  {/each}
</div>
