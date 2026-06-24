package tr.voseraproject.griefPreventionGUI.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import tr.voseraproject.griefPreventionGUI.GriefPreventionGUI;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordBotMessenger {

    public static void sendEmbed(GriefPreventionGUI plugin, String channelId, String title, String description, int colorHex, EmbedField... fields) {
        if (!plugin.getConfig().getBoolean("discord-bot.enabled", true)) return;
        String botToken = plugin.getConfig().getString("discord-bot.token");
        if (botToken == null || botToken.isEmpty() || botToken.startsWith("BURAYA")) return;
        if (channelId == null || channelId.isEmpty() || channelId.startsWith("BURAYA")) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://discord.com/api/v10/channels/" + channelId + "/messages");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bot " + botToken);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "MinecraftPlugin (GriefPreventionGUI, 1.0)");
                connection.setDoOutput(true);

                JsonObject json = new JsonObject();
                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();

                embed.addProperty("title", title);
                embed.addProperty("description", description);
                embed.addProperty("color", colorHex);

                if (fields.length > 0) {
                    JsonArray jsonFields = new JsonArray();
                    for (EmbedField field : fields) {
                        JsonObject f = new JsonObject();
                        f.addProperty("name", field.name);
                        f.addProperty("value", field.value);
                        f.addProperty("inline", field.inline);
                        jsonFields.add(f);
                    }
                    embed.add("fields", jsonFields);
                }

                embeds.add(embed);
                json.add("embeds", embeds);

                String jsonString = json.toString();
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != 200 && responseCode != 204) {
                    plugin.getLogger().warning("Discord Bot API hatası! Kod: " + responseCode);
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static class EmbedField {
        String name;
        String value;
        boolean inline;

        public EmbedField(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
    }
}