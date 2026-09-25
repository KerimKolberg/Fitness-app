package com.kkfittracking.model

/**
 * The "+Super-sets" screen as one flat list, in the order the exercises are done. A superset is a
 * [Start] row, its exercises and an [End] row; exercises outside those pairs are done on their own.
 * Only exercises move, so the pairs always stay whole and never nest. The same list arranges a
 * day or a plan.
 */
sealed interface ArrangeRow {
    val key: String

    data class Start(
        val supersetId: String,
        /** Seconds to walk to the next exercise of the superset. */
        val transitionSeconds: Int,
        /** Seconds of rest after each round. */
        val roundRestSeconds: Int,
        /** Rounds planned; null keeps going round after round. */
        val rounds: Int? = null,
        /** Every exercise ends its last round with a drop set. */
        val dropOnLastRound: Boolean = false,
    ) : ArrangeRow {
        override val key: String get() = "start-$supersetId"
    }

    data class End(val supersetId: String) : ArrangeRow {
        override val key: String get() = "end-$supersetId"
    }

    data class Item(
        /** The id of the day's or the plan's entry. */
        val id: String,
        val exerciseName: String,
        val categoryColor: Int,
        /** Extra information, such as "3 sets logged". */
        val detail: String? = null,
        /** The exercise's own rest between sets, which a superset's timing replaces. */
        val ownRestSeconds: Int? = null,
        /** In a superset: it joins only the last this many rounds. */
        val memberRounds: Int? = null,
        /** In a superset: its own choice about a drop set on its last round; null follows the superset. */
        val memberDropSet: Boolean? = null,
    ) : ArrangeRow {
        override val key: String get() = id
    }
}

/** An exercise of a day or a plan, as the arrange screen needs it. */
data class ArrangeEntry(
    val id: String,
    val exerciseName: String,
    val categoryColor: Int,
    val supersetId: String?,
    val transitionSeconds: Int?,
    val roundRestSeconds: Int?,
    val detail: String? = null,
    val ownRestSeconds: Int? = null,
    val supersetRounds: Int? = null,
    val supersetDropLast: Boolean = false,
    val memberRounds: Int? = null,
    val memberDropSet: Boolean? = null,
)

/** The timing a superset gets when it has none of its own. */
data class SupersetTiming(val transitionSeconds: Int, val roundRestSeconds: Int)

/** What is saved for each exercise after arranging. */
data class ArrangedExercise(
    val id: String,
    val sortOrder: Int,
    val supersetId: String?,
    val transitionSeconds: Int?,
    val roundRestSeconds: Int?,
    val supersetRounds: Int? = null,
    val supersetDropLast: Boolean = false,
    val memberRounds: Int? = null,
    val memberDropSet: Boolean? = null,
)

/** Builds the arrange list, keeping the current order. */
fun arrangementOf(entries: List<ArrangeEntry>, defaults: SupersetTiming): List<ArrangeRow> {
    fun item(entry: ArrangeEntry) =
        ArrangeRow.Item(
            entry.id, entry.exerciseName, entry.categoryColor, entry.detail, entry.ownRestSeconds,
            entry.memberRounds, entry.memberDropSet,
        )
    return groupSupersets(entries) { it.supersetId }.flatMap { block ->
        when (block) {
            is Block.Single -> listOf(item(block.item))
            is Block.Superset -> {
                val start = ArrangeRow.Start(
                    supersetId = block.id,
                    transitionSeconds = block.items.firstNotNullOfOrNull { it.transitionSeconds } ?: defaults.transitionSeconds,
                    roundRestSeconds = block.items.firstNotNullOfOrNull { it.roundRestSeconds } ?: defaults.roundRestSeconds,
                    rounds = block.items.firstNotNullOfOrNull { it.supersetRounds },
                    dropOnLastRound = block.items.any { it.supersetDropLast },
                )
                listOf(start) + block.items.map(::item) + ArrangeRow.End(block.id)
            }
        }
    }
}

/** Moves the exercise at [from] to [to]. Superset start and end rows never move on their own. */
fun moveRow(rows: List<ArrangeRow>, from: Int, to: Int): List<ArrangeRow> {
    if (from !in rows.indices || rows[from] !is ArrangeRow.Item) return rows
    val target = to.coerceIn(0, rows.lastIndex)
    if (target == from) return rows
    return rows.toMutableList().apply { add(target, removeAt(from)) }
}

/** Which superset each exercise is in, by exercise key. Exercises on their own are left out. */
fun supersetMembership(rows: List<ArrangeRow>): Map<String, String> {
    val result = mutableMapOf<String, String>()
    var current: String? = null
    rows.forEach { row ->
        when (row) {
            is ArrangeRow.Start -> current = row.supersetId
            is ArrangeRow.End -> current = null
            is ArrangeRow.Item -> current?.let { result[row.key] = it }
        }
    }
    return result
}

