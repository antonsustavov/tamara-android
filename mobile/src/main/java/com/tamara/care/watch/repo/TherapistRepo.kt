package com.tamara.care.watch.repo

import com.tamara.care.watch.data.entity.TherapistEntity
import com.tamara.care.watch.data.model.ModelState
import com.tamara.care.watch.data.network.NetworkApi
import com.tamara.care.watch.repo.base.BaseRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TherapistRepo @Inject constructor(
    private var networkApi: NetworkApi,
) : BaseRepo() {


//    suspend fun getTherapistInfo(transmitterId: String? = null): ModelState<TherapistEntity> {
//        return try {
//            val response = networkApi.getTherapistInfo(transmitterId)
//            return if (response.isSuccessful) {
//                ModelState.Success(
//                    response.body()!!
//                )
//
//            } else {
//                handleError(response)
//            }
//        } catch (e: Exception) {
//            handleError()
//        }
//    }
}