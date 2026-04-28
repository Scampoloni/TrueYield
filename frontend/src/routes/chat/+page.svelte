<script lang="ts">
  import { page } from '$app/state';

  type ChatMessage = {
    role: 'user' | 'assistant';
    text: string;
  };

  const roles = $derived(page.data.roles ?? []);

  function buildWelcomeMessage(): string {
    if (roles.includes('fund-manager')) {
      return `👋 Hello! I'm your ESG analyst assistant. As a **Fund Manager** you can ask me:
- *"List all my portfolios"*
- *"What holdings are in portfolio X?"*
- *"Show me the ESG evidence for holding Y"*
- *"Create a new portfolio called Green Alpha"*
- *"Add holding AAPL to portfolio Green Alpha"*`;
    }
    if (roles.includes('auditor')) {
      return `👋 Hello! I'm your ESG analyst assistant. As an **Auditor** you can ask me:
- *"List all portfolios in the system"*
- *"What are the holdings in portfolio X?"*
- *"Show me the ESG evidence and risk scores for holding Y"*
- *"Which holdings have the highest ESG risk?"*`;
    }
    if (roles.includes('compliance-officer')) {
      return `👋 Hello! I'm your ESG analyst assistant. As a **Compliance Officer** you can ask me:
- *"List all portfolios in the system"*
- *"What is the ESG risk profile of portfolio X?"*
- *"Which holdings have negative ESG sentiment?"*
- *"Show me evidence for holding Y"*`;
    }
    return `👋 Hello! I'm your ESG analyst assistant. Ask me anything about portfolios, holdings, or ESG evidence.`;
  }

  let messages = $state<ChatMessage[]>([{ role: 'assistant', text: buildWelcomeMessage() }]);
  let input = $state('');
  let loading = $state(false);
  let inlineError = $state('');

  function escapeHtml(value: string) {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function renderAssistantMarkdown(value: string) {
    const escaped = escapeHtml(value);
    const boldParsed = escaped.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    return boldParsed.replace(/\n/g, '<br>');
  }

  async function sendMessage() {
    const text = input.trim();
    if (!text || loading) return;

    inlineError = '';
    messages = [...messages, { role: 'user', text }];
    input = '';
    loading = true;

    try {
      const res = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: text })
      });
      const data = await res.json();

      if (!res.ok) {
        const errorText =
          (data && typeof data.error === 'string' && data.error) ||
          (data && typeof data.message === 'string' && data.message) ||
          'Chat request failed';
        inlineError = errorText;
        messages = [...messages, { role: 'assistant', text: `Error: ${errorText}` }];
        return;
      }

      const answer = data?.answer && typeof data.answer === 'string'
        ? data.answer
        : 'No response available.';
      messages = [...messages, { role: 'assistant', text: answer }];
    } catch {
      inlineError = 'Chat request failed. Please try again.';
      messages = [...messages, { role: 'assistant', text: 'Error: Chat request failed. Please try again.' }];
    } finally {
      loading = false;
    }
  }
</script>

<div class="topbar">
  <div>
    <div class="pg-ttl">ESG Analyst Chat</div>
    <div class="pg-sub">Ask about portfolios, holdings, and evidence.</div>
  </div>
</div>

<div class="content">
  <section class="glass-table chat-wrap">
    <div class="chat-log" role="log" aria-live="polite">
      {#each messages as message}
        <div class="row" class:user={message.role === 'user'}>
          <div class="bubble" class:user={message.role === 'user'}>
            {#if message.role === 'assistant'}
              {@html renderAssistantMarkdown(message.text)}
            {:else}
              {message.text}
            {/if}
          </div>
        </div>
      {/each}

      {#if loading}
        <div class="row">
          <div class="bubble assistant-loading" aria-label="Assistant is typing">
            <span></span><span></span><span></span>
          </div>
        </div>
      {/if}
    </div>

    {#if inlineError}
      <div class="alert-error" style="margin: 10px 0 0 0;">{inlineError}</div>
    {/if}

    <div class="composer">
      <input
        class="form-input"
        type="text"
        bind:value={input}
        placeholder="Ask about ESG risks, holdings, or create entities..."
        onkeydown={(event) => {
          if (event.key === 'Enter') {
            event.preventDefault();
            sendMessage();
          }
        }}
        disabled={loading}
      />
      <button class="btn btn-primary" onclick={sendMessage} disabled={loading || !input.trim()}>
        {loading ? 'Sending...' : 'Send'}
      </button>
    </div>
  </section>
</div>

<style>
  .chat-wrap {
    min-height: calc(100vh - 220px);
    display: flex;
    flex-direction: column;
    padding: 16px;
  }

  .chat-log {
    flex: 1;
    overflow: auto;
    padding: 8px;
    border-radius: 10px;
    background: rgba(2, 6, 23, 0.35);
    border: 1px solid var(--line);
  }

  .row {
    display: flex;
    margin: 8px 0;
  }

  .row.user {
    justify-content: flex-end;
  }

  .bubble {
    max-width: 78%;
    border-radius: 14px;
    padding: 10px 12px;
    line-height: 1.4;
    font-size: 14px;
    color: var(--text2);
    background: rgba(15, 23, 42, 0.75);
    border: 1px solid var(--line);
  }

  .bubble.user {
    color: #e0f2fe;
    background: linear-gradient(180deg, rgba(14, 116, 144, 0.35), rgba(2, 132, 199, 0.28));
    border-color: rgba(14, 165, 233, 0.35);
  }

  .composer {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 10px;
    margin-top: 12px;
  }

  .assistant-loading {
    display: inline-flex;
    gap: 6px;
    align-items: center;
  }

  .assistant-loading span {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--text3);
    animation: blink 1s infinite ease-in-out;
  }

  .assistant-loading span:nth-child(2) {
    animation-delay: 0.15s;
  }

  .assistant-loading span:nth-child(3) {
    animation-delay: 0.3s;
  }

  @keyframes blink {
    0%,
    80%,
    100% {
      opacity: 0.25;
      transform: translateY(0);
    }
    40% {
      opacity: 1;
      transform: translateY(-1px);
    }
  }

  @media (max-width: 800px) {
    .bubble {
      max-width: 92%;
    }

    .composer {
      grid-template-columns: 1fr;
    }
  }
</style>
