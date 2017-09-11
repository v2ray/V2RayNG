package com.v2ray.ang.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.*
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.tbruyelle.rxpermissions.RxPermissions
import com.v2ray.ang.R
import com.v2ray.ang.service.V2RayVpnService
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.Utils
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import android.os.Bundle
import com.v2ray.ang.AppConfig
import org.jetbrains.anko.startActivityForResult
import java.lang.ref.SoftReference

class MainActivity : BaseActivity() {
    companion object {
        private const val REQUEST_CODE_VPN_PREPARE = 0
        private const val REQUEST_SCAN = 1
    }

    var fabChecked = false
        set(value) {
            field = value
            adapter.changeable = !value
            if (value) {
                fab.imageResource = R.drawable.ic_fab_check
            } else {
                fab.imageResource = R.drawable.ic_fab_uncheck
            }
        }

    private val adapter by lazy { MainRecyclerAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener {
            if (fabChecked) {
                sendMsg(AppConfig.MSG_STATE_STOP, "")
            } else {
                val intent = VpnService.prepare(this)
                if (intent == null)
                    startV2Ray()
                else
                    startActivityForResult(intent, REQUEST_CODE_VPN_PREPARE)
            }
        }

        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
    }

    fun startV2Ray() {
        if (AngConfigManager.genStoreV2rayConfig()) {
            toast(R.string.toast_services_start)
            V2RayVpnService.startV2Ray(this)
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this.applicationContext, V2RayVpnService::class.java)
        intent.`package` = "com.v2ray.ang"
        bindService(intent, mConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()

        unbindService(mConnection)
    }

    public override fun onResume() {
        super.onResume()
        adapter.updateConfigList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_VPN_PREPARE ->
                startV2Ray()
            REQUEST_SCAN ->
                importConfig(data?.getStringExtra("SCAN_RESULT"))
//            IntentIntegrator.REQUEST_CODE -> {
//                if (resultCode == RESULT_CANCELED) {
//                } else {
//                    val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
//                    importConfig(scanResult.contents)
//                }
//            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.import_qrcode -> {
            importQRcode()
            true
        }
        R.id.import_clipboard -> {
            importClipboard()
            true
        }
        R.id.import_manually -> {
            startActivity<ServerActivity>("position" to -1, "isRunning" to fabChecked)
            adapter.updateConfigList()
            true
        }
        R.id.settings -> {
            startActivity<SettingsActivity>("isRunning" to fabChecked)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    override fun onBackPressed() {
        super.onBackPressed()
    }

    /**
     * import config from qrcode
     */
    fun importQRcode(): Boolean {
        try {
            startActivityForResult(Intent("com.google.zxing.client.android.SCAN")
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), REQUEST_SCAN)
        } catch (e: Exception) {
            RxPermissions.getInstance(this)
                    .request(Manifest.permission.CAMERA)
                    .subscribe {
                        if (it)
                            startActivityForResult<ScannerActivity>(REQUEST_SCAN)
                        else
                            toast(R.string.toast_permission_denied)
                    }
        }
//        val integrator = IntentIntegrator(this)
//        integrator.initiateScan(IntentIntegrator.ALL_CODE_TYPES)
        return true
    }

    /**
     * import config from clipboard
     */
    fun importClipboard(): Boolean {
        try {
            val clipboard = Utils.getClipboard(this)
            importConfig(clipboard)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun importConfig(server: String?) {
        if (server == null) {
            return
        }
        val resId = AngConfigManager.importConfig(server)
        if (resId > 0) {
            toast(resId)
        } else {
            toast(R.string.toast_success)
            adapter.updateConfigList()
        }
    }

    private var mMsgReceive: Messenger? = null

    private class ReceiveMessageHandler(activity: MainActivity) : Handler() {
        internal var mReference: SoftReference<MainActivity> = SoftReference(activity)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val activity = mReference.get()
            when (msg.what) {
                AppConfig.MSG_STATE_RUNNING -> {
                    activity?.fabChecked = true
                }
                AppConfig.MSG_STATE_NOT_RUNNING -> {
                    activity?.fabChecked = false
                }
                AppConfig.MSG_STATE_START_SUCCESS -> {
                    activity?.fabChecked = true
                    activity?.toast(R.string.toast_services_success)
                }
                AppConfig.MSG_STATE_START_FAILURE -> {
                    activity?.fabChecked = false
                    activity?.toast(R.string.toast_services_failure)
                }
                AppConfig.MSG_STATE_STOP_SUCCESS -> {
                    activity?.fabChecked = false
                }
            }
        }
    }

    private var mMsgSend: Messenger? = null
    val mConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mMsgSend = null
            mMsgReceive = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mMsgSend = Messenger(service)
            mMsgReceive = Messenger(ReceiveMessageHandler(this@MainActivity))

            sendMsg(AppConfig.MSG_REGISTER_CLIENT, "")
        }
    }

    fun sendMsg(what: Int, content: String) {
        try {
            val msg = Message.obtain()
            msg.replyTo = mMsgReceive
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