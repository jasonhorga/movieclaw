package io.movieclaw.androidtv.ui.library

import android.app.Application
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import io.movieclaw.androidtv.AppGraph
import io.movieclaw.androidtv.ui.TV_QUALIFIERS
import io.movieclaw.androidtv.ui.TvEnv
import io.movieclaw.androidtv.ui.accounts.AccountsScreen
import io.movieclaw.androidtv.ui.focusedText
import io.movieclaw.androidtv.ui.press
import io.movieclaw.androidtv.ui.savePreview
import io.movieclaw.androidtv.ui.setTvContent
import io.movieclaw.androidtv.ui.shell.LocalRouter
import io.movieclaw.androidtv.ui.shell.Router
import io.movieclaw.core.model.generated.LibraryItemView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 海报墙挂片名（「谁在看」页「海报标题」开关）：默认关，海报下不挂字；打开后每张海报下挂片名与年份，
 * 按遥控器往下走到最后一排，片名也整行露在屏幕里（焦点只在海报上，滚动要把片名那一截算进去）。
 * 真的合集墙，假服务端给 12 部（3 排：5 + 5 + 2），都没有海报图（海报位上是灰字片名占位）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WallTitlesTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun openWall(): TvEnv {
        val items = (1L..12L).map { LibraryItemView(mediaItemId = it, libraryId = 1, kind = "movie", title = "片$it", year = 2000 + it) }
        val env = rule.setTvContent(mapOf("/collections/5/items" to items)) {
            CompositionLocalProvider(LocalRouter provides Router()) { CollectionWallScreen(5, "合集") }
        }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        assertEquals("焦点落在第一张", "片1", rule.focusedText())
        return env
    }

    @Test
    fun wallHasNoTitlesByDefault() {
        val env = openWall()
        assertFalse(env.graph.wallTitles)
        // 只有海报位上的占位字，海报下没有片名、没有年份
        rule.onAllNodesWithText("片1", useUnmergedTree = true).assertCountEquals(1)
        rule.onAllNodesWithText("2001", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun titlesShowUnderPostersAndLastRowStaysInView() {
        // 之前在「谁在看」页打开过：本机记着，这次进墙就挂片名
        AppGraph(rule.activity.application).saveWallTitles(true)
        val env = openWall()
        assertTrue(env.graph.wallTitles)
        // 海报位上的占位字 + 海报下的片名；年份在片名下面
        rule.onAllNodesWithText("片1", useUnmergedTree = true).assertCountEquals(2)
        val title = rule.onAllNodesWithText("片1", useUnmergedTree = true)[1].getBoundsInRoot()
        val year = rule.onNodeWithText("2001", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("年份在片名下面：$title / $year", year.top >= title.bottom)
        rule.savePreview("wall-titles")

        // 往下两排到最后一排（5 + 5 + 2 的第 11 部）：片名与年份整行都在屏幕里
        rule.press(KeyEvent.KEYCODE_DPAD_DOWN, settleMs = 1_000)
        rule.press(KeyEvent.KEYCODE_DPAD_DOWN, settleMs = 1_000)
        assertEquals("片11", rule.focusedText())
        val screen = rule.onRoot().getBoundsInRoot()
        val lastYear = rule.onNodeWithText("2011", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("最后一排的年份被屏幕下沿裁掉：$lastYear / $screen", lastYear.bottom <= screen.bottom)
        rule.savePreview("wall-titles-last-row")
    }

    @Test
    fun switchOnAccountsPageTogglesAndPersists() {
        val env = rule.setTvContent(emptyMap()) {
            CompositionLocalProvider(LocalRouter provides Router()) { AccountsScreen() }
        }
        rule.waitForIdle()
        assertFalse(env.graph.wallTitles)
        rule.onNodeWithText("海报标题：隐藏").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertTrue(env.graph.wallTitles)
        rule.onNodeWithText("海报标题：显示").assertExists()
        // 下次启动照样是开着的
        assertTrue(AppGraph(rule.activity.application).wallTitles)
        // 再按一下关回去，也记住
        rule.onNodeWithText("海报标题：显示").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertFalse(env.graph.wallTitles)
        assertFalse(AppGraph(rule.activity.application).wallTitles)
    }
}
