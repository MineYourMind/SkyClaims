package net.mohron.skyclaims.config.type;

import net.mohron.skyclaims.config.ConfigManager;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class GlobalConfig {
	@Setting(value = "Config-Version")
	public Integer version;
	@Setting(value = "Database")
	public DatabaseConfig database;
	@Setting(value = "World")
	public WorldConfig world;
	@Setting(value = "Misc")
	public MiscConfig misc;

	public GlobalConfig() {
		version = ConfigManager.CONFIG_VERSION;
		world = new WorldConfig();
		database = new DatabaseConfig();
		misc = new MiscConfig();
	}
}