<script lang="ts">
  let { rows = 3 }: { rows?: number } = $props();
</script>

<div class="skeleton-pad">
  {#each Array(rows) as _}
    <div class="skeleton" style="height:20px;"></div>
  {/each}
</div>
