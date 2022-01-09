package com.tamara.care.watch.data.entity

import com.tamara.care.watch.R

data class HomeRoomEntity(
    val _id: Int? = null,
    val name: Int? = null,
    val icon: Int,
    val color: Int,
    val type: RoomType? = null

) {
    companion object {
        fun getRooms(): List<HomeRoomEntity> {
            return listOf(
                HomeRoomEntity(
                    _id = 1, icon = R.drawable.ic_laboratory_foreground, color = R.color.transparent,
                    type = RoomType.LABARATORY
                ),
                HomeRoomEntity(
                    _id = 2, icon = R.drawable.ic_exit, color = R.color.transparent,
                    type = RoomType.ENTER
                ),
                HomeRoomEntity(
                    _id = 3, icon = R.drawable.ic_meeting_office_foreground, color = R.color.transparent,
                    type = RoomType.MOFFICE
                ),
                HomeRoomEntity(
                    _id = 4, icon = R.drawable.ic_hall_foreground, color = R.color.transparent,
                    type = RoomType.HALL
                ),
                HomeRoomEntity(
                    _id = 5, icon = R.drawable.ic_living_room, color = R.color.transparent,
                    type = RoomType.TOILET
                ),
                HomeRoomEntity(
                    _id = 6, icon = R.drawable.ic_office_foreground, color = R.color.transparent,
                    type = RoomType.OFFICE
                ),

                HomeRoomEntity(
                    name = R.string.indices,
                    icon = R.drawable.ic_health_indices_yellow,
                    color = R.color.transparent,
                ),
                HomeRoomEntity(
                    name = R.string.financial,
                    icon = R.drawable.ic_financial_yellow,
                    color = R.color.transparent,
                )

            )
        }
    }
}


