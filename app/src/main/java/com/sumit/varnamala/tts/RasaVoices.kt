package com.sumit.varnamala.tts

/**
 * A curated set of Rasa speakers. The model has 1024 speaker slots and its speaker table is
 * gated upstream, so gender was estimated from pitch instead: speakers read the same phrases in
 * Hindi, Tamil, Bengali and Telugu and the median pitch was measured. The listed female voices
 * stayed at 215-245 Hz and the male voices at 135-175 Hz in every language. This is an estimate
 * from measurement, not from the upstream labels; ids 0-149 and a 1-in-16 sample above that were
 * measured, so more voices exist. Replace the ids here if the upstream table says otherwise.
 */
object RasaVoices {
    enum class Gender { FEMALE, MALE }

    data class Voice(val speakerId: Int, val gender: Gender, val number: Int)

    private fun voices(gender: Gender, speakerIds: List<Int>) =
        speakerIds.mapIndexed { index, id -> Voice(id, gender, index + 1) }

    val female: List<Voice> = voices(Gender.FEMALE, listOf(0, 8, 11, 18))
    val male: List<Voice> = voices(Gender.MALE, listOf(1, 7, 9))
    val all: List<Voice> = female + male

    val default: Voice get() = all.firstOrNull() ?: Voice(0, Gender.FEMALE, 1)

    fun find(speakerId: Int): Voice = all.firstOrNull { it.speakerId == speakerId } ?: default

    /** Speaking styles of the model (its 32 style ids; these are the documented ones worth exposing). */
    enum class Style(val emotionId: Int) {
        NEUTRAL(0), NEWS(10), CONVERSATION(4), STORY(3), HAPPY(8);

        companion object {
            fun fromId(id: Int) = entries.firstOrNull { it.emotionId == id } ?: NEUTRAL
        }
    }
}
