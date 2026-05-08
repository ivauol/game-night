package com.example

import at.favre.lib.crypto.bcrypt.BCrypt

// Simple class for hashing passwords before they go

class Hasher {
    companion object {
        /**
         * hashPassword()
         * @param plaintext - password the user enters
         * @return The salted hash of the password.
         */
        fun hashPassword(plaintext: String): String {
            val hashedPass = BCrypt.withDefaults().hashToString(12, plaintext.toCharArray())
            return hashedPass
        }

        /**
         * verifyPassword()
         * @param givenPass - password the user enters
         * @param hashPass - hashed password in the database
         * @return The salted hash of the password.
         */
        fun verifyPassword(
            givenPass: String,
            hashPass: String,
        ): Boolean {
            val result = BCrypt.verifyer().verify(givenPass.toCharArray(), hashPass)
            return result.verified
        }
    }
}
