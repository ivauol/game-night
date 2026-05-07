package com.example

import org.jetbrains.exposed.dao.id.EntityID

data class User(
    val id: EntityID<Int>,
    val username: String,
    val password: String,
)
