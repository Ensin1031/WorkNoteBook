package com.example.worknotebook

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
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
    private val onItemClick: (Note) -> Unit
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
        return NoteViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    class NoteViewHolder(
        itemView: View,
        private val onItemClick: (Note) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_note)
        private val viewContainer: LinearLayout = itemView.findViewById(R.id.ll_note_item_card)
        private val tvUpdateAt: TextView = itemView.findViewById(R.id.tv_note_update_at)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_note_title)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_note_content)
        private var currentNote: Note? = null

        init {
            itemView.setOnClickListener {
                currentNote?.let { onItemClick(it) }
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
    private var filters: NodeFilters = NodeFilters(
        byPriorityDesc = true,
        byUpdatedAtDesc = true,
        byActiveDesc = true,
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotesAdapter

    private lateinit var addNoteBTN: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }
    companion object {
        @JvmStatic
        fun newInstance() = NotesFragment()
    }
    override fun onResume() {
        super.onResume()
        loadNotes(filters = filters)
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
                    loadNotes(filters)
                }
            }
        }
    }
    private fun initView(view: View) {
        session = UserSessionManager.getInstance(requireContext())

        recyclerView = view.findViewById(R.id.rv_page_note_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotesAdapter { note ->
            // Переход к просмотру заметки (режим просмотра)
            NoteItemActivity.startForEdit(
                context = requireContext(),
                note = note,
                viewMode = true
            )
        }

        recyclerView.adapter = adapter
        addNoteBTN = view.findViewById(R.id.btn_create_note)
        addNoteBTN.setOnClickListener {
            // Переходим на страницу создания заметки (режим создания)
            NoteItemActivity.startForCreate(requireContext())
        }
    }
    private fun loadNotes(filters: NodeFilters) {
        lifecycleScope.launch {
            val notes = session.getNotes(filters = filters)
            adapter.submitList(notes)
        }
    }

}