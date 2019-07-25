package com.example.demo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;

import org.matrix.androidsdk.call.CallSoundsManager;
import org.matrix.androidsdk.util.Log;

public class SoundsManager {
    private static final String TAG= SoundsManager.class.getSimpleName();

    public interface ISpeakerOn {
        void onSpeaterOn(boolean on);
    }
    public interface IMicMute {
        void onMicMute(boolean mute);
    }

    private ISpeakerOn speakerOn;
    public void setSpeakerOn(ISpeakerOn speakerOn) {
        this.speakerOn= speakerOn;
    }
    private IMicMute micMute;
    public void setMicMute(IMicMute micMute) {
        this.micMute= micMute;
    }

    private final CallSoundsManager callSoundsManager;
    private final AudioManager audioManager;
    public SoundsManager(Context context) {
        callSoundsManager= CallSoundsManager.getSharedInstance(context);

        audioManager= (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void toggleSpeater() {
        if(AppInstance.getLocalCall() == null) {
            Log.w(TAG, "Should not toggle speaker without a local call!");
            return;
        }

        callSoundsManager.toggleSpeaker();
        if(speakerOn != null)
            speakerOn.onSpeaterOn(isSpeakerOn());
    }

    public void toggleMicMute() {
        if(AppInstance.getLocalCall() == null) {
            Log.w(TAG, "Should not toggle mic mute without a local call!");
            return;
        }

        // FIXME - dos'nt work
        boolean mute= isMicMute();
        callSoundsManager.setMicrophoneMute(!mute);
        if(micMute != null)
            micMute.onMicMute(isMicMute());
    }

    public boolean isSpeakerOn() {
        return callSoundsManager.isSpeakerphoneOn();
    }

    public boolean isMicMute() {
        return callSoundsManager.isMicrophoneMute();
    }

    public boolean isHeadsetPlugged() {
        if(audioManager.isWiredHeadsetOn())
            return true;
        return isBTHeadsetPlugged();
    }

    public boolean isBTHeadsetPlugged() {
        return  BluetoothAdapter.getDefaultAdapter()
                    .getProfileConnectionState(BluetoothProfile.HEADSET)
                == BluetoothAdapter.STATE_CONNECTED;
    }
}
