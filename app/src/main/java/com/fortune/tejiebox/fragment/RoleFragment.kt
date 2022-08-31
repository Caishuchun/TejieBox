package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.IntegralActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.bean.RoleListBean
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_role.view.*
import kotlinx.android.synthetic.main.item_role.view.*
import java.util.concurrent.TimeUnit

private const val GAME_ID = "game_id"

class RoleFragment : Fragment() {

    private var mView: View? = null
    private var gameId: Int? = null
    private var getGameRoleObservable: Disposable? = null
    private var mData = arrayListOf<RoleListBean.Data.Role>()
    private var mAdapter: BaseAdapterWithPosition<RoleListBean.Data.Role>? = null

    private var mGameVersion = ""
    private var isHaveInfo = false

    companion object {
        @JvmStatic
        fun newInstance(gameId: Int) =
            RoleFragment().apply {
                arguments = Bundle().apply {
                    putInt(GAME_ID, gameId)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            gameId = it.getInt(GAME_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_role, container, false)
        initView()
        getInfo()
        return mView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && !isHaveInfo) {
            getInfo()
        }
    }

    private fun getInfo() {
        DialogUtils.showBeautifulDialog(requireContext())
        val gameRole = RetrofitUtils.builder().getGameRole(gameId!!)
        getGameRoleObservable = gameRole.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            isHaveInfo = true
                            mGameVersion = it.data.gameVersion
                            mData.clear()
                            if (it.data.roleList.isEmpty()) {
                                ToastUtils.show("没有角色信息")
                            }
                            mData.addAll(it.data.roleList)
                            mAdapter?.notifyDataSetChanged()
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(requireActivity())
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    private fun initView() {
        mAdapter = BaseAdapterWithPosition.Builder<RoleListBean.Data.Role>()
            .setData(mData)
            .setLayoutId(R.layout.item_role)
            .addBindView { itemView, itemData, position ->
                itemView.tv_item_roleName.text = itemData.roleName
                itemView.tv_item_RoleLevel.text = "${itemData.lev}级"
                itemView.tv_item_roleJob.text = itemData.jobName

                itemView.tv_item_areaName.text = itemData.areaName
                itemView.tv_item_serviceName.text = itemData.serverName

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        (activity as IntegralActivity).toIntegralFragment(itemData, mGameVersion)
                    }
            }
            .create()

        mView?.rv_roleFragment?.let {
            it.adapter = mAdapter
            it.layoutManager = SafeLinearLayoutManager(requireContext())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        getGameRoleObservable?.dispose()
        getGameRoleObservable = null
    }

}