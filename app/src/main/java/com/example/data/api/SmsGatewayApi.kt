package com.example.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SmsGatewayApi {

    @POST("device/activate")
    suspend fun activateDevice(
        @Body request: DeviceActivationRequest
    ): Response<DeviceActivationResponse>

    @POST("device/status")
    suspend fun checkDeviceStatus(
        @Body request: DeviceCheckRequest
    ): Response<DeviceCheckResponse>

    @POST("sms/sync")
    suspend fun syncTransaction(
        @Body request: SmsSyncRequest
    ): Response<SmsSyncResponse>
}
