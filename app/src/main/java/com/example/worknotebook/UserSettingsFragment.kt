package com.example.worknotebook

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale


class UserSettingsFragment : Fragment() {

    private val sharedViewModel: MainSharedViewModel by activityViewModels()

    private lateinit var session: UserSessionManager
    private lateinit var logoutBTN: Button
    private lateinit var progressBar: ProgressBar
    private var hasConnection: Boolean = false

    // Verified
    private lateinit var checkUserVerified: ImageView

    // Login
    private lateinit var inputLogin: TextView
    private lateinit var btnEditLogin: ImageButton
    private lateinit var btnEditLoginClose: ImageButton
    private lateinit var containerEditLogin: LinearLayout
    private lateinit var setNewLogin: EditText
    private lateinit var btnNewLoginSave: ImageButton

    // Email
    private lateinit var inputEmail: TextView
    private lateinit var btnEditEmail: ImageButton

    // Password
    private lateinit var inputPassword: TextView
    private lateinit var btnEditPassword: ImageButton
    private lateinit var btnEditPasswordClose: ImageButton
    private lateinit var containerEditPassword: LinearLayout
    private lateinit var setOldPassword: EditText
    private lateinit var setNewPassword: EditText
    private lateinit var btnNewPasswordSave: ImageButton

    // ФИО
    private lateinit var inputName: TextView
    private lateinit var btnEditName: ImageButton

    // Birthdate
    private lateinit var inputBirthdate: TextView
    private lateinit var btnEditBirthdate: ImageButton
    private lateinit var btnEditBirthdateClose: ImageButton
    private lateinit var containerEditBirthdate: LinearLayout
    private lateinit var setNewBirthdate: EditText
    private var selectedBirthdateDate: Long? = null
    private lateinit var btnNewBirthdateSave: ImageButton
    private lateinit var setBirthdateSaveError: TextView

    // Gender
    private lateinit var inputGender: TextView
    private lateinit var btnEditGender: ImageButton
    private lateinit var btnEditGenderClose: ImageButton
    private lateinit var containerEditGender: LinearLayout
    private lateinit var inputLayoutEditGender: TextInputLayout
    private lateinit var radioGroupEditGender: RadioGroup
    private lateinit var radioGenderUnset: RadioButton
    private lateinit var radioGenderMale: RadioButton
    private lateinit var radioGenderFemale: RadioButton
    private lateinit var btnNewGenderSave: ImageButton

    //UserSettings
    private lateinit var switchSyncMeetingsImmediately: SwitchMaterial
    private lateinit var switchSyncNotesImmediately: SwitchMaterial

