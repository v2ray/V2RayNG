package com.v2ray.ang.util

import android.text.TextUtils
import com.google.gson.Gson
import com.v2ray.ang.AngApplication
import com.v2ray.ang.dto.AngConfig
import com.v2ray.ang.dto.V2rayConfig
import org.json.JSONObject

object V2rayConfigUtil {
    val lib2rayObj: JSONObject by lazy {
        JSONObject("""{
                    "enabled": true,
                    "listener": {
                    "onUp": "#none",
                    "onDown": "#none"
                    },
                    "env": [
                    "V2RaySocksPort=10808"
                    ],
                    "render": [],
                    "escort": [],
                    "vpnservice": {
                    "Target": "${"$"}{datadir}tun2socks",
                    "Args": [
                    "--netif-ipaddr",
                    "26.26.26.2",
                    "--netif-netmask",
                    "255.255.255.0",
                    "--socks-server-addr",
                    "127.0.0.1:${"$"}V2RaySocksPort",
                    "--tunfd",
                    "3",
                    "--tunmtu",
                    "1500",
                    "--sock-path",
                    "/dev/null",
                    "--loglevel",
                    "4",
                    "--enable-udprelay"
                    ],
                    "VPNSetupArg": "m,1500 a,26.26.26.1,24 r,0.0.0.0,0"
                    },
                    "preparedDomainName": {
                      "domainName": [
                        "<v2ray.cool>:<10086>"
                      ],
                      "tcpVersion": "tcp4",
                      "udpVersion": "udp4"
                    }
                }""")
    }

    val requestObj: JSONObject by lazy {
        JSONObject("""{"version":"1.1","method":"GET","path":["/"],"headers":{"Host":[""],"User-Agent":["Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.75 Safari/537.36","Mozilla/5.0 (iPhone; CPU iPhone OS 10_0_2 like Mac OS X) AppleWebKit/601.1 (KHTML, like Gecko) CriOS/53.0.2785.109 Mobile/14A456 Safari/601.1.46"],"Accept-Encoding":["gzip, deflate"],"Connection":["keep-alive"],"Pragma":"no-cache"}}""")
    }
    val responseObj: JSONObject by lazy {
        JSONObject("""{"version":"1.1","status":"200","reason":"OK","headers":{"Content-Type":["application/octet-stream","application/x-msdownload","text/html","application/x-shockwave-flash"],"Transfer-Encoding":["chunked"],"Connection":["keep-alive"],"Pragma":"no-cache"}}""")
    }

    data class Result(var status: Boolean, var content: String)

    /**
     * 生成v2ray的客户端配置文件
     */
    fun getV2rayConfig(app: AngApplication, config: AngConfig): Result {
        val result = Result(false, "")
        try {
            //检查设置
            if (config.index < 0
                    || config.vmess.count() <= 0
                    || config.index > config.vmess.count() - 1
                    ) {
                return result
            }

            //取得默认配置
            val assets = AssetsUtil.readTextFromAssets(app.assets, "v2ray_config.json")
            if (TextUtils.isEmpty(assets)) {
                return result
            }

            //转成Json
            val v2rayConfig = Gson().fromJson(assets, V2rayConfig::class.java) ?: return result
//            if (v2rayConfig == null) {
//                return result
//            }

            //vmess协议服务器配置
            outbound(config, v2rayConfig)

            //routing
            routing(config, v2rayConfig)

            //增加lib2ray
            val finalConfig = addLib2ray(v2rayConfig)

            result.status = true
            result.content = finalConfig
            return result

        } catch (e: Exception) {
            e.printStackTrace()
            return result
        }
    }

