package org.example.project.viewmodel

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.example.project.core.common.Resource
import org.example.project.domain.model.RegisterTenantResult
import org.example.project.domain.repository.AuthRepository
import org.example.project.domain.usecase.RegisterTenantUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var registerTenantUseCase: RegisterTenantUseCase
    private lateinit var viewModel: RegisterViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        registerTenantUseCase = RegisterTenantUseCase(authRepository)
        viewModel = RegisterViewModel(registerTenantUseCase, authRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `validateStep1 returns false when inputs are blank`() {
        val result = viewModel.validateStep1()
        assertFalse(result)
        assertEquals(LoginError.EmptyStoreName, viewModel.registerState.value.errorMessage)
    }

    @Test
    fun `validateStep1 validates storeCode format`() {
        viewModel.OnStoreNameChanged("Fashion Store")
        viewModel.onStoreCodeChanged("INVALID CODE!") // has spaces / symbols
        val result = viewModel.validateStep1()
        assertFalse(result)
        assertEquals(LoginError.InvalidStoreCodeFormat, viewModel.registerState.value.errorMessage)
    }

    @Test
    fun `validateStep1 validates username format and length`() {
        viewModel.OnStoreNameChanged("Fashion Store")
        viewModel.onStoreCodeChanged("my-store")
        viewModel.onUserNameChanged("ab") // too short (< 4 chars)
        val result = viewModel.validateStep1()
        assertFalse(result)
        assertEquals(LoginError.InvalidUsernameFormat, viewModel.registerState.value.errorMessage)
    }

    @Test
    fun `validateStep1 validates password length`() {
        viewModel.OnStoreNameChanged("Fashion Store")
        viewModel.onStoreCodeChanged("my-store")
        viewModel.onUserNameChanged("admin_store")
        viewModel.onPasswordChanged("12345") // < 8 chars
        val result = viewModel.validateStep1()
        assertFalse(result)
        assertEquals(LoginError.PasswordTooShort, viewModel.registerState.value.errorMessage)
    }

    @Test
    fun `validateStep1 succeeds with valid step 1 data`() {
        viewModel.OnStoreNameChanged("Fashion Store")
        viewModel.onStoreCodeChanged("my-store")
        viewModel.onUserNameChanged("admin_store")
        viewModel.onPasswordChanged("Password123@")
        viewModel.onFullNameChanged("Nguyen Van A")
        
        val result = viewModel.validateStep1()
        assertTrue(result)
        assertEquals(null, viewModel.registerState.value.errorMessage)
    }

    @Test
    fun `registerTenants validates step 2 email and phone format`() {
        viewModel.OnStoreNameChanged("Fashion Store")
        viewModel.onStoreCodeChanged("my-store")
        viewModel.onUserNameChanged("admin_store")
        viewModel.onPasswordChanged("Password123@")
        viewModel.onFullNameChanged("Nguyen Van A")

        viewModel.onContactEmailChanged("invalid-email")
        viewModel.registerTenants()
        assertEquals(LoginError.InvalidEmailFormat, viewModel.registerState.value.errorMessage)

        viewModel.onContactEmailChanged("contact@store.com")
        viewModel.onContactPhoneChanged("123") // invalid phone
        viewModel.registerTenants()
        assertEquals(LoginError.InvalidPhoneFormat, viewModel.registerState.value.errorMessage)
    }

    @Test
    fun `registerTenants succeeds and emits navigation event`() = runTest(testDispatcher) {
        viewModel.OnStoreNameChanged("Fashion Store")
        viewModel.onStoreCodeChanged("my-store")
        viewModel.onUserNameChanged("admin_store")
        viewModel.onPasswordChanged("Password123@")
        viewModel.onFullNameChanged("Nguyen Van A")
        viewModel.onContactEmailChanged("contact@store.com")
        viewModel.onContactPhoneChanged("0912345678")
        viewModel.onEmailChanged("owner@store.com")
        viewModel.onPhoneChanged("0987654321")

        coEvery { registerTenantUseCase(any()) } returns Resource.Success(
            RegisterTenantResult(
                success = true,
                message = "Success"
            )
        )

        viewModel.registerTenants()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(RegisterResult.Success, viewModel.registerState.value.registerResult)
        assertFalse(viewModel.registerState.value.isLoading)
    }
}
