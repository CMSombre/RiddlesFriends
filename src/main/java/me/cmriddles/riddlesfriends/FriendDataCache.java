package me.cmriddles.riddlesfriends;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FriendDataCache {
    private final DatabaseHandler databaseHandler;

    public FriendDataCache(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
        createTablesIfNotExist();
    }

    private void createTablesIfNotExist() {
        try (Connection connection = databaseHandler.getConnection();
             PreparedStatement createFriendsTable = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS friends (player1 VARCHAR(255), player2 VARCHAR(255), PRIMARY KEY (player1, player2))"
             );
             PreparedStatement createFriendRequestsTable = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS friend_requests (sender VARCHAR(255), receiver VARCHAR(255), PRIMARY KEY (sender, receiver))"
             )) {

            createFriendsTable.executeUpdate();
            createFriendRequestsTable.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addFriend(String player1, String player2) {
        try (Connection connection = databaseHandler.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO friends (player1, player2) VALUES (?, ?)"
             )) {
            preparedStatement.setString(1, player1);
            preparedStatement.setString(2, player2);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeFriend(String player1, String player2) {
        try (Connection connection = databaseHandler.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "DELETE FROM friends WHERE (player1 = ? AND player2 = ?) OR (player1 = ? AND player2 = ?)"
             )) {
            preparedStatement.setString(1, player1);
            preparedStatement.setString(2, player2);
            preparedStatement.setString(3, player2);
            preparedStatement.setString(4, player1);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isFriend(String player1, String player2) {
        try (Connection connection = databaseHandler.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT * FROM friends WHERE (player1 = ? AND player2 = ?) OR (player1 = ? AND player2 = ?)"
             )) {
            preparedStatement.setString(1, player1);
            preparedStatement.setString(2, player2);
            preparedStatement.setString(3, player2);
            preparedStatement.setString(4, player1);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isFriendRequestPending(String sender, String receiver) {
        try (Connection connection = databaseHandler.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT * FROM friend_requests WHERE sender = ? AND receiver = ?"
             )) {
            preparedStatement.setString(1, sender);
            preparedStatement.setString(2, receiver);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void addFriendRequest(String sender, String receiver) {
        try (Connection connection = databaseHandler.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO friend_requests (sender, receiver) VALUES (?, ?)"
             )) {
            preparedStatement.setString(1, sender);
            preparedStatement.setString(2, receiver);
            preparedStatement.executeUpdate();

            Player targetPlayer = Bukkit.getPlayerExact(receiver);
            if (targetPlayer != null) {
                // Particle effect for receiver
                targetPlayer.spawnParticle(Particle.HEART, targetPlayer.getLocation(), 1);
                // Sound effect for receiver
                targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeFriendRequest(String sender, String receiver) {
        try (Connection connection = databaseHandler.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "DELETE FROM friend_requests WHERE sender = ? AND receiver = ?"
             )) {
            preparedStatement.setString(1, sender);
            preparedStatement.setString(2, receiver);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getFriendRequests(String player) {
        List<String> friendRequests = new ArrayList<>();
        try (Connection connection = databaseHandler.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT sender FROM friend_requests WHERE receiver = ?"
             )) {
            preparedStatement.setString(1, player);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    friendRequests.add(resultSet.getString("sender"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friendRequests;
    }

    public List<String> getFriendList(String player) {
        List<String> friends = new ArrayList<>();
        try (Connection connection = databaseHandler.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT player2 FROM friends WHERE player1 = ? UNION SELECT player1 FROM friends WHERE player2 = ?"
             )) {
            preparedStatement.setString(1, player);
            preparedStatement.setString(2, player);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    friends.add(resultSet.getString("player2"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friends;
    }

    public boolean hasFriendRequests(String player) {
        try (Connection connection = databaseHandler.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT * FROM friend_requests WHERE receiver = ?"
             )) {
            preparedStatement.setString(1, player);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasFriendList(String player) {
        try (Connection connection = databaseHandler.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT * FROM friends WHERE player1 = ? OR player2 = ?"
             )) {
            preparedStatement.setString(1, player);
            preparedStatement.setString(2, player);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean acceptFriendRequest(String player1, String player2) {
        if (!isFriend(player1, player2)) {
            addFriend(player1, player2);
            removeFriendRequest(player2, player1);

            Player senderPlayer = Bukkit.getPlayerExact(player1);
            if (senderPlayer != null) {
                // Particle effect for sender
                senderPlayer.spawnParticle(Particle.HEART, senderPlayer.getLocation(), 1);
                // Sound effect for sender
                senderPlayer.playSound(senderPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            }
            return true;
        }
        return false;
    }
}