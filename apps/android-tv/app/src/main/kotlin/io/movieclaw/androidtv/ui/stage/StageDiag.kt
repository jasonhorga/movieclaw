package io.movieclaw.androidtv.ui.stage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * 诊断版专用（不合入上游）：首页大图在部分设备上整块发白，逐层关掉 / 换回旧画法来定位是哪一层。
 * 在「谁在看」页循环切换，回首页看效果；只存在内存里，重启 App 回到「正常」。
 */
object StageDiag {
    val labels = listOf("正常", "无压暗", "旧压暗", "只剧照")

    var mode by mutableIntStateOf(0)

    val label: String get() = labels[mode]

    fun next() {
        mode = (mode + 1) % labels.size
    }

    /** 0：现在的画法（合成着色器压暗） */
    val normal get() = mode == 0
    /** 1：去掉左侧压暗层 */
    val noScrim get() = mode == 1
    /** 2：压暗换回 10-09 之前的离屏 Canvas + DstIn */
    val legacyScrim get() = mode == 2
    /** 3：去掉边缘色底、下沿渐隐和压暗，只留剧照 */
    val imageOnly get() = mode == 3
}
