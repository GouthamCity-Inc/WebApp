package edu.northeastern.gatewayapplication.utils

import edu.northeastern.gatewayapplication.pojo.Assignment
import jakarta.servlet.http.HttpServletRequest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class Utils {

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

    fun requestContainBodyOrParams(requestBody: String?, httpRequest: HttpServletRequest): Boolean{
        return requestBody.isNullOrEmpty().not() || httpRequest.parameterMap.isNotEmpty()
    }
}