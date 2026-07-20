package config

// CASE 5 - Host maps to the leaf of app.cache.host.
type CacheConfig struct {
	Host string
}

// CASE 6 - DatabaseConfig and Host map to [app.database] and its host key.
type DatabaseConfig struct {
	Host string
}

// CASE 7 - Normalization maps quoted-service and base.url to these declarations.
type QuotedServiceConfig struct {
	BaseURL string
}

// CASE 8 - ProductsConfig and Name map to [[products]] and its name key.
type ProductsConfig struct {
	Name string
}
