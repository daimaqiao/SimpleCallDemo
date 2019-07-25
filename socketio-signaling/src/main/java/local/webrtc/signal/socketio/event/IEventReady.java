package local.webrtc.signal.socketio.event;

public interface IEventReady<T> {
    void onEvent(T t);
}
