package com.example.worknotebook

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class MainSharedViewModel : ViewModel() {

    private val _hasConnection = MutableStateFlow(false)
    val hasConnection: StateFlow<Boolean> = _hasConnection
    fun setConnectionState(value: Boolean) {
        _hasConnection.value = value
    }

    private val _noteFilters = MutableStateFlow(NodeFilters(
        byPriorityDesc = true,
        byUpdatedAtDesc = true,
        byActiveDesc = true,
    ))
    val noteFilters: StateFlow<NodeFilters> = _noteFilters
    fun setNoteFilters(filters: NodeFilters) {
        _noteFilters.value = filters
    }

    private val _meetingFilters = MutableStateFlow(MeetingFilters(
        byMeetingAtDesc = true,
        byUpdatedAtDesc = true,
        byActiveDesc = true,
    ))
    val meetingFilters: StateFlow<MeetingFilters> = _meetingFilters
    fun setMeetingFilters(filters: MeetingFilters) {
        _meetingFilters.value = filters
    }
}


class MainSelectionActivity : AppCompatActivity() {

    private val sharedViewModel: MainSharedViewModel by viewModels()

    private var hasConnection: Boolean = false

    private lateinit var session: UserSessionManager
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigation: NavigationBarView
    private lateinit var btnHeaderFilters: ImageButton
    private lateinit var btnHeaderMenu: ImageButton
    private var currentNavItemId: Int = R.id.nav_notes
    private lateinit var backConnectBTN: ImageButton

