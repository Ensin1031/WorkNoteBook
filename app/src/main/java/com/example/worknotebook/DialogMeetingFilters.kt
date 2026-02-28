package com.example.worknotebook

import android.app.Dialog
import android.content.res.ColorStateList
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.switchmaterial.SwitchMaterial
import java.time.Instant
import java.time.ZoneId
import kotlin.getValue


class DialogMeetingFilters() : DialogFragment() {

    private val sharedViewModel: MainSharedViewModel by activityViewModels()
    private lateinit var initialFilters: MeetingFilters

    private var footerBTNContainer: LinearLayout? = null
    private var btnCancel: Button? = null
    private var btnOk: Button? = null

    private var scrollContainer: ScrollView? = null
    private var ettGoToDate: EditText? = null
    private var datePickerGoToDate: DatePicker? = null
    private var viewOnlyActive: SwitchMaterial? = null
    private var viewOnlyNotActive: SwitchMaterial? = null
    private var viewOnlySyncByBack: SwitchMaterial? = null
    private var viewOnlyNotSyncByBack: SwitchMaterial? = null

    companion object {
        private const val ARG_FILTERS = "arg_meeting_filters"
        fun newInstance(filters: MeetingFilters): DialogMeetingFilters {
            return DialogMeetingFilters().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_FILTERS, filters)
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialFilters = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_FILTERS, MeetingFilters::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_FILTERS)
        } ?: throw IllegalArgumentException("MeetingFilters must be provided")
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(
            R.layout.dialog_meeting_filters,
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

        initViews(dialog = dialog)

        val resultFilters: MeetingFilters = initialFilters.copy()

        ettGoToDate?.apply {
            setText(if (resultFilters.goToDate != null) { formatMeetingDate(resultFilters.goToDate!!) } else { "" })
            if (resultFilters.goToDate != null) {
                ettGoToDate!!.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_close_red, 0)
                ettGoToDate!!.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(ettGoToDate!!.context, R.color.redColor))
            }
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val editText = view as EditText
                    val drawableEnd = editText.compoundDrawablesRelative[2] // индекс 2 — правая иконка
                    if (drawableEnd != null) {
                        // Определяем, попал ли клик в область иконки (правый край)
                        val isClickOnEndIcon = event.x >= editText.width - editText.totalPaddingRight
                        if (isClickOnEndIcon) {
                            editText.text = null
                            resultFilters.goToDate = null
                            ettGoToDate!!.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                            ettGoToDate!!.compoundDrawableTintList = null
                            return@setOnTouchListener true
                        }
                    }
                }
                false
            }
            setOnClickListener {
                if (datePickerGoToDate != null) {
                    ettGoToDate!!.visibility = View.GONE
                    datePickerGoToDate!!.visibility = View.VISIBLE
                    footerBTNContainer?.visibility = View.GONE
                    scrollContainer?.visibility = View.GONE
                    val calendar = Calendar.getInstance()
                    val (year, month, day) = if (resultFilters.goToDate != null) {
                        val localDate = Instant.ofEpochMilli(resultFilters.goToDate!!).atZone(ZoneId.systemDefault()).toLocalDate()
                        Triple(localDate.year, localDate.monthValue - 1, localDate.dayOfMonth)
                    } else {
                        Triple(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                    }
                    datePickerGoToDate!!.init(year, month, day) { _, y, m, d ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.set(y, m, d, 0, 0, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val ts = cal.timeInMillis
                        resultFilters.goToDate = ts
                        ettGoToDate!!.setText(formatMeetingDate(ts))
                        ettGoToDate!!.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_close_red, 0)
                        ettGoToDate!!.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(ettGoToDate!!.context, R.color.redColor))
                        ettGoToDate!!.visibility = View.VISIBLE
                        datePickerGoToDate!!.visibility = View.GONE
                        footerBTNContainer?.visibility = View.VISIBLE
                        scrollContainer?.visibility = View.VISIBLE
                    }
                }
            }
        }

        viewOnlyActive?.setOnCheckedChangeListener(null)
        viewOnlyActive?.apply {
            isChecked = initialFilters.viewOnlyActive
            setOnCheckedChangeListener { _, isChecked ->
                resultFilters.viewOnlyActive = isChecked
            }
        }
        viewOnlyNotActive?.setOnCheckedChangeListener(null)
        viewOnlyNotActive?.apply {
            isChecked = initialFilters.viewOnlyNotActive
            setOnCheckedChangeListener { _, isChecked ->
                resultFilters.viewOnlyNotActive = isChecked
            }
        }
        viewOnlySyncByBack?.setOnCheckedChangeListener(null)
        viewOnlySyncByBack?.apply {
            isChecked = initialFilters.viewOnlySyncByBack
            setOnCheckedChangeListener { _, isChecked ->
                resultFilters.viewOnlySyncByBack = isChecked
            }
        }
        viewOnlyNotSyncByBack?.setOnCheckedChangeListener(null)
        viewOnlyNotSyncByBack?.apply {
            isChecked = initialFilters.viewOnlyNotSyncByBack
            setOnCheckedChangeListener { _, isChecked ->
                resultFilters.viewOnlyNotSyncByBack = isChecked
            }
        }

        btnCancel?.setOnClickListener {
            dismiss()
        }
        btnOk?.setOnClickListener {
            sharedViewModel.setMeetingFilters(resultFilters)
            dismiss()
        }
    }
    private fun initViews(dialog: AlertDialog) {
        footerBTNContainer = dialog.findViewById(R.id.ll_meetings_filters_btn_container)
        btnCancel = dialog.findViewById(R.id.btn_meetings_filters_cancel)
        btnOk = dialog.findViewById(R.id.btn_meetings_filters_apply)
        scrollContainer = dialog.findViewById(R.id.sc_meetings_filter_scroll_container)

        ettGoToDate = dialog.findViewById(R.id.ett_meetings_filter_go_to_date)
        datePickerGoToDate = dialog.findViewById(R.id.dp_meetings_filter_go_to_date)

        viewOnlyActive = dialog.findViewById(R.id.sw_meetings_filter_view_active)
        viewOnlyNotActive = dialog.findViewById(R.id.sw_meetings_filter_view_not_active)
        viewOnlySyncByBack = dialog.findViewById(R.id.sw_meetings_filter_view_sync_by_back)
        viewOnlyNotSyncByBack = dialog.findViewById(R.id.sw_meetings_filter_view_not_sync_by_back)
    }
}
