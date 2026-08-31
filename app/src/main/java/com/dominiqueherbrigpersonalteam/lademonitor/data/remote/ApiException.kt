package com.dominiqueherbrigpersonalteam.lademonitor.data.remote

import com.dominiqueherbrigpersonalteam.lademonitor.LademonitorApp
import com.dominiqueherbrigpersonalteam.lademonitor.R

/** Kotlin port of the iOS `APIError` enum. [message] is always a user-facing, localized string. */
sealed class ApiException(message: String) : Exception(message) {
    object NotConfigured : ApiException(
        LademonitorApp.appContext.getString(R.string.api_error_not_configured)
    )

    object InvalidResponse : ApiException(
        LademonitorApp.appContext.getString(R.string.api_error_invalid_response)
    )

    class Server(val statusCode: Int, val serverMessage: String) :
        ApiException(
            LademonitorApp.appContext.getString(R.string.api_error_server, statusCode, serverMessage)
        )

    class Decoding(cause: Throwable) :
        ApiException(
            LademonitorApp.appContext.getString(R.string.api_error_decoding, cause.localizedMessage.orEmpty())
        )

    class Network(cause: Throwable) :
        ApiException(
            LademonitorApp.appContext.getString(R.string.api_error_network, cause.localizedMessage.orEmpty())
        )
}
