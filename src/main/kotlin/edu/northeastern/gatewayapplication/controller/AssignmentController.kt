package edu.northeastern.gatewayapplication.controller

import edu.northeastern.gatewayapplication.pojo.Assignment
import edu.northeastern.gatewayapplication.service.AccountService
import edu.northeastern.gatewayapplication.service.AssignmentService
import edu.northeastern.gatewayapplication.utils.Utils
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val logger = mu.KotlinLogging.logger {}

@RestController
@RequestMapping("/v1/assignments")
class AssignmentController(
    private val accountService: AccountService,
    private val assignmentService: AssignmentService) {

    private val utils = Utils()

    @GetMapping(produces = ["application/json"])
    fun getAssignments(authentication: Authentication, @RequestBody(required = false) requestBody: String?, httpRequest: HttpServletRequest): ResponseEntity<List<Assignment>> {
        logger.info { "Getting assignments for user ${authentication.name}" }
        if (utils.requestContainBodyOrParams(requestBody, httpRequest))
            return ResponseEntity.badRequest().build()

        return ResponseEntity.ok(assignmentService.getAll())
    }

    @GetMapping("/{id}", produces = ["application/json"])
    fun getAssignment(authentication: Authentication, @PathVariable id: UUID): ResponseEntity<Assignment> {
        logger.info { "Getting assignment for user ${authentication.name}" }
        val assignment = assignmentService.get(id)
        if (assignment.isEmpty) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(assignment.get())
    }

    @PutMapping("/{id}", produces = ["application/json"], consumes = ["application/json"])
    fun updateAssignment(authentication: Authentication, @PathVariable id: UUID, @RequestBody assignment: Assignment): ResponseEntity<Assignment> {
        logger.info { "Updating assignment for user ${authentication.name}" }

        val assignmentInDB = assignmentService.get(id)
        // 404 if assignment doesn't exist
        if (assignmentInDB.isEmpty) {
            return ResponseEntity.notFound().build()
        }

        val requestUser = accountService.getByEmail(authentication.name)
        val userOfAssignment = assignmentInDB.get().user
        // 403 if user is not the owner of the assignment
        if (requestUser.get().id != userOfAssignment!!.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        // 400 if points are not in range
        if (utils.isValidPoints(assignment.points!!).not()) {
            return ResponseEntity.badRequest().build()
        }

        assignmentInDB.map {
            it.name = assignment.name
            it.points = assignment.points
            it.attempts = assignment.attempts
            it.deadline = assignment.deadline
            it.updated = utils.getCurrentTimeInISO8601()
        }

        // 204 if successful
        assignmentService.save(assignmentInDB.get())
        return ResponseEntity.noContent().build()
    }


    @PostMapping(produces = ["application/json"], consumes = ["application/json"])
    fun createAssignment(authentication: Authentication, @RequestBody assignment: Assignment): ResponseEntity<Assignment> {
        logger.info { "Creating assignment for user ${authentication.name}" }
        // 400 if points are not in range or mandatory fields are not present
        if (utils.areMandatoryFieldsPresent(assignment).not() || utils.isValidPoints(assignment.points!!).not()) {
            return ResponseEntity.badRequest().build()
        }

        val currentTime = utils.getCurrentTimeInISO8601()
        assignment.apply {
            created = currentTime
            updated = currentTime
        }

        val account = accountService.getByEmail(authentication.name)
        account.get().assignments.add(assignment)
        assignment.user = account.get()

        return ResponseEntity<Assignment>(assignmentService.save(assignment), HttpStatus.CREATED)
    }

    @DeleteMapping("/{id}", produces = ["application/json"])
    fun deleteAssignment(authentication: Authentication, @PathVariable id: UUID): ResponseEntity<Assignment> {
        logger.info { "Deleting assignment for user ${authentication.name}" }
        val assignment = assignmentService.get(id)
        if (assignment.isEmpty) {
            return ResponseEntity.notFound().build()
        }

        val requestUser = accountService.getByEmail(authentication.name)
        if (requestUser.get().id != assignment.get().user?.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        assignmentService.delete(id)
        return ResponseEntity.noContent().build()
    }
}