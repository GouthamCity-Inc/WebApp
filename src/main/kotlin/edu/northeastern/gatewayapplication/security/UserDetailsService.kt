package edu.northeastern.gatewayapplication.security

import edu.northeastern.gatewayapplication.pojo.Account
import edu.northeastern.gatewayapplication.service.AccountService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class UserDetailsService(val accountService: AccountService): UserDetailsService {

    override fun loadUserByUsername(username: String?): UserDetails {
        val account: Optional<Account> = accountService.getByEmail(username!!)

        account.orElseThrow { UsernameNotFoundException("User not found: $username") }

        return account.map { UserDetails(it) }.get()
    }
}