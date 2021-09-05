package com.xiaomi.mico.romupdate;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.os.SystemProperties;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.text.TextUtils;
import com.elvishew.xlog.XLog;
import com.google.gson.JsonSyntaxException;
import com.xiaomi.mico.romupdate.IRomUpdateService;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

public class RomUpdateService extends Service {
    public static final String ACTION_ROM_UPDATE_EVENT = "com.xiaomi.mico.romupdate.event";
    private static final String CHANNEL_ID_FOREGROUND_SERVICE = "com.xiaomi.mico.romupdate.channelId";
    private static final String CHANNEL_NAME_FOREGROUND_SERVICE = "rom_update_service_notification_channel";
    private static final int DELAY_AFTER_BOOT_COMPLETED = 180000;
    private static final int DELAY_AFTER_NETWORK_AVAILABLE = 180000;
    private static final int DELAY_AFTER_PROCESS_STARTUP = 180000;
    private static final int DELAY_AFTER_SCREEN_ON = 60000;
    private static final int DELAY_CHECK_PERIOD = 14400000;
    private static final int DELAY_FIRST_TRY_SILENT_UPDATE = 60000;
    private static final int DELAY_NO_DELAY = 0;
    private static final int DELAY_RETRY_SHOWING_NEW_VERSION = 600000;
    private static final int DELAY_RETRY_SILENT_UPDATE = 3600000;
    public static final int ERROR_CHECK_UPDATE_FAILED = -1;
    public static final int ERROR_INVALID_HTTP_RESPONSE = -2;
    public static final int ERROR_NETWORK_ERROR = -6;
    public static final int ERROR_NETWORK_NOT_AVAILABLE = -4;
    public static final int ERROR_NETWORK_TIMEOUT = -5;
    public static final int ERROR_NEW_VERSION_UNAVAILABLE = -3;
    public static final int ERROR_NO_ERROR = 0;
    public static final int EVENT_CHECK_END = 2;
    public static final int EVENT_CHECK_START = 1;
    public static final String EXTRA_NEW_VERSION_NAME = "new_version_name";
    public static final String EXTRA_ROM_UPDATE_ERROR = "rom_update_error";
    public static final String EXTRA_ROM_UPDATE_EVENT_TYPE = "rom_update_event_type";
    public static final String EXTRA_ROM_UPDATE_LOG = "rom_update_log";
    public static final String EXTRA_ROM_UPDATE_SILENT = "rom_update_silent";
    public static final String EXTRA_ROM_UPDATE_STATE = "rom_update_state";
    private static final long HOUR_MILLISECONDS = TimeUnit.SECONDS.toMillis(HOUR_SECONDS);
    private static final long HOUR_SECONDS = TimeUnit.HOURS.toSeconds(1);
    private static final int MSG_BOOT_COMPLETED = 2;
    private static final int MSG_CANCEL_UPDATE = 6;
    private static final int MSG_CHECK_UPDATE = 4;
    private static final int MSG_DO_SILENT_UPDATE = 10;
    private static final int MSG_FORCE_UPDATE = 7;
    private static final int MSG_NETWORK_AVAILABLE = 3;
    private static final int MSG_PERFORM_UPDATE = 5;
    private static final int MSG_PROCESS_STARTUP = 1;
    private static final int MSG_REBOOT = 8;
    private static final int MSG_TRY_SHOWING_NEW_VERSION = 11;
    private static final int MSG_TRY_SILENT_UPDATE = 9;
    public static final String REASON_BOOT_COMPLETED = "BOOT_COMPLETED";
    public static final String REASON_FORCE_UPDATE = "FORCE_UPDATE";
    public static final String REASON_NETWORK_CONNECTED = "NETWORK_CONNECTED";
    public static final String REASON_PERIODIC_CHECK = "PERIODIC_CHECK";
    public static final String REASON_PROCESS_STARTUP = "PROCESS_STARTUP";
    public static final String REASON_SILENT_UPDATE = "SILENT_UPDATE";
    public static final String REASON_UNKNOWN = "UNKNOWN";
    public static final String REASON_USER_REQUEST = "USER_REQUEST";
    private static final int SILENT_UPDATE_BEGIN_HOUR = 3;
    private static final int SILENT_UPDATE_END_HOUR = 5;
    private static final String SSE_MSG_ALREADY_PROCESSING_UPDATE = "Already processing an update, cancel it first.";
    private static final String SSE_MSG_NO_ONGOING_UPDATE_TO_CANCEL = "No ongoing update to cancel.";
    private static final String SSE_MSG_WAITING_FOR_REBOOT = "An update already applied, waiting for reboot";
    private static final String TAG = "RomUpdateService: ";
    private final IRomUpdateService.Stub binder = new IRomUpdateService.Stub() {
        /* class com.xiaomi.mico.romupdate.RomUpdateService.AnonymousClass1 */

        @Override // com.xiaomi.mico.romupdate.IRomUpdateService
        public void checkUpdate() {
            RomUpdateService.this.sendCheckMessage(RomUpdateService.REASON_USER_REQUEST, 0);
        }

        @Override // com.xiaomi.mico.romupdate.IRomUpdateService
        public void registerCheckUpdateListener(ICheckUpdateListener iCheckUpdateListener) {
            XLog.d("RomUpdateService: registerCheckUpdateListener, listener = " + iCheckUpdateListener);
            synchronized (RomUpdateService.this.checkUpdateListenerList) {
                if (!RomUpdateService.this.checkUpdateListenerList.contains(iCheckUpdateListener)) {
                    RomUpdateService.this.checkUpdateListenerList.add(iCheckUpdateListener);
                }
            }
        }

        @Override // com.xiaomi.mico.romupdate.IRomUpdateService
        public void unregisterCheckUpdateListener(ICheckUpdateListener iCheckUpdateListener) {
            XLog.d("RomUpdateService: unregisterCheckUpdateListener, listener = " + iCheckUpdateListener);
            synchronized (RomUpdateService.this.checkUpdateListenerList) {
                if (RomUpdateService.this.checkUpdateListenerList.contains(iCheckUpdateListener)) {
                    RomUpdateService.this.checkUpdateListenerList.remove(iCheckUpdateListener);
                }
            }
        }

        @Override // com.xiaomi.mico.romupdate.IRomUpdateService
        public void forceUpdate(String str) {
            Message obtainMessage = RomUpdateService.this.updateServiceHandler.obtainMessage();
            obtainMessage.what = 7;
            obtainMessage.obj = str;
            RomUpdateService.this.updateServiceHandler.sendMessage(obtainMessage);
        }

        @Override // com.xiaomi.mico.romupdate.IRomUpdateService
        public void performUpdate() {
            RomUpdateService.this.sendPerformUpdateMessage(RomUpdateService.REASON_USER_REQUEST, 0);
        }

        @Override // com.xiaomi.mico.romupdate.IRomUpdateService
        public void cancelUpdate() {
            Message obtainMessage = RomUpdateService.this.updateServiceHandler.obtainMessage();
            obtainMessage.what = 6;
            RomUpdateService.this.updateServiceHandler.sendMessage(obtainMessage);
        }

        @Override // com.xiaomi.mico.romupdate.IRomUpdateService
        public void registerUpdateListener(IUpdateListener iUpdateListener) {
            XLog.d("RomUpdateService: registerUpdateListener, listener = " + iUpdateListener);
            synchronized (RomUpdateService.this.updateListenerList) {
                if (!RomUpdateService.this.updateListenerList.contains(iUpdateListener)) {
                    RomUpdateService.this.updateListenerList.add(iUpdateListener);
                }
            }
        }

        @Override // com.xiaomi.mico.romupdate.IRomUpdateService
        public void unregisterUpdateListener(IUpdateListener iUpdateListener) {
            XLog.d("RomUpdateService: unregisterUpdateListener, listener = " + iUpdateListener);
            synchronized (RomUpdateService.this.updateListenerList) {
                if (RomUpdateService.this.updateListenerList.contains(iUpdateListener)) {
                    RomUpdateService.this.updateListenerList.remove(iUpdateListener);
                }
            }
        }
    };
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        /* class com.xiaomi.mico.romupdate.RomUpdateService.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) {
                XLog.e("RomUpdateService: null action is received.");
                return;
            }
            XLog.d(RomUpdateService.TAG + intent.getAction() + " is received by broadcastReceiver in RomUpdateService.");
            String action = intent.getAction();
            char c = 65535;
            int hashCode = action.hashCode();
            if (hashCode != -2128145023) {
                if (hashCode != -1454123155) {
                    if (hashCode == -1172645946 && action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                        c = 0;
                    }
                } else if (action.equals("android.intent.action.SCREEN_ON")) {
                    c = 1;
                }
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                c = 2;
            }
            if (c != 0) {
                if (c == 1) {
                    RomUpdateService.this.isScreenOn = true;
                } else if (c == 2) {
                    RomUpdateService.this.isScreenOn = false;
                }
            } else if (Utils.isNetworkAvailable()) {
                XLog.d("RomUpdateService: Network becomes available.");
                RomUpdateService.this.isNetworkAvailable = true;
                RomUpdateService.this.updateServiceHandler.sendEmptyMessage(3);
            } else {
                RomUpdateService.this.isNetworkAvailable = false;
            }
        }
    };
    private ArrayList<ICheckUpdateListener> checkUpdateListenerList = new ArrayList<>();
    private String currentVersionDescription = "";
    private long downloadBeginTs = 0;
    private long downloadEndTs = 0;
    private long downloadSpeed = 0;
    private long finalizeBeginTs = 0;
    private long finalizeEndTs = 0;
    private long finalizeSpeed = 0;
    private boolean forceUpdate = false;
    private HandlerThread handlerThread = null;
    private HttpClient httpClient = new HttpClient();
    private boolean isNetworkAvailable = true;
    private boolean isScreenOn = true;
    private String newVersionDescription = "";
    private NewVersionInfo newVersionInfo = new NewVersionInfo();
    private String newVersionName = "";
    private String newVersionUrl = "";
    private String[] payloadMetadata = null;
    private long payloadSize = 0;
    private RomUpdatePreferences preferences = RomUpdatePreferences.getInstance();
    private int serviceError = 0;
    private RomUpdateState serviceState = RomUpdateState.Idle;
    private boolean silentUpdate = false;
    private UpdateEngine updateEngine = new UpdateEngine();
    private RomUpdateEngineCallback updateEngineCallback = new RomUpdateEngineCallback();
    private int updateEngineStatus = 0;
    private ArrayList<IUpdateListener> updateListenerList = new ArrayList<>();
    private UpdateServiceHandler updateServiceHandler = null;
    private long verifyBeginTs = 0;
    private long verifyEndTs = 0;
    private long verifySpeed = 0;

    public enum RomUpdateEventType {
        CheckingUpdateStarted,
        CheckingUpdateCompleted,
        UpdatingStarted,
        UpdatingStateChanged,
        UpdatingCompleted,
        UpdatingFailed,
        CancellingUpdateStarted,
        CancellingUpdateCompleted,
        RebootStarted
    }

    public enum RomUpdateState {
        Idle,
        CheckingUpdate,
        StartingUpdate,
        Downloading,
        Verifying,
        Finalizing,
        CancellingUpdate,
        Rebooting,
        Disabled
    }

    private void onStartForeground() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            if (notificationManager.getNotificationChannel(CHANNEL_ID_FOREGROUND_SERVICE) == null) {
                notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID_FOREGROUND_SERVICE, CHANNEL_NAME_FOREGROUND_SERVICE, 2));
            }
            Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID_FOREGROUND_SERVICE);
            builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, RomUpdateActivity.class), 0));
            builder.setContentText("");
            builder.setContentTitle("");
            builder.setSmallIcon(R.drawable.ic_launcher_foreground);
            startForeground(1, builder.build());
        }
    }

    public void onCreate() {
        super.onCreate();
        this.preferences.load();
        this.handlerThread = new HandlerThread("MicoRomUpdateServiceHandlerThread");
        this.handlerThread.start();
        this.updateServiceHandler = new UpdateServiceHandler(this.handlerThread.getLooper());
        if (this.updateEngine == null) {
            XLog.d("RomUpdateService: updateEngine is null, create it.");
            this.updateEngine = new UpdateEngine();
        }
        if (this.updateEngineCallback == null) {
            XLog.d("RomUpdateService: updateEngineCallback is null, create it.");
            this.updateEngineCallback = new RomUpdateEngineCallback();
        }
        this.updateEngine.bind(this.updateEngineCallback);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        registerReceiver(this.broadcastReceiver, intentFilter);
    }

    public void onDestroy() {
        unregisterReceiver(this.broadcastReceiver);
        try {
            this.handlerThread.join();
        } catch (Exception e) {
            XLog.e("RomUpdateService: handlerThread.join() throws exception: " + e);
        }
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        String action = intent != null ? intent.getAction() : null;
        XLog.d("RomUpdateService: RomUpdateService is started by action " + action);
        if (Constants.ACTION_PROCESS_STARTUP.equals(action)) {
            XLog.d("RomUpdateService: process startup, pid = " + Process.myPid());
            onProcessStartup();
            return 1;
        } else if (Constants.ACTION_OS_BOOT_COMPLETED.equals(action)) {
            XLog.d("RomUpdateService: OS boot is completed.");
            onBootCompleted();
            return 1;
        } else if (Constants.ACTION_CHECK_UPDATE.equals(action)) {
            sendCheckMessage(intent.getStringExtra(Constants.EXTRA_CHECK_UPDATE_REASON), 0);
            return 1;
        } else if (Constants.ACTION_FORCE_UPDATE.equals(action)) {
            Message obtainMessage = this.updateServiceHandler.obtainMessage();
            obtainMessage.what = 7;
            obtainMessage.obj = intent.getStringExtra(Constants.EXTRA_FORCE_UPDATE_INFO);
            this.updateServiceHandler.sendMessage(obtainMessage);
            return 1;
        } else if (!Constants.ACTION_CANCEL_UPDATE.equals(action)) {
            return 1;
        } else {
            Message obtainMessage2 = this.updateServiceHandler.obtainMessage();
            obtainMessage2.what = 6;
            this.updateServiceHandler.sendMessage(obtainMessage2);
            return 1;
        }
    }

    private void onProcessStartup() {
        XLog.d("RomUpdateService: onProcessStartup");
        this.updateServiceHandler.sendEmptyMessageDelayed(1, 0);
    }

    private void onBootCompleted() {
        XLog.d("RomUpdateService: onBootCompleted");
        this.updateServiceHandler.sendEmptyMessageDelayed(2, 0);
    }

    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void sendCheckMessage(String str, long j) {
        XLog.d("RomUpdateService: MSG_CHECK_UPDATE will be sent after " + j + " ms, the reason is " + str);
        Message obtainMessage = this.updateServiceHandler.obtainMessage();
        obtainMessage.what = 4;
        obtainMessage.obj = str;
        this.updateServiceHandler.sendMessageDelayed(obtainMessage, j);
    }

    private void sendTryShowingNewVersion(long j) {
        XLog.d("RomUpdateService: MSG_TRY_SHOWING_NEW_VERSION will be sent after " + j + " ms.");
        Message obtainMessage = this.updateServiceHandler.obtainMessage();
        obtainMessage.what = 11;
        this.updateServiceHandler.sendMessageDelayed(obtainMessage, j);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void sendTrySilentUpdateMessage(long j) {
        XLog.d("RomUpdateService: MSG_TRY_SILENT_UPDATE will be sent after " + j + " ms.");
        Message obtainMessage = this.updateServiceHandler.obtainMessage();
        obtainMessage.what = 9;
        this.updateServiceHandler.sendMessageDelayed(obtainMessage, j);
    }

    private void sendDoSilentUpdateMessage(long j) {
        XLog.d("RomUpdateService: MSG_DO_SILENT_UPDATE will be sent after " + j + " ms.");
        Message obtainMessage = this.updateServiceHandler.obtainMessage();
        obtainMessage.what = 10;
        this.updateServiceHandler.sendMessageDelayed(obtainMessage, j);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void sendPerformUpdateMessage(String str, long j) {
        Message obtainMessage = this.updateServiceHandler.obtainMessage();
        obtainMessage.what = 5;
        obtainMessage.obj = str;
        this.updateServiceHandler.sendMessageDelayed(obtainMessage, j);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void sendRebootMessage(long j) {
        Message obtainMessage = this.updateServiceHandler.obtainMessage();
        obtainMessage.what = 8;
        this.updateServiceHandler.sendMessageDelayed(obtainMessage, j);
    }

    /* access modifiers changed from: private */
    public class UpdateServiceHandler extends Handler {
        public UpdateServiceHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            XLog.d("RomUpdateService: UpdateServiceHandler.handleMessage, msg.what = " + message.what);
            switch (message.what) {
                case 1:
                    RomUpdateService.this.sendCheckMessage(RomUpdateService.REASON_PROCESS_STARTUP, 180000);
                    RomUpdateService.this.sendTrySilentUpdateMessage(240000);
                    return;
                case 2:
                    RomUpdateService.this.sendCheckMessage(RomUpdateService.REASON_BOOT_COMPLETED, 180000);
                    RomUpdateService.this.sendTrySilentUpdateMessage(240000);
                    return;
                case 3:
                    removeMessages(3);
                    RomUpdateService.this.onNetworkAvailable();
                    return;
                case 4:
                    removeMessages(4);
                    RomUpdateService.this.handCheckUpdate((String) message.obj);
                    RomUpdateService.this.sendCheckMessage(RomUpdateService.REASON_PERIODIC_CHECK, 14400000);
                    return;
                case 5:
                    removeMessages(5);
                    RomUpdateService.this.handPerformUpdate((String) message.obj);
                    return;
                case 6:
                    removeMessages(6);
                    RomUpdateService.this.handCancelUpdate();
                    return;
                case 7:
                    removeMessages(7);
                    RomUpdateService.this.handleForceUpdate((String) message.obj);
                    return;
                case 8:
                    RomUpdateService.this.handleReboot();
                    return;
                case 9:
                    removeMessages(9);
                    RomUpdateService.this.handleTrySilentUpdate();
                    return;
                case 10:
                    removeMessages(10);
                    RomUpdateService.this.sendCheckMessage(RomUpdateService.REASON_SILENT_UPDATE, 0);
                    return;
                case 11:
                    removeMessages(11);
                    RomUpdateService.this.tryShowingNewVersion();
                    return;
                default:
                    return;
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void broadcastRomUpdateEvent(RomUpdateEventType romUpdateEventType, RomUpdateState romUpdateState) {
        XLog.i("RomUpdateService: broadcastRomUpdateEvent: eventType is " + romUpdateEventType + ", old state is " + this.serviceState + ", new state is " + romUpdateState + ", new error is " + this.serviceError + ", new silent mode is " + this.silentUpdate);
        this.serviceState = romUpdateState;
        Intent intent = new Intent(ACTION_ROM_UPDATE_EVENT);
        intent.putExtra(EXTRA_ROM_UPDATE_EVENT_TYPE, romUpdateEventType.ordinal());
        intent.putExtra(EXTRA_ROM_UPDATE_STATE, romUpdateState.ordinal());
        intent.putExtra(EXTRA_ROM_UPDATE_ERROR, this.serviceError);
        if (romUpdateEventType == RomUpdateEventType.CheckingUpdateCompleted) {
            intent.putExtra(EXTRA_NEW_VERSION_NAME, this.newVersionInfo.versionName);
            NewVersionInfo newVersionInfo2 = this.newVersionInfo;
            if (newVersionInfo2 == null || "".equals(newVersionInfo2.versionName)) {
                intent.putExtra(EXTRA_ROM_UPDATE_LOG, this.currentVersionDescription);
            } else {
                intent.putExtra(EXTRA_ROM_UPDATE_LOG, this.newVersionDescription);
            }
        }
        if (romUpdateEventType == RomUpdateEventType.UpdatingStarted) {
            intent.putExtra(EXTRA_ROM_UPDATE_SILENT, this.silentUpdate);
        }
        sendBroadcast(intent);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handCheckUpdate(String str) {
        CheckResponse checkResponse;
        XLog.d("RomUpdateService: handleCheckUpdate, reason = " + str);
        if (this.serviceState != RomUpdateState.Idle) {
            XLog.i("RomUpdateService: RomUpdateService status is " + this.serviceState + ", ignore checking update");
        } else if (!Utils.isNetworkAvailable()) {
            XLog.e("RomUpdateService: Network is not available.");
            this.isNetworkAvailable = false;
            this.serviceError = -4;
        } else {
            this.serviceError = 0;
            broadcastRomUpdateEvent(RomUpdateEventType.CheckingUpdateStarted, RomUpdateState.CheckingUpdate);
            notifyCheckEvent(1, null);
            String lowerCase = Utils.getBuildModel().toLowerCase();
            String str2 = SystemProperties.get("ro.mi.sw_ver");
            this.preferences.setCurrentVersionName(str2);
            this.preferences.commit();
            String str3 = SystemProperties.get("ro.mi.sw_channel");
            String sn = Utils.getSn();
            String valueOf = String.valueOf(System.currentTimeMillis());
            String paramHash = Utils.getParamHash(lowerCase, str2, str3, sn, "zh_CN", valueOf);
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            linkedHashMap.put(Constants.PRODUCT_MODEL, lowerCase);
            linkedHashMap.put(Constants.CURRENT_ROM_VERSION, str2);
            linkedHashMap.put(Constants.CURRENT_ROM_CHANNEL, str3);
            linkedHashMap.put(Constants.PRODUCT_FILTER_ID, sn);
            linkedHashMap.put(Constants.LOCALE, "zh_CN");
            linkedHashMap.put(Constants.TIMESTAMP, valueOf);
            linkedHashMap.put(Constants.PARAM_HASH, paramHash);
            String str4 = Constants.CHECK_ROM_UPDATE_URL_COMMON_PART + lowerCase;
            int i = 0;
            while (true) {
                if (i >= 3) {
                    break;
                }
                XLog.d("RomUpdateService: checking rom update, retry = %d", Integer.valueOf(i));
                this.newVersionInfo.reset();
                String str5 = this.httpClient.get(str4, linkedHashMap);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    XLog.e("RomUpdateService: InterruptedException: " + e.getMessage());
                }
                if (str5 == null) {
                    XLog.e("RomUpdateService: null http response.");
                    this.serviceError = -1;
                } else {
                    XLog.d("RomUpdateService: check rom update, http response: " + str5);
                    try {
                        checkResponse = (CheckResponse) MicoGson.getGson().fromJson(str5, CheckResponse.class);
                    } catch (JsonSyntaxException e2) {
                        XLog.e(TAG + e2);
                        checkResponse = null;
                    }
                    if (checkResponse == null || checkResponse.data == null) {
                        XLog.e("RomUpdateService: get null json object from invalid http response.");
                        this.serviceError = -2;
                    } else {
                        this.serviceError = 0;
                        checkResponse.dump();
                        if (checkResponse.data.currentInfo != null) {
                            this.currentVersionDescription = checkResponse.data.currentInfo.description;
                            this.preferences.setCurrentVersionName(str2);
                            this.preferences.setCurrentVersionDescription(this.currentVersionDescription);
                            this.preferences.commit();
                        }
                        if (checkResponse.data.updateInfo != null) {
                            this.newVersionName = checkResponse.data.updateInfo.version;
                            this.newVersionDescription = checkResponse.data.updateInfo.description;
                            this.newVersionUrl = checkResponse.data.updateInfo.link;
                            this.payloadSize = (long) checkResponse.data.updateInfo.size;
                            this.payloadMetadata = getPayloadMetadata(checkResponse.data.updateInfo.otherParam);
                            this.preferences.setNewVersionName(this.newVersionName);
                            this.preferences.setNewVersionUrl(this.newVersionUrl);
                            this.preferences.setNewVersionDescription(checkResponse.data.updateInfo.description);
                            this.preferences.setPayloadSize(this.payloadSize);
                            this.preferences.setPayloadMetadata(checkResponse.data.updateInfo.otherParam);
                            this.preferences.commit();
                            this.newVersionInfo.set(checkResponse.data.updateInfo);
                            if (REASON_SILENT_UPDATE.equals(str)) {
                                sendPerformUpdateMessage(REASON_SILENT_UPDATE, 0);
                            } else {
                                sendTryShowingNewVersion(0);
                            }
                        }
                    }
                }
                i++;
            }
            if (this.newVersionInfo.versionName == null || this.newVersionInfo.versionName.length() == 0) {
                notifyCheckEvent(2, null);
                if (REASON_SILENT_UPDATE.equals(str)) {
                    sendTrySilentUpdateMessage(3600000);
                }
            } else {
                notifyCheckEvent(2, this.newVersionInfo);
            }
            broadcastRomUpdateEvent(RomUpdateEventType.CheckingUpdateCompleted, RomUpdateState.Idle);
        }
    }

    private long getRandomDelay() {
        long j = HOUR_SECONDS;
        long hashCode = ((long) Utils.getSn().hashCode()) % j;
        if (hashCode < 0) {
            hashCode += j;
        }
        long midnightTimeStamp = ((Utils.getMidnightTimeStamp() + (HOUR_MILLISECONDS * 3)) + TimeUnit.SECONDS.toMillis(hashCode)) - System.currentTimeMillis();
        return midnightTimeStamp < 0 ? (-midnightTimeStamp) % HOUR_MILLISECONDS : midnightTimeStamp;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleTrySilentUpdate() {
        Calendar instance = Calendar.getInstance();
        instance.setTime(new Date());
        int i = instance.get(11);
        if (3 > i || i >= 5) {
            XLog.d("RomUpdateService: Now is not time for silent update. Retry silent update after a delay of 3600000 ms.");
            sendTrySilentUpdateMessage(3600000);
            return;
        }
        long randomDelay = getRandomDelay();
        XLog.d("RomUpdateService: Do silent update after a delay of " + randomDelay + " ms.");
        sendDoSilentUpdateMessage(randomDelay);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handPerformUpdate(String str) {
        XLog.d("RomUpdateService: handPerformUpdate, reason = " + str);
        if (this.serviceState != RomUpdateState.Idle) {
            XLog.i("RomUpdateService: RomUpdateService state is " + this.serviceState + ", ignore performing update");
        } else if ("".equals(this.newVersionName) || "".equals(this.newVersionUrl) || 0 == this.payloadSize || this.payloadMetadata == null) {
            XLog.e("RomUpdateService: invalid new version information for updating.");
            this.serviceError = -3;
            broadcastRomUpdateEvent(RomUpdateEventType.UpdatingFailed, RomUpdateState.Idle);
        } else {
            this.updateEngineStatus = 0;
            resetTimestampAndSpeed();
            if (REASON_SILENT_UPDATE.equals(str)) {
                this.silentUpdate = true;
            } else {
                this.silentUpdate = false;
            }
            if (REASON_FORCE_UPDATE.equals(str)) {
                this.forceUpdate = true;
            } else {
                this.forceUpdate = false;
            }
            this.serviceError = 0;
            broadcastRomUpdateEvent(RomUpdateEventType.UpdatingStarted, RomUpdateState.StartingUpdate);
            try {
                this.updateEngine.applyPayload(this.newVersionUrl, 0, this.payloadSize, this.payloadMetadata);
            } catch (ServiceSpecificException e) {
                XLog.e("RomUpdateService: UpdateEngine.applyPayload throws android.os.ServiceSpecificException: " + e.getMessage());
                if (SSE_MSG_WAITING_FOR_REBOOT.equals(e.getMessage())) {
                    XLog.d("RomUpdateService: This device will reboot.");
                }
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handCancelUpdate() {
        XLog.d("RomUpdateService: handCancelUpdate");
        if (this.serviceState == RomUpdateState.CancellingUpdate) {
            XLog.i("RomUpdateService: RomUpdateService state is " + this.serviceState + ", ignore cancelling update");
            return;
        }
        broadcastRomUpdateEvent(RomUpdateEventType.CancellingUpdateStarted, RomUpdateState.CancellingUpdate);
        try {
            this.updateEngine.cancel();
        } catch (ServiceSpecificException e) {
            XLog.e("RomUpdateService: UpdateEngine.applyPayload throws android.os.ServiceSpecificException: " + e.getMessage());
            if (SSE_MSG_NO_ONGOING_UPDATE_TO_CANCEL.equals(e.getMessage())) {
                XLog.d("RomUpdateService: There is no ongoing update to cancel.");
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleForceUpdate(String str) {
        ForceUpdateInfo forceUpdateInfo;
        String str2;
        XLog.d("RomUpdateService: handleForceUpdate, forceUpdateInfoStr = " + str);
        if (this.serviceState != RomUpdateState.Idle) {
            XLog.i("RomUpdateService: RomUpdateService state is " + this.serviceState + ", ignore forcing update");
        } else if (Utils.isEmpty(str)) {
            XLog.e("RomUpdateService: Force update information is null or empty.");
        } else {
            try {
                String decode = URLDecoder.decode(str, "UTF-8");
                if (decode != null) {
                    try {
                        forceUpdateInfo = (ForceUpdateInfo) MicoGson.getGson().fromJson(decode, ForceUpdateInfo.class);
                    } catch (JsonSyntaxException e) {
                        XLog.e(TAG + e);
                        forceUpdateInfo = null;
                    }
                    if (forceUpdateInfo == null) {
                        XLog.e("RomUpdateService: Get null json object from URL in forceUpdateInfoStr.");
                        return;
                    }
                    this.newVersionName = forceUpdateInfo.version;
                    this.newVersionUrl = forceUpdateInfo.url;
                    this.payloadSize = getPayloadFileSize(forceUpdateInfo.extra);
                    this.payloadMetadata = getPayloadMetadata(forceUpdateInfo.extra);
                    String str3 = this.newVersionName;
                    if (str3 == null || str3.length() == 0 || (str2 = this.newVersionUrl) == null || str2.length() == 0 || 0 == this.payloadSize || this.payloadMetadata == null) {
                        XLog.e("RomUpdateService: New version information is invalid. Please check new version first, or force update with valid new version information.");
                        return;
                    }
                    this.preferences.setNewVersionName(this.newVersionName);
                    this.preferences.setNewVersionUrl(this.newVersionUrl);
                    this.preferences.setPayloadSize(this.payloadSize);
                    this.preferences.setPayloadMetadata(forceUpdateInfo.extra);
                    this.preferences.commit();
                    sendPerformUpdateMessage(REASON_FORCE_UPDATE, 0);
                }
            } catch (UnsupportedEncodingException e2) {
                XLog.e("RomUpdateService: Failed to decode forceUpdateInfoStr. " + e2);
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleReboot() {
        broadcastRomUpdateEvent(RomUpdateEventType.RebootStarted, RomUpdateState.Rebooting);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            XLog.e("RomUpdateService: InterruptedException: " + e.getMessage());
        }
        this.preferences.reset();
        ((PowerManager) RomUpdateApplication.appContext.getSystemService("power")).reboot(!this.isScreenOn ? "quiescent" : "deviceowner");
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void tryShowingNewVersion() {
        ActivityManager.RunningAppProcessInfo topProcessInfo = Utils.getTopProcessInfo();
        if (topProcessInfo == null) {
            XLog.e("Failed to get top process.");
        } else if (BuildConfig.APPLICATION_ID.equals(topProcessInfo.processName)) {
            XLog.d("Top process is com.xiaomi.mico.romupdate, there is no need to show new version.");
        } else if ("com.xiaomi.micolauncher".equals(topProcessInfo.processName)) {
            Intent intent = new Intent(RomUpdateApplication.appContext, CheckUpdateActivity.class);
            intent.setFlags(268435456);
            startActivity(intent);
        } else {
            sendTryShowingNewVersion(600000);
        }
    }

    private long getPayloadFileSize(String str) {
        XLog.d("RomUpdateService: metadataJsonStr = " + str);
        try {
            return Long.parseLong(new JSONObject(str).getString("FILE_SIZE"));
        } catch (JSONException e) {
            XLog.e("RomUpdateService: JSONException: " + e);
            return 0;
        }
    }

    private String[] getPayloadMetadata(String str) {
        String str2;
        String str3;
        String str4;
        JSONException e;
        XLog.d("RomUpdateService: metadataJsonStr = " + str);
        String str5 = null;
        try {
            JSONObject jSONObject = new JSONObject(str);
            str2 = jSONObject.getString("FILE_HASH");
            try {
                str4 = jSONObject.getString("FILE_SIZE");
            } catch (JSONException e2) {
                e = e2;
                str4 = null;
                str3 = str4;
                XLog.e("RomUpdateService: JSONException: " + e);
                return new String[]{"FILE_HASH=" + str2, "FILE_SIZE=" + str4, "METADATA_HASH=" + str3, "METADATA_SIZE=" + str5};
            }
            try {
                str3 = jSONObject.getString("METADATA_HASH");
                try {
                    str5 = jSONObject.getString("METADATA_SIZE");
                } catch (JSONException e3) {
                    e = e3;
                }
            } catch (JSONException e4) {
                e = e4;
                str3 = null;
                XLog.e("RomUpdateService: JSONException: " + e);
                return new String[]{"FILE_HASH=" + str2, "FILE_SIZE=" + str4, "METADATA_HASH=" + str3, "METADATA_SIZE=" + str5};
            }
        } catch (JSONException e5) {
            e = e5;
            str2 = null;
            str4 = null;
            str3 = str4;
            XLog.e("RomUpdateService: JSONException: " + e);
            return new String[]{"FILE_HASH=" + str2, "FILE_SIZE=" + str4, "METADATA_HASH=" + str3, "METADATA_SIZE=" + str5};
        }
        return new String[]{"FILE_HASH=" + str2, "FILE_SIZE=" + str4, "METADATA_HASH=" + str3, "METADATA_SIZE=" + str5};
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void resetDownloadTimestampAndSpeed() {
        this.downloadBeginTs = 0;
        this.downloadEndTs = 0;
        this.downloadSpeed = 0;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void resetVerifyTimestampAndSpeed() {
        this.verifyBeginTs = 0;
        this.verifyEndTs = 0;
        this.verifySpeed = 0;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void resetFinalizeTimestampAndSpeed() {
        this.finalizeBeginTs = 0;
        this.finalizeEndTs = 0;
        this.finalizeSpeed = 0;
    }

    private void resetTimestampAndSpeed() {
        resetDownloadTimestampAndSpeed();
        resetVerifyTimestampAndSpeed();
        resetFinalizeTimestampAndSpeed();
    }

    private class RomUpdateEngineCallback extends UpdateEngineCallback {
        private RomUpdateEngineCallback() {
        }

        public void onStatusUpdate(int i, float f) {
            XLog.d("RomUpdateService: RomUpdateEngineCallback.onStatusUpdate, status = " + i + ", percent = " + f);
            RomUpdateService.this.updateEngineStatus = i;
            if (RomUpdateService.this.serviceState == RomUpdateState.Downloading || RomUpdateService.this.serviceState == RomUpdateState.Verifying || RomUpdateService.this.serviceState == RomUpdateState.Finalizing) {
                RomUpdateService romUpdateService = RomUpdateService.this;
                romUpdateService.notifyUpdateEngineStatus(romUpdateService.updateEngineStatus, f);
            }
            if (i != 0) {
                if (i == 2) {
                    XLog.d("RomUpdateService: update engine status: UPDATE_AVAILABLE");
                } else if (i == 3) {
                    if (RomUpdateService.this.serviceState != RomUpdateState.Downloading) {
                        RomUpdateService.this.broadcastRomUpdateEvent(RomUpdateEventType.UpdatingStateChanged, RomUpdateState.Downloading);
                    }
                    if (RomUpdateService.this.downloadBeginTs == 0) {
                        RomUpdateService.this.resetDownloadTimestampAndSpeed();
                        RomUpdateService.this.downloadBeginTs = System.currentTimeMillis();
                        XLog.d("RomUpdateService: download begin timestamp: " + RomUpdateService.this.downloadBeginTs);
                    } else if (f == 1.0f) {
                        RomUpdateService.this.downloadEndTs = System.currentTimeMillis();
                        long j = RomUpdateService.this.downloadEndTs - RomUpdateService.this.downloadBeginTs;
                        RomUpdateService romUpdateService2 = RomUpdateService.this;
                        romUpdateService2.downloadSpeed = romUpdateService2.payloadSize / (j / 1000);
                        XLog.d("RomUpdateService: download end timestamp: " + RomUpdateService.this.downloadEndTs);
                        XLog.d("RomUpdateService: download stage: payload size = " + RomUpdateService.this.payloadSize + " (bytes), download time = " + j + " (ms), download speed = " + RomUpdateService.this.downloadSpeed + " (bytes/s)");
                    }
                } else if (i == 4) {
                    if (RomUpdateService.this.serviceState != RomUpdateState.Verifying) {
                        RomUpdateService.this.broadcastRomUpdateEvent(RomUpdateEventType.UpdatingStateChanged, RomUpdateState.Verifying);
                    }
                    if (RomUpdateService.this.verifyBeginTs == 0) {
                        RomUpdateService.this.resetVerifyTimestampAndSpeed();
                        RomUpdateService.this.verifyBeginTs = System.currentTimeMillis();
                        XLog.d("RomUpdateService: verify begin timestamp: " + RomUpdateService.this.verifyBeginTs);
                    } else if (((double) f) == 1.0d) {
                        RomUpdateService.this.verifyEndTs = System.currentTimeMillis();
                        long j2 = RomUpdateService.this.verifyEndTs - RomUpdateService.this.verifyBeginTs;
                        RomUpdateService romUpdateService3 = RomUpdateService.this;
                        romUpdateService3.verifySpeed = romUpdateService3.payloadSize / (j2 / 1000);
                        XLog.d("RomUpdateService: verify end timestamp: " + RomUpdateService.this.verifyEndTs);
                        XLog.d("RomUpdateService: verify stage: payload size = " + RomUpdateService.this.payloadSize + " (bytes), verify time = " + j2 + " (ms), verify speed = " + RomUpdateService.this.verifySpeed + " (bytes/s)");
                    }
                } else if (i == 5) {
                    if (RomUpdateService.this.serviceState != RomUpdateState.Finalizing) {
                        RomUpdateService.this.broadcastRomUpdateEvent(RomUpdateEventType.UpdatingStateChanged, RomUpdateState.Finalizing);
                    }
                    if (RomUpdateService.this.finalizeBeginTs == 0) {
                        RomUpdateService.this.resetFinalizeTimestampAndSpeed();
                        RomUpdateService.this.finalizeBeginTs = System.currentTimeMillis();
                        XLog.d("RomUpdateService: finalize begin timestamp: " + RomUpdateService.this.finalizeBeginTs);
                    } else if (((double) f) == 1.0d) {
                        RomUpdateService.this.finalizeEndTs = System.currentTimeMillis();
                        long j3 = RomUpdateService.this.finalizeEndTs - RomUpdateService.this.finalizeBeginTs;
                        RomUpdateService romUpdateService4 = RomUpdateService.this;
                        romUpdateService4.finalizeSpeed = romUpdateService4.payloadSize / (j3 / 1000);
                        XLog.d("RomUpdateService: finalize end timestamp: " + RomUpdateService.this.finalizeEndTs);
                        XLog.d("RomUpdateService: finalize stage: payload size = " + RomUpdateService.this.payloadSize + " (bytes), finalize time = " + j3 + " (ms), finalize speed = " + RomUpdateService.this.finalizeSpeed + " (bytes/s)");
                    }
                } else if (i == 6) {
                    XLog.i("RomUpdateService: update engine status: UPDATED_NEED_REBOOT");
                }
            } else if (RomUpdateService.this.serviceState != RomUpdateState.Idle) {
                RomUpdateService.this.broadcastRomUpdateEvent(RomUpdateEventType.UpdatingStateChanged, RomUpdateState.Idle);
            }
        }

        public void onPayloadApplicationComplete(int i) {
            XLog.d("RomUpdateService: RomUpdateEngineCallback.onPayloadApplicationComplete, errorCode = " + i);
            RomUpdateService.this.serviceError = i;
            RomUpdateService.this.notifyUpdateError(i);
            if (i == 0) {
                XLog.i("RomUpdateService: Applying payload is finished successfully.");
                RomUpdateService.this.broadcastRomUpdateEvent(RomUpdateEventType.UpdatingCompleted, RomUpdateState.Idle);
                RomUpdateService.this.sendRebootMessage(0);
            } else if (i != 48) {
                RomUpdateService.this.broadcastRomUpdateEvent(RomUpdateEventType.UpdatingFailed, RomUpdateState.Idle);
            } else {
                XLog.i("RomUpdateService: update is cancelled by user.");
                RomUpdateService.this.broadcastRomUpdateEvent(RomUpdateEventType.CancellingUpdateCompleted, RomUpdateState.Idle);
            }
        }
    }

    private void notifyCheckEvent(int i, NewVersionInfo newVersionInfo2) {
        synchronized (this.checkUpdateListenerList) {
            Iterator<ICheckUpdateListener> it = this.checkUpdateListenerList.iterator();
            while (it.hasNext()) {
                try {
                    it.next().onCheckEvent(i, newVersionInfo2);
                } catch (RemoteException e) {
                    XLog.e("RomUpdateService: notifyCheckEvent RemoteException: " + e);
                    try {
                        it.remove();
                    } catch (Exception e2) {
                        XLog.e("RomUpdateService: Exception is caught when removing exceptional ICheckUpdateListener." + e2);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyUpdateEngineStatus(int i, float f) {
        synchronized (this.updateListenerList) {
            Iterator<IUpdateListener> it = this.updateListenerList.iterator();
            while (it.hasNext()) {
                try {
                    it.next().onUpdateEngineStatusChange(i, f);
                } catch (RemoteException e) {
                    XLog.e("RomUpdateService: notifyUpdateEngineStatus RemoteException: " + e);
                    try {
                        it.remove();
                    } catch (Exception e2) {
                        XLog.e("RomUpdateService: Exception is caught when removing exceptional IUpdateListener." + e2);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyUpdateError(int i) {
        synchronized (this.updateListenerList) {
            Iterator<IUpdateListener> it = this.updateListenerList.iterator();
            while (it.hasNext()) {
                try {
                    it.next().onUpdateError(i);
                } catch (RemoteException e) {
                    XLog.e("RomUpdateService: notifyUpdateError RemoteException: " + e);
                    try {
                        it.remove();
                    } catch (Exception e2) {
                        XLog.e("RomUpdateService: Exception is caught when removing exceptional IUpdateListener." + e2);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void onNetworkAvailable() {
        XLog.d("RomUpdateService: Network available: " + this.isNetworkAvailable);
        if (this.serviceState == RomUpdateState.Idle) {
            sendCheckMessage(REASON_NETWORK_CONNECTED, 180000);
        }
    }

    public static class ForceUpdateInfo {
        public String checksum;
        public String extra;
        public String hardware;
        public String url;
        public String version;

        public boolean isValid() {
            return !TextUtils.isEmpty(this.extra) && !TextUtils.isEmpty(this.checksum) && !TextUtils.isEmpty(this.version) && !TextUtils.isEmpty(this.url) && !TextUtils.isEmpty(this.hardware);
        }
    }
}
