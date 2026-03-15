package com.asuka.player.platform

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.launch

internal class SerialTaskQueue(
    dispatcher: CoroutineDispatcher,
    private val tag: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val channel = Channel<QueueEntry>(capacity = Channel.UNLIMITED)
    @Volatile
    private var closed = false
    private val consumerJob: Job

    init {
        consumerJob = scope.launch {
            for (entry in channel) {
                when (entry) {
                    is QueueEntry.Task -> execute(entry)
                    is QueueEntry.Barrier -> entry.completion.complete(Unit)
                }
            }
        }
    }

    fun dispatch(block: suspend () -> Unit) {
        if (closed) return
        val result: ChannelResult<Unit> = channel.trySend(
            QueueEntry.Task(
                block = block,
                completion = null,
            ),
        )
        if (result.isFailure) {
            Log.w(tag, "dropping queued task after queue closed")
        }
    }

    suspend fun dispatchAndAwait(block: suspend () -> Unit) {
        if (closed) return
        val completion = CompletableDeferred<Unit>()
        channel.send(
            QueueEntry.Task(
                block = block,
                completion = completion,
            ),
        )
        completion.await()
    }

    suspend fun awaitIdle() {
        if (closed) return
        val completion = CompletableDeferred<Unit>()
        channel.send(QueueEntry.Barrier(completion))
        completion.await()
    }

    fun close() {
        if (closed) return
        closed = true
        channel.close()
        consumerJob.invokeOnCompletion { scope.cancel() }
    }

    private suspend fun execute(entry: QueueEntry.Task) {
        val result = runCatching { entry.block() }
        result.onFailure { error -> Log.e(tag, "queued task failed", error) }
        val completion = entry.completion ?: return
        result.fold(
            onSuccess = { completion.complete(Unit) },
            onFailure = { completion.completeExceptionally(it) },
        )
    }

    private sealed interface QueueEntry {
        data class Task(
            val block: suspend () -> Unit,
            val completion: CompletableDeferred<Unit>?,
        ) : QueueEntry

        data class Barrier(
            val completion: CompletableDeferred<Unit>,
        ) : QueueEntry
    }

    companion object {
        fun io(tag: String): SerialTaskQueue {
            return SerialTaskQueue(
                dispatcher = Dispatchers.IO.limitedParallelism(1),
                tag = tag,
            )
        }
    }
}
