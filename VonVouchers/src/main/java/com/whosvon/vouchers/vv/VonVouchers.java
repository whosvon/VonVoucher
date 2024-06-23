package com.whosvon.vouchers.vv;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VonVouchers extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        config = this.getConfig();
        this.getCommand("vonvoucher").setExecutor(this);
        this.getCommand("vonvoucher").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("VonVouchers has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("VonVouchers has been disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.YELLOW + translateColorCodes("&e/voucher give"));
            player.sendMessage(ChatColor.YELLOW + translateColorCodes("&e/voucher create"));
            player.sendMessage(ChatColor.YELLOW + translateColorCodes("&e/voucher remove"));
            player.sendMessage(ChatColor.YELLOW + translateColorCodes("&e/voucher list"));
            player.sendMessage(ChatColor.YELLOW + translateColorCodes("&e/voucher reload"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + translateColorCodes("&cUsage: /vonvoucher create <VoucherName> <Command>"));
                    return true;
                }

                String voucherName = args[1];
                String commandToExecute = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));

                if (!player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                    ItemStack voucherItem = player.getInventory().getItemInMainHand().clone();
                    ItemMeta meta = voucherItem.getItemMeta();
                    meta.setDisplayName(translateColorCodes("&6" + voucherName));
                    List<String> lore = new ArrayList<>();
                    lore.add(translateColorCodes("&bRight-click to use this voucher."));
                    meta.setLore(lore);
                    voucherItem.setItemMeta(meta);

                    config.set("vouchers." + voucherName + ".command", commandToExecute);
                    config.set("vouchers." + voucherName + ".item", voucherItem.serialize());
                    saveConfig();
                    player.sendMessage(ChatColor.GREEN + translateColorCodes("&aVoucher " + voucherName + " created successfully."));
                } else {
                    player.sendMessage(ChatColor.RED + translateColorCodes("&cYou must be holding an item to create a voucher."));
                }
                break;

            case "remove":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + translateColorCodes("&cUsage: /vonvoucher remove <VoucherName>"));
                    return true;
                }

                voucherName = args[1];

                if (config.contains("vouchers." + voucherName)) {
                    config.set("vouchers." + voucherName, null);
                    saveConfig();
                    player.sendMessage(ChatColor.GREEN + translateColorCodes("&aVoucher " + voucherName + " removed successfully."));
                } else {
                    player.sendMessage(ChatColor.RED + translateColorCodes("&cVoucher " + voucherName + " does not exist."));
                }
                break;

            case "give":
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + translateColorCodes("&cUsage: /vonvoucher give <Player> <VoucherName> <AmountOfVouchers>"));
                    return true;
                }

                Player targetPlayer = Bukkit.getPlayer(args[1]);
                voucherName = args[2];
                int amount;

                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + translateColorCodes("&cAmount of vouchers must be a number."));
                    return true;
                }

                if (targetPlayer == null) {
                    player.sendMessage(ChatColor.RED + translateColorCodes("&cPlayer not found."));
                    return true;
                }

                if (!config.contains("vouchers." + voucherName)) {
                    player.sendMessage(ChatColor.RED + translateColorCodes("&cVoucher " + voucherName + " does not exist."));
                    return true;
                }

                ItemStack voucherItem = ItemStack.deserialize(config.getConfigurationSection("vouchers." + voucherName + ".item").getValues(true));
                voucherItem.setAmount(amount);
                targetPlayer.getInventory().addItem(voucherItem);
                player.sendMessage(ChatColor.GREEN + translateColorCodes("&aGave " + amount + " " + voucherName + " voucher(s) to " + targetPlayer.getName() + "."));
                targetPlayer.sendMessage(ChatColor.GREEN + translateColorCodes("&aYou received " + amount + " " + voucherName + " voucher(s)."));
                break;

            case "list":
                ConfigurationSection vouchersSection = config.getConfigurationSection("vouchers");
                if (vouchersSection == null || vouchersSection.getKeys(false).isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + translateColorCodes("&eNo vouchers available."));
                } else {
                    Set<String> voucherKeys = vouchersSection.getKeys(false);
                    player.sendMessage(ChatColor.YELLOW + translateColorCodes("&eVouchers:"));
                    for (String key : voucherKeys) {
                        player.sendMessage(ChatColor.AQUA + translateColorCodes("&b- " + key));
                    }
                }
                break;

            case "reload":
                reloadConfig();
                config = getConfig();
                player.sendMessage(ChatColor.GREEN + translateColorCodes("&aVonVouchers configuration reloaded."));
                break;

            default:
                player.sendMessage(ChatColor.RED + translateColorCodes("&cUnknown command. Use /vonvoucher to see all available commands."));
                break;
        }

        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(translateColorCodes("&6Vouchers"))) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                String voucherName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
                String commandToExecute = config.getString("vouchers." + voucherName + ".command");

                if (commandToExecute != null) {
                    player.getInventory().remove(clickedItem);
                    player.closeInventory();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute.replace("%player%", player.getName()));
                    player.sendMessage(ChatColor.GREEN + translateColorCodes("&aVoucher " + voucherName + " used successfully."));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String voucherName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

            if (config.contains("vouchers." + voucherName)) {
                String commandToExecute = config.getString("vouchers." + voucherName + ".command");

                if (commandToExecute != null) {
                    player.getInventory().remove(item);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute.replace("%player%", player.getName()));
                    player.sendMessage(ChatColor.GREEN + translateColorCodes("&aVoucher " + voucherName + " used successfully."));
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("create");
            completions.add("remove");
            completions.add("give");
            completions.add("list");
            completions.add("reload");
        }
        return completions;
    }

    private String translateColorCodes(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}