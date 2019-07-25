package local.webrtc.signal.socketio.config;

import com.google.gson.JsonElement;

import local.webrtc.androidsdk.config.ILocalCallConfig;

public class SocketioConfig implements ILocalCallConfig {
    private final String ioUri;
    private final String userId;
    public SocketioConfig(String ioUri, String userId) {
        this.ioUri= ioUri;
        this.userId= userId;
    }

    public String getUri() {
        return ioUri;
    }
    public String getUserId() {
        return userId;
    }

    private JsonElement turnServer= null;
    @Override
    public JsonElement getTurnServer() {
        return turnServer;
    }

    public void setTurnServer(JsonElement turnServer) {
        this.turnServer= turnServer;
    }


}
