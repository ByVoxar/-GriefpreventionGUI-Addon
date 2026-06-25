package tr.voseraproject.griefPreventionGUI;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.economy.Economy;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import tr.voseraproject.griefPreventionGUI.commands.ClaimGuiCommand;
import tr.voseraproject.griefPreventionGUI.commands.ClaimReloadCommand;
import tr.voseraproject.griefPreventionGUI.listeners.ClaimSettingsListener;
import tr.voseraproject.griefPreventionGUI.listeners.MenuClickListener;
import tr.voseraproject.griefPreventionGUI.managers.ClaimExpirationManager;
import tr.voseraproject.griefPreventionGUI.managers.ClaimMenuManager;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GriefPreventionGUI extends JavaPlugin {

    private static GriefPreventionGUI instance;
    private Economy economy;
    private GriefPrevention griefPrevention;

    private ClaimMenuManager menuManager;
    private ClaimExpirationManager expirationManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Pattern hexShortPattern = Pattern.compile("(&#|#)([A-Fa-f0-9]{6})");
    private final Pattern hexLegacyPattern = Pattern.compile("&x(&[A-Fa-f0-9]){6}");
    private final Map<UUID, Long> chatInputPlayers = new HashMap<>();

    private FileConfiguration lang;
    private FileConfiguration menuConfig;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        saveResourceIfAbsent("languages/lang_tr.yml");
        saveResourceIfAbsent("menus/menu.yml");

        loadLang();
        loadMenuConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault veya uyumlu bir Ekonomi eklentisi bulunamadı! Eklenti kapatılıyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.griefPrevention = GriefPrevention.instance;
        if (this.griefPrevention == null) {
            getLogger().severe("GriefPrevention bulunamadı! Eklenti kapatılıyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.expirationManager = new ClaimExpirationManager(this);
        this.expirationManager.loadData();
        this.menuManager = new ClaimMenuManager(this);

        getServer().getPluginManager().registerEvents(new MenuClickListener(this), this);
        getServer().getPluginManager().registerEvents(new ClaimSettingsListener(this), this);

        ClaimGuiCommand cmdExecutor = new ClaimGuiCommand(this);
        String[] commands = {"claimgui", "claimlerim", "claimsatış", "claimsat"};
        for (String cmd : commands) {
            if (getCommand(cmd) != null) {
                getCommand(cmd).setExecutor(cmdExecutor);
            } else {
                getLogger().warning(cmd + " komutu plugin.yml dosyasında bulunamadı!");
            }
        }
        if (getCommand("claimreload") != null) {
            getCommand("claimreload").setExecutor(new ClaimReloadCommand(this));
        }
    }

    @Override
    public void onDisable() {
        if (expirationManager != null) expirationManager.saveData();
    }

    private void saveResourceIfAbsent(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            saveResource(resourcePath, false);
        }
    }

    public void loadLang() {
        String langKey = getConfig().getString("language", "lang_tr");
        File langFile = new File(getDataFolder(), "languages/" + langKey + ".yml");
        if (!langFile.exists()) {
            getLogger().warning("Dil dosyası bulunamadı: " + langFile.getName() + " — varsayılan kullanılıyor.");
            InputStream is = getResource("languages/" + langKey + ".yml");
            if (is != null) {
                lang = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
            } else {
                lang = new YamlConfiguration();
            }
        } else {
            lang = YamlConfiguration.loadConfiguration(langFile);
        }
    }

    public void loadMenuConfig() {
        File menuFile = new File(getDataFolder(), "menus/menu.yml");
        if (!menuFile.exists()) {
            getLogger().warning("menu.yml bulunamadı — varsayılan kullanılıyor.");
            InputStream is = getResource("menus/menu.yml");
            if (is != null) {
                menuConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
            } else {
                menuConfig = new YamlConfiguration();
            }
        } else {
            menuConfig = YamlConfiguration.loadConfiguration(menuFile);
        }
    }

    public void reloadAllConfigs() {
        reloadConfig();
        loadLang();
        loadMenuConfig();
    }

    public Component parseMsg(String path) {
        String prefix = lang.getString("prefix", "");
        String msg = lang.getString(path, "&c[Mesaj bulunamadı: " + path + "]");
        return parseRaw(prefix + msg);
    }

    public Component parseMsgNoPrefix(String path) {
        String msg = lang.getString(path, "&c[Mesaj bulunamadı: " + path + "]");
        return parseRaw(msg);
    }

    public Component parseRaw(String text) {
        if (text == null) return Component.empty();
        Matcher legacyMatcher = hexLegacyPattern.matcher(text);
        StringBuilder sb1 = new StringBuilder();
        while (legacyMatcher.find()) {
            String match = legacyMatcher.group();
            StringBuilder hex = new StringBuilder("#");
            for (int i = 2; i < match.length(); i += 2) {
                hex.append(match.charAt(i));
            }
            legacyMatcher.appendReplacement(sb1, "<color:" + hex + ">");
        }
        legacyMatcher.appendTail(sb1);
        text = sb1.toString();
        Matcher shortMatcher = hexShortPattern.matcher(text);
        StringBuilder sb2 = new StringBuilder();
        while (shortMatcher.find()) {
            shortMatcher.appendReplacement(sb2, "<color:#" + shortMatcher.group(2) + ">");
        }
        shortMatcher.appendTail(sb2);
        text = sb2.toString();
        text = text
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&r", "<reset>")
                .replace("&l", "<bold>")
                .replace("&o", "<italic>")
                .replace("&n", "<underlined>")
                .replace("&m", "<strikethrough>")
                .replace("&k", "<obfuscated>");

        text = "<italic:false>" + text;

        return miniMessage.deserialize(text);
    }


    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        this.economy = rsp.getProvider();
        return this.economy != null;
    }

    public static GriefPreventionGUI getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public GriefPrevention getGriefPrevention() { return griefPrevention; }
    public ClaimMenuManager getMenuManager() { return menuManager; }
    public ClaimExpirationManager getExpirationManager() { return expirationManager; }
    public Map<UUID, Long> getChatInputPlayers() { return chatInputPlayers; }
    public FileConfiguration getLang() { return lang; }
    public FileConfiguration getMenuConfig() { return menuConfig; }
}
