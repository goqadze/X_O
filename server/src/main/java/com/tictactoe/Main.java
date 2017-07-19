package com.tictactoe;

import com.corundumstudio.socketio.*;

import java.util.*;

public class Main {

    private static Map<UUID, UUID> userSessionIds = new HashMap<>();
    private static Map<String, Game> roomGame = new HashMap<>();

    public static void main(String[] args) throws InterruptedException {
        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(8080);

        final SocketIOServer server = new SocketIOServer(config);

        server.addEventListener("sign_in", String.class, (SocketIOClient client, String userName, AckRequest ackSender) -> {
            client.set("user_id", UUID.randomUUID());
            client.set("user_name", userName);

            userSessionIds.put(client.get("user_id"), client.getSessionId());

            System.out.println(userName + " - connected");

            User ackUserObjectData = new User(client.get("user_id"), client.get("user_name"));
            client.sendEvent("set_user", new AckCallback<String>(String.class) {
                @Override
                public void onSuccess(String result) {
                    System.out.println(client.get("user_name") + " - set_user called on client side ");

                    updateOnlineUsersList(server);
                }
            }, ackUserObjectData);
        });

        server.addDisconnectListener((SocketIOClient client) -> {
            System.out.println(client.get("user_name") + " - disconnected");

            SocketIOClient opponent = getOpponent(client, server);
            if (opponent != null) {
                opponent.sendEvent("end_game");
            }

            client.getAllRooms().forEach((roomName) -> {
                server.getRoomOperations(roomName).getClients().forEach(c -> {
                    c.leaveRoom(roomName);
                });
                roomGame.remove(roomName);
            });

            userSessionIds.remove((UUID) client.get("user_id"));
            updateOnlineUsersList(server);
        });

        server.addEventListener("join_game_offer", UUID.class, (challenger, opponentId, ackSender) -> {
            SocketIOClient challenged = server.getClient(userSessionIds.get(opponentId));

            if (challenged != null) {
                String userName = challenger.get("user_name");

                UUID userId = challenger.get("user_id");

                User ackOpponentObject = new User(userId, userName);

                challenged.sendEvent("join_game_offer", new AckCallback<Boolean>(Boolean.class) {
                    @Override
                    public void onSuccess(Boolean yesNo) {
                        if (yesNo) {
                            String roomName = UUID.randomUUID().toString();

                            Random r = new Random();

                            int X = (Math.abs(r.nextInt()) % 2) + 1; // 1 or 2

                            challenger.set("player_type", X);
                            challenged.set("player_type", 3 - X);

                            challenger.set("points", 0);
                            challenged.set("points", 0);

                            challenger.joinRoom(roomName);
                            challenged.joinRoom(roomName);

                            challenger.sendEvent("start_game", new GameInitData(X, 0, 0));
                            challenged.sendEvent("start_game", new GameInitData(3 - X, 0, 0));

                            roomGame.put(roomName, new Game());

                            updateOnlineUsersList(server);

                        } else {
                            challenger.sendEvent("join_game_deny");
                        }
                    }
                }, ackOpponentObject);
            }
        });

        server.addEventListener("make_move", Move.class, (sender, move, ackRequest) -> {
            try {
                String roomName = getUserRoomName(sender);

                if (roomName == null)
                    throw new NullPointerException("Room name cannot be null");

                Game game = roomGame.get(roomName);

                MoveType playerType = MoveType.FromInt(sender.get("player_type"));

                if (game == null || playerType == game.getCurrentMove() || !game.makeMove(move)) {
                    System.out.println("invalid move");
                    return;
                }

                BroadcastOperations broadcastOperations = server.getRoomOperations(roomName);

                if (broadcastOperations != null) {
                    move.setType(game.getCurrentMove());
                    move.setIsWinner(game.checkWinner());

                    broadcastOperations.sendEvent("make_move", move);

                    MoveType winnerOrDraw = game.checkWinner();
                    if (winnerOrDraw != MoveType.NONE) {
                        game.reset();

                        SocketIOClient player1 = sender;
                        SocketIOClient player2 = getOpponent(player1, server);

                        if (player2 == null)
                            throw new NullPointerException("opponent cannot be null");

                        int t1 = player1.get("player_type");
                        int t2 = player2.get("player_type");

                        if (winnerOrDraw.getValue() == t1) {
                            player1.set("points", (int) player1.get("points") + 1);
                        } else if (winnerOrDraw.getValue() == t2) {
                            player2.set("points", (int) player2.get("points") + 1);
                        }

                        int player1Points = player1.get("points");
                        int player2Points = player2.get("points");

                        player1.set("player_type", t2);
                        player2.set("player_type", t1);

                        t1 = player1.get("player_type");
                        t2 = player2.get("player_type");

                        player1.sendEvent("start_game", new GameInitData(t1, player1Points, player2Points));
                        player2.sendEvent("start_game", new GameInitData(t2, player2Points, player1Points));
                    }
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        });

        server.addEventListener("resign", String.class, ((client, data, ackSender) -> {
            SocketIOClient opponent = getOpponent(client, server);
            opponent.sendEvent("end_game");

            String roomName = getUserRoomName(client);

            client.leaveRoom(roomName);
            opponent.leaveRoom(roomName);

            updateOnlineUsersList(server);
        }));

        server.addEventListener("message", String.class, (client, message, ackRequest) -> {
            SocketIOClient opponent = getOpponent(client, server);
            if (opponent != null) {
                opponent.sendEvent("message", message);
            }
        });

        server.start();
        Thread.sleep(Integer.MAX_VALUE);
        server.stop();
    }

    private static void updateOnlineUsersList(SocketIOServer server) {
        List<User> onlineUsersList = new ArrayList<>();
        for (UUID uuid : userSessionIds.values()) {
            SocketIOClient c = server.getClient(uuid);

            if (c == null) {
                userSessionIds.remove(uuid);
                continue;
            }

            if (c.getAllRooms().stream().filter(a -> !a.isEmpty()).count() > 0) continue;

            onlineUsersList.add(new User(c.get("user_id"), c.get("user_name")));
        }

        server.getBroadcastOperations().sendEvent("online_users", onlineUsersList);
    }

    private static SocketIOClient getOpponent(SocketIOClient client, SocketIOServer server) {
        String roomName = getUserRoomName(client);
        if (roomName != null) {
            for (SocketIOClient c : server.getRoomOperations(roomName).getClients()) {
                if (client != null && client.equals(c)) continue;
                return c;
            }
        }
        return null;
    }

    private static String getUserRoomName(SocketIOClient client) {
        return client.getAllRooms()
                .stream()
                .filter(r -> !r.isEmpty())
                .findFirst().orElse(null);
    }
}

