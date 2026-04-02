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
class PersonRepository( 
    private val jdbcTemplate: JdbcTemplate
)
    data class UserInfo(
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val passwordHash: String
)
    {
    private val rowMapper = RowMapper<userinfo> { rs, _ ->
        UserInfo(
            username = rs.getLong("username"),
            firstName = rs.getString("firstName"),
            lastName = rs.getString("lastName"),
            email = rs.getString("email"),
            passwordHash = rs.getString("password_hash"),
        )
    }

    fun findByUsername(username: String): UserInfo? {
        val sql = """
            SELECT username, firstName, lastName, email, passwordHash
            FROM userinfo
            WHERE username = ?
        """.trimIndent()

        return jdbcTemplate.query(sql, rowMapper, username).firstOrNull()
    }

    private val encoder = BCryptPasswordEncoder()

    fun authenticate(username: String, password: String): Boolean {
        val user = findByUsername(username) ?: return false
        return encoder.matches(password, user.passwordHash)
    }

    fun create(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String
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


