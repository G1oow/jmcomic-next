package com.par9uet.jm.utils

import com.par9uet.jm.data.models.Comment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentKeywordFilterTest {

    private fun sampleComment(
        id: Int = 1,
        content: String = "正常讨论剧情",
        username: String = "reader",
        nickname: String = "普通读者",
        replies: List<Comment> = emptyList(),
    ) = Comment(
        userId = id * 10,
        comicId = 100,
        id = id,
        time = "2026-01-01",
        content = content,
        likeCount = 0,
        username = username,
        nickname = nickname,
        avatar = "",
        parentId = 0,
        spoiler = false,
        replyCommentList = replies,
    )

    @Test
    fun parsesBatchKeywordInputAndDeduplicatesCaseInsensitively() {
        val parsed = parseCommentKeywordInput("  加V, 加v，看主页; 澳门；\n  http:// \n  ")
        assertEquals(listOf("加V", "看主页", "澳门", "http://"), parsed)
    }

    @Test
    fun resolvesActiveKeywordsOnlyWhenEnabled() {
        assertTrue(
            resolveActiveCommentKeywords(
                enabled = false,
                customKeywords = listOf("广告词"),
                enabledPresetIds = listOf(COMMENT_PRESET_PROMO_LINK),
            ).isEmpty()
        )

        val resolved = resolveActiveCommentKeywords(
            enabled = true,
            customKeywords = listOf("定制广告", "HTTP://"),
            enabledPresetIds = listOf(COMMENT_PRESET_PROMO_LINK, "unknown_preset"),
        )
        assertTrue(resolved.contains("http://"))
        assertTrue(resolved.contains("定制广告"))
        assertEquals(1, resolved.count { it.equals("http://", ignoreCase = true) })
    }

    @Test
    fun filtersTopLevelAdsAndPreservesCleanComments() {
        val adComment = sampleComment(id = 1, content = "<p>进主页看完整版，加 v 123456</p>")
        val fullWidthAd = sampleComment(id = 2, content = "访问 ＷＷＷ．ｅｘａｍｐｌｅ．ＣＯＭ 领取福利")
        val normalComment = sampleComment(id = 3, content = "画风太棒了，期待下一话！")

        val activeKeywords = resolveActiveCommentKeywords(
            enabled = true,
            customKeywords = emptyList(),
            enabledPresetIds = listOf(COMMENT_PRESET_PROMO_LINK, COMMENT_PRESET_CONTACT),
        )
        val filtered = listOf(adComment, fullWidthAd, normalComment).filterBlockedComments(activeKeywords)
        assertEquals(listOf(3), filtered.map { it.id })
    }

    @Test
    fun filtersAdRepliesWithoutHidingCleanParentComment() {
        val spamReply = sampleComment(id = 11, content = "威尼斯人首存送豪礼")
        val cleanReply = sampleComment(id = 12, content = "同感，这章分镜很舒服")
        val parent = sampleComment(
            id = 10,
            content = "这一话节奏很好",
            replies = listOf(spamReply, cleanReply),
        )

        val activeKeywords = resolveActiveCommentKeywords(
            enabled = true,
            customKeywords = emptyList(),
            enabledPresetIds = listOf(COMMENT_PRESET_GAMBLING_SCAM),
        )
        val filtered = listOf(parent).filterBlockedComments(activeKeywords)
        assertEquals(1, filtered.size)
        assertEquals(listOf(12), filtered.first().replyCommentList.map { it.id })
    }

    @Test
    fun matchesAdNicknameAndStripsHtmlMarkup() {
        val adAccountComment = sampleComment(
            id = 20,
            content = "好",
            nickname = "点我头像看资源",
        )
        val htmlOnlyComment = sampleComment(
            id = 21,
            content = "<p class=\"com-box\">剧情很感人<br/>赞</p>",
        )

        assertTrue(adAccountComment.matchesCommentKeywords(listOf("点我头像")))
        assertFalse(htmlOnlyComment.matchesCommentKeywords(listOf(".com", "com-box")))
        assertEquals("剧情很感人 赞", extractCommentPlainText(htmlOnlyComment.content))
    }

    @Test
    fun normalizesPresetIdsInCanonicalOrder() {
        val normalized = normalizeCommentFilterPresetIds(
            listOf(
                COMMENT_PRESET_RESOURCE_SELL,
                "invalid",
                COMMENT_PRESET_PROMO_LINK,
                COMMENT_PRESET_RESOURCE_SELL,
            )
        )
        assertEquals(
            listOf(COMMENT_PRESET_PROMO_LINK, COMMENT_PRESET_RESOURCE_SELL),
            normalized,
        )
    }
}
