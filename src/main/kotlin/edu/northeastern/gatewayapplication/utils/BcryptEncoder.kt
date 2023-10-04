package edu.northeastern.gatewayapplication.utils

import org.springframework.context.annotation.Scope
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
@Scope("singleton")
class BcryptEncoder {

    val encoder: BCryptPasswordEncoder = BCryptPasswordEncoder()

    fun encode(password: String): String {
        return encoder.encode(password)
    }
}