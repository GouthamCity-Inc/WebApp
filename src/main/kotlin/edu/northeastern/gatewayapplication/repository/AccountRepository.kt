package edu.northeastern.gatewayapplication.repository

import edu.northeastern.gatewayapplication.pojo.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AccountRepository: JpaRepository<Account, UUID>