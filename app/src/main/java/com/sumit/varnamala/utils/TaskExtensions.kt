package com.sumit.varnamala.utils

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Extension function to convert Google's Task API to Kotlin coroutines
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        continuation.resume(result)
    }
    addOnFailureListener { exception ->
        continuation.resumeWithException(exception)
    }
    
    continuation.invokeOnCancellation {
        // If the coroutine that launched this task is cancelled,
        // we can't directly cancel the task but we don't need to 
        // continue processing the result
        if (isComplete || isCanceled) {
            return@invokeOnCancellation
        }
        
        // This doesn't actually cancel the task; the task will still complete
        // but we just won't process the result since the coroutine is cancelled
    }
}