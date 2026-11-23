package com.par9uet.jm.ui.screens.tabScreen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabTransitionDirectionTest {

    @Test
    fun targetOnTheRightEntersFromTheLeft() {
        // 底栏顺序 home → collect → user：往右切时页面从左边滑进来。
        assertTrue(entersFromLeft("home", "collect"))
        assertTrue(entersFromLeft("home", "user"))
        assertTrue(entersFromLeft("collect", "user"))
        // AI 入口被隐藏也不影响顺序。
        assertTrue(entersFromLeft("home", "user"))
    }

    @Test
    fun targetOnTheLeftEntersFromTheRight() {
        assertFalse(entersFromLeft("user", "collect"))
        assertFalse(entersFromLeft("user", "home"))
        assertFalse(entersFromLeft("collect", "home"))
    }

    @Test
    fun unknownRoutesFallBackToRightEntry() {
        assertFalse(entersFromLeft(null, "user"))
        assertFalse(entersFromLeft("home", null))
        assertFalse(entersFromLeft("removed", "user"))
    }
}
