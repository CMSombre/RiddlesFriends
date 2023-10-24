package me.cmriddles.riddlesfriends;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RiddlesFriendMain extends JavaPlugin {

    private FriendDataCache friendDataCache;

    @Override
    public void onEnable() {
        getLogger().info("RiddlesFriend plugin has been enabled.");

        // Initialize database connection parameters
        String jdbcUrl = "jdbc:mysql://localhost:3306/riddlesfriend?useSSL=false"; // Replace with your database URL
        String username = "CMRiddles"; // Replace with your database username
        String password = "GodSpeed742"; // Replace with your database password

        // Initialize FriendDataCache with DatabaseHandler
        DatabaseHandler databaseHandler = new DatabaseHandler(jdbcUrl, username, password);
        friendDataCache = new FriendDataCache(databaseHandler);

        // Other setup code can go here if needed
    }

    @Override
    public void onDisable() {
        getLogger().info("RiddlesFriend plugin has been disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("friend")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;
            String playerName = player.getName();

            if (args.length == 0) {
                sendHelpMessage(sender);
                return true;
            }

            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "help":
                    sendHelpMessage(sender);
                    break;
                case "list":
                    sendFriendList(sender, playerName);
                    break;
                case "add":
                    handleFriendAdd(sender, playerName, args);
                    break;
                case "remove":
                    handleFriendRemove(sender, playerName, args);
                    break;
                case "accept":
                    String friendToAccept = (args.length >= 2) ? args[1] : null;
                    handleFriendAccept(sender, playerName, friendToAccept);
                    break;
                case "deny":
                    String friendToDeny = (args.length >= 2) ? args[1] : null;
                    handleFriendDeny(sender, playerName, friendToDeny);
                    break;
                default:
                    sender.sendMessage("Unknown command. Type /" + label + " help for a list of commands.");
            }
            return true;
        }
        return false;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("Available commands:");
        sender.sendMessage("/friend list - Show who is currently on your friend list");
        sender.sendMessage("/friend add <player> - Add a player to your friend list");
        sender.sendMessage("/friend remove <player> - Remove a player from your friend list");
        sender.sendMessage("/friend accept [player] - Accept a friend request");
        sender.sendMessage("/friend deny [player] - Deny a friend request");
    }

    private void sendFriendList(CommandSender sender, String playerName) {
        if (friendDataCache.hasFriendList(playerName)) {
            sender.sendMessage("Your friend list:");
            for (String friend : friendDataCache.getFriendList(playerName)) {
                sender.sendMessage("- " + friend);
            }
        } else {
            sender.sendMessage("Your friend list is empty.");
        }
    }

    private void handleFriendAdd(CommandSender sender, String playerName, String[] args) {
        if (args.length >= 2) {
            String friendToAdd = args[1];
            if (!friendDataCache.isFriend(playerName, friendToAdd) && !friendDataCache.isFriendRequestPending(playerName, friendToAdd)) {
                friendDataCache.addFriendRequest(playerName, friendToAdd);
                Player targetPlayer = Bukkit.getPlayer(friendToAdd);
                if (targetPlayer != null) {
                    targetPlayer.sendMessage("You have received a friend request from " + playerName +
                            ". Type /friend accept " + playerName + " to accept.");

                    // Play sound to the target player
                    targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                    // Display particle effects to the target player
                    targetPlayer.spawnParticle(Particle.VILLAGER_HAPPY, targetPlayer.getLocation(), 50);

                    sender.sendMessage("Friend request sent to " + friendToAdd);
                } else {
                    sender.sendMessage("Could not find player: " + friendToAdd);
                }
            } else if (friendDataCache.isFriend(playerName, friendToAdd)) {
                sender.sendMessage(friendToAdd + " is already in your friend list.");
            } else {
                sender.sendMessage("A friend request to " + friendToAdd + " is already pending or exists.");
            }
        } else {
            sender.sendMessage("Usage: /friend add <player>");
        }
    }

    private void handleFriendRemove(CommandSender sender, String playerName, String[] args) {
        if (args.length >= 2) {
            String friendToRemove = args[1];
            if (friendDataCache.isFriend(playerName, friendToRemove)) {
                friendDataCache.removeFriend(playerName, friendToRemove);
                sender.sendMessage("Removed " + friendToRemove + " from your friend list.");
            } else {
                sender.sendMessage(friendToRemove + " is not in your friend list.");
            }
        } else {
            sender.sendMessage("Usage: /friend remove <player>");
        }
    }

    private void handleFriendAccept(CommandSender sender, String playerName, String friendToAccept) {
        if (friendToAccept == null) {
            // Handle accepting friend requests without specifying a player
            if (friendDataCache.hasFriendRequests(playerName)) {
                for (String pendingRequest : friendDataCache.getFriendRequests(playerName)) {
                    // Accept all pending friend requests
                    friendDataCache.acceptFriendRequest(playerName, pendingRequest);
                    sender.sendMessage("You are now friends with " + pendingRequest + "!");
                }
            } else {
                sender.sendMessage("No pending friend requests to accept.");
            }
        } else {
            // Handle accepting friend requests with specifying a player (friendToAccept)
            if (friendDataCache.isFriendRequestPending(friendToAccept, playerName)) {
                if (friendDataCache.acceptFriendRequest(playerName, friendToAccept)) {
                    sender.sendMessage("You are now friends with " + friendToAccept + "!");
                } else {
                    sender.sendMessage("Failed to accept friend request from " + friendToAccept + ".");
                }
            } else {
                sender.sendMessage("No pending friend request from " + friendToAccept + ".");
            }
        }
    }

    private void handleFriendDeny(CommandSender sender, String playerName, String friendToDeny) {
        if (friendToDeny == null) {
            // Handle denying friend requests without specifying a player
            if (friendDataCache.hasFriendRequests(playerName)) {
                for (String pendingRequest : friendDataCache.getFriendRequests(playerName)) {
                    // Deny all pending friend requests
                    friendDataCache.removeFriendRequest(pendingRequest, playerName);
                    sender.sendMessage("Denied friend request from " + pendingRequest + ".");
                }
            } else {
                sender.sendMessage("No pending friend requests to deny.");
            }
        } else {
            // Handle denying friend requests with specifying a player (friendToDeny)
            if (friendDataCache.isFriendRequestPending(friendToDeny, playerName)) {
                friendDataCache.removeFriendRequest(friendToDeny, playerName);
                sender.sendMessage("Denied friend request from " + friendToDeny + ".");
            } else {
                sender.sendMessage("No pending friend request from " + friendToDeny + ".");
            }
        }
    }
}