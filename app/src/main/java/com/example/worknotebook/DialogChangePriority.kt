package com.example.worknotebook

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment


class DialogChangePriority() : DialogFragment() {

    private var initialPriority: NotePriority = NotePriority.NORMAL
    private lateinit var group: RadioGroup
    private lateinit var high: RadioButton
    private lateinit var normal: RadioButton
    private lateinit var low: RadioButton

    companion object {
        const val RESULT_KEY = "change_priority_result"
        private const val ARG_PRIORITY = "arg_priority"

        fun newInstance(priority: NotePriority): DialogChangePriority {
            return DialogChangePriority().apply {
                arguments = Bundle().apply {
                    putString(ARG_PRIORITY, priority.name)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val priorityName = arguments?.getString(ARG_PRIORITY)
        initialPriority = priorityName?.let {
            NotePriority.valueOf(it)
        } ?: NotePriority.NORMAL
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val view = requireActivity().layoutInflater.inflate(
            R.layout.dialog_change_priority,
            null
        )

        group = view.findViewById(R.id.rg_change_priority)
        high = view.findViewById(R.id.rb_change_priority_high)
        normal = view.findViewById(R.id.rb_change_priority_normal)
        low = view.findViewById(R.id.rb_change_priority_low)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }

    private fun updatePriorityIcons(checkedId: Int) {
        // Сбрасываем иконки у всех
        listOf(high, normal, low).forEach { it.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0) }

        // Устанавливаем галочку для выбранной
        val selectedRadio = when (checkedId) {
            R.id.rb_change_priority_high -> high
            R.id.rb_change_priority_normal -> normal
            R.id.rb_change_priority_low -> low
            else -> null
        }
        selectedRadio?.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0)
    }
    override fun onStart() {

        val initialId: Int = when (initialPriority) {
            NotePriority.HIGH -> R.id.rb_change_priority_high
            NotePriority.NORMAL -> R.id.rb_change_priority_normal
            NotePriority.LOW -> R.id.rb_change_priority_low

        }
        group.check(initialId)
        updatePriorityIcons(initialId)

        group.setOnCheckedChangeListener { _, checkedId ->
            updatePriorityIcons(checkedId)
        }

        super.onStart()

        val dialog = dialog as AlertDialog

        val btnCancel = dialog.findViewById<Button>(R.id.btn_change_priority_cancel)
        val btnOk = dialog.findViewById<Button>(R.id.btn_change_priority_ok)

        btnCancel?.setOnClickListener {
            dismiss()
        }

        btnOk?.setOnClickListener {
            val priority: NotePriority = when (
                group.checkedRadioButtonId
            ) {
                R.id.rb_change_priority_high -> NotePriority.HIGH
                R.id.rb_change_priority_normal -> NotePriority.NORMAL
                R.id.rb_change_priority_low -> NotePriority.LOW
                else -> NotePriority.NORMAL
            }
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                Bundle().apply {
                    putString(RESULT_KEY, priority.name)
                }
            )
            dismiss()
        }
    }
}
