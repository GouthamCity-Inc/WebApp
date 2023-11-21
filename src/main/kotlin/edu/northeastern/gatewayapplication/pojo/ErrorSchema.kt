package edu.northeastern.gatewayapplication.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize
data class ErrorSchema(

    @JsonProperty("error_message")
    val message: String,

    @JsonProperty("error_code")
    val status: Int,

    @JsonProperty("timestamp")
    val timestamp: String
)