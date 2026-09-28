package com.freeapp.founderrooms

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform