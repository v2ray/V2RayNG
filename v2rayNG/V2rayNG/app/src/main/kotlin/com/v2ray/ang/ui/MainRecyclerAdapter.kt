package com.v2ray.ang.ui

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.v2ray.ang.R
import com.v2ray.ang.dto.AngConfig
import com.v2ray.ang.extension.alertView
import com.v2ray.ang.extension.selector
import com.v2ray.ang.util.AngConfigManager
import kotlinx.android.synthetic.main.item_qrcode.view.*
import kotlinx.android.synthetic.main.item_recycler_main.view.*
import org.jetbrains.anko.*

class MainRecyclerAdapter(val activity: BaseActivity) : RecyclerView.Adapter<MainRecyclerAdapter.BaseViewHolder>() {
    companion object {
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_FOOTER = 2
    }

    private var mActivity: BaseActivity = activity
    private lateinit var configs: AngConfig
    private val share_method: Array<out String> by lazy {
        mActivity.resources.getStringArray(R.array.share_method)
    }

    var changeable: Boolean = true
        set(value) {
            if (field == value)
                return
            field = value
            notifyDataSetChanged()
        }

    init {
        updateConfigList()
    }

    override fun getItemCount() = configs.vmess.count() + 1

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (holder is MainViewHolder) {
            val name = configs.vmess[position].remarks
            val guid = configs.vmess[position].guid
            val address = configs.vmess[position].address
            val port = configs.vmess[position].port

            holder.name.text = name
            holder.radio.isChecked = (position == configs.index)
            holder.statistics.text = "$address : $port"
            holder.itemView.backgroundColor = Color.TRANSPARENT

            holder.layout_share.setOnClickListener {
                mActivity.selector(null, share_method.asList()) {
                    i ->
                    try {
                        when (i) {
                            0 -> {
                                val iv = mActivity.layoutInflater.inflate(R.layout.item_qrcode, null)
                                iv.iv_qcode.setImageBitmap(AngConfigManager.share2QRCode(position))
                                mActivity.alertView("", iv) {
                                    show()
                                }
                            }
                            1 -> {
                                if (AngConfigManager.share2Clipboard(position) == 0) {
                                    mActivity.toast(R.string.toast_success)
                                } else {
                                    mActivity.toast(R.string.toast_failure)
                                }
                            }
                            else ->
                                mActivity.toast("else")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            holder.layout_edit.setOnClickListener {
                mActivity.startActivity<ServerActivity>("position" to position, "isRunning" to !changeable)
            }

            holder.infoContainer.setOnClickListener {
                if (changeable) {
                    AngConfigManager.setActiveServer(position)
                    notifyDataSetChanged()
                }
            }
        } else {
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM ->
                return MainViewHolder(parent.context.layoutInflater
                        .inflate(R.layout.item_recycler_main, parent, false))
            else ->
                return FooterViewHolder(parent.context.layoutInflater
                        .inflate(R.layout.item_recycler_footer, parent, false))
        }
    }

    fun updateConfigList() {
        configs = AngConfigManager.configs
        notifyDataSetChanged()
    }

    fun updateSelectedItem() {
        notifyItemChanged(configs.index)
    }

    override fun getItemViewType(position: Int): Int {
        if (position == configs.vmess.count()) {
            return VIEW_TYPE_FOOTER
        } else {
            return VIEW_TYPE_ITEM
        }
    }

    open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class MainViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val radio = itemView.btn_radio!!
        val name = itemView.tv_name!!
        val statistics = itemView.tv_statistics!!
        val infoContainer = itemView.info_container!!
        val layout_edit = itemView.layout_edit!!
        val layout_share = itemView.layout_share!!
    }

    class FooterViewHolder(itemView: View) : BaseViewHolder(itemView)

}
