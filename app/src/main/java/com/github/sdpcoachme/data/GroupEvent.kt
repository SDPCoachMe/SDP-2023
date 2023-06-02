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
            return when (groupEventId) {
                "@@eventarnold.schwarzenegger@coachme.ch2023-06-06T19:00" ->
                    R.drawable.muscles
                "@@eventcristiano.ronaldo@coachme.ch2023-06-07T16:00" ->
                    R.drawable.acting
                "@@eventeliud.kipchoge@coachme.ch2023-06-06T10:00" ->
                    R.drawable.running
                "@@eventerling.haaland@coachme.ch2023-06-05T15:00" ->
                    R.drawable.viking
                "@@eventlaure.manaudou@coachme.ch2023-06-03T15:00" ->
                    R.drawable.dolphin
                "@@eventlebron.james@coachme.ch2023-06-07T15:00" ->
                    R.drawable.dunk
                "@@eventserena.williams@coachme.ch2023-06-03T09:00" ->
                    R.drawable.tennis
                "@@eventstephen.curry@coachme.ch2023-06-03T10:00" ->
                    R.drawable.dribble
                else ->
                    // Default to tennis for when Lindsay creates his event in the demo
                    R.drawable.tennis
            }
//            // TODO: code similar to the one in UserInfo.getPictureResource(String)
//            // Empty id means GroupEventId is probably loading, so return gray background picture
//            if (groupEventId.isEmpty()) {
//                return R.drawable.loading_picture
//            }
//
//            val prefix = "group_event_picture_"
//            val fieldNames = R.drawable::class.java.fields.filter {
//                it.name.startsWith(prefix)
//            }
//            // mod returns same sign as divisor, so no need to use abs
//            val index = groupEventId.hashCode().mod(fieldNames.size)
//            val field = fieldNames[index]
//
//            return field.get(null) as Int
        }
    }
}
