package com.example.vpnwithoutndk;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalVpnService extends VpnService {

    private static final String TAG = LocalVpnService.class.getSimpleName();
    private static final String VPN_ADDRESS = "10.0.0.2";
    // private static final String VPN_ADDRESS_IPV6 ="0000:0000:0000:0000:0000:ffff:0a00:0002";

    private static final String VPN_ROUTE = "0.0.0.0";

    public static final String BROADCAST_VPN_STATE = "com.example.vpnwithoutndk.VPN_STATE";
    public static final String ACTION_DISCONNECT = "com.example.vpnwithoutndk.STOP";


    String[] appPackages = {
            /*"com.example.vpnwithoutndk",*/
            "com.google.android.youtube"
    };
    private static Boolean isRunning = false;

    private ParcelFileDescriptor vpnInterface = null;


    private PendingIntent pendingIntent;

    private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
    private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
    private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;
    private ExecutorService executorService;

    private Selector udpSelector;
    private Selector tcpSelector;

    @Override
    public void onCreate() {
        super.onCreate();

        isRunning = true;
        setupVPN();
        try {
            udpSelector = Selector.open();
            tcpSelector = Selector.open();
            deviceToNetworkUDPQueue = new ConcurrentLinkedQueue<>();
            deviceToNetworkTCPQueue = new ConcurrentLinkedQueue<>();
            networkToDeviceQueue = new ConcurrentLinkedQueue<>();
            if (deviceToNetworkTCPQueue.iterator().hasNext()) {
                System.out.println("deviceTONetwork" + deviceToNetworkTCPQueue.toArray());
            }

            for (Packet packet : deviceToNetworkTCPQueue) {
                ByteBuffer backingBuffer = packet.backingBuffer;
                Packet.IP4Header ip4Header = packet.ip4Header;
                Packet.TCPHeader tcpHeader = packet.tcpHeader;

                System.out.println("data" + backingBuffer + " " + ip4Header + " " + tcpHeader);

            }

            executorService = Executors.newFixedThreadPool(25);
            executorService.submit(new UDPInput(networkToDeviceQueue, udpSelector));
            executorService.submit(new UDPOutput(deviceToNetworkUDPQueue, udpSelector, this));
            executorService.submit(new TCPInput(networkToDeviceQueue, tcpSelector));
            executorService.submit(new TCPOutput(deviceToNetworkTCPQueue, networkToDeviceQueue, tcpSelector, this));
            executorService.submit(new VPNRunnable(vpnInterface.getFileDescriptor(),
                    deviceToNetworkUDPQueue, deviceToNetworkTCPQueue, networkToDeviceQueue));
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_VPN_STATE)
                    .putExtra("running", true));
            Log.i(TAG, "Started");
        } catch (IOException e) {
            // TODO: Here and elsewhere, we should explicitly notify the user of any errors
            // and suggest that they stop the service, since we can't do it ourselves
            Log.e(TAG, "Error starting service", e);
            cleanup();
        }

        MainActivity mainActivity = new MainActivity();
        mainActivity.onBindListner(new StopListenrService() {
            @Override
            public void onItemClick() {
                VpnService.Builder builder;


                disconnect();
            }
        });


    }
