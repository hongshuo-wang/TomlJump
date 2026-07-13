export function sync(): boolean {
  return true;
}

interface OpenAIConfig {
  apiKey: string
  model: string
  baseUrl: string
}
