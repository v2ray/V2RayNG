package com.v2ray.ang.dto

data class AngConfig(
        var index: Int,
        var bypassMainland: Boolean = false,
        var vmess: ArrayList<VmessBean>
) {
    data class VmessBean(var guid: String = "123456",
                         var address: String = "v2ray.cool",
                         var port: Int = 10086,
                         var id: String = "a3482e88-686a-4a58-8126-99c9df64b7bf",
                         var alterId: Int = 64,
                         var security: String = "aes-128-cfb",
                         var network: String = "tcp",
                         var remarks: String = "def",
                         var headerType: String = "",
                         var requestHost: String = "",
                         var streamSecurity: String = "")
}