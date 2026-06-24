package tr.voseraproject.griefPreventionGUI.listeners;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import tr.voseraproject.griefPreventionGUI.GriefPreventionGUI;
import tr.voseraproject.griefPreventionGUI.managers.ClaimExpirationManager;

public class ClaimSettingsListener implements Listener {

    private final GriefPreventionGUI plugin;
    public ClaimSettingsListener(GriefPreventionGUI plugin) { this.plugin = plugin; }

    private Claim getClaimAt(org.bukkit.Location loc) {
        return GriefPrevention.instance.dataStore.getClaimAt(loc, false, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Claim claim = getClaimAt(event.getLocation());
        if (claim == null) return;
        ClaimExpirationManager em = plugin.getExpirationManager();

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            if (!em.getSetting(claim.getID(), "spawner-spawn")) event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Monster) {
            if (!em.getSetting(claim.getID(), "monster-spawn")) event.setCancelled(true);
        } else {
            if (!em.getSetting(claim.getID(), "animal-spawn")) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) return;
        Claim claim = getClaimAt(victim.getLocation());
        if (claim == null) return;

        if (!plugin.getExpirationManager().getSetting(claim.getID(), "pvp")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Claim claim = getClaimAt(event.getBlock().getLocation());
        if (claim == null) return;

        if (!claim.getOwnerName().equals(player.getName())) {
            if (event.getBlock().getType() == Material.SPAWNER) {
                if (!plugin.getExpirationManager().getSetting(claim.getID(), "spawner-break")) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (!plugin.getExpirationManager().getSetting(claim.getID(), "block-break")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Claim claim = getClaimAt(event.getBlock().getLocation());
        if (claim == null) return;

        if (!claim.getOwnerName().equals(player.getName())) {
            if (!plugin.getExpirationManager().getSetting(claim.getID(), "block-place")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) return;

        Claim claim = getClaimAt(event.getClickedBlock().getLocation());
        if (claim == null || claim.getOwnerName().equals(player.getName())) return;

        ClaimExpirationManager em = plugin.getExpirationManager();
        Material mat = event.getClickedBlock().getType();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL) {
            String name = mat.name();

            if (name.contains("CHEST") || name.contains("SHULKER_BOX") || name.equals("BARREL") || name.equals("HOPPER") || name.equals("DISPENSER") || name.equals("DROPPER")) {
                if (!em.getSetting(claim.getID(), "container-open")) event.setCancelled(true);
                return;
            }

            if (name.contains("DOOR") || name.contains("BUTTON") || name.equals("LEVER") || name.contains("FENCE_GATE") || name.contains("PRESSURE_PLATE")) {
                if (!em.getSetting(claim.getID(), "interact")) event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Claim claim = getClaimAt(event.getRightClicked().getLocation());
        if (claim == null || claim.getOwnerName().equals(player.getName())) return;

        ClaimExpirationManager em = plugin.getExpirationManager();

        if (event.getRightClicked() instanceof Villager) {
            if (!em.getSetting(claim.getID(), "villager-trade")) event.setCancelled(true);
        }
        if (event.getRightClicked() instanceof ArmorStand || event.getRightClicked() instanceof ItemFrame) {
            if (!em.getSetting(claim.getID(), "armor-stand")) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDecorationDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getEntity() instanceof ArmorStand || event.getEntity() instanceof ItemFrame || event.getEntity() instanceof Painting) {
            Claim claim = getClaimAt(event.getEntity().getLocation());
            if (claim == null || claim.getOwnerName().equals(player.getName())) return;

            if (!plugin.getExpirationManager().getSetting(claim.getID(), "armor-stand")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Claim claim = getClaimAt(event.getItem().getLocation());
        if (claim == null || claim.getOwnerName().equals(player.getName())) return;

        if (!plugin.getExpirationManager().getSetting(claim.getID(), "inventory-pickup")) {
            event.setCancelled(true);
        }
    }
}