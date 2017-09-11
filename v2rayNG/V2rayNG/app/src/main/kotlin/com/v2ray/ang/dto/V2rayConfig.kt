package com.v2ray.ang.dto

data class V2rayConfig(val port: Int,
                       val log: LogBean,
                       val inbound: InboundBean,
                       var outbound: OutboundBean,
                       val outboundDetour: List<OutboundDetourBean>,
                       val dns: DnsBean,
                       val routing: RoutingBean) {

    data class LogBean(val access: String,
                       val error: String,
                       val loglevel: String)

    data class InboundBean(
            val listen: String,
            val protocol: String,
            val settings: InSettingsBean,
            val domainOverride: List<String>) {

        data class InSettingsBean(val auth: String,
                                  val udp: Boolean)
    }

    data class OutboundBean(val tag: String,
                            val protocol: String,
                            var settings: OutSettingsBean,
                            var streamSettings: StreamSettingsBean,
                            var mux: MuxBean) {

        data class OutSettingsBean(var vnext: List<VnextBean>) {

            data class VnextBean(var address: String,
                                 var port: Int,
                                 var users: List<UsersBean>) {

                data class UsersBean(var id: String,
                                     var alterId: Int,
                                     var security: String)
            }
        }

        data class StreamSettingsBean(var network: String,
                                      var security: String,
                                      var tcpSettings: TcpsettingsBean?,
                                      var kcpsettings: KcpsettingsBean?) {

            data class TcpsettingsBean(var connectionReuse: Boolean = true,
                                       var header: HeaderBean = HeaderBean()) {
                data class HeaderBean(var type: String = "none",
                                      var request: Any? = null,
                                      var response: Any? = null)
            }

            data class KcpsettingsBean(var mtu: Int = 1350,
                                       var tti: Int = 20,
                                       var uplinkCapacity: Int = 12,
                                       var downlinkCapacity: Int = 100,
                                       var congestion: Boolean = false,
                                       var readBufferSize: Int = 1,
                                       var writeBufferSize: Int = 1,
                                       var header: HeaderBean = HeaderBean()) {
                data class HeaderBean(var type: String = "none")
            }
        }
    }

    data class MuxBean(var enabled: Boolean)


    data class OutboundDetourBean(val protocol: String,
                                  var settings: OutboundDetourSettingsBean,
                                  val tag: String) {
        data class OutboundDetourSettingsBean(var response: Response) {
            data class Response(var type: String)
        }
    }

    data class DnsBean(val servers: List<String>)

    data class RoutingBean(val strategy: String,
                           val settings: SettingsBean) {

        data class SettingsBean(val domainStrategy: String,
                                var rules: ArrayList<RulesBean>) {

            data class RulesBean(var type: String,
                                 var port: String,
                                 var ip: List<String>?,
                                 var domain: List<String>?,
                                 var outboundTag: String)
        }
    }
}