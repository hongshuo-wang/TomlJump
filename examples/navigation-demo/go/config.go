package config

// CASE 1 - Command/Ctrl-click GoServiceConfig or GoToken: one target, so jump directly.
type GoServiceConfig struct {
	GoToken string `toml:"go_token"`
	Schema  string
}
