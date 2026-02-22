package com.example.worknotebook

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class AuthActivity : AppCompatActivity() {

    private lateinit var session: UserSessionManager
    private lateinit var userLogin: EditText
    private lateinit var userPass: EditText
    private lateinit var btnAuth: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errCheckConnectText: TextView
    private lateinit var errCheckConnectBTNRefresh: ImageButton
    private lateinit var textToReg: TextView
    private lateinit var linkToReg: TextView

    private var hasConnection: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.auth_page_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        initListeners()
        RetrofitClient.init(this)
        startConnectionCheck()
    }
    private fun initViews() {
        session = UserSessionManager(this)
        userLogin = findViewById(R.id.ett_auth_login)
        userPass = findViewById(R.id.ett_auth_pass)
        btnAuth = findViewById(R.id.btn_auth)
        progressBar = findViewById(R.id.auth_page_progress_bar)
        errCheckConnectText = findViewById(R.id.auth_page_error_check_connect_text)
        errCheckConnectBTNRefresh = findViewById(R.id.btn_auth_page_check_connection_refresh)
        textToReg = findViewById(R.id.go_to_registration_text)
        linkToReg = findViewById(R.id.go_to_registration)
        showInitFields()
    }
    private fun initListeners() {
        btnAuth.setOnClickListener {
            val login = userLogin.text.toString().trim()
            val password = userPass.text.toString().trim()
            var canAuth = true

            if (login.isEmpty()) {
                userLogin.error = resources.getString(R.string.AddLogin)
                canAuth = false
            }
            if (password.isEmpty()) {
                userPass.error = resources.getString(R.string.AddPass)
                canAuth = false
            }

            if (canAuth) {
                authorization(LoginRequest(
                    login = login,
                    password = password
                ))
            }
        }
        errCheckConnectBTNRefresh.setOnClickListener {
            startConnectionCheck()
        }
        linkToReg.apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener {
                val intent = Intent(this@AuthActivity, RegistrationActivity::class.java)
                startActivity(intent)
            }
        }
    }
    private fun showInitFields() {
        userLogin.isEnabled = true
        userPass.isEnabled = true
        btnAuth.visibility = View.VISIBLE
        handleSuccessfulConnection()
    }
    private fun handleSuccessfulConnection() {
        progressBar.visibility = View.GONE
        errCheckConnectText.visibility = View.GONE
        errCheckConnectBTNRefresh.visibility = View.GONE
        textToReg.visibility = View.VISIBLE
        linkToReg.visibility = View.VISIBLE
    }
    private fun showErrorConnect() {
        progressBar.visibility = View.GONE
        errCheckConnectText.visibility = View.VISIBLE
        errCheckConnectBTNRefresh.visibility = View.VISIBLE
    }
    private fun showLoadingState() {
        textToReg.visibility = View.GONE
        linkToReg.visibility = View.GONE
        errCheckConnectText.visibility = View.GONE
        errCheckConnectBTNRefresh.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
    }
    private fun startAuthProcess() {
        userLogin.isEnabled = false
        userPass.isEnabled = false
        btnAuth.visibility = View.GONE
        showLoadingState()
    }
    private fun endErrorAuthProcess() {
        userLogin.isEnabled = true
        userPass.isEnabled = true
        btnAuth.visibility = View.VISIBLE
        showErrorConnect()
    }
    private fun errorAuthView(message: String, userData: LoginRequest) {
        AlertDialog.Builder(this@AuthActivity)
            .setMessage(message)
            .setPositiveButton(resources.getString(R.string.LoginLocal)) { dialog, _ ->
                dialog.dismiss()
                hasConnection = false
                authorization(userData)
            }
            .setNegativeButton(resources.getString(R.string.Cancel)) { dialog, _ ->
                dialog.dismiss()
                showInitFields()
            }
            .show()
    }
    private fun startConnectionCheck() {
        // Кнопку перехода к регистрации показываем, только если есть коннект с сервером
        showLoadingState()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.check()
                if (response.isSuccessful && response.body() == true) {
                    // Сервер доступен
                    hasConnection = true
                    handleSuccessfulConnection()
                } else {
                    // Сервер вернул не успешный статус
                    hasConnection = false
                    showErrorConnect()
                }
            } catch (e: Exception) {
                // Ошибка сети (нет интернета, таймаут и т.п.)
                hasConnection = false
                showErrorConnect()
            }
        }
    }
    private fun authorization(userData: LoginRequest) {

        startAuthProcess()

        // если hasConnection - попробуем авторизироваться через бэк
        if (hasConnection) {

            fun goToLocalAuth() {
                AlertDialog.Builder(this@AuthActivity)
                    .setMessage(resources.getString(R.string.LoginErrorQuestion))
                    .setPositiveButton(resources.getString(R.string.LoginLocal)) { dialog, _ ->
                        dialog.dismiss()
                        hasConnection = false
                        authorization(userData)
                    }
                    .setNegativeButton(resources.getString(R.string.Cancel)) { dialog, _ ->
                        dialog.dismiss()
                        showInitFields()
                    }
                    .show()
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.login(userData)
                    if (response.isSuccessful) {
                        val accessToken = response.body()?.accessToken
                        val userAuthData = response.body()?.user

                        if (accessToken != null && userAuthData?.externalId != null) {
                            val user = session.getMergedUser(UserCreate(
                                externalId = userAuthData.externalId,
                                name = userAuthData.name,
                                login = userAuthData.login,
                                email = userAuthData.email,
                                password = userData.password,
                                verified = userAuthData.verified,
                                isAdmin = userAuthData.isAdmin,
                                createdAt = userAuthData.createdAt,
                                updatedAt = userAuthData.updatedAt,
                                birthdateAt = userAuthData.birthdateAt,
                                gender = userAuthData.gender
                            ))
                            if (user != null) {
                                login(user = user, token = accessToken, resources.getString(R.string.LoginSuccessful))
                            } else {
                                goToLocalAuth()
                            }
                        } else {
                            goToLocalAuth()
                        }
                    } else {
                        Toast.makeText(
                            this@AuthActivity,
                            resources.getString(R.string.LoginUserNotFound),
                            Toast.LENGTH_SHORT
                        ).show()
                        showInitFields()
                    }
                } catch (e: Exception) {
                    AlertDialog.Builder(this@AuthActivity)
                        .setMessage(resources.getString(R.string.LoginErrorLocalQuestion))
                        .setPositiveButton(resources.getString(R.string.LoginLocal)) { dialog, _ ->
                            dialog.dismiss()
                            hasConnection = false
                            authorization(userData)
                        }
                        .setNegativeButton(resources.getString(R.string.Cancel)) { dialog, _ ->
                            dialog.dismiss()
                            endErrorAuthProcess()
                        }
                        .show()
                }
            }

        } else {
            // если hasConnection = false - только локальная авторизация
            val userLocal = session.getAuthorizationVerifyUser(userData)
            if (userLocal != null) {
                login(user = userLocal, token = null, message = resources.getString(R.string.LoginLocalSuccessful))
            } else {
                Toast.makeText(
                    this@AuthActivity,
                    resources.getString(R.string.LoginUserNotFound),
                    Toast.LENGTH_SHORT
                ).show()
                endErrorAuthProcess()
            }
        }
    }

    private fun login(user: User, token: String?, message: String) {
        // прописываем пользователя в сессию, редирект на рабочие страницы
        session.login(
            user = user,
            token = token
        )
        Toast.makeText(
            this@AuthActivity,
            message,
            Toast.LENGTH_SHORT
        ).show()
        val intent = Intent(this@AuthActivity, MainSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }

}