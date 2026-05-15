package com.hjw.qbremote.ui

/**
 * ViewModel 层使用的 fallback 错误消息（无法访问 Context.getString）。
 * 这些常量集中了之前散落在 15+ 处的硬编码中文字符串。
 * TODO: 迁移到 AndroidViewModel 后替换为 R.string.* 资源。
 */
internal object VmErr {
    const val CONNECT_SERVER = "连接服务器失败"
    const val ADD_SERVER = "添加服务器失败"
    const val UPDATE_SERVER = "更新服务器失败"
    const val DELETE_SERVER = "删除服务器失败"
    const val SWITCH_SERVER = "切换服务器失败"
    const val REORDER_SERVERS = "调整服务器顺序失败"
    const val UPDATE_CHART_DISPLAY = "更新图表显示失败"
    const val UPDATE_CHART_ORDER = "更新图表排序失败"
    const val RESET_CHART_SETTINGS = "恢复图表设置失败"
    const val EXPORT_TORRENT = "导出种子失败"
    const val LOAD_TORRENT_DETAIL = "加载种子详情失败"
    const val FETCH_SPEED_LIMITS = "获取限速设置失败"
    const val SAVE_SPEED_LIMITS = "保存限速设置失败"
    const val TOGGLE_ALT_SPEED = "切换备用限速失败"
    const val ADD_TORRENT = "添加种子失败"
    const val CONNECT_FIRST = "请先连接服务器。"
    const val SELECT_SERVER_FIRST = "请先选择服务器。"
    const val ADD_TORRENT_EMPTY = "请填写种子链接或选择种子文件。"
}
