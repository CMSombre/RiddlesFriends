package me.cmriddles.riddlesfriends;

import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RiddlesFriendMain extends JavaPlugin {

    private Map<String, List<String>> friendData;
    private Map<String, List<String>> friendRequests;
    private Gson gson;

    @Override
    public void onEnable() {
        getLogger().info("RiddlesFriend plugin has been enabled.");
        gson = new Gson();

        // Initialize data structures
        friendData = new HashMap<>();
        friendRequests = new HashMap<>();

        // Ensure the plugin's data folder exists (Bukkit will create it if not)
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Load friend data from JSON file or create a new map if it doesn't exist
        File dataFile = new File(getDataFolder(), "frienddata.json");
        if (dataFile.exists()) {
            try (FileReader reader = new FileReader(dataFile)) {
                friendData = loadMapFromJson(reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Load friend requests from JSON file or create a new map if it doesn't exist
        File requestsFile = new File(getDataFolder(), "friendrequests.json");
        if (requestsFile.exists()) {
            try (FileReader reader = new FileReader(requestsFile)) {
                friendRequests = loadMapFromJson(reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("RiddlesFriend plugin has been disabled.");

        // Save friend data to JSON file
        saveMapToJson(friendData, "frienddata.json");

        // Save friend requests to JSON file
        saveMapToJson(friendRequests, "friendrequests.json");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("friend")) {
            if (args.length == 0) {
                sender.sendMessage("Usage: /friend help");
                return true;
            }

            Player player = (sender instanceof Player) ? (Player) sender : null;
            String playerName = player != null ? player.getName() : "CONSOLE";

            if (args[0].equalsIgnoreCase("help")) {
                // Command: /friend help - Shows a list of commands
                sender.sendMessage("Available commands:");
                sender.sendMessage("/friend list - Show who is currently on your friend list");
                sender.sendMessage("/friend add <player> - Add a player to your friend list");
                sender.sendMessage("/friend remove <player> - Remove a player from your friend list");
                sender.sendMessage("/friend accept [optional: <player>] - Accept a friend request");
                sender.sendMessage("/friend deny [optional: <player>] - Deny a friend request");
            } else if (args[0].equalsIgnoreCase("list")) {
                // Command: /friend list - Show friend list
                List<String> friendList = friendData.getOrDefault(playerName, new ArrayList<>());
                if (!friendList.isEmpty()) {
                    sender.sendMessage("Your friend list: " + friendList.toString());
                } else {
                    sender.sendMessage("Your friend list is empty.");
                }
            } else if (args[0].equalsIgnoreCase("add")) {
                // Command: /friend add <player> - Add a player to friend list
                if (player != null && args.length >= 2) {
                    String friendToAdd = args[1];
                    handleFriendAdd(sender, playerName, friendToAdd);
                } else {
                    sender.sendMessage("Usage: /friend add <player>");
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
                // Command: /friend remove <player> - Remove a player from friend list
                if (player != null && args.length >= 2) {
                    String friendToRemove = args[1];
                    handleFriendRemove(sender, playerName, friendToRemove);
                } else {
                    sender.sendMessage("Usage: /friend remove <player>");
                }
            } else if (args[0].equalsIgnoreCase("accept")) {
                // Command: /friend accept [optional: <player>] - Accept a friend request
                handleFriendAccept(sender, playerName, args);
            } else if (args[0].equalsIgnoreCase("deny")) {
                // Command: /friend deny [optional: <player>] - Deny a friend request
                handleFriendDeny(sender, playerName, args);
            } else {
                sender.sendMessage("Unknown command. Type /friend help for a list of commands.");
            }
            return true;
        }
        return false;
    }

    // Helper method to add friend request
    private void handleFriendAdd(CommandSender sender, String playerName, String friendToAdd) {
        if (!isFriend(playerName, friendToAdd) && !isFriendRequestPending(friendToAdd, playerName)) {
            // Add friend request
            friendRequests.computeIfAbsent(friendToAdd, k -> new ArrayList<>()).add(playerName);
            Player targetPlayer = getServer().getPlayer(friendToAdd);
            if (targetPlayer != null) {
                // Notify the player with sound and particle effects
                notifyPlayer(targetPlayer);
                targetPlayer.sendMessage("You have an incoming friend request from " + playerName +
                        ", to accept type /friend accept or /friend deny.");
            }
            sender.sendMessage("Friend request sent to " + friendToAdd);
        } else if (isFriend(playerName, friendToAdd)) {
            sender.sendMessage(friendToAdd + " is already in your friend list.");
        } else {
            sender.sendMessage("A friend request to " + friendToAdd + " is already pending or exists.");
        }
    }

    // Helper method to remove friend
    private void handleFriendRemove(CommandSender sender, String playerName, String friendToRemove) {
        if (isFriend(playerName, friendToRemove)) {
            // Remove friend from sender's list
            friendData.get(playerName).remove(friendToRemove);
            sender.sendMessage("Removed " + friendToRemove + " from your friend list.");
        } else {
            sender.sendMessage(friendToRemove + " is not in your friend list.");
        }
    }

    // Helper method to accept friend request
    private void handleFriendAccept(CommandSender sender, String playerName, String[] args) {
        String requester;
        if (args.length >= 2) {
            requester = args[1];
        } else {
            // If no username is specified, use the first pending request (if available)
            List<String> requests = friendRequests.getOrDefault(playerName, new ArrayList<>());
            if (requests.isEmpty()) {
                sender.sendMessage("No pending friend requests.");
                return;
            }
            requester = requests.get(0);
        }

        if (isFriendRequestPending(playerName, requester)) {
            boolean accepted = acceptFriendRequest(playerName, requester);
            if (accepted) {
                sender.sendMessage("Accepted friend request from " + requester);
                Player requesterPlayer = getServer().getPlayer(requester);
                if (requesterPlayer != null) {
                    requesterPlayer.sendMessage(playerName + " has accepted your friend request.");
                }
                // Inform the requester that they have been added to the friend list
                sender.sendMessage("You have added " + requester + " to your friend list.");
            } else {
                sender.sendMessage("Failed to accept friend request from " + requester);
            }
        } else {
            sender.sendMessage("No pending friend request from " + requester);
        }
    }

    // Helper method to deny friend request
    private void handleFriendDeny(CommandSender sender, String playerName, String[] args) {
        String requester;
        if (args.length >= 2) {
            requester = args[1];
        } else {
            // If no username is specified, use the first pending request (if available)
            List<String> requests = friendRequests.getOrDefault(playerName, new ArrayList<>());
            if (requests.isEmpty()) {
                sender.sendMessage("No pending friend requests.");
                return;
            }
            requester = requests.get(0);
        }

        if (isFriendRequestPending(playerName, requester)) {
            boolean removed = removeFriendRequest(requester, playerName);
            if (removed) {
                sender.sendMessage("Denied friend request from " + requester);
                Player requesterPlayer = getServer().getPlayer(requester);
                if (requesterPlayer != null) {
                    requesterPlayer.sendMessage(playerName + " has denied your friend request.");
                }
            } else {
                sender.sendMessage("Failed to deny friend request from " + requester);
            }
        } else {
            sender.sendMessage("No pending friend request from " + requester);
        }
    }

    private boolean isFriend(String player, String friend) {
        return friendData.containsKey(player) && friendData.get(player).contains(friend);
    }

    private boolean isFriendRequestPending(String requester, String target) {
        return friendRequests.containsKey(target) && friendRequests.get(target).contains(requester);
    }

    private boolean acceptFriendRequest(String player, String requester) {
        if (isFriendRequestPending(player, requester)) {
            // Add requester to player's friend list and remove the request
            friendData.computeIfAbsent(player, k -> new ArrayList<>()).add(requester);
            return removeFriendRequest(requester, player);
        }
        return false;
    }

    private boolean removeFriendRequest(String requester, String target) {
        // Remove requester from pending friend requests
        List<String> requests = friendRequests.getOrDefault(target, new ArrayList<>());
        boolean removed = requests.remove(requester);
        friendRequests.put(target, requests);
        return removed;
    }

    private void saveMapToJson(Map<String, List<String>> map, String fileName) {
        File dataFile = new File(getDataFolder(), fileName);
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(map, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, List<String>> loadMapFromJson(FileReader reader) {
        Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
        return gson.fromJson(reader, type);
    }

    private void notifyPlayer(Player player) {
        // Play a sound effect and particle effect to notify the player
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        player.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
    }
}
