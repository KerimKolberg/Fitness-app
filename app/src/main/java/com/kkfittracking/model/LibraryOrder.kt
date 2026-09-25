package com.kkfittracking.model

/**
 * Items in the user's order: those whose key is in [order] first, in that order, then the others
 * as they were. An empty order keeps the default.
 */
fun <T> List<T>.inUserOrder(order: List<String>, keyOf: (T) -> String): List<T> {
    if (order.isEmpty()) return this
    val rank = order.withIndex().associate { it.value to it.index }
    return sortedBy { rank[keyOf(it)] ?: Int.MAX_VALUE }
}

/**
 * Moves [key] one place up (-1) or down (+1) among [keys] (all of them, in the order shown) and
 * returns the whole new order to remember.
 */
fun moveInOrder(keys: List<String>, key: String, direction: Int): List<String> {
    val index = keys.indexOf(key)
    val target = index + direction
    if (index < 0 || target !in keys.indices) return keys
    return keys.toMutableList().apply { add(target, removeAt(index)) }
}
