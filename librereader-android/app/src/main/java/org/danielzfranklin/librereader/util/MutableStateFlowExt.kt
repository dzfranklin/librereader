package org.danielzfranklin.librereader.util

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Transform may be called multiple times with different values
 */
fun<T> MutableStateFlow<T>.atomicUpdate(transform: (T) -> T) {
    // TODO: Is this the best approach?
    var cached = value
    while (!compareAndSet(cached, transform(cached))) {
        cached = value
    }
}