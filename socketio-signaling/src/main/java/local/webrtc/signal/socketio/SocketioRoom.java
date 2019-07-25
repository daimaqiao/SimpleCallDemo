package local.webrtc.signal.socketio;

import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.Event;

import local.webrtc.androidsdk.call.ILocalCall;
import local.webrtc.androidsdk.signal.ILocalRoom;
import local.webrtc.androidsdk.signal.ILocalSession;

public class SocketioRoom implements ILocalRoom {
    protected boolean leftFlag= false;
    protected ILocalCall localCall= null;

    final private ILocalSession session;
    final private String roomId;
    public SocketioRoom(ILocalSession session, String roomId) {
        this.session= session;
        this.roomId= roomId;
    }

    @Override
    public void sendEvent(Event event, ApiCallback<Void> callback) {
        session.sendMessage(event.toJsonObject().toString());
        callback.onSuccess(null);
    }

    @Override
    public void onEventReceived(String from, Event event) {
        if(leftFlag || localCall == null)
            return;
        if(event.isCallEvent()) {
            localCall.handleCallEvent(event);

            //FIXME - debug
            sendText("copy event "+ event.eventId);
        }
    }

    @Override
    public void leave() {
        leftFlag= true;
        session.leaveRoom(roomId);
    }

    @Override
    public boolean isReady() {
        if(hasLeft())
            return false;
        return session != null && session.hasConnected();
    }

    @Override
    public boolean hasLeft() {
        return leftFlag;
    }

    @Override
    public void bindCall(ILocalCall call) {
        localCall= call;
    }

    // FIXME - not implemented yet
    @Override
    public int getNumberOfJoinedMembers() {
        return 2;
    }

    // FIXME - not implementied yet
    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    public String getRoomId() {
        return roomId;
    }

    @Override
    public void sendText(String text) {
        session.sendMessage(text);
    }

}
