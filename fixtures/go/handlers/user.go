package handlers

func UserHandler() bool {
	return true
}

type OpenAIConfig struct {
	APIKey  string `toml:"api_key"`
	Model   string `toml:"model"`
	BaseURL string `toml:"base_url"`
}
