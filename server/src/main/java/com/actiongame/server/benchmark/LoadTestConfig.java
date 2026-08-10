package com.actiongame.server.benchmark;

public class LoadTestConfig {
    private final int clientCount;
    private final long durationMs;
    private final int actionIntervalMs;
    private final int playersPerRoom;
    private final String host;
    private final int port;

    public LoadTestConfig(int clientCount, long durationMs, int actionIntervalMs,
                          int playersPerRoom, String host, int port) {
        this.clientCount = clientCount;
        this.durationMs = durationMs;
        this.actionIntervalMs = actionIntervalMs;
        this.playersPerRoom = playersPerRoom;
        this.host = host;
        this.port = port;
    }

    public int getClientCount() { return clientCount; }
    public long getDurationMs() { return durationMs; }
    public int getActionIntervalMs() { return actionIntervalMs; }
    public int getPlayersPerRoom() { return playersPerRoom; }
    public String getHost() { return host; }
    public int getPort() { return port; }

    public int getRoomCount() {
        return Math.max(1, (clientCount + playersPerRoom - 1) / playersPerRoom);
    }

    public String roomIdForClient(int index) {
        return "bench-room-" + (index / playersPerRoom);
    }
}
