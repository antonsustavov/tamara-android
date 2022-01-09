package com.tamara.care.watch.data.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TimeEntity(
    val from: String? = null,
    val to: String? = null,
    val status: String? = null
) : Parcelable