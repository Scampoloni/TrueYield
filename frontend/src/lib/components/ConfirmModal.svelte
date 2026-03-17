<script lang="ts">
  let { title, description, confirmLabel = 'Confirm', onConfirm, onCancel, danger = false }: {
    title: string;
    description: string;
    confirmLabel?: string;
    onConfirm: () => void;
    onCancel: () => void;
    danger?: boolean;
  } = $props();

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Escape') onCancel();
  }
</script>

<svelte:window onkeydown={handleKeydown} />

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="modal-overlay" onclick={onCancel} role="presentation">
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div class="modal" onclick={(e) => e.stopPropagation()} role="dialog" aria-modal="true" aria-label={title}>
    <div class="modal-title">{title}</div>
    <div class="modal-desc">{description}</div>
    <div class="modal-actions">
      <button class="btn btn-ghost" onclick={onCancel}>Cancel</button>
      <button class="btn" class:btn-danger={danger} class:btn-primary={!danger} onclick={onConfirm}>
        {confirmLabel}
      </button>
    </div>
  </div>
</div>
