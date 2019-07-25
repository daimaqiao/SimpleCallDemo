package local.webrtc.signal.socketio;

import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.util.JsonUtils;

public class EventUtils {

    public static Event toEventObject(String jsonText) {
        if(jsonText != null && !jsonText.isEmpty())
            return JsonUtils.toClass(jsonText, Event.class);
        return new Event();
    }

}