/*
    @Override
    public boolean handleMessage(Message message) {
        Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updateForegroundNotification(message.what);
        }

        return true;
    }*/

    private void setupVPN() {
        if (vpnInterface == null) {

            PackageManager packageManager = getPackageManager();

            Builder builder = new Builder();
            builder.addAddress(VPN_ADDRESS, 32);
            //builder.addAddress(VPN_ADDRESS_IPV6,128 );
            builder.addRoute(VPN_ROUTE, 0);
            // builder.allowBypass();
         /*  for (String appPackage: appPackages) {
                try {
                    packageManager.getPackageInfo(appPackage, 0);
                    builder.addAllowedApplication(appPackage);
                } catch (PackageManager.NameNotFoundException e) {
                    // The app isn't installed.
                }
            }*/
            vpnInterface = builder
                    .setSession(getString(R.string.app_name))
                    .setConfigureIntent(pendingIntent)
                    .establish();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                updateForegroundNotification(0);
            }
        }
    }


    private static class VPNRunnable implements Runnable {
        private static final String TAG = "vpnRunnable";

        private FileDescriptor vpnFileDescriptor;

        private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
        private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
        private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;

        public VPNRunnable(FileDescriptor vpnFileDescriptor,
                           ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue,
                           ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue,
                           ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue) {
            this.vpnFileDescriptor = vpnFileDescriptor;
            this.deviceToNetworkUDPQueue = deviceToNetworkUDPQueue;
            this.deviceToNetworkTCPQueue = deviceToNetworkTCPQueue;
            this.networkToDeviceQueue = networkToDeviceQueue;
        }

        @Override
        public void run() {
            Log.i(TAG, "Started");

            FileChannel vpnInput = new FileInputStream(vpnFileDescriptor).getChannel();
            FileChannel vpnOutput = new FileOutputStream(vpnFileDescriptor).getChannel();


            try {
                System.out.println("vpninput"+ vpnInput.size());
            } catch (IOException e) {
                e.printStackTrace();
            }


            try {
                ByteBuffer bufferToNetwork = null;
                boolean dataSent = true;
                boolean dataReceived;
                while (!Thread.interrupted()) {
                    if (dataSent)
                        bufferToNetwork = ByteBufferPool.acquire();
                    else
                        bufferToNetwork.clear();
                    // TODO: Block when not connected
                    int readBytes = vpnInput.read(bufferToNetwork);



                    if (readBytes > 0) {
                        dataSent = true;
                        bufferToNetwork.flip();
                        Packet packet = new Packet(bufferToNetwork);



                        if (packet.isUDP()) {
                            deviceToNetworkUDPQueue.offer(packet);
                        } else if (packet.isTCP()) {
                            deviceToNetworkTCPQueue.offer(packet);
                        } else {
                            Log.w(TAG, "Unknown packet type");
                            Log.w(TAG, packet.ip4Header.toString());
                            dataSent = false;
                        }
                    } else {
                        dataSent = false;
                    }

                    ByteBuffer bufferFromNetwork = networkToDeviceQueue.poll();
                    if (bufferFromNetwork != null) {
                        bufferFromNetwork.flip();
                        while (bufferFromNetwork.hasRemaining()) {
                            vpnOutput.write(bufferFromNetwork);// close here for block all types of network call
                        }
                        dataReceived = true;

                        ByteBufferPool.release(bufferFromNetwork);
                    } else {
                        dataReceived = false;
                    }

                    // TODO: Sleep-looping is not very battery-friendly, consider blocking instead
                    // Confirm if throughput with ConcurrentQueue is really higher compared to BlockingQueue
                    if (!dataSent && !dataReceived)
                        Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Log.i(TAG, "Stopping");
            } catch (IOException e) {
                Log.w(TAG, e.toString(), e);
            } finally {
                closeResources(vpnInput, vpnOutput);
            }
        }
    }

    private static void closeResources(Closeable... resources) {
        for (Closeable resource : resources) {
            try {
                resource.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void cleanup() {
        deviceToNetworkTCPQueue = null;
        deviceToNetworkUDPQueue = null;
        networkToDeviceQueue = null;
        ByteBufferPool.clear();
        closeResources(udpSelector, tcpSelector, vpnInterface);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public static boolean isRunning() {
        return isRunning;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        executorService.shutdownNow();
        cleanup();
        Log.i(TAG, "Stopped");
    }

    private void disconnect() {
        isRunning = false;

        executorService.shutdownNow();
        cleanup();
        stopForeground(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateForegroundNotification(final int message) {
        final String NOTIFICATION_CHANNEL_ID = "Vpn";
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        mNotificationManager.createNotificationChannel(new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT));
        startForeground(1, new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                //.setContentText(getString(message))
                .setContentIntent(pendingIntent)
                .build());
    }


}
