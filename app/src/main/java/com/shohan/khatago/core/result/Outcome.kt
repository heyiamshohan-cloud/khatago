package com.shohan.khatago.core.result

/**
 * A tiny, explicit result type used by validation and write paths so the UI can
 * show a human sentence instead of an exception.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val message: String) : Outcome<Nothing>

    companion object {
        inline fun <T> run(message: String, block: () -> T): Outcome<T> =
            try {
                Success(block())
            } catch (_: Exception) {
                Failure(message)
            }
    }
}

inline fun <T> Outcome<T>.onSuccess(block: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) block(value)
    return this
}

inline fun <T> Outcome<T>.onFailure(block: (String) -> Unit): Outcome<T> {
    if (this is Outcome.Failure) block(message)
    return this
}
