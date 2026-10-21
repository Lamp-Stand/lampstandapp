package net.lampstandapp.app.data

import kotlinx.coroutines.CancellationException

/**
 * Like runCatching, but rethrows CancellationException so structured
 * concurrency isn't broken. Without this, a cancelled coroutine (e.g. when
 * a Composable leaves composition) gets converted into Result.failure and
 * keeps running, firing callbacks against a dead composition — the
 * "coroutine scope left the composition" crash.
 */
internal suspend inline fun <T> runCatchingCancellable(
    crossinline block: suspend () -> T
): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
