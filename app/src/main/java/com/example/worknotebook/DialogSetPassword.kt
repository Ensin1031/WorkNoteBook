package com.example.worknotebook

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText


class DialogSetPassword(
    private val onPasswordEntered: (String) -> Unit
) : DialogFragment() {

    private lateinit var passwordEditText: TextInputEditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val view = requireActivity().layoutInflater.inflate(
            R.layout.dialog_set_password,
            null
        )

        passwordEditText = view.findViewById(R.id.editPassword)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false) // запрет закрытия
            .create()

        dialog.setCanceledOnTouchOutside(false) // запрет клика вне окна

        return dialog
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as AlertDialog

        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnOk = dialog.findViewById<Button>(R.id.btnOk)

        btnCancel?.setOnClickListener {
            dismiss()
        }

        btnOk?.setOnClickListener {
            val password = passwordEditText.text?.toString()?.trim().orEmpty()
            onPasswordEntered(password)
            // НЕ закрываем автоматически, если хочешь проверку
            dismiss()
        }
    }
}