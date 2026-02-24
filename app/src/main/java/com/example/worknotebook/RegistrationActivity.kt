package com.example.worknotebook

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.launch
import retrofit2.Response

class RegistrationActivity : AppCompatActivity() {

    private lateinit var session: UserSessionManager
    private lateinit var userName: EditText
    private lateinit var userLogin: EditText
    private lateinit var userEmail: EditText
    private lateinit var userPass: EditText
    private lateinit var btnRegistration: Button
    private lateinit var linkToAuthText: TextView
    private lateinit var linkToAuth: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registration_page_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        RetrofitClient.init(this)
        initListeners()

    }
    private fun initViews() {
        session = UserSessionManager.getInstance(this)
        userName = findViewById(R.id.ett_registration_name)
        userLogin = findViewById(R.id.ett_registration_login)
        userEmail = findViewById(R.id.ett_registration_email)
        userPass = findViewById(R.id.ett_registration_pass)
        btnRegistration = findViewById(R.id.btn_registration)
        linkToAuthText = findViewById(R.id.tv_registration_go_to_auth_text)
        linkToAuth = findViewById(R.id.go_to_auth)
        progressBar = findViewById(R.id.registration_page_progress_bar)
        setViewState()
    }
    private fun initListeners() {
        btnRegistration.setOnClickListener {
            val name = userName.text.toString().trim()
            val login = userLogin.text.toString().trim()
            val email = userEmail.text.toString().trim()
            val pass = userPass.text.toString().trim()
            var canRegister = true

            if (name.isEmpty()) {
                userName.error = resources.getString(R.string.AddName)
                canRegister = false
            }
            if (login.isEmpty()) {
                userLogin.error = resources.getString(R.string.AddLogin)
                canRegister = false
            }
            if (email.isEmpty()) {
                userEmail.error = resources.getString(R.string.AddEmail)
                canRegister = false
            }
            if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                userEmail.error = resources.getString(R.string.IncorrectEmail)
                canRegister = false
            }
            if (pass.isEmpty()) {
                userPass.error = resources.getString(R.string.AddPass)
                canRegister = false
            }

            if (canRegister) {
                val existing = session.getCheckedUserByLogin(login) ?: session.getCheckedUserByEmail(email)
                if (existing != null) {
                    Toast.makeText(this@RegistrationActivity, resources.getString(R.string.NotRegistrationUserExisting), Toast.LENGTH_SHORT).show()
                }
                register(
                    RegisterRequest(
                        name = name,
                        login = login,
                        email = email,
                        password = pass
                    )
                )
            }
        }
        linkToAuth.apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener {
                val intent = Intent(this@RegistrationActivity, AuthActivity::class.java)
                startActivity(intent)
            }
        }
    }
    private fun register(userData: RegisterRequest) {
        showLoadingState()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.register(userData)
                if (response.isSuccessful) {
                    val createdUser = response.body()?.user
                    if (
                        createdUser != null && session.register(
                            UserCreate(
                                externalId = createdUser.externalId,
                                name = createdUser.name,
                                login = createdUser.login,
                                email = createdUser.email,
                                password = userData.password,
                                verified = createdUser.verified,
                                isAdmin = createdUser.isAdmin,
                                createdAt = createdUser.createdAt,
                                updatedAt = createdUser.updatedAt,
                                birthdateAt = createdUser.birthdateAt,
                                gender = createdUser.gender
                            ),
                            token = response.body()?.accessToken
                        )) {
                        setViewState()
                        Toast.makeText(
                            this@RegistrationActivity,
                            resources.getString(R.string.RegistrationSuccessful),
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(this@RegistrationActivity, MainSelectionActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        setViewState()
                    }
                } else {
                    viewErrors(errors = parseRegisterError(response))
                    setViewState()
                }
            } catch (e: Exception) {
                AlertDialog.Builder(this@RegistrationActivity)
                    .setMessage("${resources.getString(R.string.Error)}:\n$e")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                setViewState()
            }
        }
    }
    private fun viewErrors(errors: ErrorRegisterResponse) {
        userName.error = errors.nameError
        userLogin.error = errors.loginError
        userEmail.error = errors.emailError
        userPass.error = errors.passwordError

        if (!errors.unknownError.isNullOrBlank()) {
            AlertDialog.Builder(this@RegistrationActivity)
                .setMessage("${resources.getString(R.string.Error)}:\n${errors.unknownError}")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
    private fun parseRegisterError(response: Response<*>): ErrorRegisterResponse {
        val errorBody = response.errorBody()
        if (errorBody == null) {
            return ErrorRegisterResponse(unknownError = "Ошибка ${response.code()}")
        }

        val errorString = try {
            errorBody.string()
        } catch (e: Exception) {
            return ErrorRegisterResponse(unknownError = "Не удалось прочитать ошибку")
        }

        // Пытаемся распарсить как обёртку с detail
        return try {
            val wrapper = Gson().fromJson(errorString, ErrorDetailWrapper::class.java)
            if (wrapper.detail != null) {
                // Преобразуем Map в ErrorRegisterResponse
                ErrorRegisterResponse(
                    nameError = wrapper.detail["name"],
                    loginError = wrapper.detail["login"],
                    emailError = wrapper.detail["email"],
                    passwordError = wrapper.detail["password"],
                    unknownError = wrapper.detail["detail"]
                )
            } else {
                // Если структура не совпала, пробуем распарсить напрямую в ErrorRegisterResponse
                try {
                    Gson().fromJson(errorString, ErrorRegisterResponse::class.java)
                } catch (e: JsonSyntaxException) {
                    ErrorRegisterResponse(unknownError = errorString)
                }
            }
        } catch (e: Exception) {
            // Всё сломалось – берем сырой текст
            ErrorRegisterResponse(unknownError = errorString)
        }
    }

    private fun showLoadingState() {
        userName.isEnabled = false
        userLogin.isEnabled = false
        userEmail.isEnabled = false
        userPass.isEnabled = false
        btnRegistration.visibility = View.GONE
        linkToAuthText.visibility = View.GONE
        linkToAuth.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
    }
    private fun setViewState() {
        userName.isEnabled = true
        userLogin.isEnabled = true
        userEmail.isEnabled = true
        userPass.isEnabled = true
        btnRegistration.visibility = View.VISIBLE
        linkToAuthText.visibility = View.VISIBLE
        linkToAuth.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }
}