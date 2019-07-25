package com.example.demo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.demo.util.Permissions;

import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import local.webrtc.androidsdk.call.ILocalCall;
import local.webrtc.androidsdk.call.ILocalCallsManager;
import local.webrtc.androidsdk.call.LocalCallsManager;
import local.webrtc.androidsdk.signal.ILocalRoom;
import local.webrtc.androidsdk.signal.ILocalSession;
import local.webrtc.signal.socketio.SocketioSession;
import local.webrtc.signal.socketio.config.SocketioConfig;

public class MainActivity extends AppCompatActivity {
    private static final String TAG= MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        initUi();
    }

    @SuppressLint("SetTextI18n")
    void initUi() {
        AppInstance.setAppContext(getApplicationContext());

        buttonLogin.setVisibility(View.VISIBLE);
        buttonEnd.setVisibility(View.VISIBLE);
        buttonCallInRoom.setVisibility(View.GONE);

        String userId= editUserId.getText().toString();
        int n= new Random().nextInt(100);
        editUserId.setText(userId+n);
    }

    @BindView(R.id.textTargetUri)
    TextView textTargetUri;

    @BindView(R.id.editUserId)
    EditText editUserId;
    @BindView(R.id.editRoomId)
    EditText editRoomId;

    @BindView(R.id.buttonLogin)
    Button buttonLogin;
    @OnClick(R.id.buttonLogin)
    void onButtonLogin() {
        if(!Permissions.requestAudioPermissions(this)) {
            Toast.makeText(this, "需要授予麦克风权限！", Toast.LENGTH_SHORT).show();
            return;
        }

        editUserId.setEnabled(false);
        editRoomId.setEnabled(false);

        String target= textTargetUri.getText().toString();
        String userId= editUserId.getText().toString();
        String roomId= editRoomId.getText().toString();

        AppInstance.setUserId(userId);
        AppInstance.setRoomId(roomId);

        SocketioConfig demoConfig= new SocketioConfig(target, userId);
        ILocalSession session= new SocketioSession(demoConfig);
        session.open();

        ILocalRoom room= session.joinRoom(roomId);
        ILocalCallsManager manager = new LocalCallsManager(session, getApplicationContext());

        AppInstance.setLocalSession(session);
        AppInstance.setLocalRoom(room);
        AppInstance.setLocalCallsManager(manager);

        DemoCall demoCall= new DemoCall(manager);
        AppInstance.setDemoCall(demoCall);

        buttonLogin.setEnabled(false);
        buttonCallInRoom.setVisibility(View.VISIBLE);

        demoCall.setHandleIncomingCall(this::onIncomingCall);

        if(room != null)
            demoCall.setDemoStatus(DemoCall.Status.READY);
        else
            Toast.makeText(this, "未连接到服务器，请检查网络！", Toast.LENGTH_SHORT).show();
    }


    @BindView(R.id.buttonCallInRoom)
    Button buttonCallInRoom;
    @OnClick(R.id.buttonCallInRoom)
    void onButtonCallInRoom() {
        AppInstance.getDemoCall().setDemoStatus(DemoCall.Status.CALL_OUT);
        buttonCallInRoom.setEnabled(false);

        final Intent intent = new Intent(this, CallViewActivity.class);
        intent.putExtra("status", CallViewActivity.Status.OUTGOING);
        startActivity(intent);
    }

    private void onIncomingCall(ILocalCall call) {
        AppInstance.setLocalCall(call);
        AppInstance.getDemoCall().setDemoStatus(DemoCall.Status.CALL_IN);
        buttonCallInRoom.setEnabled(false);

        final Intent intent = new Intent(this, CallViewActivity.class);
        intent.putExtra("status", CallViewActivity.Status.INCOMING);
        startActivity(intent);
    }

    @BindView(R.id.buttonEnd)
    Button buttonEnd;
    @OnClick(R.id.buttonEnd)
    void onButtonEnd() {
        AppInstance.end();
    }

}
