package edu.northeastern.gatewayapplication.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.*

@Entity
@Table(name = "submission")
data class Submission(

    @Id
    @GeneratedValue(generator = "uuid2")
    @Column(name = "id", nullable = false, columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var id: UUID? = null,

    @Column(name = "assignment_id")
    @JsonProperty("assignment_id")
    var assignID: UUID? = null,

    @Column(name = "submission_url")
    @JsonProperty("submission_url")
    val submissionURL: String? = null,

    @Column(name = "submission_date")
    @JsonProperty("submission_date")
    var submissionDate: String? = null,

    @Column(name = "submission_updated")
    @JsonProperty("submission_updated")
    var submissionUpdated: String? = null,
){
    override fun toString(): String {
        return "Submission(id=$id, assignID=$assignID, submissionURL=$submissionURL, submissionDate=$submissionDate, submissionUpdated=$submissionUpdated"
    }
}