package com.aitutor.app.data.remote.dto

import com.aitutor.app.domain.model.RankEntry

/**
 * 排行榜响应 DTO
 */
data class LeaderboardResponse(
    val rankings: List<LeaderboardItem> = emptyList(),
    val myRank: LeaderboardItem? = null
)

data class LeaderboardItem(
    val userId: String = "",
    val nickname: String = "",
    val avatar: String = "",
    val score: Int = 0,
    val rank: Int = 0
) {
    fun toRankEntry(isMe: Boolean = false): RankEntry {
        return RankEntry(
            rank = rank,
            nickname = nickname,
            score = score,
            isMe = isMe
        )
    }
}
