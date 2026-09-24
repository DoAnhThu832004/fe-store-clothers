package org.example.project.viewmodel

import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.example.project.domain.repository.IAuthRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.example.project.core.common.Resource
import org.example.project.data.remote.dto.UserInfoDto

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    // Mock dependencies
    private lateinit var authRepository: IAuthRepository
    private lateinit var viewModel: LoginViewModel

    // Dispatcher for controlling coroutines time in Test
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Relaxed mock helps to mock functions without explicitly specifying behavior for each
        authRepository = mockk(relaxed = true)
        
        viewModel = LoginViewModel(authRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `INP_01 - onStoreCodeChanged cap nhat dung storeCode va clear errorMessage`() {
        // Arrange
        // Call login() with empty inputs to simulate a state with validation error
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle() // Wait for coroutine
        
        val input = "STORE_123"

        // Act
        viewModel.onStoreCodeChanged(input)

        // Assert
        val state = viewModel.loginUiState.value
        assertEquals(input, state.storeCode, "storeCode phai bang gia tri da nhap")
        assertNull(state.error, "error phai duoc clear ve null")
    }

    @Test
    fun `INP_02 - onUserNameChanged cap nhat dung userName va clear errorMessage`() {
        // Arrange
        viewModel.onStoreCodeChanged("STORE") 
        viewModel.login() // This will cause EmptyUsername error
        testDispatcher.scheduler.advanceUntilIdle()
        
        val input = "admin_user"

        // Act
        viewModel.onUserNameChanged(input)

        // Assert
        val state = viewModel.loginUiState.value
        assertEquals(input, state.userName, "userName phai bang gia tri da nhap")
        assertNull(state.error, "error phai duoc clear ve null")
    }

    @Test
    fun `INP_03 - onPasswordChanged cap nhat dung password va clear errorMessage`() {
        // Arrange
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("USER")
        viewModel.login() // This will cause EmptyPassword error
        testDispatcher.scheduler.advanceUntilIdle()
        
        val input = "12345678"

        // Act
        viewModel.onPasswordChanged(input)

        // Assert
        val state = viewModel.loginUiState.value
        assertEquals(input, state.password, "password phai bang gia tri da nhap")
        assertNull(state.error, "error phai duoc clear ve null")
    }

    @Test
    fun `INP_04 - onPasswordVisibilityChanged dao nguoc gia tri isPasswordVisible`() {
        // Arrange
        val initialState = viewModel.loginUiState.value.isPasswordVisible
        assertFalse(initialState, "Mac dinh password khong duoc hien thi (false)")

        // Act 1
        viewModel.onPasswordVisibilityChanged()

        // Assert 1
        assertTrue(viewModel.loginUiState.value.isPasswordVisible, "Click lan 1 phai la true")

        // Act 2
        viewModel.onPasswordVisibilityChanged()

        // Assert 2
        assertFalse(viewModel.loginUiState.value.isPasswordVisible, "Click lan 2 phai doi ve false")
    }

    @Test
    fun `INP_05 - onErrorShown clear duoc error va reset loginResult ve Idle`() {
        // Arrange
        viewModel.login() // Create an error state
        testDispatcher.scheduler.advanceUntilIdle()
        
        val stateBefore = viewModel.loginUiState.value
        assertTrue(stateBefore.error != null, "Phai dang co loi de test viec clear")

        // Act
        viewModel.onErrorShown()

        // Assert
        val stateAfter = viewModel.loginUiState.value
        assertNull(stateAfter.error, "error phai bang null sau khi da show xong")
        assertEquals(LoginResult.Idle, stateAfter.loginResult, "loginResult phai quay ve Idle")
    }

    @Test
    fun `VAL_01 - login bao loi khi storeCode rong`() = runTest {
        viewModel.onStoreCodeChanged("   ") // Toàn khoảng trắng
        
        viewModel.loginUiState.test {
            val initialState = awaitItem()
            assertNull(initialState.error)

            viewModel.login()

            val stateWithError = awaitItem()
            assertEquals(LoginError.EmptyStoreCode, stateWithError.error)
        }
    }

    @Test
    fun `VAL_02 - login bao loi khi userName rong`() = runTest {
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("")
        
        viewModel.loginUiState.test {
            awaitItem() // Skip initial state
            
            viewModel.login()
            
            assertEquals(LoginError.EmptyUsername, awaitItem().error)
            coVerify(exactly = 0) { authRepository.login(any(), any(), any()) }
        }
    }

    @Test
    fun `VAL_03 - login bao loi khi password rong`() = runTest {
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("USER")
        viewModel.onPasswordChanged("")
        
        viewModel.loginUiState.test {
            awaitItem() // Skip initial state
            
            viewModel.login()
            
            assertEquals(LoginError.EmptyPassword, awaitItem().error)
            coVerify(exactly = 0) { authRepository.login(any(), any(), any()) }
        }
    }

    @Test
    fun `ASYNC_02 - login tu dong trim userName truoc khi goi API`() = runTest {
        // Arrange
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("  admin_user  ")
        viewModel.onPasswordChanged("123456")

        // Act
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { 
            authRepository.login(
                storeCode = "STORE",
                username = "admin_user", // Must be trimmed
                password = "123456"
            )
        }
    }

    @Test
    fun `ASYNC_01 - login block multiple calls if isLoading is true`() = runTest {
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("USER")
        viewModel.onPasswordChanged("123456")
        
        // Mock repository to simulate a long-running API call (1000ms delay)
        coEvery { authRepository.login(any(), any(), any()) } coAnswers { 
            delay(1000)
            Resource.Success(mockk(relaxed = true))
        }

        // 1. Kích hoạt login lần 1 (sẽ làm isLoading = true và dừng lại ở delay)
        viewModel.login()
        
        // Phải runCurrent() để coroutine thực sự bắt đầu và nhảy vào khối delay()
        testDispatcher.scheduler.runCurrent()
        
        assertTrue(viewModel.loginUiState.value.isLoading, "Phai dang loading (true)")

        // 2. Kích hoạt login lần 2 ngay lập tức trong lúc isLoading vẫn đang true
        viewModel.login()
        
        // Cho thời gian trôi qua hết để coroutine hoàn tất
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify API chỉ được gọi đúng 1 lần, lần gọi thứ 2 đã bị chặn
        coVerify(exactly = 1) { authRepository.login(any(), any(), any()) }
    }

    @Test
    fun `ASYNC_03 - login quan ly State Loading chinh xac`() = runTest {
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("USER")
        viewModel.onPasswordChanged("123456")
        
        coEvery { authRepository.login(any(), any(), any()) } returns Resource.Success(mockk(relaxed = true))

        viewModel.loginUiState.test {
            awaitItem() // Skip state ban đầu
            
            viewModel.login()
            
            // Lần emit đầu tiên khi ấn login: Loading = true
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading, "Truoc khi goi API, isLoading phai = true")
            assertNull(loadingState.error)
            
            // Lần emit thứ hai sau khi API trả về Success: Loading = false, loginResult = Success
            val successState = awaitItem()
            assertFalse(successState.isLoading, "Sau khi goi API xong, isLoading phai = false")
            assertEquals(LoginResult.Success, successState.loginResult)
        }
    }

    @Test
    fun `NAV_01 - login thanh cong voi ROLE_OWNER chuyen huong ve AdminDashboard`() = runTest {
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("USER")
        viewModel.onPasswordChanged("123456")
        
        val userInfo = mockk<UserInfoDto>(relaxed = true) {
            every { roles } returns listOf("ROLE_OWNER")
        }
        coEvery { authRepository.login(any(), any(), any()) } returns Resource.Success(userInfo)

        // Dùng Turbine để hứng sự kiện Navigation
        viewModel.navigationEvent.test {
            viewModel.login()
            
            val event = awaitItem()
            assertEquals(LoginNavigationEvent.ToAdminDashboard, event)
            
            // Đảm bảo không còn event nào bị emit thừa
            expectNoEvents()
        }
    }

    @Test
    fun `NAV_03 - login thanh cong voi ROLE_CASHIER chuyen huong ve ToPOS`() = runTest {
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("USER")
        viewModel.onPasswordChanged("123456")
        
        val userInfo = mockk<UserInfoDto>(relaxed = true) {
            every { roles } returns listOf("ROLE_CASHIER")
        }
        coEvery { authRepository.login(any(), any(), any()) } returns Resource.Success(userInfo)

        viewModel.navigationEvent.test {
            viewModel.login()
            assertEquals(LoginNavigationEvent.ToPOS, awaitItem())
        }
    }

    @Test
    fun `NAV_04 - login thanh cong voi ROLE_WAREHOUSE_STAFF chuyen huong ve ToWarehouse`() = runTest {
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("USER")
        viewModel.onPasswordChanged("123456")
        
        val userInfo = mockk<UserInfoDto>(relaxed = true) {
            every { roles } returns listOf("ROLE_WAREHOUSE_STAFF")
        }
        coEvery { authRepository.login(any(), any(), any()) } returns Resource.Success(userInfo)

        viewModel.navigationEvent.test {
            viewModel.login()
            assertEquals(LoginNavigationEvent.ToWarehouse, awaitItem())
        }
    }

    @Test
    fun `NAV_05 - login thanh cong voi Role la chuyen huong ve fallback AdminDashboard`() = runTest {
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("USER")
        viewModel.onPasswordChanged("123456")
        
        val userInfo = mockk<UserInfoDto>(relaxed = true) {
            every { roles } returns listOf("ROLE_NEW_UNKNOWN")
        }
        coEvery { authRepository.login(any(), any(), any()) } returns Resource.Success(userInfo)

        viewModel.navigationEvent.test {
            viewModel.login()
            assertEquals(LoginNavigationEvent.ToAdminDashboard, awaitItem())
        }
    }

    @Test
    fun `ERR_01 - API tra ve loi mang, cap nhat dung state NetworkUnavailable`() = runTest {
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("USER")
        viewModel.onPasswordChanged("123456")
        
        // Giả lập rớt mạng
        val networkException = Exception("Connection timeout")
        coEvery { authRepository.login(any(), any(), any()) } returns Resource.Error(networkException)

        viewModel.loginUiState.test {
            awaitItem() // Skip initial state
            
            viewModel.login()
            
            // Bỏ qua trạng thái Loading = true đầu tiên
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            
            // Hứng trạng thái trả về sau khi API lỗi
            val errorState = awaitItem()
            
            assertFalse(errorState.isLoading, "Phải tắt loading khi có lỗi")
            assertEquals(LoginError.NetworkUnavailable, errorState.error)
            assertEquals(LoginResult.Error(LoginError.NetworkUnavailable), errorState.loginResult)
        }
    }

    @Test
    fun `ERR_02 - API tra ve loi sai mat khau, cap nhat dung state InvalidCredentials`() = runTest {
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("USER")
        viewModel.onPasswordChanged("123456")
        
        // Giả lập API trả về sai credentials
        val authException = Exception("Tài khoản hoặc mật khẩu bị sai")
        coEvery { authRepository.login(any(), any(), any()) } returns Resource.Error(authException)

        viewModel.loginUiState.test {
            awaitItem() 
            viewModel.login()
            
            awaitItem() // Bỏ qua Loading = true
            
            val errorState = awaitItem()
            assertEquals(LoginError.InvalidCredentials, errorState.error)
            assertEquals(LoginResult.Error(LoginError.InvalidCredentials), errorState.loginResult)
        }
    }

    @Test
    fun `ERR_03 - API tra ve loi chua xac dinh (null message), loginResult va error cap nhat dung Unknown`() = runTest {
        viewModel.onStoreCodeChanged("STORE")
        viewModel.onUserNameChanged("USER")
        viewModel.onPasswordChanged("123456")
        
        // Giả lập một Exception hoàn toàn không có message (message = null)
        val unknownException = Exception((null as String?))
        coEvery { authRepository.login(any(), any(), any()) } returns Resource.Error(unknownException)

        viewModel.loginUiState.test {
            awaitItem()
            viewModel.login()
            
            awaitItem() // Bỏ qua Loading
            
            val errorState = awaitItem()
            // Vì message là null, ViewModel phải bọc nó vào LoginError.Unknown(null) an toàn
            assertEquals(LoginError.Unknown(null), errorState.error)
            assertEquals(LoginResult.Error(LoginError.Unknown(null)), errorState.loginResult)
        }
    }
}
