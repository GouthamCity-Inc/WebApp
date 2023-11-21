package edu.northeastern.gatewayapplication.repository

import edu.northeastern.gatewayapplication.pojo.Submission
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface SubmissionRepository: JpaRepository<Submission, UUID>