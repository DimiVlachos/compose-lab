package dev.dimvlachos.lab.agenticdemo.presentation

sealed interface TranscriptItem {
    val key: String

    data class User(override val key: String, val text: String) : TranscriptItem

    data class Agent(override val key: String, val text: String) : TranscriptItem

    data class Surface(override val key: String, val surfaceId: String) : TranscriptItem

    data class Action(override val key: String, val name: String) : TranscriptItem

    data class Correction(override val key: String, val errors: Int) : TranscriptItem

    data class Failure(override val key: String, val reason: FailureReason) : TranscriptItem
}

enum class FailureReason {
    Network,
    Refused,
}
