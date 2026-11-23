package com.par9uet.jm.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateVersionTest {

    @Test
    fun normalizesTagPrefixesAndSuffixes() {
        assertEquals("1.2.6", normalizeVersion("v1.2.6"))
        assertEquals("1.2.6", normalizeVersion("V1.2.6"))
        assertEquals("1.2.6", normalizeVersion("  1.2.6  "))
        assertEquals("1.2.6", normalizeVersion("1.2.6-beta1"))
        assertEquals("1.2.6", normalizeVersion("1.2.6 更新"))
    }

    @Test
    fun comparesVersionsSegmentBySegment() {
        assertTrue(compareVersion("1.2.7", "1.2.6") > 0)
        assertTrue(compareVersion("1.3.0", "1.2.9") > 0)
        assertTrue(compareVersion("2.0", "1.9.9") > 0)
        assertTrue(compareVersion("1.2.6", "1.2.10") < 0)
        assertEquals(0, compareVersion("v1.2.6", "1.2.6"))
        assertEquals(0, compareVersion("1.2", "1.2.0"))
    }

    @Test
    fun treatsUnknownInstalledVersionAsNoUpdate() {
        assertFalse(isUpdateAvailable("1.2.6", UNKNOWN_APP_VERSION))
        assertFalse(isUpdateAvailable("1.2.6", ""))
    }

    @Test
    fun detectsUpdateOnlyForNewerRelease() {
        assertTrue(isUpdateAvailable("1.2.7", "1.2.6"))
        assertFalse(isUpdateAvailable("1.2.6", "1.2.6"))
        assertFalse(isUpdateAvailable("1.2.5", "1.2.6"))
    }

    @Test
    fun prefersApkAssetMatchingReleaseVersion() {
        val assets = listOf(
            ReleaseApkAsset("jm-mobile_v1.2.5_aaaa.apk", "https://example.com/old.apk"),
            ReleaseApkAsset("jm-mobile_v1.2.6_bbbb.apk", "https://example.com/new.apk"),
        )

        assertEquals("https://example.com/new.apk", selectApkAsset(assets, "v1.2.6")?.downloadUrl)
    }

    @Test
    fun fallsBackToFirstApkWhenNameDoesNotMatch() {
        val assets = listOf(
            ReleaseApkAsset("release.apk", "https://example.com/any.apk"),
            ReleaseApkAsset("notes.txt", "https://example.com/notes.txt"),
        )

        assertEquals("release.apk", selectApkAsset(assets, "1.2.6")?.name)
    }

    @Test
    fun ignoresReleaseWithoutDownloadableApk() {
        val assets = listOf(
            ReleaseApkAsset("notes.txt", "https://example.com/notes.txt"),
            ReleaseApkAsset("broken.apk", ""),
        )

        assertNull(selectApkAsset(assets, "1.2.6"))
        assertNull(selectApkAsset(emptyList(), "1.2.6"))
    }
}
