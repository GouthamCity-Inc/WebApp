package edu.northeastern.gatewayapplication.security

import edu.northeastern.gatewayapplication.utils.BcryptEncoder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.provisioning.JdbcUserDetailsManager
import org.springframework.security.provisioning.UserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import javax.sql.DataSource


@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var userDetailsService: UserDetailsService

    @Autowired
    private lateinit var bcryptEncoder: BcryptEncoder

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setUserDetailsService(userDetailsService)
        authProvider.setPasswordEncoder(bcryptEncoder.encoder)
        return authProvider
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {

        http.csrf { csrf -> csrf.disable() }
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests { authorize ->
                authorize.requestMatchers("/healthz").permitAll()
                authorize.anyRequest().permitAll()
            }
            .httpBasic(Customizer.withDefaults())
        return http.build()

    }

    @Bean
    fun userDetailsManager(): UserDetailsManager {
        val jdbcUserDetailsManager = JdbcUserDetailsManager(dataSource)
        jdbcUserDetailsManager.usersByUsernameQuery = "select email,password,'true' as enabled from account where email= ?"

        return jdbcUserDetailsManager
    }
}