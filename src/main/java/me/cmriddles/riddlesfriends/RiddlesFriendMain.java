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
        friendData = new HashMap<>();
        friendRequests = new HashMap<>();
        ensureDataFolderExists();
        loadFriendData();
    }

    @Override
    public void onDisable() {
        getLogger().info("RiddlesFriend plugin has been disabled.");
        saveFriendData();
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
                    handleFriendAccept(sender, playerName, args);
                    break;
                case "deny":
                    handleFriendDeny(sender, playerName, args);
                    break;
                default:
                    sender.sendMessage("Unknown command. Type /friend help for a list of commands.");
            }
            return true;
        }
        return false;
    }

    private void ensureDataFolderExists() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
    }

    private void loadFriendData() {
        loadMapFromJson("frienddata.json", friendData);
        loadMapFromJson("friendrequests.json", friendRequests);
    }

    private void saveFriendData() {
        saveMapToJson("frienddata.json", friendData);
        saveMapToJson("friendrequests.json", friendRequests);
    }

    private void loadMapFromJson(String fileName, Map<String, List<String>> map) {
        File dataFile = new File(getDataFolder(), fileName);
        if (dataFile.exists()) {
            try (FileReader reader = new FileReader(dataFile)) {
                Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
                map.putAll(gson.fromJson(reader, type));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveMapToJson(String fileName, Map<String, List<String>> map) {
        File dataFile = new File(getDataFolder(), fileName);
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(map, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("Available commands:");
        sender.sendMessage("/friend list - Show who is currently on your friend list");
        sender.sendMessage("/friend add <player> - Add a player to your friend list");
        sender.sendMessage("/friend remove <player> - Remove a player from your friend list");
        sender.sendMessage("/friend accept [optional: <player>] - Accept a friend request");
        sender.sendMessage("/friend deny [optional: <player>] - Deny a friend request");
    }

    private void sendFriendList(CommandSender sender, String playerName) {
        List<String> friendList = friendData.getOrDefault(playerName, new ArrayList<>());
        if (!friendList.isEmpty()) {
            sender.sendMessage("Your friend list: " + friendList.toString());
        } else {
            sender.sendMessage("Your friend list is empty.");
        }
    }

    private void handleFriendAdd(CommandSender sender, String playerName, String[] args) {
        if (args.length >= 2) {
            String friendToAdd = args[1];
            if (!isFriend(playerName, friendToAdd) && !isFriendRequestPending(friendToAdd, playerName)) {
                friendRequests.computeIfAbsent(friendToAdd, k -> new ArrayList<>()).add(playerName);
                Player targetPlayer = getServer().getPlayer(friendToAdd);
                if (targetPlayer != null) {
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
        } else {
            sender.sendMessage("Usage: /friend add <player>");
        }
    }

    private void handleFriendRemove(CommandSender sender, String playerName, String[] args) {
        if (args.length >= 2) {
            String friendToRemove = args[1];
            if (isFriend(playerName, friendToRemove)) {
                friendData.get(playerName).remove(friendToRemove);
                sender.sendMessage("Removed " + friendToRemove + " from your friend list.");
            } else {
                sender.sendMessage(friendToRemove + " is not in your friend list.");
            }
        } else {
            sender.sendMessage("Usage: /friend remove <player>");
        }
    }

    private void handleFriendAccept(CommandSender sender, String playerName, String[] args) {
        String requester;
        if (args.length >= 2) {
            requester = args[1];
        } else {
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
                sender.sendMessage("You have added " + requester + " to your friend list.");
            } else {
                sender.sendMessage("Failed to accept friend request from " + requester);
            }
        } else {
            sender.sendMessage("No pending friend request from " + requester);
        }
    }

    private void handleFriendDeny(CommandSender sender, String playerName, String[] args) {
        String requester;
        if (args.length >= 2) {
            requester = args[1];
        } else {
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
            friendData.computeIfAbsent(player, k -> new ArrayList<>()).add(requester);
            return removeFriendRequest(requester, player);
        }
        return false;
    }

    private boolean removeFriendRequest(String requester, String target) {
        List<String> requests = friendRequests.getOrDefault(target, new ArrayList<>());
        boolean removed = requests.remove(requester);
        friendRequests.put(target, requests);
        return removed;
    }

    private void notifyPlayer(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        player.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
    }
}
