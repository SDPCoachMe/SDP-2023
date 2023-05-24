package com.github.sdpcoachme.data

import com.github.sdpcoachme.R
import com.github.sdpcoachme.data.schedule.Event

/**
 * Group event data class
 *
 * @property event
 * @property organizer email of the organiser
 * @property maxParticipants
 * @property participants email of the participants
 * @property groupEventId
 * @constructor Create empty Group event
 */
data class GroupEvent(
    val event: Event,
    val organizer: String,
    val maxParticipants: Int,
    val participants: List<String> = emptyList(),
    val groupEventId: String = "@@event" + organizer + event.start,
) {
    // Constructor needed to make the data class serializable
    constructor() : this(event = Event(), groupEventId = "", organizer = "", maxParticipants = 0, participants = emptyList())

    /**
     * Overloaded version of getPictureResource(String) that uses the id of this GroupEvent
     */
    fun getPictureResource(): Int {
        return getPictureResource(groupEventId)
    }

    companion object {

        /**
         * Returns the resource id for the picture of this group event. Note that this is temporary,
         * and will be replaced by a real picture in a future version. For now, this functions hashes
         * the group event's id and returns one of the predefined group event pictures located in the
         * drawable folder.
         *
         * @param groupEventId The id of the group event for which to return the picture
         */
        // TODO: this method is temporary
        fun getPictureResource(groupEventId: String): Int {
            // TODO: code similar to the one in UserInfo.getPictureResource(String)
            // Empty id means GroupEventId is probably loading, so return gray background picture
            if (groupEventId.isEmpty()) {
                return R.drawable.loading_picture
            }

            val prefix = "group_event_picture_"
            val fieldNames = R.drawable::class.java.fields.filter {
                it.name.startsWith(prefix)
            }
            // mod returns same sign as divisor, so no need to use abs
            val index = groupEventId.hashCode().mod(fieldNames.size)
            val field = fieldNames[index]

            return field.get(null) as Int
        }
    }
}
