package com.example.worknotebook

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton

class NotesFragment : Fragment() {

    private lateinit var addNoteBTN: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view = view)
    }

    private fun initView(view: View) {
        addNoteBTN = view.findViewById(R.id.btn_create_note)
        addNoteBTN.setOnClickListener {
            // Переходим на страницу создания
            NoteItemActivity.startForCreate(requireContext())
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = NotesFragment()
    }
}