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

    @BindView(R.id.call_speaker_view)
    ImageView callSpeakerView;
    @BindView(R.id.mute_audio)
    ImageView muteAudio;

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

        AppInstance.getSoundsManager().setSpeakerOn(this::updateCallSpeakerView);
        AppInstance.getSoundsManager().setMicMute(this::updateMuteAudio);

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

    @OnClick(R.id.call_speaker_view)
    void onCallSpeaker() {
        AppInstance.getSoundsManager().toggleSpeater();
    }

    @OnClick(R.id.mute_audio)
    void onMuteAudio() {
        if(true) {
            Toast.makeText(this, "麦克风静音功能暂不可用 :-(", Toast.LENGTH_SHORT).show();
            return;
        }
        AppInstance.getSoundsManager().toggleMicMute();
    }

    @OnClick(R.id.call_other_member)
    void onTest() {
        AppInstance.getLocalRoom().sendText(new Date().toString());

    }

    private void updateCallSpeakerView(boolean on) {
        int iconId = on ? R.mipmap.ic_material_speaker_phone_pink_red
                : R.mipmap.ic_material_speaker_phone_grey;
        callSpeakerView.setImageResource(iconId);
        Toast.makeText(this, "外放："+ (on?"开":"关"), Toast.LENGTH_SHORT).show();
    }
    private void updateMuteAudio(boolean mute) {
        int iconId = mute ? R.mipmap.ic_material_mic_off_pink_red
                : R.mipmap.ic_material_mic_off_grey;
        muteAudio.setImageResource(iconId);
        Toast.makeText(this, "麦克风："+ (mute?"静音（可能无效）":"恢复"), Toast.LENGTH_SHORT).show();
    }

}

