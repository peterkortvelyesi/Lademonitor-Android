package com.dominiqueherbrigpersonalteam.lademonitor.data.remote

/** Kotlin port of the iOS `APIError` enum. [message] is always a user-facing German string. */
sealed class ApiException(message: String) : Exception(message) {
    object NotConfigured : ApiException(
        "Keine Server-URL konfiguriert. Bitte in den Einstellungen eintragen."
    )

    object InvalidResponse : ApiException("Ungültige Antwort vom Server.")

    class Server(val statusCode: Int, val serverMessage: String) :
        ApiException("Serverfehler ($statusCode): $serverMessage")

    class Decoding(cause: Throwable) :
        ApiException("Antwort konnte nicht gelesen werden: ${cause.localizedMessage}")

    class Network(cause: Throwable) :
        ApiException("Verbindung fehlgeschlagen: ${cause.localizedMessage}")
}
