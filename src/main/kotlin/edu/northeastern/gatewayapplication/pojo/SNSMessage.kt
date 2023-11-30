package edu.northeastern.gatewayapplication.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class SNSMessage(
    @JsonProperty("assignment_id")
    val id: String,

    @JsonProperty("submission_id")
    val submissionID: String,

    @JsonProperty("submission_url")
    val url: String,

    @JsonProperty("email")
    val email: String,

    val status: String,

    val message: String,

    @JsonProperty("attempt")
    val attempt: Int,

    val timestamp: String
)