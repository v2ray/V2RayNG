package com.v2ray.ang.ui

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.defaultDPreference
import com.v2ray.ang.extension.onClick
import de.psdev.licensesdialog.LicensesDialogFragment
import org.jetbrains.anko.act
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.startActivity

class SettingsActivity : BaseActivity() {
    companion object {
        const val PREF_BYPASS_MAINLAND = "pref_bypass_mainland"
        //        const val PREF_START_ON_BOOT = "pref_start_on_boot"
        const val PREF_PER_APP_PROXY = "pref_per_app_proxy"
        const val PREF_LICENSES = "pref_licenses"
        const val PREF_FEEDBACK = "pref_feedback"
        const val PREF_VERSION = "pref_version"
        //        const val PREF_AUTO_RESTART = "pref_auto_restart"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        val perAppProxy by lazy { findPreference(PREF_PER_APP_PROXY) as CheckBoxPreference }
        //        val autoRestart by lazy { findPreference(PREF_AUTO_RESTART) as CheckBoxPreference }
        val licenses: Preference by lazy { findPreference(PREF_LICENSES) }
        val feedback: Preference by lazy { findPreference(PREF_FEEDBACK) }
        val version: Preference by lazy { findPreference(PREF_VERSION) }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_settings)

            licenses.onClick {
                val fragment = LicensesDialogFragment.Builder(act)
                        .setNotices(R.raw.licenses)
                        .setIncludeOwnLicense(false)
                        .build()
                fragment.show((act as AppCompatActivity).supportFragmentManager, null)
            }

            feedback.onClick {
                openUri("https://github.com/d4boy/v2rayNG/issues")
            }

            perAppProxy.setOnPreferenceClickListener {
                startActivity<PerAppProxyActivity>()
                perAppProxy.isChecked = true
                false
            }

            version.summary = BuildConfig.VERSION_NAME
        }

        override fun onStart() {
            super.onStart()

            perAppProxy.isChecked = defaultSharedPreferences.getBoolean(PREF_PER_APP_PROXY, false)

            defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onStop() {
            super.onStop()
            defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            when (key) {
//                PREF_AUTO_RESTART ->
//                    act.defaultDPreference.setPrefBoolean(key, sharedPreferences.getBoolean(key, false))

                PREF_PER_APP_PROXY ->
                    act.defaultDPreference.setPrefBoolean(key, sharedPreferences.getBoolean(key, false))
            }
        }

        private fun openUri(uriString: String) {
            val uri = Uri.parse(uriString)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

}