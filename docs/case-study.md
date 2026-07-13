# TrueYield case study

## Problem

ESG-related portfolio review often requires analysts to collect scattered source material, reason about uncertainty and record a defensible human decision. This prototype explores a small workflow around those activities.

## Design decisions

- **Evidence first:** ingestion completes before the advisory analysis starts, preventing a score from being calculated against an empty evidence set.
- **Grounded advisory output:** the model is instructed to cite supplied evidence IDs. Unknown citations or incomplete output do not produce a score.
- **Human decision remains authoritative:** approve and reject actions require a rationale and are recorded as application events.
- **Explicit boundaries:** ESG evidence signals are operational prioritisation aids, not legal or regulatory classifications.

## What to discuss in an interview

Explain the role model, state transitions, evidence provenance, fallback behaviour, test strategy and why a human reviewer can override or reject an advisory output. Do not claim that the prototype detects greenwashing reliably, guarantees compliance or has been used by customers.
