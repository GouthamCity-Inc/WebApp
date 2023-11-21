package edu.northeastern.gatewayapplication.service

import edu.northeastern.gatewayapplication.pojo.Submission
import edu.northeastern.gatewayapplication.repository.SubmissionRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SubmissionService(
    private val repository: SubmissionRepository
) {
    fun get(id: UUID): Submission {
        return repository.findById(id).get()
    }

    fun getAll(): List<Submission> {
        return repository.findAll().toList()
    }

    fun delete(id: UUID) {
        repository.deleteById(id)
    }

    fun save(submission: Submission): Submission {
        return repository.save(submission)
    }
}