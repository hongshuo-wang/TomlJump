package config

// CASE 2 - Command/Ctrl-click MultiTargetConfig or SharedToken.
// Both multiple-primary.toml and multiple-secondary.toml match, so the IDE should show its target chooser.
type MultiTargetConfig struct {
	SharedToken string `toml:"shared_token"`
}
