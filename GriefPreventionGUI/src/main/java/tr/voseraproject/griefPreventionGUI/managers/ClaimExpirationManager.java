package tr.voseraproject.griefPreventionGUI.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import tr.voseraproject.griefPreventionGUI.GriefPreventionGUI;
import tr.voseraproject.griefPreventionGUI.utils.DiscordBotMessenger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ClaimExpirationManager {

    private final GriefPreventionGUI plugin;
    private final File dataFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<Long, ClaimData> storage = new HashMap<>();

    public static class ClaimData {
        public long remainingMinutes = 4320L;
        public Map<String, Boolean> settings = new HashMap<>();
        public boolean isForSale  = false;
        public double  salePrice  = 0.0;
        public String  sellerName = "";

        public ClaimData() {
            settings.put("animal-spawn",    true);
            settings.put("monster-spawn",   true);
            settings.put("spawner-spawn",   true);
            settings.put("pvp",             false);
            settings.put("block-break",     false);
            settings.put("block-place",     false);
            settings.put("spawner-break",   false);
            settings.put("container-open",  false);
            settings.put("interact",        false);
            settings.put("villager-trade",  false);
            settings.put("armor-stand",     false);
            settings.put("inventory-pickup",false);
        }
    }

    public ClaimExpirationManager(GriefPreventionGUI plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "claims_data.json");
        startExpiryTask();
    }

    public long getRemainingTime(Long claimId) {
        return storage.computeIfAbsent(claimId, k -> new ClaimData()).remainingMinutes;
    }

    public void addRemainingTime(Long claimId, long minutes) {
        storage.computeIfAbsent(claimId, k -> new ClaimData()).remainingMinutes += minutes;
        saveData();
    }


    public boolean getSetting(Long claimId, String setting) {
        return storage.computeIfAbsent(claimId, k -> new ClaimData())
                .settings.getOrDefault(setting, true);
    }

    public void toggleSetting(Long claimId, String setting) {
        ClaimData data = storage.computeIfAbsent(claimId, k -> new ClaimData());
        data.settings.put(setting, !data.settings.getOrDefault(setting, true));
        saveData();
    }

    private void startExpiryTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (GriefPrevention.instance == null || GriefPrevention.instance.dataStore == null) return;

            java.util.Collection<Claim> allClaims = GriefPrevention.instance.dataStore.getClaims();
            if (allClaims == null) return;

            for (Claim claim : allClaims) {
                if (claim == null || claim.parent != null) continue;

                long claimId = claim.getID();
                ClaimData data = storage.computeIfAbsent(claimId, k -> new ClaimData());
                data.remainingMinutes--;

                if (data.remainingMinutes <= 0) {
                    sendExpiredDiscordMessage(claim);
                    GriefPrevention.instance.dataStore.deleteClaim(claim);
                    storage.remove(claimId);
                    plugin.getLogger().info("Claim ID " + claimId + " süresi dolduğu için silindi.");
                }
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveData);
        }, 1200L, 1200L);
    }

    private void sendExpiredDiscordMessage(Claim claim) {
        String ownerName = claim.getOwnerName();
        if (ownerName == null || ownerName.isEmpty())
            ownerName = plugin.getLang().getString("discord.unknown-owner", "Bilinmeyen Sahip");

        org.bukkit.Location loc = claim.getLesserBoundaryCorner();
        int cx = loc.getBlockX() + (claim.getWidth() / 2);
        int cz = loc.getBlockZ() + (claim.getHeight() / 2);

        String coords   = plugin.getLang().getString("discord.coords-format", "X: %x% | Y: %y% | Z: %z%")
                .replace("%x%", String.valueOf(cx))
                .replace("%y%", String.valueOf(loc.getBlockY()))
                .replace("%z%", String.valueOf(cz));

        String size     = plugin.getLang().getString("discord.size-format", "%w%x%h% (%area% Blok)")
                .replace("%w%", String.valueOf(claim.getWidth()))
                .replace("%h%", String.valueOf(claim.getHeight()))
                .replace("%area%", String.valueOf(claim.getArea()));

        String dateFormat = plugin.getLang().getString("discord.date-format", "dd/MM/yyyy HH:mm");
        String date = DateTimeFormatter.ofPattern(dateFormat).format(LocalDateTime.now());

        String channelId = plugin.getConfig().getString("discord-bot.expired-channel-id");

        DiscordBotMessenger.sendEmbed(plugin, channelId,
                plugin.getLang().getString("discord.expired-title",  "⏰ Claim Süresi Sona Erdi!"),
                plugin.getLang().getString("discord.expired-desc",   "Süresi uzatılmayan bir claim silindi."),
                16711680,
                new DiscordBotMessenger.EmbedField(plugin.getLang().getString("discord.expired-field-owner",  "Eski Sahibi"),    ownerName, false),
                new DiscordBotMessenger.EmbedField(plugin.getLang().getString("discord.expired-field-date",   "Silinme Tarihi"), date,      true),
                new DiscordBotMessenger.EmbedField(plugin.getLang().getString("discord.expired-field-size",   "Boyut"),          size,      true),
                new DiscordBotMessenger.EmbedField(plugin.getLang().getString("discord.expired-field-coords", "Koordinatlar"),   coords,    false)
        );
    }

    public boolean isClaimForSale(Long claimId)  { return storage.computeIfAbsent(claimId, k -> new ClaimData()).isForSale; }
    public double  getSalePrice(Long claimId)     { return storage.computeIfAbsent(claimId, k -> new ClaimData()).salePrice; }
    public String  getSellerName(Long claimId)    { return storage.computeIfAbsent(claimId, k -> new ClaimData()).sellerName; }

    public void setSaleInfo(Long claimId, boolean forSale, double price, String seller) {
        ClaimData d = storage.computeIfAbsent(claimId, k -> new ClaimData());
        d.isForSale  = forSale;
        d.salePrice  = price;
        d.sellerName = seller;
        saveData();

        if (forSale) sendMarketDiscordMessage(claimId, d, price, seller);
    }

    private void sendMarketDiscordMessage(long claimId, ClaimData d, double price, String seller) {
        Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);
        if (claim == null) return;

        org.bukkit.Location loc = claim.getLesserBoundaryCorner();
        int cx = loc.getBlockX() + (claim.getWidth() / 2);
        int cz = loc.getBlockZ() + (claim.getHeight() / 2);

        String coords = plugin.getLang().getString("discord.coords-format", "X: %x% | Y: %y% | Z: %z%")
                .replace("%x%", String.valueOf(cx))
                .replace("%y%", String.valueOf(loc.getBlockY()))
                .replace("%z%", String.valueOf(cz));

        String size = plugin.getLang().getString("discord.size-format", "%w%x%h% (%area% Blok)")
                .replace("%w%", String.valueOf(claim.getWidth()))
                .replace("%h%", String.valueOf(claim.getHeight()))
                .replace("%area%", String.valueOf(claim.getArea()));

        long days  = d.remainingMinutes / 1440;
        long hours = (d.remainingMinutes % 1440) / 60;
        String dayWord  = plugin.getLang().getString("words.day",  "Gün");
        String hourWord = plugin.getLang().getString("words.hour", "Saat");

        String remaining = plugin.getLang().getString("discord.remaining-format", "%days% Gün %hours% Saat")
                .replace("%days%", String.valueOf(days))
                .replace("%hours%", String.valueOf(hours))
                .replace("%day_word%", dayWord)
                .replace("%hour_word%", hourWord);

        String priceStr = plugin.getLang().getString("discord.price-format", "%price%$")
                .replace("%price%", String.valueOf(price));

        String channelId = plugin.getConfig().getString("discord-bot.market-channel-id");

        DiscordBotMessenger.sendEmbed(plugin, channelId,
                plugin.getLang().getString("discord.market-title", "⛺ Yeni Satılık Arazi!"),
                plugin.getLang().getString("discord.market-desc",  "Bir oyuncu arazisini satışa sundu."),
                16776960,
                new DiscordBotMessenger.EmbedField(plugin.getLang().getString("discord.market-field-seller",    "Satıcı"),         seller,    true),
                new DiscordBotMessenger.EmbedField(plugin.getLang().getString("discord.market-field-price",     "Satış Fiyatı"),   priceStr,  true),
                new DiscordBotMessenger.EmbedField(plugin.getLang().getString("discord.market-field-size",      "Boyut"),          size,      true),
                new DiscordBotMessenger.EmbedField(plugin.getLang().getString("discord.market-field-coords",    "Koordinatlar"),   coords,    true),
                new DiscordBotMessenger.EmbedField(plugin.getLang().getString("discord.market-field-remaining", "Kalan Süresi"),   remaining, true)
        );
    }

    public void loadData() {
        storage.clear();
        if (!dataFile.exists()) return;
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<HashMap<Long, ClaimData>>(){}.getType();
            Map<Long, ClaimData> loaded = gson.fromJson(reader, type);
            if (loaded != null) storage = loaded;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void saveData() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(storage, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}