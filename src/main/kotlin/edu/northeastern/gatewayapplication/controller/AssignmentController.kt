package edu.northeastern.gatewayapplication.controller

import com.timgroup.statsd.NonBlockingStatsDClient
import edu.northeastern.gatewayapplication.pojo.Assignment
import edu.northeastern.gatewayapplication.service.AccountService
import edu.northeastern.gatewayapplication.service.AssignmentService
import edu.northeastern.gatewayapplication.utils.Utils
import io.micrometer.core.instrument.Metrics
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*

private val logger = mu.KotlinLogging.logger {}
private val metricsReporter = NonBlockingStatsDClient("webapp", "localhost", 8125)

@RestController
@RequestMapping("/v1/assignments")
class AssignmentController(
    private val accountService: AccountService,
    private val assignmentService: AssignmentService) {

    private val utils = Utils()

    @GetMapping(produces = ["application/json"])
    fun getAssignments(authentication: Authentication?, @RequestBody(required = false) requestBody: String?, httpRequest: HttpServletRequest): ResponseEntity<List<Assignment>> {
        metricsReporter.increment("assignment.get")
        if (authentication == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info { "Getting assignments for user ${authentication.name}" }
        if (utils.requestContainBodyOrParams(requestBody, httpRequest))
            return ResponseEntity.badRequest().build()

        return ResponseEntity.ok(assignmentService.getAll())
    }

    @GetMapping("/{id}", produces = ["application/json"])
    fun getAssignment(authentication: Authentication?, @PathVariable id: UUID): ResponseEntity<Assignment> {
        metricsReporter.increment("assignment.get.$id")
        if (authentication == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info { "Getting assignment for user ${authentication.name}" }
        val assignment = assignmentService.get(id)
        if (assignment.isEmpty) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(assignment.get())
    }

    @PutMapping("/{id}", produces = ["application/json"], consumes = ["application/json"])
    fun updateAssignment(authentication: Authentication?, @PathVariable id: UUID, @RequestBody assignment: Assignment): ResponseEntity<Assignment> {
        metricsReporter.increment("assignment.put")
        if (authentication == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info { "Updating assignment for user ${authentication.name}" }

        if (utils.areMandatoryFieldsPresent(assignment).not() ||
            utils.isValidPoints(assignment.points!!).not() ||
            utils.isValidAttempts(assignment.attempts!!).not() ||
            utils.isValidDate(assignment.deadline!!).not()
        ) {
            return ResponseEntity.badRequest().build()
        }

        val assignmentInDB = assignmentService.get(id)
        if (assignmentInDB.isPresent.not()) {
            return ResponseEntity.notFound().build()
        }

        val requestUser = accountService.getByEmail(authentication.name)
        val userOfAssignment = assignmentInDB.get().user
        // 403 if user is not the owner of the assignment
        if (requestUser.get().id != userOfAssignment!!.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        // 404 if assignment doesn't exist
        if (assignmentInDB.isEmpty) {
            return ResponseEntity.notFound().build()
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
    fun createAssignment(authentication: Authentication?, @RequestBody assignment: Assignment): ResponseEntity<Assignment> {
        metricsReporter.increment("assignment.post")
        if (authentication == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info { "Creating assignment for user ${authentication.name}" }
        // 400 if points are not in range or mandatory fields are not present
        if (utils.areMandatoryFieldsPresent(assignment).not() ||
            utils.isValidPoints(assignment.points!!).not() ||
            utils.isValidAttempts(assignment.attempts!!).not() ||
            utils.isValidDate(assignment.deadline!!).not()
        ) {
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
    fun deleteAssignment(authentication: Authentication?, @PathVariable id: UUID): ResponseEntity<Assignment> {
        metricsReporter.increment("assignment.get")
        if (authentication == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

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