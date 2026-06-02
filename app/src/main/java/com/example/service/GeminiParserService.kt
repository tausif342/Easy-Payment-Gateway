package com.example.service

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class ParsedSmsResult(
    @Json(name = "amount") val amount: Double?,
    @Json(name = "sender") val sender: String?,
    @Json(name = "date") val date: String?,
    @Json(name = "currency") val currency: String?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun parseSmsWithGemini(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

class GeminiParserService {

    private val tag = "GeminiParserService"
    private val baseUrl = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Parses the raw SMS message content into a structured JSON using Gemini API.
     * Returns a ParsedSmsResult object if successful, or null on error.
     */
    suspend fun parseRawSms(smsContent: String): ParsedSmsResult? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(tag, "Gemini API key is not configured or uses placeholder! Set it in the Secrets panel.")
            return null
        }

        val prompt = """
            Extract valid transaction details from this raw payment confirmation SMS message.
            Raw SMS message:
            "$smsContent"
            
            Return the result strictly as a valid JSON object matching the following structure:
            {
              "amount": double representing the money amount (e.g. 1200.0 or 5000.0),
              "sender": string representing the finance provider/vendor (e.g. "bKash", "Nagad", "Rocket", "Upay", or similar bank),
              "date": string representing the date/time of transaction,
              "currency": string representing the currency initials (e.g. "BDT", "USD")
            }
            
            Return ONLY the valid raw JSON object. Do not format with markdown blocks like ```json.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.1f
            )
        )

        return try {
            val response = apiService.parseSmsWithGemini(apiKey, request)
            if (response.isSuccessful) {
                val body = response.body()
                val rawTextJson = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawTextJson.isNullOrBlank()) {
                    Log.d(tag, "Raw Gemini Response: $rawTextJson")
                    moshi.adapter(ParsedSmsResult::class.java).fromJson(rawTextJson)
                } else {
                    Log.e(tag, "Empty response from Gemini API candidates.")
                    null
                }
            } else {
                Log.e(tag, "Gemini API Call failed: ${response.code()} ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during Gemini SMS parsing: ${e.message}", e)
            null
        }
    }
}
