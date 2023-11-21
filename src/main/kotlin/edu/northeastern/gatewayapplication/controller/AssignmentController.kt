package edu.northeastern.gatewayapplication.controller

import com.timgroup.statsd.NonBlockingStatsDClient
import edu.northeastern.gatewayapplication.pojo.Assignment
import edu.northeastern.gatewayapplication.pojo.Submission
import edu.northeastern.gatewayapplication.service.AccountService
import edu.northeastern.gatewayapplication.service.AssignmentService
import edu.northeastern.gatewayapplication.service.SubmissionService
import edu.northeastern.gatewayapplication.utils.GenericUtils
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.net.URL
import java.util.*


private val logger = mu.KotlinLogging.logger {}
private val metricsReporter = NonBlockingStatsDClient("webapp", "localhost", 8125)

@RestController
@RequestMapping("/v1/assignments")
class AssignmentController(
    private val accountService: AccountService,
    private val assignmentService: AssignmentService,
    private val submissionService: SubmissionService
) {

    private val utils = GenericUtils()

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

    @PostMapping("/{id}/submission", produces = ["application/json"], consumes = ["application/json"])
    suspend fun createSubmission(authentication: Authentication?, @PathVariable id: UUID, @RequestBody submission: Submission): ResponseEntity<*> {
        metricsReporter.increment("assignment.submissions.post")
        val response: ResponseEntity.BodyBuilder

        if (authentication == null){
            response = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            return response.body(utils.generateErrorSchema("access credentials not provided", 401))
        }


        if (utils.isURLPresentInSubmission(submission).not()){
            response = ResponseEntity.status(HttpStatus.BAD_REQUEST)
            return response.body(utils.generateErrorSchema("mandatory field: submission_url is not present", 400))
        }

        // check if the url contains a zip downloadable
        val url = URL(submission.submissionURL)
        val contentType = url.openConnection().contentType
        if (contentType != "application/zip"){
            response = ResponseEntity.status(HttpStatus.BAD_REQUEST)
            return response.body(utils.generateErrorSchema("submission url does not return downloadable zip", 400))
        }

        // if assignment doesn't exist, return 404
        val assignmentOptional = assignmentService.get(id)
        if (assignmentOptional.isEmpty) {
            response = ResponseEntity.status(HttpStatus.NOT_FOUND)
            return response.body(utils.generateErrorSchema("assignment with ID: $id not found", 404))
        }

        // if user is not the owner of the assignment, return 403 unauthorized
        val assignment = assignmentOptional.get()
        if (assignment.user != null && assignment.user!!.email != authentication.name) {
            response = ResponseEntity.status(HttpStatus.FORBIDDEN)
            return response.body(utils.generateErrorSchema("${authentication.name} is not the owner of the assignment", 403))
        }

        // if the size of submissions list is greater than the number of attempts, return 400
        if (assignment.submissions.size >= assignment.attempts!!) {
            response = ResponseEntity.status(HttpStatus.BAD_REQUEST)
           return response.body(utils.generateErrorSchema("exceeded maximum attempts of ${assignment.attempts}", 400))
        }

        submission.assignID = assignment.id
        submission.submissionDate = utils.getCurrentTimeInISO8601()
        submission.submissionUpdated = utils.getCurrentTimeInISO8601()
        submissionService.save(submission)

//
//        SnsClient { region = "us-east-1" }.use { snsClient ->
//            val snsResponse = snsClient.listTopics(ListTopicsRequest { })
//            val topic = snsResponse.topics?.find { it.topicArn?.endsWith("csye-submissions.fifo") == true }!!
//            val request = PublishRequest {
//                message = "Hello!"
//                topicArn = topic.topicArn
//            }
//            snsClient.publish(request)
//        }
//
//        val snsClient = AmazonSNSClientBuilder.defaultClient()
//        val topics = snsClient.listTopics().topics
//
//        val submissionTopic = topics.stream().filter {topic ->
//            topic.topicArn.endsWith("csye-submissions.fifo")
//        }.findFirst()
//
//        if (submissionTopic.isPresent){
//            val publishRequest = PublishRequest(submissionTopic.get().topicArn, "Hello")
//            snsClient.publish(publishRequest)
//        }

        return ResponseEntity.ok(submission)
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

//    fun generateErrorEntityResponse(status: HttpStatus, message: String): ResponseEntity<*> {
//        val errorSchema = utils.generateErrorSchema(message, status.value())
//        return ResponseEntity.status(status).body(errorSchema)
//    }
}