package edu.gwu.androidtweets.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusDto(
    val id: String,
    val content: String,
    val account: AccountDto
)

@Serializable
data class AccountDto(
    val username: String,
    val acct: String,
    @SerialName("display_name") val displayName: String,
    val avatar: String
)
