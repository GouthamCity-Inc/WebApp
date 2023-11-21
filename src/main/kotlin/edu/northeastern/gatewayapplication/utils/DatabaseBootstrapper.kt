package edu.northeastern.gatewayapplication.utils

import com.opencsv.CSVReader
import edu.northeastern.gatewayapplication.pojo.Account
import edu.northeastern.gatewayapplication.service.AccountService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import java.io.FileReader
import java.io.IOException

private val logger = KotlinLogging.logger{}

@Component
class DatabaseBootstrapper(
    private val accountService: AccountService,
    private val bcryptEncoder: BcryptEncoder
): ApplicationListener<ApplicationReadyEvent> {

    @Value("\${application.config.csv-file}")
    private lateinit var filePath: String

    private val utils = GenericUtils()

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        logger.info { "Bootstrapping the database" }
        logger.info { filePath }

        try {
            CSVReader(FileReader(filePath)).use { reader ->
                val header = reader.readNext() // Read headers if present

                if (header != null) {
                    // Print headers
                    header.forEach { columnHeader -> print("$columnHeader\t") }
                    println() // Move to the next line
                }

                var line: Array<String>?
                while (reader.readNext().also { line = it } != null) {
                    val currentTime = utils.getCurrentTimeInISO8601()
                    val account = Account(
                        firstName = line!![0],
                        lastName = line!![1],
                        email = line!![2],
                        password = bcryptEncoder.encode(line!![3]),
                        accountCreated = currentTime,
                        accountUpdated = currentTime

                    )
                    accountService.save(account)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

//        csvReader().open(filePath) {
//            readAllWithHeaderAsSequence().forEach { row: Map<String, String> ->
//
//                val currentTime = utils.getCurrentTimeInISO8601()
//
//                val account = Account(
//                    firstName = row["first_name"]!!,
//                    lastName = row["last_name"]!!,
//                    email = row["email"]!!,
//                    password = bcryptEncoder.encode(row["password"]!!),
//                    accountCreated = currentTime,
//                    accountUpdated = currentTime
//                )
//                accountService.save(account)
//            }
//        }
    }

}