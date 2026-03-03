package com.example.worknotebook

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale


class NotesAdapter(
    private val onItemClick: (Note) -> Unit,
    private val onItemLongClick: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private var notes = listOf<Note>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note_card, parent, false)
        return NoteViewHolder(view, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    class NoteViewHolder(
        itemView: View,
        private val onItemClick: (Note) -> Unit,
        private val onItemLongClick: (Note) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_note)
        private val viewContainer: LinearLayout = itemView.findViewById(R.id.ll_note_item_card)
        private val ivSync: ImageView = itemView.findViewById(R.id.iv_is_sync_note)
        private val tvUpdateAt: TextView = itemView.findViewById(R.id.tv_note_update_at)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_note_title)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_note_content)
        private var currentNote: Note? = null

        init {
            itemView.setOnClickListener {
                currentNote?.let { onItemClick(it) }
            }
            itemView.setOnLongClickListener {
                currentNote?.let { note ->
                    onItemLongClick(note)
                    true
                } ?: false
            }
        }

        fun bind(note: Note) {
            currentNote = note

            // TODO пока оставляю если нужно будет устанавливать иной цвет рамки. потом удалить.
//            val colorRes = when (note.priority) {
//                NotePriority.HIGH -> R.color.redColor
//                NotePriority.NORMAL -> R.color.yellowColor
//                NotePriority.LOW -> R.color.greenColor
//            }

            // TODO пока оставляю как вариант с большей прозрачностью. потом удалить.
//            val backgroundColorRes = when (note.priority) {
//                NotePriority.HIGH -> R.color.redColor_05
//                NotePriority.NORMAL -> R.color.yellowColor_05
//                NotePriority.LOW -> R.color.greenColor_05
//            }

            val backgroundColorRes = when (note.priority) {
                NotePriority.HIGH -> R.color.redColor_10
                NotePriority.NORMAL -> R.color.yellowColor_10
                NotePriority.LOW -> R.color.greenColor_10
            }

            val backgroundColor = ContextCompat.getColor(
                itemView.context,
                if (!note.isActive) { R.color.textColor_10 } else { backgroundColorRes }
            )

            cardView.strokeColor = backgroundColor
            viewContainer.setBackgroundColor(backgroundColor)

            // Дата последнего обновления
            if (note.updatedAt != null) {
                val date = Date(note.updatedAt!!)
                val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
                tvUpdateAt.apply {
                    text = dateFormat.format(date)
                    visibility = View.VISIBLE
                }
            } else {
                tvUpdateAt.visibility = View.GONE
            }

            ivSync.visibility = if (note.isSync) { View.GONE } else { View.VISIBLE }

            // Заголовок
            if (note.title.isNotBlank()) {
                tvTitle.apply {
                    text = note.title
                    visibility = View.VISIBLE
                }
            } else {
                tvTitle.visibility = View.GONE
            }

            // Контент
            if (note.content.isNotBlank()) {
                tvContent.apply {
                    text = note.content
                    visibility = View.VISIBLE
                }
            } else {
                tvContent.visibility = View.GONE
            }
        }
    }
}


class NotesFragment : Fragment() {

    private val sharedViewModel: MainSharedViewModel by activityViewModels()

