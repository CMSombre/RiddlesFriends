package me.cmriddles.riddlesfriends;

import java.util.List;
import java.util.Map;

public class FriendData {
    private Map<String, List<String>> friends;
    private Map<String, List<String>> pendingRequests;

    public FriendData(Map<String, List<String>> friends, Map<String, List<String>> pendingRequests) {
        this.friends = friends;
        this.pendingRequests = pendingRequests;
    }

    public Map<String, List<String>> getFriends() {
        return friends;
    }

    public Map<String, List<String>> getPendingRequests() {
        return pendingRequests;
    }
}
