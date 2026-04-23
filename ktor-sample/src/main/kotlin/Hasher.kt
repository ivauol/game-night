package com.example

import at.favre.lib.crypto.bcrypt.BCrypt

class Hasher {
    companion object {
        fun hashPassword(plaintext: String): String {
            val hashedPass = BCrypt.withDefaults().hashToString(12, plaintext.toCharArray())
            return hashedPass
        }

        fun verifyPassword(givenPass: String, hashPass: String): Boolean {
            val result = BCrypt.verifyer().verify(givenPass.toCharArray(), hashPass)
            return result.verified
        }
    }
}