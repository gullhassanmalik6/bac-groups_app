package com.cryptopos.pos.data.repository

import com.cryptopos.pos.BuildConfig
import com.cryptopos.pos.data.mapper.toDomain
import com.cryptopos.pos.data.remote.api.CryptoPosApi
import com.cryptopos.pos.data.remote.dto.AuthorizeTerminalSessionRequestDto
import com.cryptopos.pos.data.remote.dto.CreateTerminalSessionRequestDto
import com.cryptopos.pos.data.remote.dto.DeviceHeartbeatRequestDto
import com.cryptopos.pos.data.remote.dto.RegisterDeviceRequestDto
import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.terminal.TerminalSession
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalRemoteRepository @Inject constructor(
    private val api: CryptoPosApi,
) : TerminalRemoteGateway {
    override val remoteEnabled: Boolean get() = BuildConfig.USE_REMOTE_TERMINAL

    override suspend fun createAndAuthorize(
        amountMinor: Long,
        currency: String,
        transactionType: String,
        protocolCode: String?,
        paymentMethodToken: String,
        scenario: String?,
        idempotencyKey: String,
    ): TerminalSession {
        val created = try {
            api.createTerminalSession(
                CreateTerminalSessionRequestDto(
                    amountMinor = amountMinor,
                    currency = currency,
                    transactionType = transactionType,
                    protocolCode = protocolCode,
                    idempotencyKey = idempotencyKey,
                ),
            )
        } catch (error: Exception) {
            throw PosError.NetworkUnavailable(error.message ?: "Terminal API unavailable")
        }
        val sessionDto = created.data
            ?: throw PosError.Server(created.message.ifBlank { "Failed to create terminal session" })

        val authorized = try {
            api.authorizeTerminalSession(
                id = sessionDto.id,
                body = AuthorizeTerminalSessionRequestDto(
                    paymentMethodToken = paymentMethodToken,
                    scenario = scenario,
                ),
            )
        } catch (error: Exception) {
            throw PosError.Gateway(error.message ?: "Authorize failed", error)
        }
        val result = authorized.data
            ?: throw PosError.Gateway(authorized.message.ifBlank { "Authorize failed" })
        return result.toDomain()
    }

    /**
     * @return remote device id when registration succeeds; null on soft failure.
     */
    suspend fun registerDeviceBestEffort(
        serial: String,
        model: String,
        androidVersion: String?,
        appVersion: String?,
        manufacturer: String = "Generic",
        connectivityStatus: String? = null,
        printerStatus: String? = null,
        cardReaderStatus: String? = null,
        overallStatus: String? = null,
    ): String? = runCatching {
        api.registerDevice(
            RegisterDeviceRequestDto(
                serialNumber = serial,
                model = model,
                androidVersion = androidVersion,
                manufacturer = manufacturer,
                appVersion = appVersion,
                connectivityStatus = connectivityStatus,
                printerStatus = printerStatus,
                cardReaderStatus = cardReaderStatus,
                overallStatus = overallStatus,
            ),
        ).data?.id
    }.getOrNull()

    suspend fun heartbeatBestEffort(
        deviceId: String?,
        serial: String?,
        connectivityStatus: String,
        printerStatus: String?,
        cardReaderStatus: String?,
        overallStatus: String?,
    ) {
        runCatching {
            api.deviceHeartbeat(
                DeviceHeartbeatRequestDto(
                    deviceId = deviceId,
                    serialNumber = serial,
                    connectivityStatus = connectivityStatus,
                    printerStatus = printerStatus,
                    cardReaderStatus = cardReaderStatus,
                    overallStatus = overallStatus,
                ),
            )
        }
    }
}
