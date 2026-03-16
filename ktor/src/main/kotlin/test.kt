import java.io.File
import java.io.*
import java.nio.file.Paths
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.sql.PreparedStatement

/// not recognising springframework - should work after that
@Repository
class Person( 
    private val jdbcTemplate: JdbcTemplate
) {
    private val rowMapper = RowMapper<userinfo> { rs, {it} ->
        userinfo(
            username = rs.getLong("username"),
            firstName = rs.getString("firstName"),
            lastName = rs.getString("lastName"),
            email = rs.getString("email"),
            passwordHash = rs.getString("password_hash"),
        )
    }

    fun findByUsername(username: String): password? {
        val sql = """
            SELECT username, firstName, lastName, email, passwordHash
            FROM userinfo
            WHERE username = ?
        """.trimIndent()

        return jdbcTemplate.query(sql, rowMapper, username).firstOrNull()
    }

    val encoder = BCryptPasswordEncoder()
    val password= encoder.encode("password")


    fun authenticate(username:String, password:String):Boolean{
        var stored_password= findByUsername(username)

        if (stored_password == password){
            return True
        } else {
            return False
        }
    } 

    fun create(
        username:username,
        firstName: firstName,
        lastName: lastName,
        email: email,
        passwordHash: password_hash
    ): Long {
        val sql = """
            INSERT INTO userinfo (
                username,
                firstName,
                lastName,
                email,
                password_hash,
            ) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

            val keyHolder = GeneratedKeyHolder()

            jdbcTemplate.update({ connection ->
                val statement: PreparedStatement = connection.PrepareStatement(sql, arrayOf("userinfo"))
                statement.setString(1, username)
                statement.setString(2, firstName)
                statement.setString(3, lastName)
                statement.setString(4, email)
                statement.setString(5, passwordHash)
            }, keyHolder)

            return keyHolder.key?.toLong() ?: error("Failed to create user account")
    }

}


