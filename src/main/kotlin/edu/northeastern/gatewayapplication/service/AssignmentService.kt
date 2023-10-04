package edu.northeastern.gatewayapplication.service

import edu.northeastern.gatewayapplication.pojo.Assignment
import edu.northeastern.gatewayapplication.repository.AssignmentRepository
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.math.log

private val logger = mu.KotlinLogging.logger {}

@Service
class AssignmentService(private val repository: AssignmentRepository) {

    @Autowired
    private lateinit var entityManager: EntityManager

    fun get(id: UUID): Optional<Assignment> {
        return repository.findById(id)
    }

    fun getAll(): List<Assignment> {
        return repository.findAll().toList()
    }

    @Transactional
    fun delete(id: UUID) {
        val query = entityManager.createQuery("DELETE FROM Assignment a WHERE a.id = :id")
        query.setParameter("id", id)
        query.executeUpdate()
        logger.info { "Deleted assignment with id $id" }
    }

    fun save(assignment: Assignment): Assignment {
        return repository.save(assignment)
    }
}