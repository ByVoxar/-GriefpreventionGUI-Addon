package tr.voseraproject.griefPreventionGUI.commands;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import tr.voseraproject.griefPreventionGUI.GriefPreventionGUI;

public class ClaimGuiCommand implements CommandExecutor {

    private final GriefPreventionGUI plugin;

    public ClaimGuiCommand(GriefPreventionGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLang().getString("console-only", "Bu komut sadece oyuncular için geçerlidir."));
            return true;
        }

        if (label.equalsIgnoreCase("claimsatış") || label.equalsIgnoreCase("claimsatis")
                || label.equalsIgnoreCase("claimpazarı") || label.equalsIgnoreCase("claimpazari")) {
            plugin.getMenuManager().openMarketMenu(player);
            return true;
        }

        if (label.equalsIgnoreCase("claimlerim") || label.equalsIgnoreCase("clist")
                || label.equalsIgnoreCase("claimlist")) {
            plugin.getMenuManager().openPlayerClaimsMenu(player);
            return true;
        }

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);

        if (label.equalsIgnoreCase("claimsat")) {
            if (claim == null) {
                player.sendMessage(plugin.parseMsg("not-in-claim"));
                return true;
            }
            if (!claim.getOwnerName().equals(player.getName())) {
                player.sendMessage(plugin.parseMsg("not-claim-owner"));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(plugin.parseMsg("claimsat-usage"));
                return true;
            }
            try {
                double price = Double.parseDouble(args[0]);
                if (price < 0) {
                    player.sendMessage(plugin.parseMsg("claimsat-negative-price"));
                    return true;
                }
                plugin.getExpirationManager().setSaleInfo(claim.getID(), true, price, player.getName());
                player.sendMessage(plugin.parseRaw(
                        plugin.getLang().getString("claimsat-success", "&aArsan &e%price%$ &afiyatla eklendi!")
                                .replace("%price%", String.valueOf(price))));
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.parseMsg("claimsat-invalid-price"));
            }
            return true;
        }

        if (claim == null) {
            player.sendMessage(plugin.parseMsg("not-in-claim"));
            return true;
        }
        if (!claim.getOwnerName().equals(player.getName())) {
            player.sendMessage(plugin.parseMsg("not-claim-owner"));
            return true;
        }
        plugin.getMenuManager().openMainMenu(player, claim);
        return true;
    }
}