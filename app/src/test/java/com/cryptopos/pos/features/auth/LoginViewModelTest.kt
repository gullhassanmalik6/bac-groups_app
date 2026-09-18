package com.cryptopos.pos.features.auth

import com.cryptopos.pos.data.local.datastore.RememberedLogin
import com.cryptopos.pos.data.local.datastore.RememberedLoginStore
import com.cryptopos.pos.domain.model.UserSession
import com.cryptopos.pos.domain.repository.AuthRepository
import com.cryptopos.pos.domain.usecase.LoginUseCase
import com.cryptopos.pos.domain.usecase.SyncDeviceUseCase
import com.cryptopos.pos.hardware.device.MockDeviceAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setMain() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun reset() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_success_clears_loading() = runTest {
        val remembered = FakeRememberedLoginStore()
        val vm = LoginViewModel(
            loginUseCase = LoginUseCase(SucceedingAuth()),
            syncDevice = SyncDeviceUseCase(MockDeviceAdapter()),
            rememberedLoginStore = remembered,
        )
        advanceUntilIdle()
        vm.onEmailChange("user@example.com")
        vm.onPasswordChange("Password1!")
        vm.onRememberDeviceChange(true)
        vm.login()
        advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertEquals(false, vm.state.value.loading)
        assertEquals("user@example.com", remembered.saved?.email)
    }

    @Test
    fun login_without_remember_clears_stored_credentials() = runTest {
        val remembered = FakeRememberedLoginStore(
            initial = RememberedLogin("old@example.com", "OldPass12"),
        )
        val vm = LoginViewModel(
            loginUseCase = LoginUseCase(SucceedingAuth()),
            syncDevice = SyncDeviceUseCase(MockDeviceAdapter()),
            rememberedLoginStore = remembered,
        )
        advanceUntilIdle()
        assertTrue(vm.state.value.rememberDevice)
        vm.onRememberDeviceChange(false)
        vm.onEmailChange("user@example.com")
        vm.onPasswordChange("Password1!")
        vm.login()
        advanceUntilIdle()
        assertNull(remembered.saved)
        assertFalse(vm.state.value.rememberDevice)
    }

    @Test
    fun login_validation_failure_sets_error() = runTest {
        val vm = LoginViewModel(
            loginUseCase = LoginUseCase(SucceedingAuth()),
            syncDevice = SyncDeviceUseCase(MockDeviceAdapter()),
            rememberedLoginStore = FakeRememberedLoginStore(),
        )
        advanceUntilIdle()
        vm.onEmailChange("bad")
        vm.onPasswordChange("short")
        vm.login()
        advanceUntilIdle()
        assertTrue(vm.state.value.error!!.isNotBlank())
        assertEquals(false, vm.state.value.loading)
    }
}

private class SucceedingAuth : AuthRepository {
    override val isLoggedIn: Flow<Boolean> = MutableStateFlow(false)
    override suspend fun login(email: String, password: String): UserSession =
        UserSession(userId = "1", email = email, fullName = "User", roleCode = "merchant_owner")
    override suspend fun logout() = Unit
    override suspend fun currentUserName(): String? = null
    override suspend fun currentUserEmail(): String? = null
}

private class FakeRememberedLoginStore(
    initial: RememberedLogin? = null,
) : RememberedLoginStore {
    var saved: RememberedLogin? = initial
        private set

    override suspend fun getRememberedLogin(): RememberedLogin? = saved

    override suspend fun saveRememberedLogin(email: String, password: String) {
        saved = RememberedLogin(email, password)
    }

    override suspend fun clearRememberedLogin() {
        saved = null
    }
}
