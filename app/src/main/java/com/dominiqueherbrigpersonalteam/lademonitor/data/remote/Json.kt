package com.dominiqueherbrigpersonalteam.lademonitor.data.remote

import com.squareup.moshi.Moshi

/** Single shared Moshi instance, wired up with the server-datetime adapter. */
object Json {
    val moshi: Moshi = Moshi.Builder()
        .add(ServerDateAdapter)
        .build()
}
