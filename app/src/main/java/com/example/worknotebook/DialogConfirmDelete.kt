package com.example.worknotebook

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment


class DialogConfirmDelete() : DialogFragment() {

    private val isActive: Boolean
        get() = requireArguments().getBoolean(ARG_IS_ACTIVE)

    companion object {
        const val ARG_IS_ACTIVE = "is_active"
        const val RESULT_KEY = "delete_mode"
        const val RESULT_ARCHIVE = "can_archive"
        const val RESULT_DELETE = "can_delete"

        fun newInstance(isActive: Boolean): DialogConfirmDelete {
            return DialogConfirmDelete().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_ACTIVE, isActive)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(
            R.layout.dialog_confirm_delete,
            null
        )
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

        val btnCancel = dialog.findViewById<Button>(R.id.btn_confirm_delete_cancel)
        val btnArchive = dialog.findViewById<Button>(R.id.btn_confirm_delete_archive)
        val btnDelete = dialog.findViewById<Button>(R.id.btn_confirm_delete_delete_permanently)
        if (!isActive) {
            btnArchive?.visibility = View.GONE
        }

        btnCancel?.setOnClickListener {
            dismiss()
        }
        btnArchive?.setOnClickListener {
            if (isActive) {
                parentFragmentManager.setFragmentResult(
                    RESULT_KEY,
                    Bundle().apply {
                        putString(RESULT_KEY, RESULT_ARCHIVE)
                    }
                )
                dismiss()
            }
        }
        btnDelete?.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                Bundle().apply {
                    putString(RESULT_KEY, RESULT_DELETE)
                }
            )
            dismiss()
        }
    }
}
