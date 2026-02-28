package com.example.worknotebook

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.getValue


class DialogMeetingFilters() : DialogFragment() {

    private val sharedViewModel: MainSharedViewModel by activityViewModels()
    private lateinit var initialFilters: MeetingFilters

    private var btnCancel: Button? = null
    private var btnOk: Button? = null

    private var search: EditText? = null
    private var byMeetingAtDesc: SwitchMaterial? = null
    private var byMeetingAtAsc: SwitchMaterial? = null
    private var byUpdatedAtDesc: SwitchMaterial? = null
    private var byUpdatedAtAsc: SwitchMaterial? = null
    private var byActiveDesc: SwitchMaterial? = null
    private var byActiveAsc: SwitchMaterial? = null
    private var viewOnlyActive: SwitchMaterial? = null
    private var viewOnlyNotActive: SwitchMaterial? = null
    private var bySyncByBackDesc: SwitchMaterial? = null
    private var bySyncByBackAsc: SwitchMaterial? = null
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

        search?.apply { setText(initialFilters.search) }
        byMeetingAtDesc?.setOnCheckedChangeListener(null)
        byMeetingAtDesc?.apply {
            isChecked = initialFilters.byMeetingAtDesc
            setOnCheckedChangeListener { _, isChecked ->
                resultFilters.byMeetingAtDesc = isChecked
                if (isChecked) {
                    byMeetingAtAsc?.isChecked = false
                }
            }
        }
        byMeetingAtAsc?.setOnCheckedChangeListener(null)
        byMeetingAtAsc?.apply {
            isChecked = initialFilters.byMeetingAtAsc
            setOnCheckedChangeListener { _, isChecked ->
                resultFilters.byMeetingAtAsc = isChecked
                if (isChecked) {
                    byMeetingAtDesc?.isChecked = false
                }
            }
        }
        byUpdatedAtDesc?.setOnCheckedChangeListener(null)
        byUpdatedAtDesc?.apply {
            isChecked = initialFilters.byUpdatedAtDesc
            setOnCheckedChangeListener { _, isChecked ->
                resultFilters.byUpdatedAtDesc = isChecked
                if (isChecked) {
                    byUpdatedAtAsc?.isChecked = false
                }
            }
        }
        byUpdatedAtAsc?.setOnCheckedChangeListener(null)
        byUpdatedAtAsc?.apply {
            isChecked = initialFilters.byUpdatedAtAsc
            setOnCheckedChangeListener { _, isChecked ->
                resultFilters.byUpdatedAtAsc = isChecked
                if (isChecked) {
                    byUpdatedAtDesc?.isChecked = false
                }
            }
        }
        byActiveDesc?.setOnCheckedChangeListener(null)
        byActiveDesc?.apply {
            isChecked = initialFilters.byActiveDesc
            setOnCheckedChangeListener { _, isChecked ->
                resultFilters.byActiveDesc = isChecked
                if (isChecked) {
                    byActiveAsc?.isChecked = false
                }
            }
        }
        byActiveAsc?.setOnCheckedChangeListener(null)
        byActiveAsc?.apply {
            isChecked = initialFilters.byActiveAsc
            setOnCheckedChangeListener { _, isChecked ->
                resultFilters.byActiveAsc = isChecked
                if (isChecked) {
                    byActiveDesc?.isChecked = false
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
        bySyncByBackDesc?.setOnCheckedChangeListener(null)
        bySyncByBackDesc?.apply {
            isChecked = initialFilters.bySyncByBackDesc
            setOnCheckedChangeListener { _, isChecked ->
                resultFilters.bySyncByBackDesc = isChecked
                if (isChecked) {
                    bySyncByBackAsc?.isChecked = false
                }
            }
        }
        bySyncByBackAsc?.setOnCheckedChangeListener(null)
        bySyncByBackAsc?.apply {
            isChecked = initialFilters.bySyncByBackAsc
            setOnCheckedChangeListener { _, isChecked ->
                resultFilters.bySyncByBackAsc = isChecked
                if (isChecked) {
                    bySyncByBackDesc?.isChecked = false
                }
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
            resultFilters.search = search?.text.toString().trim()
            sharedViewModel.setMeetingFilters(resultFilters)
            dismiss()
        }
    }
    private fun initViews(dialog: AlertDialog) {
        btnCancel = dialog.findViewById(R.id.btn_meetings_filters_cancel)
        btnOk = dialog.findViewById(R.id.btn_meetings_filters_apply)

        search = dialog.findViewById(R.id.ett_meetings_filter_search)
        byMeetingAtDesc = dialog.findViewById(R.id.sw_meetings_filter_by_meeting_at_desc)
        byMeetingAtAsc = dialog.findViewById(R.id.sw_meetings_filter_by_meeting_at_asc)
        byUpdatedAtDesc = dialog.findViewById(R.id.sw_meetings_filter_by_updated_at_desc)
        byUpdatedAtAsc = dialog.findViewById(R.id.sw_meetings_filter_by_updated_at_asc)
        byActiveDesc = dialog.findViewById(R.id.sw_meetings_filter_by_active_desc)
        byActiveAsc = dialog.findViewById(R.id.sw_meetings_filter_by_active_asc)
        viewOnlyActive = dialog.findViewById(R.id.sw_meetings_filter_view_active)
        viewOnlyNotActive = dialog.findViewById(R.id.sw_meetings_filter_view_not_active)
        bySyncByBackDesc = dialog.findViewById(R.id.sw_meetings_filter_by_sync_desc)
        bySyncByBackAsc = dialog.findViewById(R.id.sw_meetings_filter_by_sync_asc)
        viewOnlySyncByBack = dialog.findViewById(R.id.sw_meetings_filter_view_sync_by_back)
        viewOnlyNotSyncByBack = dialog.findViewById(R.id.sw_meetings_filter_view_not_sync_by_back)
    }
}
