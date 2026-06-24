package tr.voseraproject.griefPreventionGUI.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import tr.voseraproject.griefPreventionGUI.GriefPreventionGUI;

public class ClaimReloadCommand implements CommandExecutor {

    private final GriefPreventionGUI plugin;

    public ClaimReloadCommand(GriefPreventionGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("griefpreventiongui.admin")) {
            sender.sendMessage(plugin.parseMsg("no-permission"));
            return true;
        }
        plugin.reloadAllConfigs();
        sender.sendMessage(plugin.parseMsg("reload-success"));
        return true;
    }
}