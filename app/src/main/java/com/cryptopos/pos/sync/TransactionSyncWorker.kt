package com.cryptopos.pos.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cryptopos.pos.domain.repository.PaymentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@HiltWorker
class TransactionSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val paymentRepository: PaymentRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            paymentRepository.syncPendingPayments()
            paymentRepository.refreshTransactions()
            Result.success()
        } catch (error: Exception) {
            Timber.e(error, "Transaction sync failed")
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_ONE_TIME = "cryptopos-sync-immediate"
        const val UNIQUE_PERIODIC = "cryptopos-sync-periodic"
    }
}

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun enqueueImmediate() {
        val request = OneTimeWorkRequestBuilder<TransactionSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            TransactionSyncWorker.UNIQUE_ONE_TIME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun enqueuePeriodic() {
        val request = PeriodicWorkRequestBuilder<TransactionSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TransactionSyncWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
