package com.par9uet.jm.utils

import com.par9uet.jm.data.models.Comment

data class CommentFilterPreset(
    val id: String,
    val name: String,
    val description: String,
    val keywords: List<String>,
)

const val COMMENT_PRESET_PROMO_LINK = "promo_link"
const val COMMENT_PRESET_CONTACT = "contact"
const val COMMENT_PRESET_GAMBLING_SCAM = "gambling_scam"
const val COMMENT_PRESET_RESOURCE_SELL = "resource_sell"

val DEFAULT_COMMENT_FILTER_PRESETS: List<CommentFilterPreset> = listOf(
    CommentFilterPreset(
        id = COMMENT_PRESET_PROMO_LINK,
        name = "外链与站外引流",
        description = "屏蔽常见网址、域名推广及诱导访问外部站点的评论",
        keywords = listOf(
            "http://",
            "https://",
            "www.",
            ".com",
            ".vip",
            ".xyz",
            ".cc",
            ".top",
            ".me",
            ".icu",
            "t.me/",
            "浏览器打开",
            "复制到浏览器",
            "点我头像",
            "看我主页",
            "进主页",
            "看主页",
            "主页有惊喜",
            "看简介",
        ),
    ),
    CommentFilterPreset(
        id = COMMENT_PRESET_CONTACT,
        name = "社交引流与加群",
        description = "屏蔽留微信号、QQ、Telegram 等联系方式拉群引流广告",
        keywords = listOf(
            "加微",
            "加v",
            "加vx",
            "加qq",
            "微信号",
            "扣扣群",
            "qq群",
            "tg群",
            "电报群",
            "telegram",
            "飞机群",
            "私聊我",
            "私信我",
            "同城约",
            "约炮",
            "空降",
            "可约",
            "外围",
            "兼职日结",
        ),
    ),
    CommentFilterPreset(
        id = COMMENT_PRESET_GAMBLING_SCAM,
        name = "博彩与灰产广告",
        description = "屏蔽棋牌、赌博、刷单、诈骗等常见黑灰产推广文案",
        keywords = listOf(
            "澳门",
            "威尼斯人",
            "葡京",
            "棋牌",
            "菠菜",
            "博彩",
            "首存",
            "返水",
            "百家乐",
            "捕鱼",
            "上下分",
            "日赚",
            "刷单",
            "包赔",
            "开户送",
            "注册送",
        ),
    ),
    CommentFilterPreset(
        id = COMMENT_PRESET_RESOURCE_SELL,
        name = "卖片与付费资源",
        description = "屏蔽兜售网盘资源、收费合集、付费进群等广告",
        keywords = listOf(
            "完整版加",
            "无删减加",
            "全套资源",
            "打包出售",
            "付费进群",
            "门槛费",
            "网盘资源",
            "百度网盘",
            "夸克网盘",
            "蓝奏云",
            "解压码",
            "梯子推荐",
            "机场推荐",
            "翻墙节点",
        ),
    ),
)

private val PRESET_MAP: Map<String, CommentFilterPreset> =
    DEFAULT_COMMENT_FILTER_PRESETS.associateBy { it.id }

private val PRESET_ID_ORDER: Map<String, Int> =
    DEFAULT_COMMENT_FILTER_PRESETS.withIndex().associate { (index, preset) -> preset.id to index }

private val HTML_LINE_BREAK_REGEX = Regex("<br\\s*/?>|</p\\s*>", RegexOption.IGNORE_CASE)
private val HTML_TAG_REGEX = Regex("<[^>]+>")
private val WHITESPACE_REGEX = Regex("\\s+")

fun normalizeCommentKeyword(value: String): String = value.trim()

fun normalizeCommentKeywordList(keywords: List<String>): List<String> {
    return keywords
        .map(::normalizeCommentKeyword)
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
}

fun parseCommentKeywordInput(input: String): List<String> {
    if (input.isBlank()) return emptyList()
    return normalizeCommentKeywordList(
        input.split('\n', '\r', ',', '，', ';', '；')
    )
}

fun normalizeCommentFilterPresetIds(presetIds: List<String>): List<String> {
    return presetIds
        .map { it.trim() }
        .filter { it in PRESET_MAP }
        .distinct()
        .sortedBy { PRESET_ID_ORDER[it] ?: Int.MAX_VALUE }
}

fun resolveActiveCommentKeywords(
    enabled: Boolean,
    customKeywords: List<String>,
    enabledPresetIds: List<String>,
): List<String> {
    if (!enabled) return emptyList()
    val presetKeywords = normalizeCommentFilterPresetIds(enabledPresetIds)
        .flatMap { PRESET_MAP[it]?.keywords.orEmpty() }
    return normalizeCommentKeywordList(presetKeywords + customKeywords)
}

fun extractCommentPlainText(rawContent: String): String {
    if (rawContent.isBlank()) return ""
    return rawContent
        .replace(HTML_LINE_BREAK_REGEX, " ")
        .replace(HTML_TAG_REGEX, "")
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace(WHITESPACE_REGEX, " ")
        .trim()
}

private fun normalizeForMatch(text: String): String {
    if (text.isEmpty()) return ""
    val sb = StringBuilder(text.length)
    for (ch in text) {
        val mapped = when {
            ch == '\u3000' -> ' '
            ch in '\uFF01'..'\uFF5E' -> (ch.code - 0xFEE0).toChar()
            else -> ch
        }
        sb.append(mapped.lowercaseChar())
    }
    return sb.toString()
}

private fun buildNormalizedTargets(keywords: List<String>): List<String> {
    return normalizeCommentKeywordList(keywords)
        .map(::normalizeForMatch)
        .filter { it.isNotBlank() }
        .distinct()
}

fun Comment.matchesCommentKeywords(keywords: List<String>): Boolean {
    val targets = buildNormalizedTargets(keywords)
    if (targets.isEmpty()) return false
    return matchesNormalizedTargets(targets)
}

private fun Comment.matchesNormalizedTargets(targets: List<String>): Boolean {
    val searchableFields = listOf(
        extractCommentPlainText(content),
        nickname,
        username,
    )
    return searchableFields.any { field ->
        if (field.isBlank()) return@any false
        val normalizedField = normalizeForMatch(field)
        val compactField = normalizedField.replace(WHITESPACE_REGEX, "")
        targets.any { target ->
            normalizedField.contains(target) ||
                (target.length >= 2 && compactField.contains(target.replace(WHITESPACE_REGEX, "")))
        }
    }
}

fun List<Comment>.filterBlockedComments(keywords: List<String>): List<Comment> {
    val targets = buildNormalizedTargets(keywords)
    if (targets.isEmpty()) return this
    return filterBlockedCommentsInternal(targets)
}

private fun List<Comment>.filterBlockedCommentsInternal(targets: List<String>): List<Comment> {
    return mapNotNull { comment ->
        if (comment.matchesNormalizedTargets(targets)) {
            null
        } else if (comment.replyCommentList.isEmpty()) {
            comment
        } else {
            val filteredReplies = comment.replyCommentList.filterBlockedCommentsInternal(targets)
            if (filteredReplies.size == comment.replyCommentList.size) {
                comment
            } else {
                comment.copy(replyCommentList = filteredReplies)
            }
        }
    }
}
