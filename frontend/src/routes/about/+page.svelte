<svelte:head>
  <title>About | TrueYield</title>
  <meta name="description" content="Scope and limitations of the TrueYield academic portfolio prototype." />
</svelte:head>

<main class="page-shell">
  <a class="back" href="/login">← Back to demo sign-in</a>
  <header>
    <p class="eyebrow">Academic portfolio prototype</p>
    <h1>Evidence review before an ESG decision.</h1>
    <p class="lead">TrueYield demonstrates a human-reviewed workflow for collecting portfolio evidence, producing an advisory AI summary, and recording an auditor decision.</p>
  </header>

  <section>
    <h2>What it demonstrates</h2>
    <ul>
      <li>Role-based portfolio, evidence and audit-review workflows.</li>
      <li>Evidence-grounded AI output with cited evidence IDs and analysis metadata.</li>
      <li>Human approval or rejection with a recorded rationale and review history.</li>
    </ul>
  </section>

  <section>
    <h2>Deliberate boundaries</h2>
    <ul>
      <li>Demo data and external news sources may be incomplete, delayed or unavailable.</li>
      <li>AI output is advisory only; it is not an investment recommendation or a regulatory determination.</li>
      <li>The prototype does not certify SFDR, EU Taxonomy, CSRD or greenwashing compliance.</li>
    </ul>
  </section>

  <section>
    <h2>Technology</h2>
    <p>Java / Spring Boot, MongoDB, SvelteKit, Auth0, Docker, GitHub Actions and an optional Spring AI integration.</p>
  </section>
</main>

<style>
  .page-shell { max-width: 850px; margin: 0 auto; padding: 56px 24px; color: var(--text-1); }
  .back { color: var(--blue-light); text-decoration: none; font-size: 14px; }
  header { margin: 48px 0; }
  .eyebrow { color: #93c5fd; font-size: 12px; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; }
  h1 { font-size: clamp(38px, 7vw, 64px); line-height: 1.05; letter-spacing: -2px; max-width: 15ch; }
  .lead, li, section p { color: var(--text-2); line-height: 1.7; font-size: 17px; }
  section { border-top: 1px solid rgba(255,255,255,.1); padding: 28px 0; }
  h2 { font-size: 21px; }
  ul { padding-left: 22px; }
</style>
