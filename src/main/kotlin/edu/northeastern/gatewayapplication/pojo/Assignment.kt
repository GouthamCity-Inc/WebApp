package edu.northeastern.gatewayapplication.pojo

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UuidGenerator
import org.hibernate.type.SqlTypes
import java.sql.Date
import java.util.*

@Entity
@Table(name = "assignment")
data class Assignment(

    @Id
    @GeneratedValue(generator = "uuid2")
    @Column(name = "id", nullable = false, columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var id: UUID?,

    var name: String?,

    var points: Int?,

    @JsonProperty("num_of_attempts")
    @Column(name = "num_of_attempts")
    var attempts: Int?,

    var deadline: String?,

    @JsonProperty("assignment_created")
    @Column(name = "assignment_created")
    var created: String?,

    @JsonProperty("assignment_updated")
    @Column(name = "assignment_updated")
    var updated: String?,

    @JsonIgnore
    @ManyToOne
    var user: Account? = null,

    @JsonIgnore
    @OneToMany
    var submissions: MutableList<Submission>
){
    constructor(): this(null, null, null, 0, null, null, null, submissions = mutableListOf())

    @Override
    override fun toString(): String {
        return "Assignment(id=$id, name='$name', points=$points, attempts=$attempts, " +
                "deadline=$deadline, created=$created, updated=$updated, user=$user)" +
                "submissions=$submissions"
    }
}