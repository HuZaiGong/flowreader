---
name: provider-manager
description: >-
  Use when the user wants to add, remove, list, or configure third-party LLM
  providers (Ollama, DeepSeek, OpenAI-compatible APIs, etc.), detect local LLM
  servers, or modify opencode settings. Trigger keywords: provider, providers,
  providers.yaml, third-party, LLM, model, connect, configure, /add-provider,
  /remove-provider, /list-providers, /edit-config, /detect-providers.
---

# Provider Manager

This skill works with the `opencode-provider-manager` plugin located in
`.opencode/plugins/provider-manager.ts`.

## Available Commands

| Command | Purpose |
|---------|---------|
| `/add-provider` | Add or update a provider in providers.yaml |
| `/remove-provider` | Remove a provider from providers.yaml |
| `/list-providers` | Show all configured providers |
| `/edit-config` | Modify opencode.json settings |
| `/detect-providers` | Detect local Ollama/LM Studio servers |

## Available Tools (AI auto-calls)

- `add_provider` — add provider to YAML
- `remove_provider` — remove from YAML
- `list_providers` — read YAML and display
- `edit_config` — write opencode.json key/value
- `detect_local_providers` — curl local ports

## Workflow

1. For `/` commands: the user types `/add-provider` etc., and you follow the
   command template to help them.
2. When the user says "add ollama" or "configure deepseek": use the
   `add_provider` tool directly.
3. When the user says "change my model to X": use `edit_config` tool.
4. After any change to `providers.yaml` or `opencode.json`, remind the user to
   **restart opencode** for changes to take effect.

## providers.yaml location

The file is at `providers.yaml` in the project root. The user can also edit it
manually. The plugin reads it at startup and injects all providers into
opencode's `provider` config.

Format:

```yaml
provider-id:
  name: "Display Name"
  npm: "@ai-sdk/openai-compatible"
  base_url: "https://api.example.com/v1"
  api_key: "{env:API_KEY}"      # optional
  models:
    model-id:
      name: "Model Name"
      limit:
        context: 128000
        output: 4096
```