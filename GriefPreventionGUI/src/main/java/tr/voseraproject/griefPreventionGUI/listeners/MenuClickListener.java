package tr.voseraproject.griefPreventionGUI.listeners;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tr.voseraproject.griefPreventionGUI.GriefPreventionGUI;
import tr.voseraproject.griefPreventionGUI.managers.ClaimExpirationManager;

public class MenuClickListener implements Listener {

    private final GriefPreventionGUI plugin;

    public MenuClickListener(GriefPreventionGUI plugin) {
        this.plugin = plugin;
    }

    private String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c).trim();
    }

    private String menuTitle(String menuKey) {
        String raw = plugin.getMenuConfig().getString(menuKey + ".title", "");
        return plain(plugin.parseRaw(raw));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String titlePlain = plain(event.getView().title());

        String mainTitle     = menuTitle("main-menu");
        String expiryTitle   = menuTitle("expiry-menu");
        String deleteTitle   = menuTitle("delete-menu");
        String settingsTitle = menuTitle("settings-menu");
        String membersTitle  = menuTitle("members-menu");
        String claimListTitle = menuTitle("claim-list-menu");
        String marketTitle   = menuTitle("market-menu");

        boolean isOurMenu = titlePlain.equals(mainTitle)
                || titlePlain.equals(expiryTitle)
                || titlePlain.equals(deleteTitle)
                || titlePlain.equals(settingsTitle)
                || titlePlain.equals(membersTitle)
                || titlePlain.equals(claimListTitle)
                || titlePlain.equals(marketTitle);

        if (!isOurMenu) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ClaimExpirationManager em = plugin.getExpirationManager();
        if (titlePlain.equals(claimListTitle)) {
            String matStr = plugin.getMenuConfig().getString("claim-list-menu.item.material", "MAP");
            Material expectedMat = parseMaterial(matStr);
            if (clicked.getType() != expectedMat) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            String nameStr = plain(meta.displayName());
            String[] parts = nameStr.split("#");
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                player.sendMessage(plugin.parseMsg("claim-id-not-found"));
                return;
            }

            try {
                long claimId = Long.parseLong(parts[1].trim());
                Claim targetClaim = GriefPrevention.instance.dataStore.getClaim(claimId);
                if (targetClaim == null) return;

                if (event.getClick().isRightClick()) {
                    teleportToClaim(player, targetClaim);
                } else if (event.getClick().isLeftClick()) {
                    if (targetClaim.getOwnerName().equals(player.getName())) {
                        plugin.getMenuManager().openMainMenu(player, targetClaim);
                    } else {
                        player.sendMessage(plugin.parseMsg("not-claim-owner"));
                    }
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.parseMsg("claim-id-invalid"));
            } catch (Exception e) {
                player.closeInventory();
            }
            return;
        }

        if (titlePlain.equals(marketTitle)) {
            String matStr = plugin.getMenuConfig().getString("market-menu.item.material", "GOLD_INGOT");
            Material expectedMat = parseMaterial(matStr);
            if (clicked.getType() != expectedMat) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            String nameStr = plain(meta.displayName());
            String[] parts = nameStr.split("#");
            if (parts.length < 2 || parts[1].trim().isEmpty()) return;

            try {
                long claimId = Long.parseLong(parts[1].trim());
                Claim targetClaim = GriefPrevention.instance.dataStore.getClaim(claimId);
                if (targetClaim == null) return;

                if (event.getClick().name().contains("MIDDLE")) {
                    if (!targetClaim.getOwnerName().equals(player.getName())) {
                        player.sendMessage(plugin.parseMsg("market-not-owner-remove"));
                        return;
                    }
                    em.setSaleInfo(claimId, false, 0, "");
                    player.sendMessage(plugin.parseMsg("market-removed"));
                    plugin.getMenuManager().openMarketMenu(player);
                    return;
                }

                if (event.getClick().isLeftClick()) {
                    teleportToClaim(player, targetClaim);
                    return;
                }

                if (event.getClick().isRightClick()) {
                    if (targetClaim.getOwnerName().equals(player.getName())) {
                        player.sendMessage(plugin.parseMsg("market-self-buy"));
                        return;
                    }
                    double price = em.getSalePrice(claimId);
                    if (plugin.getEconomy().getBalance(player) >= price) {
                        plugin.getEconomy().withdrawPlayer(player, price);
                        org.bukkit.OfflinePlayer seller = Bukkit.getOfflinePlayer(targetClaim.getOwnerID());
                        plugin.getEconomy().depositPlayer(seller, price);
                        try {
                            GriefPrevention.instance.dataStore.changeClaimOwner(targetClaim, player.getUniqueId());
                        } catch (Exception ex) {
                            targetClaim.ownerID = player.getUniqueId();
                            GriefPrevention.instance.dataStore.saveClaim(targetClaim);
                        }
                        em.setSaleInfo(claimId, false, 0, "");
                        player.closeInventory();
                        player.sendMessage(plugin.parseRaw(
                                plugin.getLang().getString("market-buy-success", "&aTebrikler!")
                                        .replace("%price%", String.valueOf(price))));
                    } else {
                        player.sendMessage(plugin.parseRaw(
                                plugin.getLang().getString("market-not-enough-money", "&cYetersiz bakiye!")
                                        .replace("%price%", String.valueOf(price))));
                    }
                }
            } catch (Exception e) {
                player.closeInventory();
            }
            return;
        }

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
        if (claim == null || !claim.getOwnerName().equals(player.getName())) {
            player.closeInventory();
            player.sendMessage(plugin.parseMsg("not-claim-owner"));
            return;
        }

        Material type = clicked.getType();

        if (titlePlain.equals(mainTitle)) {
            String clockMat   = plugin.getMenuConfig().getString("main-menu.items.expiry.material", "CLOCK");
            String spyMat     = plugin.getMenuConfig().getString("main-menu.items.settings.material", "SPYGLASS");
            String barrierMat = plugin.getMenuConfig().getString("main-menu.items.delete.material", "BARRIER");
            String headMat    = plugin.getMenuConfig().getString("main-menu.items.members.material", "PLAYER_HEAD");

            if (type == parseMaterial(clockMat))   plugin.getMenuManager().openExpirationMenu(player, claim);
            else if (type == parseMaterial(spyMat))     plugin.getMenuManager().openSettingsMenu(player, claim);
            else if (type == parseMaterial(barrierMat)) plugin.getMenuManager().openDeleteConfirmMenu(player, claim);
            else if (type == parseMaterial(headMat))    plugin.getMenuManager().openMembersMenu(player, claim);
        }

        else if (titlePlain.equals(expiryTitle)) {
            String addMat = plugin.getMenuConfig().getString("expiry-menu.items.add.material", "LIME_DYE");
            String remMat = plugin.getMenuConfig().getString("expiry-menu.items.remove.material", "RED_DYE");

            if (type == parseMaterial(addMat)) {
                double price = plugin.getConfig().getDouble("daily-price");
                if (plugin.getEconomy().getBalance(player) >= price) {
                    plugin.getEconomy().withdrawPlayer(player, price);
                    em.addRemainingTime(claim.getID(), 1440);
                    player.sendMessage(plugin.parseMsg("action-success"));
                    plugin.getMenuManager().openExpirationMenu(player, claim);
                } else {
                    player.sendMessage(plugin.parseRaw(
                            plugin.getLang().getString("not-enough-money", "&cYetersiz bakiye!")
                                    .replace("%amount%", price + "$")));
                }
            } else if (type == parseMaterial(remMat)) {
                if (em.getRemainingTime(claim.getID()) > 1440) {
                    em.addRemainingTime(claim.getID(), -1440);
                    player.sendMessage(plugin.parseMsg("action-success"));
                    plugin.getMenuManager().openExpirationMenu(player, claim);
                }
            }
        }

        else if (titlePlain.equals(settingsTitle)) {
            String backMat = plugin.getMenuConfig().getString("settings-menu.items.back.material", "ARROW");
            if (type == parseMaterial(backMat) && clicked.getItemMeta() != null) {
                plugin.getMenuManager().openMainMenu(player, claim);
                return;
            }

            ConfigurationSection settingItems = plugin.getMenuConfig().getConfigurationSection("settings-menu.items");
            if (settingItems != null) {
                for (String key : settingItems.getKeys(false)) {
                    ConfigurationSection si = settingItems.getConfigurationSection(key);
                    if (si == null) continue;
                    String matStr = si.getString("material", "");
                    if (type == parseMaterial(matStr)) {
                        em.toggleSetting(claim.getID(), key);
                        player.sendMessage(plugin.parseMsg("action-success"));
                        plugin.getMenuManager().openSettingsMenu(player, claim);
                        return;
                    }
                }
            }

            if (type == Material.ARROW) {
                plugin.getMenuManager().openMainMenu(player, claim);
            }
        }

        else if (titlePlain.equals(membersTitle)) {
            String backMat  = plugin.getMenuConfig().getString("members-menu.items.back.material", "ARROW");
            String addMat   = plugin.getMenuConfig().getString("members-menu.items.add.material", "ANVIL");
            String entryMat = plugin.getMenuConfig().getString("members-menu.items.member-entry.material", "PLAYER_HEAD");

            if (type == parseMaterial(backMat)) {
                plugin.getMenuManager().openMainMenu(player, claim);
                return;
            }
            if (type == parseMaterial(addMat)) {
                player.closeInventory();
                player.sendMessage(plugin.parseMsg("enter-member-name"));
                plugin.getChatInputPlayers().put(player.getUniqueId(), claim.getID());
                return;
            }
            if (type == parseMaterial(entryMat)) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String targetName = plain(meta.displayName());
                    if (targetName.startsWith("[")) targetName = targetName.substring(1);
                    if (targetName.endsWith("]")) targetName = targetName.substring(0, targetName.length() - 1);
                    claim.dropPermission(targetName.trim());
                    GriefPrevention.instance.dataStore.saveClaim(claim);
                    player.sendMessage(plugin.parseMsg("action-success"));
                    plugin.getMenuManager().openMembersMenu(player, claim);
                }
            }
        }

        else if (titlePlain.equals(deleteTitle)) {
            String confirmMat = plugin.getMenuConfig().getString("delete-menu.items.confirm.material", "TNT");
            if (type == parseMaterial(confirmMat)) {
                double deletePrice = plugin.getConfig().getDouble("delete-price");
                if (plugin.getEconomy().getBalance(player) >= deletePrice) {
                    plugin.getEconomy().withdrawPlayer(player, deletePrice);
                    GriefPrevention.instance.dataStore.deleteClaim(claim);
                    player.closeInventory();
                    player.sendMessage(plugin.parseMsg("claim-deleted"));
                } else {
                    player.sendMessage(plugin.parseRaw(
                            plugin.getLang().getString("not-enough-money", "&cYetersiz bakiye!")
                                    .replace("%amount%", deletePrice + "$")));
                }
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onPlayerChat(io.papermc.paper.event.player.AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getChatInputPlayers().containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        Long claimId = plugin.getChatInputPlayers().remove(player.getUniqueId());

        Bukkit.getScheduler().runTask(plugin, () -> {
            Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);
            if (claim == null || !claim.getOwnerName().equals(player.getName())) return;

            String targetName = plain(event.message());
            org.bukkit.OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
            if (targetPlayer.getName() == null) {
                player.sendMessage(plugin.parseMsg("player-not-found"));
                plugin.getMenuManager().openMembersMenu(player, claim);
                return;
            }

            claim.setPermission(targetPlayer.getUniqueId().toString(), me.ryanhamshire.GriefPrevention.ClaimPermission.Build);
            GriefPrevention.instance.dataStore.saveClaim(claim);
            player.sendMessage(plugin.parseMsg("action-success"));
            plugin.getMenuManager().openMembersMenu(player, claim);
        });
    }

    private void teleportToClaim(Player player, Claim claim) {
        org.bukkit.Location loc = claim.getLesserBoundaryCorner();
        int cx = loc.getBlockX() + (claim.getWidth() / 2);
        int cz = loc.getBlockZ() + (claim.getHeight() / 2);
        org.bukkit.Location tp = new org.bukkit.Location(
                loc.getWorld(), cx + 0.5,
                loc.getWorld().getHighestBlockYAt(cx, cz) + 1.5,
                cz + 0.5);
        player.closeInventory();
        player.teleport(tp);
        player.sendMessage(plugin.parseMsg("teleported-to-claim"));
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }
}