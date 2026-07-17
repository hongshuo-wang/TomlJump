package config

// CASE 3 - Command/Ctrl-click NoMatchConfig or OrphanValue: no reliable target, so TomlJump stays quiet.
// no-match.toml uses a different container name, which deliberately prevents field matching.
type NoMatchConfig struct {
	OrphanValue string
}
