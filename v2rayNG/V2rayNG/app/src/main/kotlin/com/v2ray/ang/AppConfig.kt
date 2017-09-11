package com.v2ray.ang

/**
 *
 * App Config Const
 */
object AppConfig {
    const val ANG_CONFIG = "ang_config"
    const val PREF_CURR_CONFIG = "pref_v2ray_config"
    const val PREF_CURR_CONFIG_GUID = "pref_v2ray_config_guid"
    const val PREF_CURR_CONFIG_NAME = "pref_v2ray_config_name"
    const val VMESS_PROTOCOL: String = "vmess://"
    const val ACTION_STOP_V2RAY = "com.v2ray.ang.action.stop_v2ray"

    const val MSG_REGISTER_CLIENT = 1
    const val MSG_STATE_RUNNING = 11
    const val MSG_STATE_NOT_RUNNING = 12
    const val MSG_UNREGISTER_CLIENT = 2
    const val MSG_STATE_START = 3
    const val MSG_STATE_START_SUCCESS = 31
    const val MSG_STATE_START_FAILURE = 32
    const val MSG_STATE_STOP = 4
    const val MSG_STATE_STOP_SUCCESS = 41
}