package net.o5g.launcher.ui

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_app_entry.view.*
import net.o5g.launcher.R


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        show_sys.setOnClickListener {
            it.isSelected = !it.isSelected
            loadAppInfo()
        }
        loadAppInfo()
    }

    private fun loadAppInfo() {
        object : Thread() {
            override fun run() {
                val appList = getAppList()
                appList.sortBy {
                    "${it.name}"
                }
                runOnUiThread {
                    action_title.text = "已为您找到${appList.size}款应用(点击打开，长按更多操作)"
                    showEntry(appList)
                }
            }
        }.start()
    }

    private fun showEntry(appList: List<AppInfo>) {
        app_entry_list.adapter =
            object : ArrayAdapter<PackageInfo>(this, R.layout.item_app_entry) {
                override fun getCount(): Int {
                    return appList.size
                }

                override fun getView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {
                    return renderView(
                        convertView ?: View.inflate(
                            this@MainActivity,
                            R.layout.item_app_entry,
                            null
                        ).apply {
                            app_entry.isSelected = true
                        }, appList[position]
                    )
                }

            }

    }

    fun renderView(view: View, appInfo: AppInfo): View {
        view.icon.setImageDrawable(appInfo.icon)
        view.app_entry.text = appInfo.name
        view.setOnClickListener {
            println("open app $appInfo")
            Toast.makeText(
                this@MainActivity,
                "打开 ${appInfo.name}",
                Toast.LENGTH_SHORT
            ).show()
            startActivity(appInfo.intent)
        }
        view.setOnLongClickListener {
            showMultiBtnDialog(appInfo)
            true
        }
        return view
    }

    /* @setNeutralButton 设置中间的按钮
 * 若只需一个按钮，仅设置 setPositiveButton 即可
 */
    private fun showMultiBtnDialog(appInfo: AppInfo) {
        val items = arrayOf("卸载应用", "杀死应用", "清空应用", "取消")
        AlertDialog.Builder(this@MainActivity).apply {
            setTitle(appInfo.name)
            setIcon(appInfo.icon)
            var dialog: AlertDialog? = null
            setItems(items) { _, which -> // which 下标从0开始
                dialog?.dismiss()
                when (which) {
                    0 -> {
                        confirmDialog("确定卸载<${appInfo.name}>吗？") {
                            println("卸载${appInfo.name}")
                            removeApp(appInfo)
                        }
                    }
                    1 -> {
                        confirmDialog("确定杀死<${appInfo.name}>吗？") {
                            println("杀死${appInfo.name}")
                        }
                    }
                    2 -> {
                        confirmDialog("确定清空<${appInfo.name}>的应用数据吗？") {
                            println("清空${appInfo.name}")
                        }
                    }
                }
            }
            dialog = show()
        }

    }

    private fun removeApp(appInfo: AppInfo) {
        val uri: Uri = Uri.parse("package:${appInfo.name}")
        val intent =
            Intent(Intent.ACTION_DELETE, uri)
        startActivity(intent)
    }

    private fun confirmDialog(message: String, confirm: () -> Unit) {
        AlertDialog.Builder(this@MainActivity).apply {
            setTitle("警告")
            setMessage(message)
            setIcon(android.R.drawable.ic_dialog_alert)
            setPositiveButton("确定") { _, _ ->
                confirm()
            }
            setNegativeButton("取消") { _, _ ->
            }
            show()
        }
    }


    private fun getAppList(): MutableList<AppInfo> {
        // Return a List of all packages that are installed on the device.
        val packages: List<PackageInfo> = packageManager.getInstalledPackages(0)
        val appList = mutableListOf<AppInfo>()
        getRunningApp()
        for (packageInfo in packages) {
            // 判断系统/非系统应用
            if ((packageInfo.applicationInfo.flags.and(ApplicationInfo.FLAG_SYSTEM) == 0 ||
                        show_sys.isSelected) && packageName != packageInfo.packageName
            ) {
                val intent = packageManager.getLaunchIntentForPackage(packageInfo.packageName)
                    ?: continue
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                appList.add(
                    AppInfo(
                        packageInfo.applicationInfo.loadLabel(packageManager),
                        packageInfo.packageName,
                        intent,
                        packageManager.getApplicationIcon(packageInfo.applicationInfo)
                    )
                )
            }
        }
        return appList
    }

    private fun getRunningApp() {
        try {
            println("getRunningApp")
            val mActivityManager: ActivityManager =
                getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val intGetTastCounter = 1000
            println("getRunningApp 222")
            val mRunningService: List<ActivityManager.RunningAppProcessInfo> =
                mActivityManager.runningAppProcesses
            println("getRunningApp 333 $mRunningService")
            for (amService in mRunningService) {
                println(">>><<<${amService.processName}>>${amService.importanceReasonComponent?.packageName}")
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

data class AppInfo(
    val name: CharSequence,
    val packageName: String,
    val intent: Intent,
    val icon: Drawable
)