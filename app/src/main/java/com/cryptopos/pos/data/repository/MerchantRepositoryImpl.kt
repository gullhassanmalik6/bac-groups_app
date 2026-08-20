package com.cryptopos.pos.data.repository

import com.cryptopos.pos.data.remote.api.CryptoPosApi
import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.model.MerchantProfile
import com.cryptopos.pos.domain.model.Wallet
import com.cryptopos.pos.domain.repository.MerchantRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantRepositoryImpl @Inject constructor(
    private val api: CryptoPosApi,
) : MerchantRepository {
    override suspend fun getMerchant(): MerchantProfile {
        val data = api.merchantMe().data ?: throw PosError.Server("Merchant profile not found")
        return MerchantProfile(
            id = data.id,
            companyName = data.companyName,
            email = data.email,
            phone = data.phone,
            status = data.status,
            city = data.city,
            country = data.country,
            industry = data.industry,
            vatNumber = data.taxNumber,
        )
    }

    override suspend fun getWallets(): List<Wallet> {
        val data = api.wallets().data.orEmpty()
        return data.map {
            Wallet(
                id = it.id,
                address = it.walletAddress,
                network = it.walletNetwork,
                status = it.walletStatus,
                provider = it.walletProvider,
                isPrimary = it.isPrimary,
            )
        }
    }
}
