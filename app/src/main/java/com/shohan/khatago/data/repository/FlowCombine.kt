package com.shohan.khatago.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Combines an arbitrary number of heterogeneous flows.
 * [transform] receives the latest emission of each flow, in order.
 *
 * Kotlin's typed [combine] stops at five flows; the dashboard and the reports
 * screen legitimately need more than that, and casting inside one place is
 * clearer than nesting five-way combines three levels deep.
 */
@Suppress("UNCHECKED_CAST")
fun <R> combineAll(
    flows: List<Flow<*>>,
    transform: suspend (List<Any?>) -> R
): Flow<R> = combine(flows.map { it as Flow<Any?> }) { values -> transform(values.toList()) }
