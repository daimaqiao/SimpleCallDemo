package com.example.demo;

import android.content.Context;

import local.webrtc.androidsdk.call.ILocalCall;
import local.webrtc.androidsdk.call.ILocalCallsManager;
import local.webrtc.androidsdk.signal.ILocalRoom;
import local.webrtc.androidsdk.signal.ILocalSession;

public class AppInstance {
    private static ILocalSession localSession= null;
    public static ILocalSession getLocalSession() {
        return localSession;
    }
    public static void setLocalSession(ILocalSession localSession) {
        AppInstance.localSession= localSession;
    }

    private static ILocalCallsManager localCallsManager= null;
    public static ILocalCallsManager getLocalCallsManager() {
        return localCallsManager;
    }
    public static void setLocalCallsManager(ILocalCallsManager localCallsManager) {
        AppInstance.localCallsManager= localCallsManager;
    }

    private static ILocalRoom localRoom= null;
    public static ILocalRoom getLocalRoom() {
        return localRoom;
    }
    public static void setLocalRoom(ILocalRoom localRoom) {
        AppInstance.localRoom= localRoom;
    }

    private static ILocalCall localCall= null;
    public static ILocalCall getLocalCall() {
        return localCall;
    }
    public static void setLocalCall(ILocalCall localCall) {
        AppInstance.localCall= localCall;
    }


    private static Context appContext= null;
    public static void setAppContext(Context context) {
        AppInstance.appContext= context;
    }
    public static Context getAppContext() {
        return appContext;
    }

    private static DemoCall demoCall= null;
    public static void setDemoCall(DemoCall demoCall) {
        AppInstance.demoCall= demoCall;
    }
    public static DemoCall getDemoCall() {
        return demoCall;
    }

    private static SoundsManager soundsManager= null;
    public static void setSoundsManager(SoundsManager soundsManager) {
        AppInstance.soundsManager= soundsManager;
    }
    public static SoundsManager getSoundsManager() {
        return soundsManager;
    }

    private static boolean video= false;
    public static boolean isVideo() {
        return video;
    }
    public static void setVideo(boolean flag) {
        AppInstance.video= flag;
    }

    private static String roomId= null;
    public static String getRoomId() {
        return roomId;
    }
    public static void setRoomId(String roomId) {
        AppInstance.roomId= roomId;
    }

    private static String userId= null;
    public static String getUserId() {
        return userId;
    }
    public static void setUserId(String userId) {
        AppInstance.userId= userId;
    }


    public static void end() {
        if(localCall != null)
            localCall.hangup("end");
        System.exit(0);
    }

}
