package io.github.zimoyin.kuake

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.StructuredTaskScope.ShutdownOnFailure


suspend fun main() {

    val VT = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()

    // 使用协程
    CoroutineScope(VT).launch {
        println(51)
    }
}
