package com.example.demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CallViewActivity extends AppCompatActivity {
    private static final String TAG= CallViewActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_view);

        ButterKnife.bind(this);
        initUI();
    }

    @BindView(R.id.call_layout)
    ViewGroup callLayout;

    @BindView(R.id.call_other_member)
    ImageView callOtherMember;

    @BindView(R.id.call_menu_buttons_layout_container)
    ViewGroup callButtonsLayout;

    @BindView(R.id.incoming_call_menu_buttons_layout_container)
    ViewGroup callIncomingLayout;

    @BindView(R.id.cancel_and_goback)
    View cancelAndGoback;

    @BindView(R.id.call_view_container)
    ViewGroup callViewContainer;

    enum Status { INIT, OUTGOING, INCOMING, ACCEPTED, END }
    void updateStatus(Status status) {
        cancelAndGoback.setVisibility(View.VISIBLE);
        callOtherMember.setVisibility(View.GONE);
        callIncomingLayout.setVisibility(View.GONE);
        callButtonsLayout.setVisibility(View.GONE);
        callViewContainer.setVisibility(View.GONE);

        switch(status) {
            case INIT:
                setTitle("Demo: CallView");
                break;
            case OUTGOING:
                setTitle("Demo: Outgoing ...");
                cancelAndGoback.setVisibility(View.VISIBLE);
                callOtherMember.setVisibility(View.VISIBLE);
                break;
            case INCOMING:
                setTitle("Demo: Incoming ...");
                cancelAndGoback.setVisibility(View.GONE);
                callIncomingLayout.setVisibility(View.VISIBLE);
                callOtherMember.setVisibility(View.VISIBLE);
                break;
            case ACCEPTED:
                setTitle("Demo: Accepted!");
                cancelAndGoback.setVisibility(View.GONE);
                callButtonsLayout.setVisibility(View.VISIBLE);
                callOtherMember.setVisibility(View.VISIBLE);
                callViewContainer.setVisibility(View.VISIBLE);
                break;
            case END:
                setTitle("Demo: End!");
                break;
        }
    }

    void initUI() {
        final Intent intent= getIntent();
        Status status= Status.INIT;
        if(intent.hasExtra("status")) {
            Serializable got= intent.getSerializableExtra("status");
            if(got instanceof Status)
                status= (Status)got;
        }

        updateStatus(status);
        if(status == Status.OUTGOING)
            callInRoom();

        AppInstance.getDemoCall().setHandleCallConnected(()-> updateStatus(Status.ACCEPTED));
    }

    void callInRoom() {
        AppInstance.getDemoCall().callInRoom(null);

    }

    @OnClick(R.id.cancel_and_goback)
    void cancelAndGoback() {
        AppInstance.end();
    }

    @OnClick(R.id.accept_incoming_call)
    void onAcceptCall() {
        updateStatus(Status.ACCEPTED);

        AppInstance.getDemoCall().answerInRoom(null);

        Toast.makeText(this, "接听通话", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.reject_incoming_call)
    void onRejectCall() {
        updateStatus(Status.END);
        AppInstance.end();
    }

    @OnClick(R.id.hang_up_button)
    void onHangup() {
        updateStatus(Status.END);
        AppInstance.end();
    }

    @OnClick(R.id.room_chat_link)
    void onRoomChat() {

        Toast.makeText(this, "即将开通：打开文字交流", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.call_speaker_view)
    void onCallSpeaker() {

        Toast.makeText(this, "即将开通：使用外放音", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.call_switch_camera_view)
    void onSwitchCamara() {

        Toast.makeText(this, "即将开通：切换摄像头", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.start_video_call)
    void onStartVideo() {

        Toast.makeText(this, "即将开通：开启视频通话", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.mute_local_camera)
    void onMuteCamara() {

        Toast.makeText(this, "即将开通：关摄像头", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.mute_audio)
    void onMuteAudio() {

        Toast.makeText(this, "即将开通：本麦克风", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.call_other_member)
    void onTest() {
        AppInstance.getLocalRoom().sendText(new Date().toString());

    }

}

