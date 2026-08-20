package com.cryptopos.pos.data.remote.api

import com.cryptopos.pos.data.remote.dto.ApiResponse
import com.cryptopos.pos.data.remote.dto.AuthResponseDto
import com.cryptopos.pos.data.remote.dto.CreatePaymentRequestDto
import com.cryptopos.pos.data.remote.dto.LoginRequestDto
import com.cryptopos.pos.data.remote.dto.MerchantDto
import com.cryptopos.pos.data.remote.dto.PaymentDto
import com.cryptopos.pos.data.remote.dto.ReceiptDto
import com.cryptopos.pos.data.remote.dto.RefundRequestDto
import com.cryptopos.pos.data.remote.dto.RefreshRequestDto
import com.cryptopos.pos.data.remote.dto.TokenPairDto
import com.cryptopos.pos.data.remote.dto.TransactionListDto
import com.cryptopos.pos.data.remote.dto.UserDto
import com.cryptopos.pos.data.remote.dto.WalletDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CryptoPosApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): ApiResponse<AuthResponseDto>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): ApiResponse<TokenPairDto>

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequestDto): retrofit2.Response<Unit>

    @GET("auth/me")
    suspend fun me(): ApiResponse<UserDto>

    @GET("merchants/me")
    suspend fun merchantMe(): ApiResponse<MerchantDto>

    @GET("merchants/me/wallets")
    suspend fun wallets(): ApiResponse<List<WalletDto>>

    @POST("payments")
    suspend fun createPayment(@Body body: CreatePaymentRequestDto): ApiResponse<PaymentDto>

    @GET("payments")
    suspend fun listPayments(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): ApiResponse<TransactionListDto>

    @GET("payments/{id}")
    suspend fun getPayment(@Path("id") id: String): ApiResponse<PaymentDto>

    @GET("payments/{id}/receipt")
    suspend fun getReceipt(@Path("id") id: String): ApiResponse<ReceiptDto>

    @POST("payments/{id}/refund")
    suspend fun refundPayment(
        @Path("id") id: String,
        @Body body: RefundRequestDto = RefundRequestDto(),
    ): ApiResponse<PaymentDto>
}
