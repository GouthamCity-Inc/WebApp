package edu.northeastern.gatewayapplication.controller

import com.amazonaws.services.sns.model.AmazonSNSException
import com.timgroup.statsd.NonBlockingStatsDClient
import edu.northeastern.gatewayapplication.exception.BeyondDeadlineException
import edu.northeastern.gatewayapplication.exception.InvalidURLException
import edu.northeastern.gatewayapplication.exception.MaxAttemptsExceededException
import edu.northeastern.gatewayapplication.pojo.Assignment
import edu.northeastern.gatewayapplication.pojo.SNSMessage
import edu.northeastern.gatewayapplication.pojo.Submission
import edu.northeastern.gatewayapplication.service.AccountService
import edu.northeastern.gatewayapplication.service.AssignmentService
import edu.northeastern.gatewayapplication.service.SubmissionService
import edu.northeastern.gatewayapplication.utils.AmazonSNSUtils
import edu.northeastern.gatewayapplication.utils.GenericUtils
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
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
@PropertySource("classpath:application.properties")
class AssignmentController(
    private val accountService: AccountService,
    private val assignmentService: AssignmentService,
    private val submissionService: SubmissionService,
    @Value("\${application.config.topic-arn}")
    private val topicArn: String,
) {

    private val utils = GenericUtils()
    private val snsUtils = AmazonSNSUtils()

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
    fun createSubmission(authentication: Authentication?, @PathVariable id: UUID, @RequestBody submission: Submission): ResponseEntity<*> {
        logger.info { "Hitting /submission endpoint" }
        metricsReporter.increment("assignment.submissions.post")
        var response: ResponseEntity.BodyBuilder
        var status = "SUCCESS"
        var message: String
        var assignmentID = ""
        var submissionID = ""
        var attempt: Int = 1

        if (authentication == null){
            logger.info { "/submission - access credentials not provided" }
            response = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            return response.body(utils.generateErrorSchema("access credentials not provided", 401))
        }

        try{
            if (utils.isURLPresentInSubmission(submission).not()){
                logger.info { "/submission - bad request : submission_url not present" }
                response = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                return response.body(utils.generateErrorSchema("mandatory field: submission URL is not present", 400))
            }

            val url = URL(submission.url)
            val contentType = url.openConnection().contentType
            val contentLength = url.openConnection().contentLength
            if (contentType != "application/zip" || contentLength == 0){
                logger.info { "/submission - bad request : submission URL does not return downloadable ZIP or ZIP is empty" }
                status = "FAILURE"
                throw InvalidURLException("Submission URL does not return downloadable ZIP or ZIP is empty")
            }

            // if assignment doesn't exist, return 404
            val assignmentOptional = assignmentService.get(id)
            if (assignmentOptional.isEmpty) {
                logger.info { "/submission - bad request : assignment with ID: $id not found" }
                response = ResponseEntity.status(HttpStatus.NOT_FOUND)
                return response.body(utils.generateErrorSchema("assignment with ID: $id not found", 404))
            }

            val assignment = assignmentOptional.get()
            assignmentID = assignment.id.toString()

            if (!utils.isValidSubmission(assignment.deadline!!)){
                logger.info { "/submission - bad request: assignment deadline has passed" }
                throw BeyondDeadlineException("assignment deadline exceeded")
            }

            // if the size of submissions list is greater than the number of attempts, return 400
            if (assignment.submissions.size >= assignment.attempts!!) {
                logger.info { "/submission - bad request : maximum assignment submissions exceed for assignment ${assignment.id}" }
                throw MaxAttemptsExceededException("")

            }

            submission.assignID = assignment.id
            submission.submissionDate = utils.getCurrentTimeInISO8601()
            submission.submissionUpdated = utils.getCurrentTimeInISO8601()
            submissionService.save(submission)
            submissionID = submission.id.toString()

            assignment.submissions.add(submission)
            assignmentService.save(assignment)
            attempt = assignment.submissions.size

            logger.info { "saved submission ${assignment.submissions.size} for assignment ${assignment.id}" }

            val snsMessage = utils.serializeSNSMessage(
                SNSMessage(
                    assignmentID, submissionID, submission.url!!, authentication.name,
                    status, "", assignment.submissions.size, submission.submissionDate!!
                )
            )
            val topic = snsUtils.filterTopicByName(topicArn)
            message = snsMessage
            if (topic.isPresent) {
                val publishRequest = snsUtils.getPublishRequest(topic.get().topicArn, snsMessage)
                snsUtils.publishMessage(publishRequest)
                logger.info { "successfully published message to SNS topic with ARN ${topic.get().topicArn}" }
            }
            return ResponseEntity.ok(submission)

        } catch (e: Exception){
            return when(e){
                is InvalidURLException, is BeyondDeadlineException, is MaxAttemptsExceededException -> {
                    message = if (e is InvalidURLException) "Invalid submission URL: not a ZIP or empty ZIP received"
                            else if (e is BeyondDeadlineException) "Invalid submission: the deadline for the submission has passed"
                            else "Invalid Submission: Maximum attempts exceeded"
                    val snsMessage = utils.serializeSNSMessage(
                        SNSMessage(
                            assignmentID, submissionID, submission.url!!, authentication.name,
                            "ERROR", message, attempt, utils.getCurrentDate()
                        )
                    )
                    val topic = snsUtils.filterTopicByName(topicArn)
                    if (topic.isPresent) {
                        val publishRequest = snsUtils.getPublishRequest(topic.get().topicArn, snsMessage)
                        snsUtils.publishMessage(publishRequest)
                        logger.info { "successfully published message to SNS topic with ARN ${topic.get().topicArn}" }
                    }
                    if (e is InvalidURLException){
                        response = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        response.body(utils.generateErrorSchema(message, 400))
                    } else{
                        response = ResponseEntity.status(HttpStatus.FORBIDDEN)
                        response.body(utils.generateErrorSchema(message, 403))
                    }
                }
                is AmazonSNSException -> {
                    response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    response.body(utils.generateErrorSchema("exception occurred while publishing SNS message", 500))
                }
                else -> {
                    response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    response.body(utils.generateErrorSchema("exception occurred during submission", 500))
                }
            }
        }
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