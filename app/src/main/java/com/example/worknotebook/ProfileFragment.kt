package com.example.worknotebook

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener


class ProfileFragment : Fragment() {

    private lateinit var session: UserSessionManager
    private lateinit var editBTN: Button
    private lateinit var teID: EditText
    private lateinit var teName: EditText
    private lateinit var teLogin: EditText
    private lateinit var teEmail: EditText
    private lateinit var logoutBTN: Button

    private var userIsLogged: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) = ProfileFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
    }
    private fun initViews(view: View) {

        editBTN = view.findViewById(R.id.btn_edit_profile)
        teID = view.findViewById(R.id.ett_user_settings_id)
        teName = view.findViewById(R.id.ett_user_settings_name)
        teLogin = view.findViewById(R.id.ett_user_settings_login)
        teEmail = view.findViewById(R.id.ett_user_settings_email)
        logoutBTN = view.findViewById(R.id.btn_logout)

        session = UserSessionManager(requireContext())

        val user: User? = session.getUser()

        if (user != null) {
            RetrofitClient.init(requireContext())
            updateUserSettingsContent(user = user)

            userIsLogged = session.isOnline

            fun checkForChanges() {
                val sessionM = UserSessionManager(requireContext())
                val checkedUser: User? = sessionM.getUser()

                val nameChanged = checkedUser != null &&
                        !teName.text.toString().trim().isEmpty() &&
                        teName.text.toString().trim() != checkedUser.name.trim()

                val loginChanged = checkedUser != null &&
                        !teLogin.text.toString().trim().isEmpty() &&
                        teLogin.text.toString().trim() != checkedUser.login.trim()

                val emailChanged = checkedUser != null &&
                        !teEmail.text.toString().trim().isEmpty() &&
                        teEmail.text.toString().trim() != checkedUser.email.trim()

                editBTN.isEnabled = nameChanged || loginChanged || emailChanged
            }

            teName.addTextChangedListener { checkForChanges() }
            teLogin.addTextChangedListener { checkForChanges() }
            teEmail.addTextChangedListener { checkForChanges() }

            editBTN.setOnClickListener {
                if (editBTN.isEnabled &&
                    !teName.text.toString().trim().isEmpty() &&
                    !teLogin.text.toString().trim().isEmpty() &&
                    !teEmail.text.toString().trim().isEmpty() &&
                    user.id != null
                ) {
                    saveChangedUserData(
                        name = teName.text.toString().trim(),
                        login = teLogin.text.toString().trim(),
                        email = teEmail.text.toString().trim(),
                        userId = user.id.toInt(),
                    )
                }
            }

            logoutBTN.setOnClickListener {
                session.logout()
                val intent = Intent(requireContext(), AuthActivity::class.java)  // Переходим на экран авторизации
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  // Очищаем back stack
                startActivity(intent)
                requireActivity().finish()  // Закрываем Activity, содержащую фрагмент
            }
        } else {
            Toast.makeText(requireContext(), resources.getString(R.string.userIsNotLogged), Toast.LENGTH_SHORT).show()
            session.logout()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

    }

    private fun updateUserSettingsContent(user: User) {
        editBTN.isEnabled = false
        // Заполнение данных по ID пользователя
        teID.isEnabled = false
        teID.setText(user.id.toString())
        // Заполнение данных по ФИО пользователя
        teName.setText(user.name.trim())
        // Заполнение данных по Логину пользователя
        teLogin.setText(user.login.trim())
        // Заполнение данных по Email пользователя
        teEmail.setText(user.email.trim())
    }

    private fun saveChangedUserData(name: String, login: String, email: String, userId: Int) {
        // TODO
    }

}