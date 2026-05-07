package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class HashTest : DescribeSpec ({

    describe("hashPassword()") {
        it("should output a different string to the original input") {
            val examplePass = "example"
            val hashPass = Hasher.hashPassword(examplePass)
            (examplePass == hashPass) shouldBe false
        }
        it("should not compute to the same result twice (salted hash)") {
            val examplePass = "example"
            val hash1 = Hasher.hashPassword(examplePass)
            val hash2 = Hasher.hashPassword(examplePass)
            (hash1 == hash2) shouldBe false
        }
    }

    describe("verifyPassword()") {
        it("should accept the given password with its hash") {
            val givenPass = "example"
            val hashPass = Hasher.hashPassword(givenPass)
            val result = Hasher.verifyPassword(givenPass, hashPass)
            result shouldBe true
        }

        it("should reject an incorrect password with a different hash") {
            val givenPass = "example"
            val wrongPass = "mallory"
            val hashPass = Hasher.hashPassword(givenPass)
            val result = Hasher.verifyPassword(wrongPass, hashPass)
            result shouldBe false
        }
    }
})