package com.example.worknotebook

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var session: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // При старте приложения принудительно вызываем,
        // для предварительного создания / обновления БД
        val helper = DBHelper(this)
        helper.writableDatabase

        initViews()
        val user = session.getUser()
        if (user != null) {
            // Пользователь авторизован → на главный экран с навигацией
            navigateToMainSelection()
        } else {
            // Пользователь не авторизован → на экран входа/регистрации
            navigateToAuth()
        }
    }
    private fun initViews() {
        session = UserSessionManager.getInstance(this)
    }
    private fun navigateToMainSelection() {
        startActivity(Intent(this, MainSelectionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
    private fun navigateToAuth() {
        startActivity(Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

}