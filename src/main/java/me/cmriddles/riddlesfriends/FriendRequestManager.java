package me.cmriddles.riddlesfriends;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRequestManager {
    private Map<String, List<String>> pendingRequests; // Map of player names to their pending friend requests

    public FriendRequestManager() {
        this.pendingRequests = new HashMap<>();
    }

    public void addFriendRequest(String sender, String recipient) {
        // Add 'recipient' to the list of pending requests for 'sender'
        pendingRequests.computeIfAbsent(sender, k -> new ArrayList<>()).add(recipient);
    }

    public boolean hasPendingRequests(String player) {
        // Return true if there are pending requests for 'player', false otherwise
        return pendingRequests.containsKey(player);
    }

    public List<String> getPendingRequests(String player) {
        // Return the list of pending requests for 'player'
        return pendingRequests.getOrDefault(player, new ArrayList<>());
    }

    public void removePendingRequests(String player) {
        // Remove 'player' from the pending requests map
        pendingRequests.remove(player);
    }
}
