import type { Plugin } from "@opencode-ai/plugin"
import { tool } from "@opencode-ai/plugin/tool"
import yaml from "js-yaml"

export default (async ({ project, $ }) => {
  const worktree = project.worktree || process.cwd()
  const providersPath = `${worktree}/providers.yaml`
  const configPath = `${worktree}/opencode.json`

  let providersData: Record<string, any> = {}
  try {
    const file = Bun.file(providersPath)
    const exists = await file.exists()
    if (exists) {
      const content = await file.text()
      const parsed = yaml.load(content)
      providersData = (parsed && typeof parsed === "object" ? parsed : {}) as Record<string, any>
    }
  } catch (e) {
    console.warn("[provider-manager] Failed to read providers.yaml:", e)
  }

  async function readProvidersYaml(): Promise<Record<string, any>> {
    const content = await Bun.file(providersPath).text()
    const parsed = yaml.load(content)
    return (parsed && typeof parsed === "object" ? parsed : {}) as Record<string, any>
  }

  async function writeProvidersYaml(data: Record<string, any>): Promise<void> {
    const content = yaml.dump(data, { indent: 2, lineWidth: -1, noRefs: true, noCompatMode: true })
    await Bun.write(providersPath, content)
  }

  return {
    config: (cfg: any) => {
      cfg.provider = cfg.provider || {}
      for (const [id, conf] of Object.entries(providersData)) {
        if (!conf || typeof conf !== "object") continue
        const c = conf as Record<string, any>
        const entry: Record<string, any> = {
          npm: c.npm || "@ai-sdk/openai-compatible",
          name: c.name || id,
          options: {},
        }
        if (c.base_url) entry.options.baseURL = c.base_url
        if (c.api_key) entry.options.apiKey = c.api_key
        if (c.models) entry.models = c.models
        cfg.provider[id] = entry
      }
    },

    tool: {
      add_provider: tool({
        description: "Add or update a third-party LLM provider in providers.yaml",
        args: {
          provider_id: tool.schema.string({ description: "Provider identifier (e.g. ollama, deepseek)" }),
          name: tool.schema.string({ description: "Display name for the provider" }),
          npm: tool.schema.string({ description: "AI SDK npm package", default: "@ai-sdk/openai-compatible" }),
          base_url: tool.schema.string({ description: "API base URL (e.g. http://localhost:11434/v1)" }),
          api_key: tool.schema.string({ description: "API key or {env:VAR} reference", optional: true }),
          models: tool.schema.string({ description: 'JSON string of model configs (e.g. {"gpt-4o":{"name":"GPT-4o"}})', optional: true }),
        },
        execute: async (args) => {
          let data: Record<string, any> = {}
          try { data = await readProvidersYaml() } catch {}

          const entry: Record<string, any> = {
            name: args.name,
            npm: args.npm || "@ai-sdk/openai-compatible",
            base_url: args.base_url,
          }
          if (args.api_key) entry.api_key = args.api_key
          if (args.models) {
            try { entry.models = JSON.parse(args.models) } catch { entry.models = args.models }
          }

          data[args.provider_id] = entry
          await writeProvidersYaml(data)
          return `Provider "${args.provider_id}" added to providers.yaml. Restart opencode to apply.`
        },
      }),

      remove_provider: tool({
        description: "Remove a provider from providers.yaml",
        args: {
          provider_id: tool.schema.string({ description: "Provider identifier to remove" }),
        },
        execute: async (args) => {
          let data: Record<string, any> = {}
          try { data = await readProvidersYaml() } catch {
            return `Failed to read providers.yaml`
          }
          if (!data[args.provider_id]) {
            return `Provider "${args.provider_id}" not found in providers.yaml`
          }
          delete data[args.provider_id]
          await writeProvidersYaml(data)
          return `Provider "${args.provider_id}" removed from providers.yaml. Restart opencode to apply.`
        },
      }),

      list_providers: tool({
        description: "List all configured third-party providers from providers.yaml",
        execute: async () => {
          let data: Record<string, any> = {}
          try { data = await readProvidersYaml() } catch {
            return "No providers.yaml found or unable to read it."
          }
          const ids = Object.keys(data)
          if (ids.length === 0) return "No providers configured in providers.yaml."

          const lines = ids.map((id) => {
            const p = data[id]
            const models = p.models ? Object.keys(p.models).join(", ") : "none"
            return `- ${id} (${p.name || id})\n  base_url: ${p.base_url || "—"}\n  models: ${models}`
          })
          return `Configured providers:\n\n${lines.join("\n")}`
        },
      }),

      edit_config: tool({
        description: "Modify a setting in opencode.json (e.g. model, small_model)",
        args: {
          key: tool.schema.string({ description: "Config key to set (e.g. model, small_model)" }),
          value: tool.schema.string({ description: "Value for the config key" }),
        },
        execute: async (args) => {
          let config: Record<string, any> = {}
          try {
            const content = await Bun.file(configPath).text()
            config = JSON.parse(content)
          } catch {
            return `Failed to read or parse opencode.json at ${configPath}`
          }

          try {
            config[args.key] = JSON.parse(args.value)
          } catch {
            config[args.key] = args.value
          }

          await Bun.write(configPath, JSON.stringify(config, null, 2) + "\n")
          return `Set opencode.json: ${args.key} = ${JSON.stringify(config[args.key])}. Restart opencode to apply.`
        },
      }),

      detect_local_providers: tool({
        description: "Detect local LLM servers (Ollama, LM Studio) via curl",
        execute: async () => {
          const results: string[] = []

          try {
            const out = await $`curl -s --max-time 3 http://localhost:11434/api/tags`.text()
            const json = JSON.parse(out)
            if (json.models?.length > 0) {
              const modelList = json.models.map((m: any) => m.name).join(", ")
              results.push(`Ollama detected at http://localhost:11434\n   Models: ${modelList}`)
            }
          } catch {}

          try {
            const out = await $`curl -s --max-time 3 http://localhost:1234/v1/models`.text()
            const json = JSON.parse(out)
            if (json.data?.length > 0) {
              const modelList = json.data.map((m: any) => m.id).join(", ")
              results.push(`LM Studio detected at http://localhost:1234\n   Models: ${modelList}`)
            }
          } catch {}

          if (results.length === 0) {
            return "No local LLM servers detected. Start Ollama or LM Studio and try again."
          }
          return `Detection results:\n\n${results.join("\n\n")}`
        },
      }),
    },

    command: {
      "add-provider": {
        description: "Add or update a third-party LLM provider in providers.yaml",
        template:
          "Help the user add a new LLM provider to providers.yaml. Ask for the provider ID, display name, base URL, API key (optional), and model configurations. Use the add_provider tool to write. After adding, tell the user to restart opencode.",
      },
      "remove-provider": {
        description: "Remove a provider from providers.yaml",
        template:
          "Help the user remove a provider from providers.yaml. First use list_providers to show available providers, then ask which to remove. Use remove_provider tool. After removing, tell the user to restart opencode.",
      },
      "list-providers": {
        description: "List all configured third-party providers from providers.yaml",
        template: "Use the list_providers tool to show all configured providers.",
      },
      "edit-config": {
        description: "Modify a setting in opencode.json (e.g. model, small_model)",
        template:
          "Help the user modify opencode.json. Ask which key to change (e.g. model, small_model) and the value. Use edit_config tool. After changing, tell the user to restart opencode.",
      },
      "detect-providers": {
        description: "Detect local LLM servers (Ollama, LM Studio) on this machine",
        template:
          "Use detect_local_providers tool to find local LLM servers. Show results and offer to add detected providers to providers.yaml.",
      },
    },
  }
}) satisfies Plugin