/** The exercises of a superset, in order. */
fun membersOf(rows: List<ArrangeRow>, supersetId: String): List<ArrangeRow.Item> {
    val start = rows.indexOfFirst { it is ArrangeRow.Start && it.supersetId == supersetId }
    if (start < 0) return emptyList()
    return rows.drop(start + 1).takeWhile { it is ArrangeRow.Item }.filterIsInstance<ArrangeRow.Item>()
}

/**
 * Moves an exercise to the end of a superset, or with a null [supersetId] out of its superset, to
 * right after it.
 */
fun moveToSuperset(rows: List<ArrangeRow>, itemKey: String, supersetId: String?): List<ArrangeRow> {
    val item = rows.firstOrNull { it.key == itemKey } as? ArrangeRow.Item ?: return rows
    val current = supersetMembership(rows)[itemKey]
    if (current == supersetId) return rows
    val without = rows.filterNot { it.key == itemKey }.toMutableList()
    val endOf = { id: String? -> without.indexOfFirst { it is ArrangeRow.End && it.supersetId == id } }
    val target = if (supersetId != null) endOf(supersetId) else endOf(current) + 1
    if (target < 0 || (supersetId == null && target == 0)) return rows
    without.add(target, item)
    return without
}

/** Starts a new superset holding just this exercise, where it is now. */
fun moveToNewSuperset(rows: List<ArrangeRow>, itemKey: String, start: ArrangeRow.Start): List<ArrangeRow> {
    val outside = moveToSuperset(rows, itemKey, null)
    val index = outside.indexOfFirst { it.key == itemKey }
    if (index < 0) return rows
    return outside.toMutableList().apply {
        add(index + 1, ArrangeRow.End(start.supersetId))
        add(index, start)
    }
}

/** Adds an empty superset at the end. */
fun addSuperset(rows: List<ArrangeRow>, start: ArrangeRow.Start): List<ArrangeRow> =
    rows + start + ArrangeRow.End(start.supersetId)

/** Removes a superset; its exercises stay where they are, on their own. */
fun removeSuperset(rows: List<ArrangeRow>, supersetId: String): List<ArrangeRow> =
    rows.filterNot {
        (it is ArrangeRow.Start && it.supersetId == supersetId) || (it is ArrangeRow.End && it.supersetId == supersetId)
    }

/** Changes one exercise's row, found by key. */
fun updateItem(rows: List<ArrangeRow>, itemKey: String, change: (ArrangeRow.Item) -> ArrangeRow.Item): List<ArrangeRow> =
    rows.map { if (it is ArrangeRow.Item && it.key == itemKey) change(it) else it }

/** Changes a superset's timing. */
fun updateSuperset(rows: List<ArrangeRow>, supersetId: String, change: (ArrangeRow.Start) -> ArrangeRow.Start): List<ArrangeRow> =
    rows.map { if (it is ArrangeRow.Start && it.supersetId == supersetId) change(it) else it }

/** Supersets that have more exercises than allowed. */
fun oversizedSupersets(rows: List<ArrangeRow>): List<String> =
    rows.filterIsInstance<ArrangeRow.Start>()
        .filter { membersOf(rows, it.supersetId).size > MAX_SUPERSET_SIZE }
        .map { it.supersetId }

/**
 * What to save, in order. A superset with fewer than two exercises is not a superset, so its
 * exercise is saved as one on its own.
 */
fun arrangedExercises(rows: List<ArrangeRow>): List<ArrangedExercise> {
    val sizes = rows.filterIsInstance<ArrangeRow.Start>().associate { it.supersetId to membersOf(rows, it.supersetId).size }
    var current: ArrangeRow.Start? = null
    var order = 0
    return rows.mapNotNull { row ->
        when (row) {
            is ArrangeRow.Start -> {
                current = row
                null
            }
            is ArrangeRow.End -> {
                current = null
                null
            }
            is ArrangeRow.Item -> {
                val superset = current?.takeIf { (sizes[it.supersetId] ?: 0) >= 2 }
                ArrangedExercise(
                    id = row.id,
                    sortOrder = order++,
                    supersetId = superset?.supersetId,
                    transitionSeconds = superset?.transitionSeconds,
                    roundRestSeconds = superset?.roundRestSeconds,
                    supersetRounds = superset?.rounds,
                    supersetDropLast = superset?.dropOnLastRound == true,
                    memberRounds = row.memberRounds.takeIf { superset?.rounds != null },
                    memberDropSet = row.memberDropSet.takeIf { superset != null },
                )
            }
        }
    }
}
