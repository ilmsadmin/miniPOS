package com.minipos.core.utils

import java.util.UUID

object UuidGenerator {
    fun generate(): String = UUID.randomUUID().toString()
}
