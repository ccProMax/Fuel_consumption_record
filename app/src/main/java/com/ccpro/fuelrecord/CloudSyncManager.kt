package com.ccpro.fuelrecord

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 云同步管理器
 * 用于将油耗记录数据同步到云端
 */
class CloudSyncManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("sync_config", Context.MODE_PRIVATE)
    }

    /**
     * 同步配置
     */
    data class SyncConfig(
        val shareID: String,
        val accessToken: String,
        val password: String
    )

    /**
     * 保存同步配置
     */
    fun saveSyncConfig(config: SyncConfig) {
        prefs.edit().apply {
            putString("shareID", config.shareID)
            putString("accessToken", config.accessToken)
            putString("password", config.password)
            apply()
        }
    }

    /**
     * 加载同步配置
     */
    fun loadSyncConfig(): SyncConfig? {
        val shareID = prefs.getString("shareID", null)
        val accessToken = prefs.getString("accessToken", null)
        val password = prefs.getString("password", null)
        
        return if (shareID != null && accessToken != null && password != null) {
            SyncConfig(shareID, accessToken, password)
        } else {
            null
        }
    }

    /**
     * 从云端下载数据
     */
    suspend fun downloadData(config: SyncConfig): Result<List<FuelRecord>> = withContext(Dispatchers.IO) {
        try {
            val url = "http://8.138.203.27/?explorer/share/fileDownload&path=%7BshareItemLink%3A${config.shareID}%7D%2Fuserdata.json&accessToken=${config.accessToken}&download=1&_etag=${System.currentTimeMillis() / 1000}&shareID=${config.shareID}"
            
            android.util.Log.d("CloudSync", "下载URL: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            android.util.Log.d("CloudSync", "开始下载请求...")
            val response = client.newCall(request).execute()
            android.util.Log.d("CloudSync", "响应码: ${response.code}")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                android.util.Log.e("CloudSync", "下载失败: ${response.code}, 错误信息: $errorBody")
                return@withContext Result.failure(IOException("下载失败: ${response.code}, 错误信息: $errorBody"))
            }

            val responseBody = response.body?.string() ?: return@withContext Result.failure(IOException("响应体为空"))
            android.util.Log.d("CloudSync", "响应内容: $responseBody")
            android.util.Log.d("CloudSync", "响应内容长度: ${responseBody.length}")
            android.util.Log.d("CloudSync", "响应内容前100字符: ${responseBody.take(100)}")

            // 检查响应是否为空
            if (responseBody.isBlank()) {
                android.util.Log.e("CloudSync", "响应内容为空")
                return@withContext Result.failure(IOException("响应内容为空"))
            }

            // 解析JSON数据
            val jsonElement = try {
                gson.fromJson(responseBody, com.google.gson.JsonElement::class.java)
            } catch (e: Exception) {
                android.util.Log.e("CloudSync", "JSON解析失败: ${e.message}")
                android.util.Log.e("CloudSync", "原始响应: $responseBody")
                return@withContext Result.failure(IOException("JSON解析失败: ${e.message}"))
            }

            if (jsonElement == null) {
                android.util.Log.e("CloudSync", "JSON解析结果为空")
                android.util.Log.e("CloudSync", "原始响应: $responseBody")
                return@withContext Result.failure(IOException("JSON解析结果为空"))
            }

            // 检查是否是错误响应
            if (jsonElement.isJsonObject && jsonElement.asJsonObject.has("code")) {
                val codeElement = jsonElement.asJsonObject.get("code")
                val isSuccess = when {
                    codeElement.isJsonPrimitive && codeElement.asJsonPrimitive.isBoolean -> codeElement.asBoolean
                    codeElement.isJsonPrimitive && codeElement.asJsonPrimitive.isNumber -> codeElement.asInt == 1
                    else -> true // 如果没有明确的失败标识，默认认为成功
                }
                android.util.Log.d("CloudSync", "响应码: $codeElement, 是否成功: $isSuccess")
                if (!isSuccess) {
                    val message = if (jsonElement.asJsonObject.has("message")) {
                        jsonElement.asJsonObject.get("message").asString
                    } else if (jsonElement.asJsonObject.has("data")) {
                        jsonElement.asJsonObject.get("data").asString
                    } else {
                        "未知错误"
                    }
                    android.util.Log.e("CloudSync", "下载失败: $message")
                    return@withContext Result.failure(IOException(message))
                }
            }

            // 尝试解析为记录列表
            val recordType = object : TypeToken<List<FuelRecord>>() {}.type
            val records = try {
                // 如果响应本身就是数组，直接解析
                if (jsonElement.isJsonArray) {
                    android.util.Log.d("CloudSync", "响应是JSON数组，直接解析")
                    gson.fromJson<List<FuelRecord>>(jsonElement, recordType)
                } 
                // 如果响应是对象，尝试从data字段解析
                else if (jsonElement.isJsonObject && jsonElement.asJsonObject.has("data")) {
                    val dataArray = jsonElement.asJsonObject.get("data")
                    android.util.Log.d("CloudSync", "从data字段解析: $dataArray")
                    gson.fromJson<List<FuelRecord>>(dataArray, recordType)
                } 
                // 其他情况，尝试直接解析响应体
                else {
                    android.util.Log.d("CloudSync", "尝试直接解析响应体")
                    gson.fromJson<List<FuelRecord>>(responseBody, recordType)
                }
            } catch (e: Exception) {
                android.util.Log.e("CloudSync", "数据解析失败: ${e.message}")
                return@withContext Result.failure(IOException("数据解析失败: ${e.message}"))
            }

            android.util.Log.d("CloudSync", "下载成功，共${records.size}条记录")
            Result.success(records)
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "下载异常", e)
            Result.failure(e)
        }
    }

    /**
     * 上传数据到云端
     */
    suspend fun uploadData(config: SyncConfig, records: List<FuelRecord>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 将记录列表转换为JSON字符串
            val jsonContentStr = gson.toJson(records)
            android.util.Log.d("CloudSync", "准备上传数据: ${records.size}条记录")
            android.util.Log.d("CloudSync", "JSON内容: $jsonContentStr")

            val pathValue = "{shareItemLink:${config.shareID}}/userdata.json"

            // 构建表单数据
            val formBody = FormBody.Builder()
                .add("shareID", config.shareID)
                .add("accessToken", config.accessToken)
                .add("password", config.password)
                .add("path", pathValue)
                .add("content", jsonContentStr)
                .build()

            val request = Request.Builder()
                .url("http://8.138.203.27/?explorer/share/fileSave")
                .post(formBody)
                .build()

            android.util.Log.d("CloudSync", "开始上传请求...")
            val response = client.newCall(request).execute()
            android.util.Log.d("CloudSync", "响应码: ${response.code}")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                android.util.Log.e("CloudSync", "上传失败: ${response.code}, 错误信息: $errorBody")
                return@withContext Result.failure(IOException("上传失败: ${response.code}, 错误信息: $errorBody"))
            }

            // 检查响应
            val responseBody = response.body?.string() ?: return@withContext Result.failure(IOException("响应体为空"))
            android.util.Log.d("CloudSync", "响应内容: $responseBody")
            
            val jsonElement = gson.fromJson(responseBody, com.google.gson.JsonElement::class.java)

            if (jsonElement.isJsonObject && jsonElement.asJsonObject.has("code")) {
                val codeElement = jsonElement.asJsonObject.get("code")
                val isSuccess = when {
                    codeElement.isJsonPrimitive && codeElement.asJsonPrimitive.isBoolean -> codeElement.asBoolean
                    codeElement.isJsonPrimitive && codeElement.asJsonPrimitive.isNumber -> codeElement.asInt == 1
                    else -> false
                }
                android.util.Log.d("CloudSync", "响应码: $codeElement, 是否成功: $isSuccess")
                if (!isSuccess) {
                    val message = if (jsonElement.asJsonObject.has("message")) {
                        jsonElement.asJsonObject.get("message").asString
                    } else if (jsonElement.asJsonObject.has("data")) {
                        jsonElement.asJsonObject.get("data").asString
                    } else {
                        "上传失败"
                    }
                    android.util.Log.e("CloudSync", "上传失败: $message")
                    return@withContext Result.failure(IOException(message))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "上传异常", e)
            Result.failure(e)
        }
    }
}