    /**
     * vmess协议服务器配置
     */
    private fun outbound(config: AngConfig, v2rayConfig: V2rayConfig): Boolean {
        try {
            val vmess = config.vmess[config.index]
            v2rayConfig.outbound.settings.vnext[0].address = vmess.address
            v2rayConfig.outbound.settings.vnext[0].port = vmess.port

            v2rayConfig.outbound.settings.vnext[0].users[0].id = vmess.id
            v2rayConfig.outbound.settings.vnext[0].users[0].alterId = vmess.alterId
            v2rayConfig.outbound.settings.vnext[0].users[0].security = vmess.security

            //Mux
            v2rayConfig.outbound.mux.enabled = true

            //远程服务器底层传输配置
            v2rayConfig.outbound.streamSettings = boundStreamSettings(config)

            //如果非ip
            if (!Utils.isIpAddress(vmess.address)) {
                lib2rayObj.optJSONObject("preparedDomainName")
                        .optJSONArray("domainName")
                        .put(String.format("%s:%s", vmess.address, vmess.port))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * 远程服务器底层传输配置
     */
    private fun boundStreamSettings(config: AngConfig): V2rayConfig.OutboundBean.StreamSettingsBean {
        val streamSettings = V2rayConfig.OutboundBean.StreamSettingsBean("", "", null, null)
        try {
            //远程服务器底层传输配置
            streamSettings.network = config.vmess[config.index].network
            streamSettings.security = config.vmess[config.index].streamSecurity

            //streamSettings
            when (streamSettings.network) {
                "kcp" -> {
                    val kcpsettings = V2rayConfig.OutboundBean.StreamSettingsBean.KcpsettingsBean()
                    kcpsettings.mtu = 1350
                    kcpsettings.tti = 50
                    kcpsettings.uplinkCapacity = 12
                    kcpsettings.downlinkCapacity = 100
                    kcpsettings.congestion = false
                    kcpsettings.readBufferSize = 1
                    kcpsettings.writeBufferSize = 1
                    kcpsettings.header = V2rayConfig.OutboundBean.StreamSettingsBean.KcpsettingsBean.HeaderBean()
                    kcpsettings.header.type = config.vmess[config.index].headerType
                    streamSettings.kcpsettings = kcpsettings
                }
                "ws" -> {
                }
                else -> {
                    //tcp带http伪装
                    if (config.vmess[config.index].headerType == "http") {
                        val tcpSettings = V2rayConfig.OutboundBean.StreamSettingsBean.TcpsettingsBean()
                        tcpSettings.connectionReuse = true
                        tcpSettings.header = V2rayConfig.OutboundBean.StreamSettingsBean.TcpsettingsBean.HeaderBean()
                        tcpSettings.header.type = config.vmess[config.index].headerType

                        if (requestObj.has("headers")
                                || requestObj.optJSONObject("headers").has("Host")) {
                            requestObj.optJSONObject("headers")
                                    .optJSONArray("Host")
                                    .put(config.vmess[config.index].requestHost)
                            tcpSettings.header.request = requestObj
                            tcpSettings.header.response = responseObj
                        }
                        streamSettings.tcpSettings = tcpSettings
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return streamSettings
        }
        return streamSettings
    }

    /**
     * routing
     */
    fun routing(config: AngConfig, v2rayConfig: V2rayConfig): Boolean {
        try {
            //绕过大陆网址
            if (config.bypassMainland) {
                val rulesItem1 = V2rayConfig.RoutingBean.SettingsBean.RulesBean("", "", null, null, "")
                rulesItem1.type = "chinasites"
                rulesItem1.outboundTag = "direct"
                v2rayConfig.routing.settings.rules.add(rulesItem1)

                val rulesItem2 = V2rayConfig.RoutingBean.SettingsBean.RulesBean("", "", null, null, "")
                rulesItem2.type = "chinaip"
                rulesItem2.outboundTag = "direct"
                v2rayConfig.routing.settings.rules.add(rulesItem2)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * 增加lib2ray
     */
    private fun addLib2ray(v2rayConfig: V2rayConfig): String {
        try {
            val conf = Gson().toJson(v2rayConfig)
            val jObj = JSONObject(conf)
            jObj.put("#lib2ray", lib2rayObj)
            return jObj.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}
