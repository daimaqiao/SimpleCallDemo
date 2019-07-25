package local.webrtc.signal.socketio.event;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import io.socket.emitter.Emitter;

public class EventNewMessage implements Emitter.Listener {
    public static final String NAME = "new message";

    private final IEventReady<EventNewMessage> next;
    public EventNewMessage(IEventReady<EventNewMessage> next) {
        this.next= next;
    }

    public String username;
    public String message;

    public boolean check() {
        return !username.isEmpty() && !message.isEmpty();
    }

    @Override
    public void call(Object... args) {
        JSONObject jsonObject = (JSONObject) args[0];
        username = jsonObject.optString("username");
        message = jsonObject.optString("message");
        if(check() && next != null)
            next.onEvent(this);
    }

    @NotNull
    public String toString() {
        return String.format("username=%s, message=%s",
                username==null? "": username,
                message==null? "": message);
    }

}
