package com.example.worknotebook

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText


class DialogSetPassword() : DialogFragment() {

    companion object {
        const val RESULT_KEY = "input_password"

        fun newInstance(): DialogSetPassword {
            return DialogSetPassword()
        }
    }
    private lateinit var passwordEditText: TextInputEditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val view = requireActivity().layoutInflater.inflate(
            R.layout.dialog_set_password,
            null
        )

        passwordEditText = view.findViewById(R.id.editPassword)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as AlertDialog

        val btnCancel = dialog.findViewById<Button>(R.id.btn_set_password_cancel)
        val btnOk = dialog.findViewById<Button>(R.id.btn_set_password_ok)

        btnCancel?.setOnClickListener {
            dismiss()
        }

        btnOk?.setOnClickListener {
            val password = passwordEditText.text?.toString()?.trim().orEmpty()
            if (password.isEmpty()) {
                passwordEditText.error = resources.getString(R.string.AddPass)
            } else {
                parentFragmentManager.setFragmentResult(
                    RESULT_KEY,
                    Bundle().apply {
                        putString(RESULT_KEY, password)
                    }
                )
                dismiss()
            }
        }
    }
}