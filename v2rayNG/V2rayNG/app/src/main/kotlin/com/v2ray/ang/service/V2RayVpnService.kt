package com.v2ray.ang.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.VpnService
import android.os.*
import android.support.v7.app.NotificationCompat
import android.util.Log
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork
import com.orhanobut.logger.Logger
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.ACTION_STOP_V2RAY
import com.v2ray.ang.AppConfig.PREF_CURR_CONFIG
import com.v2ray.ang.AppConfig.PREF_CURR_CONFIG_NAME
import com.v2ray.ang.R
import com.v2ray.ang.defaultDPreference
import com.v2ray.ang.ui.MainActivity
import com.v2ray.ang.ui.PerAppProxyActivity
import com.v2ray.ang.ui.SettingsActivity
import com.v2ray.ang.util.Utils
import libv2ray.Libv2ray
import libv2ray.V2RayCallbacks
import libv2ray.V2RayVPNServiceSupportsSet
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.lang.ref.SoftReference
import java.util.concurrent.TimeUnit

class V2RayVpnService : VpnService() {
    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_PENDING_INTENT_CONTENT = 0
        const val NOTIFICATION_PENDING_INTENT_STOP_V2RAY = 1

        fun startV2Ray(context: Context) {
            val intent = Intent(context.applicationContext, V2RayVpnService::class.java)
            context.startService(intent)
        }
    }

    private val v2rayPoint = Libv2ray.newV2RayPoint()
    private val v2rayCallback = V2RayCallback()
    private var connectivitySubscription: Subscription? = null
    private lateinit var configContent: String

    private val stopV2RayReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            stopV2Ray()
        }
    }

    private lateinit var mInterface: ParcelFileDescriptor
    val fd: Int get() = mInterface.fd

    override fun onCreate() {
        super.onCreate()

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        v2rayPoint.packageName = packageName
    }

    override fun onRevoke() {
        stopV2Ray()
    }

    fun setup(parameters: String) {
        // If the old interface has exactly the same parameters, use it!
        // Configure a builder while parsing the parameters.
        val builder = Builder()

        parameters.split(" ")
                .map { it.split(",") }
                .forEach {
                    when (it[0][0]) {
                        'm' -> builder.setMtu(java.lang.Short.parseShort(it[1]).toInt())
                        'a' -> builder.addAddress(it[1], Integer.parseInt(it[2]))
                        'r' -> builder.addRoute(it[1], Integer.parseInt(it[2]))
                        's' -> builder.addSearchDomain(it[1])
                    }
                }

        builder.setSession(defaultDPreference.getPrefString(PREF_CURR_CONFIG_NAME, ""))

        val dnsServers = Utils.getDnsServers()
        for (dns in dnsServers)
            builder.addDnsServer(dns)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                defaultDPreference.getPrefBoolean(SettingsActivity.PREF_PER_APP_PROXY, false)) {
            val apps = defaultDPreference.getPrefStringSet(PerAppProxyActivity.PREF_PER_APP_PROXY_SET, null)
            val bypassApps = defaultDPreference.getPrefBoolean(PerAppProxyActivity.PREF_BYPASS_APPS, false)
            apps?.forEach {
                try {
                    if (bypassApps)
                        builder.addDisallowedApplication(it)
                    else
                        builder.addAllowedApplication(it)
                } catch (e: PackageManager.NameNotFoundException) {
                    Logger.d(e)
                }
            }
        }

        // Close the old interface since the parameters have been changed.
        try {
            mInterface.close()
        } catch (ignored: Exception) {
        }

        // Create a new interface using the builder and save the parameters.
        mInterface = builder.establish()
        Log.d("VPNService", "New interface: " + parameters)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startV2ray()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun vpnCheckIsReady() {
        val prepare = VpnService.prepare(this)

        if (prepare != null) {
            return
        }

        v2rayPoint.vpnSupportReady()
        if (v2rayPoint.isRunning) {
            sendMsg(AppConfig.MSG_STATE_START_SUCCESS, "")
        } else {
            sendMsg(AppConfig.MSG_STATE_START_FAILURE, "")
        }
    }

    private fun startV2ray() {
        if (!v2rayPoint.isRunning) {

            registerReceiver(stopV2RayReceiver, IntentFilter(ACTION_STOP_V2RAY))

            configContent = defaultDPreference.getPrefString(PREF_CURR_CONFIG, "")

            connectivitySubscription = ReactiveNetwork.observeNetworkConnectivity(this.applicationContext)
                    .subscribeOn(Schedulers.io())
                    .skip(1)
                    //.filter(Connectivity.hasState(NetworkInfo.State.CONNECTED))
                    .throttleWithTimeout(3, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { connectivity ->
                        val state = connectivity.state
                        Log.e("ReactiveNetwork", state.toString())
                        if (state == NetworkInfo.State.CONNECTED) {
                            if (v2rayPoint.isRunning) {
                                v2rayPoint.networkInterrupted()
                            }
                        }
                    }

            v2rayPoint.callbacks = v2rayCallback
            v2rayPoint.vpnSupportSet = v2rayCallback
            v2rayPoint.configureFile = "V2Ray_internal/ConfigureFileContent"
            v2rayPoint.configureFileContent = configContent
            v2rayPoint.runLoop()
        }

        showNotification()
    }

    private fun stopV2Ray() {
//        val configName = defaultDPreference.getPrefString(PREF_CURR_CONFIG_GUID, "")
//        val emptyInfo = VpnNetworkInfo()
//        val info = loadVpnNetworkInfo(configName, emptyInfo)!! + (lastNetworkInfo ?: emptyInfo)
//        saveVpnNetworkInfo(configName, info)

        if (v2rayPoint.isRunning) {
            v2rayPoint.stopLoop()
        }

        unregisterReceiver(stopV2RayReceiver)
        connectivitySubscription?.let {
            it.unsubscribe()
            connectivitySubscription = null
        }

        sendMsg(AppConfig.MSG_STATE_STOP_SUCCESS, "")
        cancelNotification()
        stopSelf()
    }

    private fun showNotification() {
        val startMainIntent = Intent(applicationContext, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(applicationContext,
                NOTIFICATION_PENDING_INTENT_CONTENT, startMainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val stopV2RayIntent = Intent(ACTION_STOP_V2RAY)
        val stopV2RayPendingIntent = PendingIntent.getBroadcast(applicationContext,
                NOTIFICATION_PENDING_INTENT_STOP_V2RAY, stopV2RayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(applicationContext)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(defaultDPreference.getPrefString(PREF_CURR_CONFIG_NAME, ""))
                .setContentText(getString(R.string.notification_action_more))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(contentPendingIntent)
                .addAction(R.drawable.ic_close_grey_800_24dp,
                        getString(R.string.notification_action_stop_v2ray),
                        stopV2RayPendingIntent)
                .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification() {
        stopForeground(true)
    }

    private inner class V2RayCallback : V2RayCallbacks, V2RayVPNServiceSupportsSet {
        override fun shutdown() = 0L

        override fun getVPNFd() = this@V2RayVpnService.fd.toLong()

        override fun prepare(): Long {
            vpnCheckIsReady()
            return 1
        }

        override fun protect(l: Long) = (if (this@V2RayVpnService.protect(l.toInt())) 0 else 1).toLong()

        override fun onEmitStatus(l: Long, s: String?): Long {
            Logger.d(s)
            return 0
        }

        override fun setup(s: String): Long {
            Logger.d(s)
            try {
                this@V2RayVpnService.setup(s)
                return 0
            } catch (e: Exception) {
                e.printStackTrace()
                return -1
            }
        }
    }

    var mMsgSend: Messenger? = null
    private var mMsgReceive = Messenger(ReceiveMessageHandler(this@V2RayVpnService))

    private class ReceiveMessageHandler(vpnService: V2RayVpnService) : Handler() {
        internal var mReference: SoftReference<V2RayVpnService> = SoftReference(vpnService)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val vpnService = mReference.get()
            when (msg.what) {
                AppConfig.MSG_REGISTER_CLIENT -> {
                    Log.e("ReceiveMessageHandler", msg.data.get("key").toString())
                    vpnService?.mMsgSend = msg.replyTo

                    val isRunning = vpnService?.v2rayPoint!!.isRunning
                            && VpnService.prepare(vpnService) == null
                    if (isRunning) {
                        vpnService?.sendMsg(AppConfig.MSG_STATE_RUNNING, "")
                    } else {
                        vpnService?.sendMsg(AppConfig.MSG_STATE_NOT_RUNNING, "")
                    }
                }
                AppConfig.MSG_UNREGISTER_CLIENT -> {
                    vpnService?.mMsgSend = null
                }
                AppConfig.MSG_STATE_START -> {
                    //nothing to do
                }
                AppConfig.MSG_STATE_STOP -> {
                    vpnService?.stopV2Ray()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return mMsgReceive.binder
    }

    fun sendMsg(what: Int, content: String) {
        try {
            val msg = Message.obtain()
//            msg.replyTo = mMsgReceive
            msg.what = what
            val bundle = Bundle()
            bundle.putString("key", content)
            msg.data = bundle
            mMsgSend?.send(msg)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

