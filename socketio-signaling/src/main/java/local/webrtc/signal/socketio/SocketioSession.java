package local.webrtc.signal.socketio;

import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.util.Log;

import java.util.concurrent.ConcurrentHashMap;

import io.socket.client.IO;
import io.socket.client.Socket;
import local.webrtc.androidsdk.call.ILocalCallsManager;
import local.webrtc.androidsdk.config.ILocalCallConfig;
import local.webrtc.androidsdk.signal.ILocalRoom;
import local.webrtc.androidsdk.signal.ILocalSession;
import local.webrtc.signal.socketio.config.SocketioConfig;
import local.webrtc.signal.socketio.event.EventNewMessage;

public class SocketioSession implements ILocalSession {
    public static final String TAG= SocketioSession.class.getSimpleName();

    private Socket ioSocket= null;
    private ConcurrentHashMap<String, ILocalRoom> roomMap= new ConcurrentHashMap<>();

    private ILocalCallsManager callManager= null;

    private final SocketioConfig socketioConfig;
    public SocketioSession(SocketioConfig config) {
        this.socketioConfig= config;
    }

    @Override
    public String getUserId() {
        return socketioConfig.getUserId();
    }

    @Override
    public String getMyUserId() {
        return getUserId();
    }

    @Override
    public String getCredentialsUserId() {
        return getUserId();
    }

    @Override
    public boolean open() {
        if(socketioConfig.getUri() == null)
            return false;
        if(isOpen())
            return true;

        try {
            ioSocket = IO.socket(socketioConfig.getUri());

            connect();
            return true;
        } catch(Exception x) {
            x.printStackTrace();
            Log.e(TAG, "Failed to open uri "+socketioConfig.getUri(), x);
        }

        if(ioSocket != null)
            ioSocket.close();
        return false;
    }
    protected void connect() {
        if(ioSocket.connected())
            return;

        ioSocket.on(Socket.EVENT_CONNECT, args -> {
            Log.i(TAG, "Connect to "+ socketioConfig.getUri());
            ioSocket.emit("add user", getUserId());
        });

        ioSocket.on(Socket.EVENT_RECONNECT, args -> {
            Log.w(TAG, "Reconnect to "+ socketioConfig.getUri());
        });

        ioSocket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.w(TAG, "Disconnect from "+ socketioConfig.getUri());
        });

        ioSocket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            Log.e(TAG, "Error when connecting to "+ socketioConfig.getUri());
        });

        ioSocket.on(Socket.EVENT_CONNECT_TIMEOUT, args -> {
            Log.e(TAG, "Timeout when connecting to "+ socketioConfig.getUri());
        });

        ioSocket.on(Socket.EVENT_RECONNECT_ERROR, args -> {
            Log.e(TAG, "Error when reconnecting to "+ socketioConfig.getUri());
        });

        ioSocket.on(Socket.EVENT_RECONNECT_FAILED, args -> {
            Log.e(TAG, "Failed to reconnect to "+ socketioConfig.getUri());
        });

        ioSocket.on(EventNewMessage.NAME, new EventNewMessage(x -> {
            Log.d(TAG, "Receive new message: "+ x);

            Event event= EventUtils.toEventObject(x.message);
            // FIXME - temp age value
            event.age= 1L;

            callManager.handleCallEvent(event);
        }));

        ioSocket.connect();
    }

    @Override
    public void close() {
        if(ioSocket == null)
            return;

        ioSocket.close();
        ioSocket= null;
    }

    @Override
    public ILocalRoom joinRoom(String roomId) {
        if(!isOpen())
            return null;

        if(roomMap.containsKey(roomId))
            leaveRoom(roomId);

        SocketioRoom room= new SocketioRoom(this, roomId);
        roomMap.put(roomId, room);
        return room;
    }

    @Override
    public void leaveRoom(String roomId) {
        ILocalRoom room= roomMap.remove(roomId);
        if(room != null)
            room.leave();
    }

    @Override
    public ILocalRoom getRoomById(String roomId) {
        if(roomId == null || roomId.isEmpty())
            return null;
        return roomMap.get(roomId);
    }

    @Override
    public boolean sendMessage(String message) {
        if(!hasConnected())
            return false;

        ioSocket.emit(EventNewMessage.NAME, message);
        return true;
    }

    @Override
    public boolean isOpen() {
        return ioSocket != null;
    }

    @Override
    public boolean hasConnected() {
        return ioSocket != null && ioSocket.connected();
    }

    @Override
    public ILocalCallConfig getLocalCallConfig() {
        return socketioConfig;
    }

    // FIXME - not implemented yet
    @Override
    public boolean getCryptoWarnOnUnknownDevices() {
        return false;
    }

    @Override
    public void bindCallManager(ILocalCallsManager callManager) {
        this.callManager= callManager;
    }

}
