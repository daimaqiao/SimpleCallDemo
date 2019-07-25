package com.example.demo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.matrix.androidsdk.call.IMXCallListener;
import org.matrix.androidsdk.crypto.data.MXDeviceInfo;
import org.matrix.androidsdk.crypto.data.MXUsersDevicesMap;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.util.Log;

import local.webrtc.androidsdk.call.ILocalCall;
import local.webrtc.androidsdk.call.ILocalCallsManager;
import local.webrtc.androidsdk.call.ILocalCallsManagerListener;

public class DemoCall implements ILocalCallsManagerListener {
    private static final String TAG= DemoCall.class.getSimpleName();

    public enum Status { INIT, READY, CALL_OUT, CALL_IN, END }
    private Status demoStatus= Status.INIT;
    public Status getDemoStatus() {
        return demoStatus;
    }
    public void setDemoStatus(Status status) {
        demoStatus= status;
    }
    public boolean isStatusReady() {
        return demoStatus == Status.READY;

    }

    public interface IHandleIncomingCall {
        void onIncomingCall(ILocalCall call);
    }
    private IHandleIncomingCall handleIncomingCall= null;
    public void setHandleIncomingCall(IHandleIncomingCall handleIncomingCall) {
        this.handleIncomingCall= handleIncomingCall;
    }

    public interface IHandleCallConnected{
        void onCallConnected();
    }
    private IHandleCallConnected handleCallConnected= null;
    public void setHandleCallConnected(IHandleCallConnected handleCallConnected) {
        this.handleCallConnected= handleCallConnected;
    }

    private ILocalCallsManager localCallsManager;
    public DemoCall(ILocalCallsManager manager) {
        localCallsManager= manager;
        localCallsManager.addListener(this);
    }

    private ILocalCall localCall= null;
    public void callInRoom(ViewGroup videoContianer) {
        AppInstance.getLocalCallsManager().createCallInRoom(
                AppInstance.getRoomId(),
                AppInstance.isVideo(),
                new ApiCallback<ILocalCall>() {
            @Override
            public void onNetworkError(Exception e) {
                Log.e(TAG, " ***** onNetworkError", e);
            }

            @Override
            public void onMatrixError(MatrixError e) {
                Exception x= new Exception(e.toString());
                Log.e(TAG, " ***** onMatrixError", x);
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e(TAG, " ***** onUnexpectedError", e);
            }

            @Override
            public void onSuccess(ILocalCall info) {
                localCall= info;
                localCall.setIsVideo(AppInstance.isVideo());
                localCall.setRooms(AppInstance.getLocalRoom(), AppInstance.getLocalRoom());
                AppInstance.setLocalCall(info);
                createCallView(videoContianer);
            }
        });

    }

    private void createCallView(ViewGroup videoContianer) {
        localCall.addListener(new IMXCallListener() {
            @Override
            public void onStateDidChange(String state) {
                Log.i(TAG, " ***** onStateDidChange="+ state);

                switch (state) {
                    case ILocalCall.CALL_STATE_RINGING:
                        // the call view is created when the user accepts the call.
                        if (localCall.isIncoming()) {
                            localCall.answer();
                        }
                        break;
                    case ILocalCall.CALL_STATE_CONNECTED:
                        if(handleCallConnected != null)
                            handleCallConnected.onCallConnected();
                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onCallError(String error) {
                Log.i(TAG, " ***** onCallError="+ error);
            }

            @Override
            public void onCallViewCreated(View callView) {
                Log.i(TAG, " ***** onCallViewCreated=" + callView);

                if(callView == null)
                    return;
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                if(videoContianer != null) {
                    videoContianer.removeAllViews();
                    videoContianer.addView(callView, 0, layoutParams);
                    callView.setVisibility(View.VISIBLE);
                    videoContianer.bringToFront();
                }
            }

            @Override
            public void onReady() {
                Log.i(TAG, " ***** onReady");

                if(localCall.isIncoming()) {
                    Log.i(TAG, " ***** Incoming call="+ localCall);

                    localCall.launchIncomingCall(null);
                } else {
                    Log.i(TAG, " ***** Outgoing call="+ localCall);

                    localCall.placeCall(null);
                }
            }

            @Override
            public void onCallAnsweredElsewhere() {
                Log.i(TAG, " ***** onCallAnsweredElsewhere");
            }

            @Override
            public void onCallEnd(int aReasonId) {
                Log.i(TAG, " ***** onCallEnd="+ aReasonId);
            }

            @Override
            public void onPreviewSizeChanged(int width, int height) {
                Log.i(TAG, " ***** onPreviewSizeChanged="+ width+ "x"+ height);
            }
        });
        localCall.createCallView();
    }


    public void answerInRoom(ViewGroup videoContianer) {
        createCallView(videoContianer);
    }

    /**
     * Called when there is an incoming call within the room.
     *
     * @param call           the incoming call
     * @param unknownDevices the unknown e2e devices list
     */
    @Override
    public void onIncomingCall(ILocalCall call, MXUsersDevicesMap<MXDeviceInfo> unknownDevices) {
        Log.i(TAG, " ***** onIncomingCall="+ call);

        localCall= call;
        AppInstance.setLocalCall(call);
        if(handleIncomingCall != null)
            handleIncomingCall.onIncomingCall(call);
    }

    /**
     * An outgoing call is started.
     *
     * @param call the outgoing call
     */
    @Override
    public void onOutgoingCall(ILocalCall call) {
        Log.i(TAG, " ***** onOutgoingCall="+ call);
    }

    /**
     * Called when a called has been hung up
     *
     * @param call the incoming call
     */
    @Override
    public void onCallHangUp(ILocalCall call) {
        Log.i(TAG, " ***** onCallHangUp="+ call);

        AppInstance.end();
    }

    /**
     * A voip conference started in a room.
     *
     * @param roomId the room id
     */
    @Override
    public void onVoipConferenceStarted(String roomId) {
        Log.i(TAG, " ***** onVoipConferenceStarted="+ roomId);

    }

    /**
     * A voip conference finished in a room.
     *
     * @param roomId the room id
     */
    @Override
    public void onVoipConferenceFinished(String roomId) {
        Log.i(TAG, " ***** onVoipConferenceFinished="+ roomId);

    }
}
