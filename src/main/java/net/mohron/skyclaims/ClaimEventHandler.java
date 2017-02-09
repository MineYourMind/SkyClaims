package net.mohron.skyclaims;


import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.event.CreateClaimEvent;
import me.ryanhamshire.griefprevention.api.event.DeleteClaimEvent;
import me.ryanhamshire.griefprevention.api.event.ResizeClaimEvent;
import net.mohron.skyclaims.lib.Permissions;
import net.mohron.skyclaims.util.ConfigUtil;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

public class ClaimEventHandler {

    private static final World WORLD = ConfigUtil.getWorld();

    @Listener
    public void onClaimCreate(CreateClaimEvent event, @Root Player player) {
        Claim claim = event.getClaim();
        if (claim.getWorld().equals(WORLD) && claim.isBasicClaim()) {
            event.setMessage(Text.of(TextColors.RED, "You are in an island world. Custom claim creation is disabled here!"));
            event.setCancelled(true);
        }
    }

    @Listener
    public void onClaimDelete(DeleteClaimEvent event, @Root Player player) {
        for (Claim claim : event.getClaims()) {
            if (claim.getWorld().equals(WORLD) && claim.isBasicClaim() &&
                    !(player.hasPermission(Permissions.COMMAND_DELETE) && player.hasPermission(Permissions.COMMAND_ADMIN))) {
                event.setMessage(Text.of(TextColors.RED, "You are in an island world. Claim deletion is disabled here!"));
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onClaimResize(ResizeClaimEvent event, @Root Player player) {
        Claim claim = event.getClaim();
        if (claim.getWorld().equals(WORLD) && claim.isBasicClaim()) {
            event.setMessage(Text.of(TextColors.RED, "You are in an island world. Claim resizing is disabled here!"));
            event.setCancelled(true);
        }
    }
}
