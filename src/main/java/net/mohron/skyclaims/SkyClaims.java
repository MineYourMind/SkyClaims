package net.mohron.skyclaims;

import com.google.inject.Inject;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.GriefPreventionApi;
import net.mohron.skyclaims.command.*;
import net.mohron.skyclaims.config.ConfigManager;
import net.mohron.skyclaims.config.type.GlobalConfig;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionService;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.mohron.skyclaims.PluginInfo.*;

@Plugin(id = ID,
		name = NAME,
		version = VERSION,
		description = DESCRIPTION,
		authors = AUTHORS,
		dependencies = {
				@Dependency(id = "griefprevention", optional = true)
		})
public class SkyClaims {
	private static SkyClaims instance;
	private static GriefPreventionApi griefPrevention;
	private static PermissionService permissionService;

	@Inject
	private PluginContainer pluginContainer;

	@Inject
	private Logger logger;

	@Inject
	private Game game;

	//@Inject
	//private Metrics metrics;

	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configDir;
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path schematicDir = Paths.get(configDir + File.separator + "schematics");
	@Inject
	@DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	private ConfigManager pluginConfigManager;
	private GlobalConfig defaultConfig;

	private Database database;

	@Listener
	public void onPostInitialization(GamePostInitializationEvent event) {
		getLogger().info(String.format("%s %s is initializing...", NAME, VERSION));

		instance = this;

		//TODO Setup the worldName with a sponge:void worldName gen modifier if not already created
	}

	@Listener(order = Order.LATE)
	public void onAboutToStart(GameAboutToStartServerEvent event) {
		SkyClaims.permissionService = Sponge.getServiceManager().provide(PermissionService.class).get();
		if (Sponge.getServiceManager().getRegistration(PermissionService.class).get().getPlugin().getId().equalsIgnoreCase("sponge")) {
			getLogger().error("Unable to initialize plugin. SkyClaims requires a permissions plugin.");
			return;
		}

		// GriefPrevention integration
		try {
			Class.forName("me.ryanhamshire.griefprevention.GriefPrevention");
			SkyClaims.griefPrevention = GriefPrevention.getApi();
			getLogger().info("GriefPrevention Integration Successful!");
		} catch (ClassNotFoundException e) {
			getLogger().info("GriefPrevention Integration Failed!");
		}

		defaultConfig = new GlobalConfig();
		pluginConfigManager = new ConfigManager(configManager);
		pluginConfigManager.save();

		registerCommands();
	}

	@Listener
	public void onServerStarted(GameStartedServerEvent event) {
		database = new Database(getConfigDir() + File.separator + "skyclaims.db");
			IslandStore.overwriteIslands(database.loadData());
		getLogger().info("ISLAND LENGTH: " + IslandStore.getIslands().size());

		getLogger().info("Registering Event Listeners");
		Sponge.getGame().getEventManager().registerListeners(this, new SchematicHandler());
		Sponge.getGame().getEventManager().registerListeners(this, new ClaimEventHandler());
		getLogger().info("Initialization complete.");
	}

	@Listener
	public void onGameStopping(GameStoppingServerEvent event) {
		getLogger().info(String.format("%S %S is stopping...", NAME, VERSION));
		getLogger().info("Shutdown actions complete.");
	}

	private void registerCommands() {
		CommandAdmin.register();
		CommandCreate.register();
		CommandCreateSchematic.register();
		CommandDelete.register();
		CommandHelp.register();
		CommandInfo.register();
		CommandIsland.register();
		CommandList.register();
		CommandReload.register();
		CommandReset.register();
		CommandSetBiome.register();
		CommandSetSpawn.register();
		CommandSetup.register();
		CommandSpawn.register();
	}

	public static SkyClaims getInstance() {
		return instance;
	}

	public GriefPreventionApi getGriefPrevention() {
		return griefPrevention;
	}

	public PermissionService getPermissionService() {
		return permissionService;
	}

	public PluginContainer getPluginContainer() {
		return pluginContainer;
	}

	public Logger getLogger() {
		return logger;
	}

	public Game getGame() {
		return game;
	}

	public GlobalConfig getConfig() {
		return this.defaultConfig;
	}

	public ConfigManager getConfigManager() {
		return this.pluginConfigManager;
	}

	public Path getConfigDir() {
		return configDir;
	}

	public Path getSchematicDir() {
		return schematicDir;
	}

	public Database getDatabase() {
		return database;
	}
}