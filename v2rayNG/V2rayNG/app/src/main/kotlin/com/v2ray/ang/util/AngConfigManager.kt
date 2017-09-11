package com.v2ray.ang.util

import android.graphics.Bitmap
import android.text.TextUtils
import com.google.gson.Gson
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig.ANG_CONFIG
import com.v2ray.ang.AppConfig.PREF_CURR_CONFIG
import com.v2ray.ang.AppConfig.PREF_CURR_CONFIG_GUID
import com.v2ray.ang.AppConfig.PREF_CURR_CONFIG_NAME
import com.v2ray.ang.AppConfig.VMESS_PROTOCOL
import com.v2ray.ang.R
import com.v2ray.ang.dto.AngConfig
import com.v2ray.ang.dto.VmessQRCode
import com.v2ray.ang.ui.SettingsActivity

object AngConfigManager {
    private lateinit var app: AngApplication
    private lateinit var angConfig: AngConfig
    val configs: AngConfig get() = angConfig

    fun inject(app: AngApplication) {
        this.app = app
        if (app.firstRun) {
        }
        loadConfig()
    }

    /**
     * loading config
     */
    fun loadConfig() {
        try {
            val context = app.defaultDPreference.getPrefString(ANG_CONFIG, "")
            if (!TextUtils.isEmpty(context)) {
                angConfig = Gson().fromJson(context, AngConfig::class.java)
            } else {
                angConfig = AngConfig(0, vmess = arrayListOf(AngConfig.VmessBean()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * add or edit server
     */
    fun addServer(vmess: AngConfig.VmessBean, index: Int): Int {
        try {
            if (index >= 0) {
                //edit
                angConfig.vmess[index] = vmess
            } else {
                //add
                vmess.guid = System.currentTimeMillis().toString()
                angConfig.vmess.add(vmess)
                if (angConfig.vmess.count() == 1) {
                    angConfig.index = 0
                }
            }

            storeConfigFile()
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        return 0
    }

    /**
     * 移除服务器
     */
    fun removeServer(index: Int): Int {
        try {
            if (index < 0 || index > angConfig.vmess.count() - 1) {
                return -1
            }

            //删除
            angConfig.vmess.removeAt(index)

            //移除的是活动的
            if (angConfig.index == index) {
                if (angConfig.vmess.count() > 0) {
                    angConfig.index = 0
                } else {
                    angConfig.index = -1
                }
            } else if (index < angConfig.index)//移除活动之前的
            {
                angConfig.index--
            }

            storeConfigFile()
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        return 0
    }

    /**
     * set active server
     */
    fun setActiveServer(index: Int): Int {
        try {
            if (index < 0 || index > angConfig.vmess.count() - 1) {
                return -1
            }
            angConfig.index = index

            storeConfigFile()
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        return 0
    }

    /**
     * store config to file
     */
    fun storeConfigFile() {
        try {
            val conf = Gson().toJson(angConfig)
            app.defaultDPreference.setPrefString(ANG_CONFIG, conf)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * gen and store v2ray config file
     */
    fun genStoreV2rayConfig(): Boolean {
        try {
            angConfig.bypassMainland = app.defaultDPreference.getPrefBoolean(SettingsActivity.PREF_BYPASS_MAINLAND, false)
            val result = V2rayConfigUtil.getV2rayConfig(app, angConfig)
            if (result.status) {
                app.defaultDPreference.setPrefString(PREF_CURR_CONFIG, result.content)
                app.defaultDPreference.setPrefString(PREF_CURR_CONFIG_GUID, currConfigGuid())
                app.defaultDPreference.setPrefString(PREF_CURR_CONFIG_NAME, currConfigName())
                return true
            } else {
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun currConfigName(): String {
        if (angConfig.index < 0
                || angConfig.vmess.count() <= 0
                || angConfig.index > angConfig.vmess.count() - 1
                )
            return ""
        return angConfig.vmess[angConfig.index].remarks
    }

    fun currConfigGuid(): String {
        if (angConfig.index < 0
                || angConfig.vmess.count() <= 0
                || angConfig.index > angConfig.vmess.count() - 1
                )
            return ""
        return angConfig.vmess[angConfig.index].guid
    }

    /**
     * import config form qrcode or...
     */
    fun importConfig(server: String?): Int {
        try {
            if (server == null || TextUtils.isEmpty(server)) {
                return R.string.toast_none_data
            }
            if (server.indexOf(VMESS_PROTOCOL) < 0) {
                return R.string.toast_incorrect_protocol
            }
            var result = server.replace(VMESS_PROTOCOL, "")
            result = Utils.decode(result)
            if (TextUtils.isEmpty(result)) {
                return R.string.toast_decoding_failed
            }
            val vmessQRCode = Gson().fromJson(result, VmessQRCode::class.java)
            if (TextUtils.isEmpty(vmessQRCode.add)
                    || TextUtils.isEmpty(vmessQRCode.port)
                    || TextUtils.isEmpty(vmessQRCode.id)
                    || TextUtils.isEmpty(vmessQRCode.aid)
                    || TextUtils.isEmpty(vmessQRCode.net)
                    ) {
                return R.string.toast_incorrect_protocol
            }

            val vmess = AngConfig.VmessBean()
            vmess.security = "chacha20-poly1305"
            vmess.network = "tcp"
            vmess.headerType = "none"

            vmess.remarks = vmessQRCode.ps
            vmess.address = vmessQRCode.add
            vmess.port = Utils.parseInt(vmessQRCode.port)
            vmess.id = vmessQRCode.id
            vmess.alterId = Utils.parseInt(vmessQRCode.aid)
            vmess.network = vmessQRCode.net
            vmess.headerType = vmessQRCode.type
            vmess.requestHost = vmessQRCode.host
            vmess.streamSecurity = vmessQRCode.tls

            addServer(vmess, -1)
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        return 0
    }

    /**
     * share config
     */
    fun shareConfig(index: Int): String {
        try {
            if (index < 0 || index > angConfig.vmess.count() - 1) {
                return ""
            }
            val vmess = angConfig.vmess[index]
            val vmessQRCode = VmessQRCode()
            vmessQRCode.ps = vmess.remarks
            vmessQRCode.add = vmess.address
            vmessQRCode.port = vmess.port.toString()
            vmessQRCode.id = vmess.id
            vmessQRCode.aid = vmess.alterId.toString()
            vmessQRCode.net = vmess.network
            vmessQRCode.type = vmess.headerType
            vmessQRCode.host = vmess.requestHost
            vmessQRCode.tls = vmess.streamSecurity
            val json = Gson().toJson(vmessQRCode)
            val conf = VMESS_PROTOCOL + Utils.encode(json)

            return conf
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * share2Clipboard
     */
    fun share2Clipboard(index: Int): Int {
        try {
            val conf = shareConfig(index)
            if (TextUtils.isEmpty(conf)) {
                return -1
            }

            Utils.setClipboard(conf, app.applicationContext)

        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        return 0
    }

    /**
     * share2QRCode
     */
    fun share2QRCode(index: Int): Bitmap? {
        try {
            val conf = shareConfig(index)
            if (TextUtils.isEmpty(conf)) {
                return null
            }
            val bitmap = Utils.createQRCode(conf)
            return bitmap

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

}