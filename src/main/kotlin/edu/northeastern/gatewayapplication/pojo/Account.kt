package edu.northeastern.gatewayapplication.pojo

import jakarta.persistence.*
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.sql.Date
import java.sql.SQLType
import java.util.*

@Entity
@Table(name = "account")
data class Account (

    @Id
    @GeneratedValue(generator = "uuid2")
    @Column(name = "id", nullable = false, columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var id: UUID? = null,

    @Column(name = "first_name")
    val firstName: String = "",

    @Column(name = "last_name")
    val lastName: String = "",

    val password: String = "",

    val email: String= "",

    @Column(name = "account_created")
    val accountCreated: String? = null,

    @Column(name = "account_updated")
    var accountUpdated: String? = null,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var assignments: MutableList<Assignment> = mutableListOf()
){
    override fun toString(): String {
        return "Account(id=$id, firstName='$firstName', lastName='$lastName', password='$password', email='$email', accountCreated=$accountCreated, accountUpdated=$accountUpdated"
    }
}