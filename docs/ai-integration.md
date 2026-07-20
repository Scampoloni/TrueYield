# AI integration

TrueYield uses Spring AI with Anthropic as an optional integration. When `ANTHROPIC_API_KEY` is absent, AI-powered features are unavailable; the core application still requires Auth0 and MongoDB.

## Prototype use cases

- Summarising evidence associated with a portfolio.
- Assigning an evidence-oriented risk narrative.
- Filtering news candidates for ESG relevance before they are stored.
- Supporting the authenticated chat interface.

News ingestion can use configured Guardian, Newsdata.io, NewsAPI.org, and Alpha Vantage credentials. Individual providers are optional and may return no suitable data.

## Controls and limitations

AI output is displayed as assistance for a human reviewer. It is not a source of truth, a regulatory classification, investment advice, or an automated decision. A reviewer must assess source material, relevance, and the workflow outcome. Provider availability, prompts, model behaviour, and source quality can affect results.

The project does not evaluate model performance, provide regulatory validation, or guarantee that a summary is complete or correct.
