package com.example.worknotebook

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.getValue


class MeetingsFragment : Fragment() {

    private val sharedViewModel: MainSharedViewModel by activityViewModels()

    private lateinit var session: UserSessionManager
    private var hasConnection: Boolean = false
    private var filters: MeetingFilters = MeetingFilters(
        byMeetingAtDesc = true,
        byUpdatedAtDesc = true,
        byActiveDesc = true,
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_meetings, container, false)
    }
    companion object {
        @JvmStatic
        fun newInstance() = MeetingsFragment()
    }
    override fun onResume() {
        super.onResume()
        loadMeetings(filters = filters)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        observeViewModel()
    }
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    sharedViewModel.hasConnection,
                    sharedViewModel.meetingFilters
                ) { isConnected, filters ->
                    Pair(isConnected, filters)
                }.collect { (isConnected, filters) ->
                    hasConnection = isConnected
                    this@MeetingsFragment.filters = filters
                    loadMeetings(filters)
                }
            }
        }
    }
    private fun initView(view: View) {
        session = UserSessionManager.getInstance(requireContext())
    }
    private fun loadMeetings(filters: MeetingFilters) {
        lifecycleScope.launch {
            val meetings = session.getMeetings(filters = filters)
            Log.d("DEBUG", "-----===== meetings filters $filters =====-----")  // TODO
            Log.d("DEBUG", "-----===== meetings $meetings =====-----")  // TODO
//            adapter.submitList(meetings)  // TODO
        }
    }

}