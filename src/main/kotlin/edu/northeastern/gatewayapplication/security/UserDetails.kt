package edu.northeastern.gatewayapplication.security

import edu.northeastern.gatewayapplication.pojo.Account
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserDetails(
    private val account: Account
): UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return listOf(GrantedAuthority { "ROLE_USER" }).toMutableList()
    }

    override fun getPassword(): String {
        return account.password
    }

    override fun getUsername(): String {
        return account.email
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }
}