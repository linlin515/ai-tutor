package com.aitutor.app.domain.model

/**
 * 排行榜类型
 */
enum class LeaderboardType {
    GLOBAL,
    FRIENDS
}

/**
 * 排行榜条目
 */
data class RankEntry(
    val rank: Int,
    val nickname: String = "",
    val score: Int = 0,
    val isMe: Boolean = false
)