    private lateinit var session: UserSessionManager
    private var hasConnection: Boolean = false
    private lateinit var progressBar: ProgressBar
    private var filters: NodeFilters = NodeFilters(
        byPriorityDesc = true,
        byUpdatedAtDesc = true,
        byActiveDesc = true,
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var textEmptyView: TextView
    private lateinit var adapter: NotesAdapter

    private lateinit var addNoteBTN: ImageButton

    private var parentNoteId: Long? = null
    private var meetingId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var localParentNoteId = 0L
        var localParentMeetingId = 0L
        arguments?.let {
            localParentNoteId = it.getLong(ARG_PARENT_NOTE_ID, 0L)
            localParentMeetingId = it.getLong(ARG_PARENT_MEETING_ID, 0L)
        }
        parentNoteId = if (localParentNoteId == 0L) { null } else { localParentNoteId }
        meetingId = if (localParentMeetingId == 0L) { null } else { localParentMeetingId }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }
    companion object {
        private const val ARG_PARENT_NOTE_ID = "parent_note_id"
        private const val ARG_PARENT_MEETING_ID = "parent_meeting_id"
        @JvmStatic
        fun newInstance(
            parentNoteId: Long? = null,
            parentMeetingId: Long? = null
        ) = NotesFragment().apply {
            arguments = Bundle().apply {
                if (parentNoteId != null) {
                    putLong(ARG_PARENT_NOTE_ID, parentNoteId)
                }
                if (parentMeetingId != null) {
                    putLong(ARG_PARENT_MEETING_ID, parentMeetingId)
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        loadNotes(filters = filters, loadByParentNoteId = parentNoteId, loadByParentMeetingId = meetingId)
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
                    sharedViewModel.noteFilters
                ) { isConnected, filters ->
                    Pair(isConnected, filters)
                }.collect { (isConnected, filters) ->
                    hasConnection = isConnected
                    this@NotesFragment.filters = filters
                    loadNotes(filters = filters, loadByParentNoteId = parentNoteId, loadByParentMeetingId = meetingId)
                }
            }
        }
    }
    private fun initView(view: View) {
        session = UserSessionManager.getInstance(requireContext())
        try {
            progressBar = requireActivity().findViewById(R.id.main_selection_page_header_progress_bar)
        } catch (e: Exception) {
            progressBar = requireActivity().findViewById(R.id.meeting_item_header_progress_bar)
        }

        textEmptyView = view.findViewById(R.id.tv_page_note_list_is_empty)
        recyclerView = view.findViewById(R.id.rv_page_note_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotesAdapter(
            onItemClick = { note ->
                // Переход к просмотру заметки
                NoteItemActivity.startForEdit(
                    context = requireContext(),
                    note = note,
                    viewMode = true,
                    parentNoteId = parentNoteId?.toInt(),
                    parentMeetingId = meetingId?.toInt()
                )
            },
            onItemLongClick = { note ->
                // Показать диалог подтверждения удаления либо восстановления
                if (note.isActive) {
                    showArchiveConfirmationDialog(note = note)
                } else {
                    showRecoverConfirmationDialog(note = note)
                }
            }
        )

        recyclerView.adapter = adapter
        addNoteBTN = view.findViewById(R.id.btn_create_note)
        addNoteBTN.apply {
            visibility = if (parentNoteId != null || meetingId != null) { View.GONE } else { View.VISIBLE }
            setOnClickListener {
                NoteItemActivity.startForCreate(
                    context = requireContext(),
                    parentNoteId = parentNoteId?.toInt(),
                    parentMeetingId = meetingId?.toInt()
                )
            }
        }
    }
    private fun getNoteName(note: Note): String {
        val raw = when {
            note.title.isNotBlank() -> note.title.trim()
            note.content.isNotBlank() -> note.content.trim()
            else -> ""
        }
        if (raw.isEmpty()) return "Без названия"
        return if (raw.length <= 20) raw else raw.take(20) + "..."
    }
    private fun showArchiveConfirmationDialog(note: Note) {
        AlertDialog.Builder(requireContext())
            .setTitle("Архивирование заметки")
            .setMessage("Вы уверены, что хотите архивировать заметку \"${getNoteName(note)}\"?")
            .setPositiveButton("Архивировать") { _, _ ->
                syncNote(note = note, isActive = false)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    private fun showRecoverConfirmationDialog(note: Note) {
        AlertDialog.Builder(requireContext())
            .setTitle("Восстановление заметки")
            .setMessage("Вы уверены, что хотите восстановить заметку \"${note.title}\"?")
            .setPositiveButton("Восстановить") { _, _ ->
                syncNote(note = note, isActive = true)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    private fun syncNote(note: Note, isActive: Boolean) {
        if (note.id == null) {
            Log.d("STRUCTURE_ERROR", "-----===== Ошибка. Попытка синхронизировать несуществующую заметку =====-----")
            error("Системная ошибка")
        }

        fun endLocalProcess(note: Note) {
            session.updateNote(note)
            endViewProcess()
            loadNotes(filters = filters, loadByParentNoteId = parentNoteId, loadByParentMeetingId = meetingId)
        }

        startViewProcess()
        if (session.getUserSettings()?.syncNotesImmediately ?: false) {
            val recoveredNote: Note = note.copy(
                isActive = isActive,
                isSync = true,
                updatedAt = System.currentTimeMillis()
            )
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.syncNote(recoveredNote)
                    if (response.isSuccessful) {
                        val noteData = response.body()
                        if (noteData != null) {
                            val syncNote = Note(
                                id = recoveredNote.id,
                                externalId = noteData.externalId,
                                userId = recoveredNote.userId,
                                externalUserId = noteData.externalUserId,
                                parentNoteId = recoveredNote.parentNoteId,
                                externalParentNoteId = noteData.externalParentNoteId,
                                meetingId = recoveredNote.meetingId,
                                externalMeetingId = noteData.externalMeetingId,
                                title = noteData.title,
                                content = noteData.content,
                                createdAt = noteData.createdAt,
                                updatedAt = noteData.updatedAt,
                                priority = noteData.priority,
                                isActive = noteData.isActive,
                                isSync = true,
                            )
                            endLocalProcess(note = syncNote)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                parseError(response),
                                Toast.LENGTH_SHORT
                            ).show()
                            endViewProcess()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            parseError(response),
                            Toast.LENGTH_SHORT
                        ).show()
                        endViewProcess()
                    }
                } catch (e: Exception) {
                    AlertDialog.Builder(requireContext())
                        .setMessage("${resources.getString(R.string.Error)}: $e")
                        .setNegativeButton(resources.getString(R.string.Cancel)) { dialog, _ ->
                            dialog.dismiss()
                            endViewProcess()
                        }
                        .show()
                }
            }
        } else {
            val recoveredNote: Note = note.copy(
                isActive = isActive,
                isSync = false,
                updatedAt = System.currentTimeMillis()
            )
            endLocalProcess(note = recoveredNote)
        }
    }
    private fun loadNotes(filters: NodeFilters, loadByParentNoteId: Long? = null, loadByParentMeetingId: Long? = null) {
        lifecycleScope.launch {
            val notes = session.getNotes(filters = filters, parentNoteId = loadByParentNoteId, meetingId = loadByParentMeetingId)
            if (notes.isNotEmpty()) {
                recyclerView.visibility = View.VISIBLE
                textEmptyView.visibility = View.GONE
                adapter.submitList(notes)
            } else {
                recyclerView.visibility = View.GONE
                textEmptyView.visibility = View.VISIBLE
            }
        }
    }
    private fun startViewProcess() {
        progressBar.visibility = View.VISIBLE
    }
    private fun endViewProcess() {
        progressBar.visibility = View.GONE
    }
}