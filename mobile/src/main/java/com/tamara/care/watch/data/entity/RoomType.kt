package com.tamara.care.watch.data.entity


enum class RoomType(val key: String) {
    LABARATORY("laboratory"),
    ENTER("enter"),
    MOFFICE("meeting office"),
    HALL("hall"),
    TOILET("toilet"),
    OFFICE("office"),
    INDICES("indices"),
    FINANCIAL("fifancial")
}

fun getRooms(): MutableList<RoomType> {
    return mutableListOf(
        RoomType.LABARATORY,
        RoomType.ENTER,
        RoomType.MOFFICE,
        RoomType.HALL,
        RoomType.TOILET,
        RoomType.OFFICE
    )
}

data class AdapterRoomEntity(
    val name: String?,
    var isOccupied: Boolean = false
)