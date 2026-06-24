package tr.voseraproject.griefPreventionGUI.managers;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tr.voseraproject.griefPreventionGUI.GriefPreventionGUI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClaimMenuManager {

    private final GriefPreventionGUI plugin;

    public ClaimMenuManager(GriefPreventionGUI plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player, Claim claim) {
        ConfigurationSection sec = plugin.getMenuConfig().getConfigurationSection("main-menu");
        if (sec == null) return;

        Component title = plugin.parseRaw(sec.getString("title", "&0Claim Yönetim Paneli"));
        int size = sec.getInt("size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        fillInventory(inv, sec);

        long remainingMinutes = plugin.getExpirationManager().getRemainingTime(claim.getID());
        String remaining = formatRemaining(remainingMinutes);

        int centerX = claim.getLesserBoundaryCorner().getBlockX() + (claim.getWidth() / 2);
        int centerZ = claim.getLesserBoundaryCorner().getBlockZ() + (claim.getHeight() / 2);

        ConfigurationSection items = sec.getConfigurationSection("items");
        if (items == null) { player.openInventory(inv); return; }

        for (String key : items.getKeys(false)) {
            ConfigurationSection item = items.getConfigurationSection(key);
            if (item == null) continue;

            String name = item.getString("name", "")
                    .replace("%width%", String.valueOf(claim.getWidth()))
                    .replace("%height%", String.valueOf(claim.getHeight()))
                    .replace("%cx%", String.valueOf(centerX))
                    .replace("%cz%", String.valueOf(centerZ))
                    .replace("%remaining%", remaining);

            List<String> rawLore = item.getStringList("lore");
            List<String> lore = new ArrayList<>();
            for (String l : rawLore) {
                lore.add(l.replace("%width%", String.valueOf(claim.getWidth()))
                        .replace("%height%", String.valueOf(claim.getHeight()))
                        .replace("%cx%", String.valueOf(centerX))
                        .replace("%cz%", String.valueOf(centerZ))
                        .replace("%remaining%", remaining));
            }

            int slot = item.getInt("slot", 0);
            Material mat = parseMaterial(item.getString("material", "STONE"));
            inv.setItem(slot, buildItem(mat, name, lore));
        }

        player.openInventory(inv);
    }

    public void openExpirationMenu(Player player, Claim claim) {
        ConfigurationSection sec = plugin.getMenuConfig().getConfigurationSection("expiry-menu");
        if (sec == null) return;

        Component title = plugin.parseRaw(sec.getString("title", "&0Süre Uzatma"));
        int size = sec.getInt("size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        double dailyPrice = plugin.getConfig().getDouble("daily-price", 100.0);
        String priceStr = String.valueOf(dailyPrice);

        ConfigurationSection items = sec.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection item = items.getConfigurationSection(key);
                if (item == null) continue;

                String name = item.getString("name", "").replace("%price%", priceStr);
                List<String> lore = replacePlaceholders(item.getStringList("lore"), "%price%", priceStr);

                int slot = item.getInt("slot", 0);
                Material mat = parseMaterial(item.getString("material", "STONE"));
                inv.setItem(slot, buildItem(mat, name, lore));
            }
        }

        player.openInventory(inv);
    }

    public void openDeleteConfirmMenu(Player player, Claim claim) {
        ConfigurationSection sec = plugin.getMenuConfig().getConfigurationSection("delete-menu");
        if (sec == null) return;

        Component title = plugin.parseRaw(sec.getString("title", "&0Silme Onayı"));
        int size = sec.getInt("size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        double deletePrice = plugin.getConfig().getDouble("delete-price", 500.0);
        String priceStr = String.valueOf(deletePrice);

        ConfigurationSection items = sec.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection item = items.getConfigurationSection(key);
                if (item == null) continue;

                String name = item.getString("name", "").replace("%price%", priceStr);
                List<String> lore = replacePlaceholders(item.getStringList("lore"), "%price%", priceStr);

                int slot = item.getInt("slot", 0);
                Material mat = parseMaterial(item.getString("material", "STONE"));
                inv.setItem(slot, buildItem(mat, name, lore));
            }
        }

        player.openInventory(inv);
    }

    public void openMembersMenu(Player player, Claim claim) {
        ConfigurationSection sec = plugin.getMenuConfig().getConfigurationSection("members-menu");
        if (sec == null) return;

        Component title = plugin.parseRaw(sec.getString("title", "&0Üye Yönetimi"));
        int size = sec.getInt("size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        fillInventory(inv, sec);

        ConfigurationSection items = sec.getConfigurationSection("items");
        if (items != null) {
            for (String key : List.of("add", "back")) {
                ConfigurationSection item = items.getConfigurationSection(key);
                if (item == null) continue;
                int slot = item.getInt("slot", 0);
                Material mat = parseMaterial(item.getString("material", "STONE"));
                inv.setItem(slot, buildItem(mat, item.getString("name", ""), item.getStringList("lore")));
            }

            ConfigurationSection entry = items.getConfigurationSection("member-entry");
            if (entry != null) {
                ArrayList<String> builders = new ArrayList<>();
                claim.getPermissions(builders, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

                String entryName = entry.getString("name", "&a%player%");
                List<String> entryLore = entry.getStringList("lore");

                int slot = 10;
                for (String builder : builders) {
                    if (slot > 16) break;
                    String name = entryName.replace("%player%", builder);
                    inv.setItem(slot, buildItem(Material.PLAYER_HEAD, name, entryLore));
                    slot++;
                }
            }
        }

        player.openInventory(inv);
    }

    public void openSettingsMenu(Player player, Claim claim) {
        ConfigurationSection sec = plugin.getMenuConfig().getConfigurationSection("settings-menu");
        if (sec == null) return;

        Component title = plugin.parseRaw(sec.getString("title", "&0Claim Ayarları"));
        int size = sec.getInt("size", 45);
        Inventory inv = Bukkit.createInventory(null, size, title);

        fillInventory(inv, sec);

        ClaimExpirationManager em = plugin.getExpirationManager();
        Long cid = claim.getID();

        String statusOn  = plugin.getLang().getString("status-on",  "&#00ff00Açık");
        String statusOff = plugin.getLang().getString("status-off", "&#ff0000Kapalı");
        String statusPfx = plugin.getLang().getString("status-prefix", "&7Durum: ");
        String changeHint = plugin.getLang().getString("settings-change-hint", "&8Değiştirmek için tıklayın.");

        ConfigurationSection items = sec.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection item = items.getConfigurationSection(key);
                if (item == null) continue;

                int slot = item.getInt("slot", 0);
                Material mat = parseMaterial(item.getString("material", "STONE"));
                String name = item.getString("name", key);
                String desc = item.getString("desc", "");
                boolean val = em.getSetting(cid, key);
                String status = statusPfx + (val ? statusOn : statusOff);

                List<String> lore = List.of("&7" + desc, "", status, changeHint);
                inv.setItem(slot, buildItem(mat, name, lore));
            }
        }

        int backSlot = sec.getInt("back-slot", 40);
        inv.setItem(backSlot, buildItem(Material.ARROW,
                plugin.getLang().getString("back-button", "&c&lGeri Dön"), List.of()));

        player.openInventory(inv);
    }

    public void openPlayerClaimsMenu(Player player) {
        ConfigurationSection sec = plugin.getMenuConfig().getConfigurationSection("claim-list-menu");
        if (sec == null) return;

        Component title = plugin.parseRaw(sec.getString("title", "&8Tüm Claim Arazileriniz"));
        int size = sec.getInt("size", 54);
        Inventory gui = Bukkit.createInventory(null, size, title);

        Collection<Claim> allClaims = GriefPrevention.instance.dataStore.getClaims();
        List<Claim> playerClaims = new ArrayList<>();
        if (allClaims != null) {
            for (Claim c : allClaims) {
                if (c.parent == null && player.getName().equals(c.getOwnerName())) {
                    playerClaims.add(c);
                }
            }
        }

        if (playerClaims.isEmpty()) {
            player.sendMessage(plugin.parseMsg("no-claims"));
            return;
        }

        ClaimExpirationManager em = plugin.getExpirationManager();
        String dayWord  = plugin.getLang().getString("words.day",  "Gün");
        String hourWord = plugin.getLang().getString("words.hour", "Saat");

        ConfigurationSection itemSec = sec.getConfigurationSection("item");
        String nameFormat = itemSec != null ? itemSec.getString("name", "&6&lClaim Arazi #%id%") : "&6&lClaim Arazi #%id%";
        List<String> loreFormat = itemSec != null ? itemSec.getStringList("lore") : List.of();
        String matStr = itemSec != null ? itemSec.getString("material", "MAP") : "MAP";
        Material mat = parseMaterial(matStr);

        int slot = 0;
        for (Claim c : playerClaims) {
            if (slot >= size) break;

            int cx = c.getLesserBoundaryCorner().getBlockX() + (c.getWidth() / 2);
            int cz = c.getLesserBoundaryCorner().getBlockZ() + (c.getHeight() / 2);
            long totalMin = em.getRemainingTime(c.getID());
            long days  = totalMin / 1440;
            long hours = (totalMin % 1440) / 60;

            String name = nameFormat
                    .replace("%id%",      String.valueOf(c.getID()))
                    .replace("%world%",   c.getLesserBoundaryCorner().getWorld().getName())
                    .replace("%cx%",      String.valueOf(cx))
                    .replace("%cz%",      String.valueOf(cz))
                    .replace("%width%",   String.valueOf(c.getWidth()))
                    .replace("%height%",  String.valueOf(c.getHeight()))
                    .replace("%area%",    String.valueOf(c.getArea()))
                    .replace("%days%",    String.valueOf(days))
                    .replace("%hours%",   String.valueOf(hours))
                    .replace("%day_word%",  dayWord)
                    .replace("%hour_word%", hourWord);

            List<String> lore = new ArrayList<>();
            for (String l : loreFormat) {
                lore.add(l.replace("%id%",      String.valueOf(c.getID()))
                        .replace("%world%",   c.getLesserBoundaryCorner().getWorld().getName())
                        .replace("%cx%",      String.valueOf(cx))
                        .replace("%cz%",      String.valueOf(cz))
                        .replace("%width%",   String.valueOf(c.getWidth()))
                        .replace("%height%",  String.valueOf(c.getHeight()))
                        .replace("%area%",    String.valueOf(c.getArea()))
                        .replace("%days%",    String.valueOf(days))
                        .replace("%hours%",   String.valueOf(hours))
                        .replace("%day_word%",  dayWord)
                        .replace("%hour_word%", hourWord));
            }

            gui.setItem(slot, buildItem(mat, name, lore));
            slot++;
        }

        player.openInventory(gui);
    }

    public void openMarketMenu(Player player) {
        ConfigurationSection sec = plugin.getMenuConfig().getConfigurationSection("market-menu");
        if (sec == null) return;

        Component title = plugin.parseRaw(sec.getString("title", "&0Satılık Claim Pazarı"));
        int size = sec.getInt("size", 54);
        Inventory gui = Bukkit.createInventory(null, size, title);

        ClaimExpirationManager em = plugin.getExpirationManager();
        String dayWord  = plugin.getLang().getString("words.day",  "Gün");
        String hourWord = plugin.getLang().getString("words.hour", "Saat");

        ConfigurationSection itemSec = sec.getConfigurationSection("item");
        String nameFormat = itemSec != null ? itemSec.getString("name", "&6&lSatılık Arazi #%id%") : "&6&lSatılık Arazi #%id%";
        List<String> loreFormat = itemSec != null ? itemSec.getStringList("lore") : List.of();
        String matStr = itemSec != null ? itemSec.getString("material", "GOLD_INGOT") : "GOLD_INGOT";
        Material mat = parseMaterial(matStr);

        Collection<Claim> allClaims = GriefPrevention.instance.dataStore.getClaims();
        int slot = 0;
        if (allClaims != null) {
            for (Claim c : allClaims) {
                if (c == null || c.parent != null) continue;
                if (slot >= size) break;
                long cid = c.getID();
                if (!em.isClaimForSale(cid)) continue;

                int cx = c.getLesserBoundaryCorner().getBlockX() + (c.getWidth() / 2);
                int cz = c.getLesserBoundaryCorner().getBlockZ() + (c.getHeight() / 2);
                long totalMin = em.getRemainingTime(cid);
                long days  = totalMin / 1440;
                long hours = (totalMin % 1440) / 60;
                double price = em.getSalePrice(cid);
                String seller = em.getSellerName(cid);

                String name = applyMarketPlaceholders(nameFormat, c, cid, cx, cz, days, hours, price, seller, dayWord, hourWord);
                List<String> lore = new ArrayList<>();
                for (String l : loreFormat) {
                    lore.add(applyMarketPlaceholders(l, c, cid, cx, cz, days, hours, price, seller, dayWord, hourWord));
                }

                gui.setItem(slot, buildItem(mat, name, lore));
                slot++;
            }
        }

        player.openInventory(gui);
    }

    private String applyMarketPlaceholders(String s, Claim c, long cid, int cx, int cz,
                                           long days, long hours, double price, String seller,
                                           String dayWord, String hourWord) {
        return s.replace("%id%",       String.valueOf(cid))
                .replace("%world%",    c.getLesserBoundaryCorner().getWorld().getName())
                .replace("%cx%",       String.valueOf(cx))
                .replace("%cz%",       String.valueOf(cz))
                .replace("%width%",    String.valueOf(c.getWidth()))
                .replace("%height%",   String.valueOf(c.getHeight()))
                .replace("%area%",     String.valueOf(c.getArea()))
                .replace("%days%",     String.valueOf(days))
                .replace("%hours%",    String.valueOf(hours))
                .replace("%day_word%",  dayWord)
                .replace("%hour_word%", hourWord)
                .replace("%price%",    String.valueOf(price))
                .replace("%seller%",   seller);
    }

    private void fillInventory(Inventory inv, ConfigurationSection sec) {
        String fillStr = sec.getString("fill-item");
        if (fillStr == null) return;
        Material fill = parseMaterial(fillStr);
        ItemStack pane = buildItem(fill, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    private String formatRemaining(long totalMinutes) {
        String dayWord  = plugin.getLang().getString("words.day",    "Gün");
        String hourWord = plugin.getLang().getString("words.hour",   "Saat");
        String minWord  = plugin.getLang().getString("words.minute", "Dakika");
        long days  = totalMinutes / 1440;
        long hours = (totalMinutes % 1440) / 60;
        long mins  = totalMinutes % 60;
        return days + " " + dayWord + ", " + hours + " " + hourWord + ", " + mins + " " + minWord;
    }

    private List<String> replacePlaceholders(List<String> list, String key, String value) {
        List<String> result = new ArrayList<>();
        for (String s : list) result.add(s.replace(key, value));
        return result;
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Geçersiz material: " + name + " — STONE kullanılıyor.");
            return Material.STONE;
        }
    }


    private ItemStack buildItem(Material material, String rawName, List<String> rawLore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.parseRaw(rawName));
            List<Component> lore = new ArrayList<>();
            for (String l : rawLore) lore.add(plugin.parseRaw(l));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}