    companion object {
        private const val KEY_NAV_ITEM = "current_nav_item"
        private const val KEY_HAS_CONNECTION_STATE = "has_connection"
        const val OPEN_FRAGMENT = "open_fragment"
        const val FRAGMENT_NOTES = "fragment_notes"
        const val FRAGMENT_MEETINGS = "fragment_meetings"
        const val FRAGMENT_USER_SETTINGS = "fragment_user_settings"
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_NAV_ITEM, currentNavItemId)
        outState.putBoolean(KEY_HAS_CONNECTION_STATE, hasConnection)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_selection)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_selection_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        initData()

        val fragmentToOpen = intent.getStringExtra(OPEN_FRAGMENT)

        // Установка начального фрагмента
        if (savedInstanceState != null) {
            // Запуск с сохраненным фрагментом
            currentNavItemId = savedInstanceState.getInt(KEY_NAV_ITEM, R.id.nav_notes)
            bottomNavigation.selectedItemId = currentNavItemId
            loadFragmentForId(currentNavItemId) // загружаем фрагмент по сохранённому ID
        } else {
            // Первый запуск
            currentNavItemId = when (fragmentToOpen) {
                FRAGMENT_NOTES -> R.id.nav_notes
                FRAGMENT_MEETINGS -> R.id.nav_meetings
                FRAGMENT_USER_SETTINGS -> R.id.nav_settings
                else -> R.id.nav_notes
            }
            initHeaderSettingsMenu(navItemId = currentNavItemId)
            bottomNavigation.selectedItemId = currentNavItemId
            loadFragment(NotesFragment.newInstance())
        }

        // Обработка нажатий на пункты меню
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            currentNavItemId = menuItem.itemId
            loadFragmentForId(currentNavItemId)
            true
        }

        // Прослушиваем событие ввода пароля -> пытаемся перелогиниться на бэке + после локально.
        supportFragmentManager.setFragmentResultListener(
            DialogSetPassword.RESULT_KEY,
            this
        ) { _, bundle ->
            val inputPassword = bundle.getString(DialogSetPassword.RESULT_KEY).orEmpty()
            val user = session.getUser()
            if (user != null) {
                if (!inputPassword.trim().isEmpty()) {
                    loginToBack(login = user.login, password = inputPassword.trim())
                }
            } else {
                logout()
            }
        }
    }
    private fun loadFragmentForId(itemId: Int) {
        initHeaderSettingsMenu(navItemId = itemId)
        val fragment = when (itemId) {
            R.id.nav_notes -> NotesFragment.newInstance()
            R.id.nav_meetings -> MeetingsFragment.newInstance()
            R.id.nav_profile -> MeetingsFragment.newInstance()
            R.id.nav_settings -> UserSettingsFragment.newInstance()
            else -> null
        }
        fragment?.let {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, it)
                .commit()
        }
    }
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    private fun initViews() {
        session = UserSessionManager.getInstance(this)
        progressBar = findViewById(R.id.main_selection_page_header_progress_bar)
        bottomNavigation = findViewById(R.id.bottom_main_navigation)
        btnHeaderFilters = findViewById(R.id.btn_main_header_filters)
        btnHeaderMenu = findViewById(R.id.btn_main_header_menu)
        backConnectBTN = findViewById(R.id.btn_back_connect_header)
    }
    private fun initHeaderSettingsMenu(navItemId: Int) {
        when (navItemId) {
            R.id.nav_notes -> {
                btnHeaderFilters.apply {
                    visibility = View.VISIBLE
                    isEnabled = true
                    setOnClickListener {
                        DialogNoteFilters
                            .newInstance(filters = sharedViewModel.noteFilters.value)
                            .show(supportFragmentManager, "DialogNoteFilters")
                    }
                }
            }
            R.id.nav_meetings -> {
                btnHeaderFilters.apply {
                    visibility = View.VISIBLE
                    isEnabled = true
                    setOnClickListener {
                        DialogMeetingFilters
                            .newInstance(filters = sharedViewModel.meetingFilters.value)
                            .show(supportFragmentManager, "DialogMeetingFilters")
                    }
                }
            }
            else -> {
                btnHeaderFilters.apply {
                    visibility = View.GONE
                    isEnabled = false
                }
            }
        }
    }
    private fun initData() {
        RetrofitClient.init(this)
        checkConnection()
        btnHeaderMenu.setOnClickListener { view ->
            showPopupMenu(view)
        }
    }
    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.main_select_page_header_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.check_connect_with_back -> {
                    checkConnection()
                    true
                }
                R.id.sync_data_with_back -> {
                    syncUserData()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }
    private fun syncUserData() {
        val user = session.getUser()
        if (user != null) {
            showLoadingState()
            lifecycleScope.launch {
                try {
                    // TODO реализовать функционал синхронизации данных. Пока, как заглушка - проверка коннекта
                    val response = RetrofitClient.api.check()
                    hasConnection = response.isSuccessful && response.body() == true
                    progressBar.visibility = View.GONE
                    initCheckConnectionData()
                    sharedViewModel.setConnectionState(hasConnection)
                } catch (e: Exception) {
                    hasConnection = false
                    progressBar.visibility = View.GONE
                    initCheckConnectionData()
                    sharedViewModel.setConnectionState(hasConnection)
                }
            }
        }
    }
    private fun initCheckConnectionData() {
        val user = session.getUser()
        if (user != null) {
            if (hasConnection && session.isOnline) {
                backConnectBTN.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.greenColor))
            } else {
                backConnectBTN.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.redDarkColor))
            }
            backConnectBTN.setOnClickListener {
                DialogSetPassword
                    .newInstance()
                    .show(supportFragmentManager, "DialogSetPassword")
            }
        } else {
            logout()
        }
    }
    private fun showLoadingState() {
        progressBar.visibility = View.VISIBLE
    }
    private fun logout() {
        Toast.makeText(this, resources.getString(R.string.userIsNotLogged), Toast.LENGTH_SHORT).show()
        session.logout()
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun checkConnection() {
        showLoadingState()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.check()
                hasConnection = response.isSuccessful && response.body() == true
                progressBar.visibility = View.GONE
                initCheckConnectionData()
                sharedViewModel.setConnectionState(hasConnection)
            } catch (e: Exception) {
                hasConnection = false
                progressBar.visibility = View.GONE
                initCheckConnectionData()
                sharedViewModel.setConnectionState(hasConnection)
            }
        }
    }
    private fun loginSuccessful() {
        Toast.makeText(
            this@MainSelectionActivity,
            resources.getString(R.string.LoginSuccessful),
            Toast.LENGTH_SHORT
        ).show()
        hasConnection = true
        progressBar.visibility = View.GONE
        initData()
        sharedViewModel.setConnectionState(hasConnection)
    }
    private fun loginFailed(message: String) {
        Toast.makeText(
            this@MainSelectionActivity,
            message,
            Toast.LENGTH_SHORT
        ).show()
        hasConnection = false
        progressBar.visibility = View.GONE
        initData()
        sharedViewModel.setConnectionState(hasConnection)
    }
    private fun loginToBack(login: String, password: String) {
        showLoadingState()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.login(LoginRequest(login = login, password = password))
                if (response.isSuccessful) {
                    val accessToken = response.body()?.accessToken
                    val userAuthData = response.body()?.user
                    if (accessToken != null && userAuthData?.externalId != null) {
                        val user = session.getMergedUser(UserCreate(
                            externalId = userAuthData.externalId,
                            name = userAuthData.name,
                            login = userAuthData.login,
                            email = userAuthData.email,
                            password = password,
                            verified = userAuthData.verified,
                            isAdmin = userAuthData.isAdmin,
                            createdAt = userAuthData.createdAt,
                            updatedAt = userAuthData.updatedAt,
                            birthdateAt = userAuthData.birthdateAt,
                            gender = userAuthData.gender
                        ))
                        if (user != null) {
                            session.login(
                                user = user,
                                token = accessToken
                            )
                            loginSuccessful()
                        } else {
                            loginFailed(resources.getString(R.string.LoginUserNotFound))
                        }
                    } else {
                        loginFailed(resources.getString(R.string.LoginUserNotFound))
                    }
                } else {
                    loginFailed(resources.getString(R.string.LoginUserNotFound))
                }
            } catch (e: Exception) {
                loginFailed(resources.getString(R.string.Error))
            }
        }
    }

}
