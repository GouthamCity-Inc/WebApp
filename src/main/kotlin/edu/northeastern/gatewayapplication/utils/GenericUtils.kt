package edu.northeastern.gatewayapplication.utils

import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import edu.northeastern.gatewayapplication.pojo.Assignment
import edu.northeastern.gatewayapplication.pojo.ErrorSchema
import edu.northeastern.gatewayapplication.pojo.SNSMessage
import edu.northeastern.gatewayapplication.pojo.Submission
import jakarta.servlet.http.HttpServletRequest
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class GenericUtils {

    fun getCurrentTimeInISO8601(): String {
        val currentTime = ZonedDateTime.now()
        val formatter = DateTimeFormatter.ISO_INSTANT
        return currentTime.format(formatter)
    }

    fun areMandatoryFieldsPresent(assignment: Assignment): Boolean{
        return assignment.name != null && assignment.points != null && assignment.attempts != null && assignment.deadline != null
    }


    fun isValidPoints(points: Int): Boolean{
        return points in 1..10
    }

    fun isValidAttempts(attempts: Int): Boolean{
        return attempts > 0
    }

    fun requestContainBodyOrParams(requestBody: String?, httpRequest: HttpServletRequest): Boolean{
        return requestBody.isNullOrEmpty().not() || httpRequest.parameterMap.isNotEmpty()
    }

    fun isValidDate(dateString: String): Boolean {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        val dateFormat = SimpleDateFormat(format)
        return try {
            val date = dateFormat.parse(dateString)
            if (date != null) {
                val currentDate = Date()
                val dateStr = dateFormat.format(currentDate)
                !date.before(dateFormat.parse(dateStr))
            } else {
                false
            }
        } catch (e: ParseException) {
            e.printStackTrace()
            false
        }
    }

    fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        val currentDate = Date()
        return dateFormat.format(Date())
    }

    fun isValidSubmission(dateString: String): Boolean {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        val dateFormat = SimpleDateFormat(format)
        return try {
            val submissionDate = dateFormat.parse(dateString)
            if (submissionDate != null) {
                val currentDate = Date()
                val dateStr = dateFormat.format(currentDate)
                dateFormat.parse(dateStr).before(submissionDate)
            } else {
                false
            }
        } catch (e: ParseException) {
            e.printStackTrace()
            false
        }
    }

    fun isURLPresentInSubmission(submission: Submission): Boolean{
        return submission.url != null
    }

    fun generateErrorSchema(message: String, status: Int): ErrorSchema {
        return ErrorSchema(message, status, getCurrentTimeInISO8601())
    }

    fun serializeSNSMessage(message: SNSMessage): String{
        val objectMapper = ObjectMapper()
        return objectMapper.writeValueAsString(message)
    }
}