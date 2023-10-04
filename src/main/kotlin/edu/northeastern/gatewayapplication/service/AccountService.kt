package edu.northeastern.gatewayapplication.service

import edu.northeastern.gatewayapplication.pojo.Account
import edu.northeastern.gatewayapplication.repository.AccountRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import java.util.*

@Service
class AccountService(
    private val repository: AccountRepository,
    private val entityManager: EntityManager
) {
    fun get(id: UUID): Account {
        return repository.findById(id).get()
    }

    fun getByEmail(email: String): Optional<Account> {
        return Optional.of(entityManager.createQuery("SELECT a FROM Account a WHERE a.email = :email", Account::class.java)
            .setParameter("email", email)
            .singleResult
        )
    }

    fun getAll(): List<Account> {
        return repository.findAll().toList()
    }

    fun delete(id: UUID) {
        repository.deleteById(id)
    }

    fun save(account: Account): Account {
        return repository.save(account)
    }
}