    // Created
    private lateinit var inputCreated: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_settings, container, false)
    }
    companion object {
        @JvmStatic
        fun newInstance() = UserSettingsFragment()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view = view)
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.hasConnection.collect { isConnected ->
                hasConnection = isConnected
                initData()
            }
        }
    }
    private fun initViews (view: View) {
        session = UserSessionManager.getInstance(requireContext())
        progressBar = requireActivity().findViewById(R.id.main_selection_page_header_progress_bar)
        logoutBTN = view.findViewById(R.id.btn_logout)
        // Verified
        checkUserVerified = view.findViewById(R.id.iv_user_settings_verified)
        // ФИО
        inputName = view.findViewById(R.id.tv_user_settings_name)
        btnEditName = view.findViewById(R.id.btn_user_settings_name)
        // Login
        inputLogin = view.findViewById(R.id.tv_user_settings_login)
        btnEditLogin = view.findViewById(R.id.btn_user_settings_login)
        btnEditLoginClose = view.findViewById(R.id.btn_user_settings_login_close)
        containerEditLogin = view.findViewById(R.id.ll_user_settings_login_edit_container)
        setNewLogin = view.findViewById(R.id.te_user_settings_login_change)
        btnNewLoginSave = view.findViewById(R.id.btn_user_settings_login_save)
        // Email
        inputEmail = view.findViewById(R.id.tv_user_settings_email)
        btnEditEmail = view.findViewById(R.id.btn_user_settings_email)
        // Password
        inputPassword = view.findViewById(R.id.tv_user_settings_password)
        btnEditPassword = view.findViewById(R.id.btn_user_settings_password)
        btnEditPasswordClose = view.findViewById(R.id.btn_user_settings_password_close)
        containerEditPassword = view.findViewById(R.id.ll_user_settings_password_edit_container)
        setOldPassword = view.findViewById(R.id.te_user_settings_password_old_change)
        setNewPassword = view.findViewById(R.id.te_user_settings_password_new_change)
        btnNewPasswordSave = view.findViewById(R.id.btn_user_settings_password_save)
        // Birthdate
        inputBirthdate = view.findViewById(R.id.tv_user_settings_birthdate)
        btnEditBirthdate = view.findViewById(R.id.btn_user_settings_birthdate)
        btnEditBirthdateClose = view.findViewById(R.id.btn_user_settings_birthdate_close)
        containerEditBirthdate = view.findViewById(R.id.ll_user_settings_birthdate_edit_container)
        setNewBirthdate = view.findViewById(R.id.te_user_settings_birthdate_change)
        btnNewBirthdateSave = view.findViewById(R.id.btn_user_settings_birthdate_save)
        setBirthdateSaveError = view.findViewById(R.id.tv_user_settings_birthdate_save_error)
        // Gender
        inputGender = view.findViewById(R.id.tv_user_settings_gender)
        btnEditGender = view.findViewById(R.id.btn_user_settings_gender)
        btnEditGenderClose = view.findViewById(R.id.btn_user_settings_gender_close)
        containerEditGender = view.findViewById(R.id.ll_user_settings_gender_edit_container)
        inputLayoutEditGender = view.findViewById(R.id.il_user_settings_gender_edit)
        radioGroupEditGender = view.findViewById(R.id.rg_user_settings_gender_edit)
        radioGenderUnset = view.findViewById(R.id.rb_user_settings_gender_edit_unset)
        radioGenderMale = view.findViewById(R.id.rb_user_settings_gender_edit_male)
        radioGenderFemale = view.findViewById(R.id.rb_user_settings_gender_edit_female)
        btnNewGenderSave = view.findViewById(R.id.btn_user_settings_gender_save)
        // UserSettings
        switchSyncMeetingsImmediately = view.findViewById(R.id.sw_user_settings_sync_meetings_immediately)
        switchSyncNotesImmediately = view.findViewById(R.id.sw_user_settings_sync_notes_immediately)
        // Created
        inputCreated = view.findViewById(R.id.tv_user_settings_created)
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun initListeners(user: User, userSettings: UserSettings) {
        // Login
        btnEditLogin.setOnClickListener {
            btnEditLogin.visibility = View.GONE
            btnEditLoginClose.visibility = View.VISIBLE
            containerEditLogin.visibility = View.VISIBLE
        }
        btnEditLoginClose.setOnClickListener {
            btnEditLogin.visibility = View.VISIBLE
            btnEditLoginClose.visibility = View.GONE
            containerEditLogin.visibility = View.GONE
        }
        btnNewLoginSave.setOnClickListener {
            val newLogin = setNewLogin.text.toString().trim()
            if (!newLogin.isEmpty() && newLogin != user.login) {
                saveUserData(user = user, login = newLogin, editField = setNewLogin)
            } else {
                setNewLogin.error = resources.getString(R.string.AddNewLogin)
            }
        }
        // Password
        btnEditPassword.setOnClickListener {
            btnEditPassword.visibility = View.GONE
            btnEditPasswordClose.visibility = View.VISIBLE
            containerEditPassword.visibility = View.VISIBLE
        }
        btnEditPasswordClose.setOnClickListener {
            btnEditPassword.visibility = View.VISIBLE
            btnEditPasswordClose.visibility = View.GONE
            containerEditPassword.visibility = View.GONE
        }
        btnNewPasswordSave.setOnClickListener {
            val oldPassword = setOldPassword.text.toString().trim()
            val newPassword = setNewPassword.text.toString().trim()
            var hasPassSaved = true
            if (oldPassword.isEmpty()) {
                setOldPassword.error = resources.getString(R.string.AddValue)
                hasPassSaved = false
            } else if (session.getAuthorizationVerifyUser(LoginRequest(login = user.login, password = oldPassword)) == null) {
                setOldPassword.error = resources.getString(R.string.InvalidPassword)
                hasPassSaved = false
            }
            if (newPassword.isEmpty()) {
                setNewPassword.error = resources.getString(R.string.AddValue)
                hasPassSaved = false
            }
            if (hasPassSaved && oldPassword == newPassword) {
                setNewPassword.error = resources.getString(R.string.ChangedPassword)
                hasPassSaved = false
            }
            if (hasPassSaved) {
                saveUserData(user = user, oldPassword = oldPassword, newPassword = newPassword, editField = setNewPassword)
            }
        }
        // Birthdate
        btnEditBirthdate.setOnClickListener {
            btnEditBirthdate.visibility = View.GONE
            btnEditBirthdateClose.visibility = View.VISIBLE
            containerEditBirthdate.visibility = View.VISIBLE
        }
        btnEditBirthdateClose.setOnClickListener {
            btnEditBirthdate.visibility = View.VISIBLE
            btnEditBirthdateClose.visibility = View.GONE
            containerEditBirthdate.visibility = View.GONE
        }
        setNewBirthdate.setOnClickListener {
            setBirthdateSaveError.text = null
            setBirthdateSaveError.visibility = View.GONE
            showDatePickerDialog(setTimestamp = selectedBirthdateDate, inputField = setNewBirthdate) { timestamp ->
                selectedBirthdateDate = timestamp
            }
        }
        setNewBirthdate.setOnTouchListener { v, event ->
            // эвент клика по кнопке очистки поля выбора даты рождения
            if (event.action == MotionEvent.ACTION_UP) {
                val editText = v as EditText
                val drawableEnd = editText.compoundDrawablesRelative[2] // индекс 2 — правая иконка
                if (drawableEnd != null) {
                    // Определяем, попал ли клик в область иконки (правый край)
                    val isClickOnEndIcon = event.x >= editText.width - editText.totalPaddingRight
                    if (isClickOnEndIcon) {
                        editText.text = null
                        selectedBirthdateDate = null
                        setNewBirthdate.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
        btnNewBirthdateSave.setOnClickListener {
            when {
                selectedBirthdateDate == null && user.birthdateAt == null -> {
                    // если старая дата отсутствует, и поле ввода пустое
                    setBirthdateSaveError.text = resources.getString(R.string.AddUserBirthdate)
                    setBirthdateSaveError.visibility = View.VISIBLE
                }
                selectedBirthdateDate != null && selectedBirthdateDate == user.birthdateAt -> {
                    // если старая дата == введенной в поле ввода
                    setBirthdateSaveError.text = resources.getString(R.string.AddNewUserBirthdate)
                    setBirthdateSaveError.visibility = View.VISIBLE
                }
                else -> {
                    // очищаем ошибки и пытаемся сохранить.
                    setBirthdateSaveError.text = null
                    setBirthdateSaveError.visibility = View.GONE
                    val outputValue = if (selectedBirthdateDate == null) { TIMESTAMP_1900 } else { selectedBirthdateDate }
                    saveUserData(user = user, birthdateAt = outputValue, editField = setBirthdateSaveError)
                }
            }
        }
        // Gender
        btnEditGender.setOnClickListener {
            btnEditGender.visibility = View.GONE
            btnEditGenderClose.visibility = View.VISIBLE
            containerEditGender.visibility = View.VISIBLE
        }
        radioGenderUnset.setOnClickListener {
            inputLayoutEditGender.isErrorEnabled = false
            inputLayoutEditGender.error = null
        }
        radioGenderMale.setOnClickListener {
            inputLayoutEditGender.isErrorEnabled = false
            inputLayoutEditGender.error = null
        }
        radioGenderFemale.setOnClickListener {
            inputLayoutEditGender.isErrorEnabled = false
            inputLayoutEditGender.error = null
        }
        btnNewGenderSave.setOnClickListener {
            val selectedGender = when (
                radioGroupEditGender.checkedRadioButtonId
            ) {
                R.id.rb_user_settings_gender_edit_unset -> GenderType.UNSET
                R.id.rb_user_settings_gender_edit_male -> GenderType.MALE
                R.id.rb_user_settings_gender_edit_female -> GenderType.FEMALE
                else -> GenderType.UNSET
            }
            if (selectedGender != user.gender) {
                saveUserData(user = user, gender = selectedGender, editField = inputLayoutEditGender)
            } else {
                inputLayoutEditGender.error = resources.getString(R.string.AddNewValue)
            }
        }
        btnEditGenderClose.setOnClickListener {
            btnEditGender.visibility = View.VISIBLE
            btnEditGenderClose.visibility = View.GONE
            containerEditGender.visibility = View.GONE
        }

        logoutBTN.setOnClickListener {
            logout()
        }

        switchSyncMeetingsImmediately.setOnCheckedChangeListener { _, isChecked ->
            applySetting(
                userSettings = userSettings,
                syncMeetingsImmediately = isChecked,
                syncNotesImmediately = switchSyncNotesImmediately.isChecked
            )
        }
        switchSyncNotesImmediately.setOnCheckedChangeListener { _, isChecked ->
            applySetting(
                userSettings = userSettings,
                syncMeetingsImmediately = switchSyncMeetingsImmediately.isChecked,
                syncNotesImmediately = isChecked
            )
        }
    }
    private fun applySetting(userSettings: UserSettings, syncMeetingsImmediately: Boolean, syncNotesImmediately: Boolean) {
        switchSyncNotesImmediately.isEnabled = false
        switchSyncMeetingsImmediately.isEnabled = false
        if (session.updateUserSettings(
                userSettings = userSettings,
                syncMeetingsImmediately = syncMeetingsImmediately,
                syncNotesImmediately = syncNotesImmediately
        )) {
            switchSyncNotesImmediately.isEnabled = true
            switchSyncMeetingsImmediately.isEnabled = true
        } else {
            switchSyncNotesImmediately.isEnabled = true
            switchSyncMeetingsImmediately.isEnabled = true
        }
    }
    private fun logout() {
        session.logout()
        val intent = Intent(requireContext(), AuthActivity::class.java)  // Переходим на экран авторизации
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  // Очищаем back stack
        startActivity(intent)
        requireActivity().finish()  // Закрываем Activity, содержащую фрагмент
    }
    private fun getViewDateTimeFromTimestamp(row: Long?, onlyDate: Boolean = false): String {
        if (row == null) return ""
        val date = Date(row)
        val dateFormat: SimpleDateFormat = if (!onlyDate) {
            SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        }
        return dateFormat.format(date)
    }
    private fun initData() {
        if (hasConnection) {
            RetrofitClient.init(requireContext())
            handleSuccessfulConnection()
        } else {
            showErrorConnect()
        }
        val user: User? = session.getUser()
        if (user != null) {
            checkUserVerified.visibility = if (user.verified) View.VISIBLE else View.GONE
            inputName.text = user.name
            inputLogin.text = user.login
            setNewLogin.setText(user.login)
            inputEmail.text = user.email
            inputBirthdate.text = getViewDateTimeFromTimestamp(user.birthdateAt, true)
            selectedBirthdateDate = user.birthdateAt
            if (user.birthdateAt != null) {
                setNewBirthdate.setText(getViewDateTimeFromTimestamp(user.birthdateAt, true))
                setNewBirthdate.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_close_red, 0)
            }
            inputCreated.text = getViewDateTimeFromTimestamp(user.createdAt)
            inputGender.text = resources.getString(user.gender.labelResId)
            when (user.gender) {
                GenderType.UNSET -> radioGenderUnset.isChecked = true
                GenderType.MALE -> radioGenderMale.isChecked = true
                GenderType.FEMALE -> radioGenderFemale.isChecked = true
            }

            val userSettings = session.getUserSettings(user.id!!)!!
            switchSyncMeetingsImmediately.setOnCheckedChangeListener(null)
            switchSyncMeetingsImmediately.isChecked = userSettings.syncMeetingsImmediately
            switchSyncNotesImmediately.setOnCheckedChangeListener(null)
            switchSyncNotesImmediately.isChecked = userSettings.syncNotesImmediately

            initListeners(user = user, userSettings = userSettings)

        } else {
            Toast.makeText(requireContext(), resources.getString(R.string.userIsNotLogged), Toast.LENGTH_SHORT).show()
            session.logout()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }
    private fun showDatePickerDialog(
        setTimestamp: Long?,
        inputField: EditText,
        onDateSelected: (Long) -> Unit
    ) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        if (setTimestamp != null) {
            calendar.timeInMillis = setTimestamp
        } else {
            // Устанавливаем дату на 20 лет назад от текущей
            calendar.add(Calendar.YEAR, -20)
        }

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val timestamp = calendar.timeInMillis
                onDateSelected(timestamp)
                // Отображаем дату в нужном формате
                val formatted = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(calendar.time)
                inputField.setText(formatted)
                inputField.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_close_red, 0)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
    private fun showLoadingState() {
        progressBar.visibility = View.VISIBLE
    }
    private fun endLoadingState() {
        progressBar.visibility = View.GONE
    }
    private fun handleSuccessfulConnection() {
        val user: User? = session.getUser()
        if (user != null && hasConnection && session.isOnline) {
            btnEditLogin.visibility = View.VISIBLE
            btnEditLoginClose.visibility = View.GONE
            btnEditPassword.visibility = View.VISIBLE
            btnEditPasswordClose.visibility = View.GONE
            btnEditBirthdate.visibility = View.VISIBLE
            btnEditBirthdateClose.visibility = View.GONE
            btnEditGender.visibility = View.VISIBLE
            btnEditGenderClose.visibility = View.GONE
        }
        containerEditLogin.visibility = View.GONE
        containerEditPassword.visibility = View.GONE
        containerEditGender.visibility = View.GONE
        containerEditBirthdate.visibility = View.GONE
    }
    private fun showErrorConnect() {
        btnEditLogin.visibility = View.GONE
        btnEditLoginClose.visibility = View.GONE
        btnEditPassword.visibility = View.GONE
        btnEditPasswordClose.visibility = View.GONE
        btnEditBirthdate.visibility = View.GONE
        btnEditBirthdateClose.visibility = View.GONE
        btnEditGender.visibility = View.GONE
        btnEditGenderClose.visibility = View.GONE
    }
    private fun saveUserData(
        user: User,
        login: String? = null,
        oldPassword: String? = null,
        newPassword: String? = null,
        birthdateAt: Long? = null,
        gender: GenderType? = null,
        editField: View
    ) {
        if (!hasConnection) {
            return
        }

        showLoadingState()

        fun viewSaveError(message: String) {
            editField.let {
                when (it) {
                    is EditText -> it.error = message
                    is TextView -> {
                        it.text = message
                        it.visibility = View.VISIBLE
                    }
                    is TextInputLayout -> it.error = message
                }
            }
            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
            ).show()
        }

        lifecycleScope.launch {
            try {
                val userRequestData = UserUpdate(
                    login = login,
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    birthdateAt = birthdateAt,
                    gender = gender
                )
                val response = RetrofitClient.api.updateUser(
                    userId = user.externalId.toInt(),
                    request = userRequestData
                )
                if (response.isSuccessful) {
                    val userUpdatedData = response.body()
                    if (userUpdatedData != null) {
                        userRequestData.updatedAt = userUpdatedData.updatedAt
                        userRequestData.verified = userUpdatedData.verified
                        userRequestData.isAdmin = userUpdatedData.isAdmin
                        session.updateSessionUser(userData = userRequestData)
                        Toast.makeText(
                            requireContext(),
                            resources.getString(R.string.SuccessfulSaving),
                            Toast.LENGTH_SHORT
                        ).show()
                        editField.let {
                            when (it) {
                                is EditText -> it.error = null
                                is TextView -> {
                                    it.text = null
                                    it.visibility = View.GONE
                                }
                                is TextInputLayout -> {
                                    it.isErrorEnabled = false
                                    it.error = null
                                }
                            }
                        }
                        initData()
                        endLoadingState()
                        handleSuccessfulConnection()
                    } else {
                        endLoadingState()
                        viewSaveError(message = parseError(response))
                    }
                } else {
                    endLoadingState()
                    viewSaveError(message = parseError(response))
                }
            } catch (e: Exception) {
                endLoadingState()
                viewSaveError(message = "$e")
            }
        }
    }

}