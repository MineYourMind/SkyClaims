package net.mohron.skyclaims.util;

import com.flowpowered.math.vector.Vector3i;
import me.ryanhamshire.griefprevention.api.claim.*;
import net.mohron.skyclaims.IslandStore;
import net.mohron.skyclaims.Region;
import net.mohron.skyclaims.SkyClaims;
import net.mohron.skyclaims.island.Island;
import net.mohron.skyclaims.island.RegenerateRegionTask;
import net.mohron.skyclaims.island.layout.ILayout;
import net.mohron.skyclaims.island.layout.SpiralLayout;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.UUID;

public class IslandUtil {
	private static final SkyClaims PLUGIN = SkyClaims.getInstance();
	private static final World WORLD = ConfigUtil.getWorld();
	private static final ClaimManager CLAIM_MANAGER = PLUGIN.getGriefPrevention().getClaimManager(WORLD);
	private static ILayout layout = new SpiralLayout();


	public static Optional<Island> createIsland(Player owner, String schematic) {
		Region region = layout.nextRegion();

		if (ConfigUtil.getDefaultBiome().isPresent())
			WorldUtil.setRegionBiome(region, ConfigUtil.getDefaultBiome().get());

		ClaimResult claimResult = forceCreateProtection(owner.getName(), owner.getUniqueId(), region);
		//CreateClaimResult claimResult = createProtection(owner, region);
		if (!claimResult.successful()) {
			//noinspection OptionalGetWithoutIsPresent
			PLUGIN.getLogger().error("Failed to create claim. Found overlapping claim: " + claimResult.getClaim().get().getUniqueId());
			return Optional.empty();
		}

		ConfigUtil.getCreateCommands().ifPresent(commands -> {
			for (String command : commands) {
				PLUGIN.getGame().getCommandManager().process(PLUGIN.getGame().getServer().getConsole(), command.replace("@p", owner.getName()));
			}
		});

		//noinspection OptionalGetWithoutIsPresent
		return Optional.of(new Island(owner, claimResult.getClaim().get(), region, schematic));
	}

	public static ClaimResult forceCreateProtection(String ownerName, UUID ownerUUID, Region region) {
		ClaimResult claimResult = null;
		while (claimResult == null || !claimResult.successful()) {
			claimResult = createProtection(ownerName, ownerUUID, region);
			if (claimResult.getResultType().equals(ClaimResultType.OVERLAPPING_CLAIM)) {
				PLUGIN.getLogger().error("Failed to create claim. Found overlapping claims. Removing them:");
				for (Claim claim : claimResult.getClaims()) {
					PLUGIN.getLogger().error(String.format(
							"Removing overlapping claim %s with region %s,%s: (%s,%s),(%s,%s)",
							claim.getUniqueId(),
							claim.getData().getLesserBoundaryCornerPos().getX() >> 5 >> 4, claim.getData().getLesserBoundaryCornerPos().getZ() >> 5 >> 4,
							claim.getData().getLesserBoundaryCornerPos().getX(), claim.getData().getGreaterBoundaryCornerPos().getX(),
							claim.getData().getLesserBoundaryCornerPos().getZ(), claim.getData().getGreaterBoundaryCornerPos().getZ()
					));
                    CLAIM_MANAGER.deleteClaim(claim, Cause.source(PLUGIN).build());
                }
			} else if (claimResult.getClaim().isPresent()) {
				CLAIM_MANAGER.addClaim(claimResult.getClaim().get(), Cause.source(PLUGIN).build());
				PLUGIN.getLogger().info("Successfully created new claim " + claimResult.getClaim().get().getUniqueId() + ".");
			}
		}
		return claimResult;
	}

	public static boolean hasIsland(UUID owner) {
		return IslandStore.getIslands().containsKey(owner);
	}

	public static Optional<Island> getIsland(UUID owner) {
		return (hasIsland(owner)) ? Optional.of(IslandStore.getIslands().get(owner)) : Optional.empty();
	}

	public static Optional<Island> getIslandByLocation(Location<World> location) {
		return getIslandByClaim(CLAIM_MANAGER.getClaimAt(location, true));
	}

	public static Optional<Island> getIslandByClaim(Claim claim) {
		Island island;
		if (claim.getOwnerUniqueId() != null && getIsland(claim.getOwnerUniqueId()).isPresent()) {
			island = getIsland(claim.getOwnerUniqueId()).get();
			return (island.getClaimId().equals(claim.getUniqueId())) ? Optional.of(island) : Optional.empty();
		} else
			return Optional.empty();
	}

	public static void resetIsland(User owner, String schematic) {
		// Send online players to spawn!
		owner.getPlayer().ifPresent(
				player -> CommandUtil.createForceTeleportConsumer(player, WorldUtil.getDefaultWorld().getSpawnLocation())
		);
		// Run reset commands
		ConfigUtil.getCreateCommands().ifPresent(commands -> {
			for (String command : commands) {
				PLUGIN.getGame().getCommandManager().process(PLUGIN.getGame().getServer().getConsole(), command.replace("@p", owner.getName()));
			}
		});
		// Destroy everything they ever loved!
		getIsland(owner.getUniqueId()).ifPresent(island -> {
			RegenerateRegionTask regenerateRegionTask = new RegenerateRegionTask(owner, island, schematic);
			PLUGIN.getGame().getScheduler().createTaskBuilder().execute(regenerateRegionTask).submit(PLUGIN);
		});
	}

	private static ClaimResult createProtection(String ownerName, UUID ownerUUID, Region region) {
		PLUGIN.getLogger().info(String.format(
				"Creating claim for %s (%s) with region %s,%s: (%s,%s),(%s,%s)",
				ownerName,
				ownerUUID,
				region.getX(), region.getZ(),
				region.getLesserBoundary().getX(), region.getLesserBoundary().getZ(),
				region.getGreaterBoundary().getX(), region.getGreaterBoundary().getZ()
		));
		return Claim.builder().world(WORLD).bounds(new Vector3i(region.getLesserBoundary().getX(), 0, region.getLesserBoundary().getZ()),
				new Vector3i(region.getGreaterBoundary().getX(), 255, region.getGreaterBoundary().getZ()))
				.type(ClaimType.BASIC)
				.requiresClaimBlocks(false)
				.cause(Cause.source(PLUGIN).build())
				.cuboid(false)
				.owner(ownerUUID)
                .sizeRestrictions(false)
				.build();
	}
}