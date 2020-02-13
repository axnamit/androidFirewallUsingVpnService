package com.example.vpnwithoutndk;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    public StopListenrService stopListenrService1;

    private static final int VPN_REQUEST_CODE = 0x0F;

    private boolean waitingForVPNStart;

    private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocalVpnService.BROADCAST_VPN_STATE.equals(intent.getAction())) {
                if (intent.getBooleanExtra("running", false))
                    waitingForVPNStart = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button vpnButton = findViewById(R.id.button);
        Button vpnStopButton = findViewById(R.id.button2);
        vpnButton.setOnClickListener(v -> startVPN());
        vpnStopButton.setOnClickListener(v ->
                startService(getServiceIntent().setAction(LocalVpnService.ACTION_DISCONNECT)));

        waitingForVPNStart = false;
        LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver,
                new IntentFilter(LocalVpnService.BROADCAST_VPN_STATE));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            waitingForVPNStart = true;
            startService(new Intent(this, LocalVpnService.class));
            enableButton(false);
        }
    }

    private void startVPN() {
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null)
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        else
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        enableButton(!waitingForVPNStart && !LocalVpnService.isRunning());
    }

    private void enableButton(boolean enable) {
        final Button vpnButton = findViewById(R.id.button);
        if (enable) {
            vpnButton.setEnabled(true);
            vpnButton.setText("start");
        } else {
            vpnButton.setEnabled(false);
            vpnButton.setText("started");
        }
    }

    public void onBindListner(StopListenrService stopListenrService) {
        this.stopListenrService1 = stopListenrService;
    }

    private Intent getServiceIntent() {
        return new Intent(this, LocalVpnService.class);
    }


}
