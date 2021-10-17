package ru.dkrasnov.pet.recycler

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.dkrasnov.pet.R



const val LOG_TAG = "RecyclerTag"
const val LOG_TAG_CHILD = "RecyclerTagChild"
val sharedRecyclerViewPool = NoLimitRecycledViewPool().apply {
    setMaxRecycledViews(1, 100)
}

@SuppressLint("NotifyDataSetChanged")
class RecyclerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.a_recycler)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler)

        val adapter = MyAdapter()
        recyclerView.adapter = adapter

        adapter.items.addAll(List(100) { it })
        adapter.notifyDataSetChanged()
    }

    companion object {

        fun createIntent(context: Context): Intent =
            Intent(context, RecyclerActivity::class.java)
    }
}

class MyAdapter : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    var items = mutableListOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        Log.d(LOG_TAG, "onCreateViewHolder")

        val view = LayoutInflater.from(parent.context).inflate(R.layout.v_recycler, parent, false)

        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Log.d(LOG_TAG, "onBindViewHolder position: $position")

        holder.bind(items[position])
    }


    override fun getItemCount(): Int =
        items.size

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val recyclerView = view.findViewById<RecyclerView>(R.id.recycler)

        private val adapter = ChildAdapter {
            this.adapterPosition
        }

        private val url = "https://cdn.pixabay.com/photo/2015/03/26/09/47/sky-690293__340.jpg"
        private val urls = List(100) { url }

        init {
            recyclerView.adapter = adapter
            recyclerView.setRecycledViewPool(sharedRecyclerViewPool)
            (recyclerView.layoutManager as LinearLayoutManager).initialPrefetchItemCount = 4
            recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {

                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    super.getItemOffsets(outRect, view, parent, state)

                    outRect.bottom = 56
                    outRect.top = 56
                    outRect.right = 56
                    outRect.left = 56
                }
            })
        }

        fun bind(item: Int) {
            adapter.urls.apply {
                clear()
                addAll(urls)
            }
            adapter.notifyDataSetChanged()
        }
    }
}

class ChildAdapter(
    private val adapterPosition: () -> Int
) : RecyclerView.Adapter<ChildAdapter.ChildViewHolder>() {

    val urls = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        Log.d(LOG_TAG_CHILD, "onCreateViewHolder adapterPosition: ${adapterPosition()}")

        val view = LayoutInflater.from(parent.context).inflate(R.layout.v_child, parent, false)

        return ChildViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        Log.d(LOG_TAG_CHILD, "onBindViewHolder position: $position")

        holder.bind(adapterPosition())
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        Log.d(LOG_TAG_CHILD, "onAttachedToRecyclerView adapterPosition: ${adapterPosition()}")
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        Log.d(LOG_TAG_CHILD, "onDetachedFromRecyclerView adapterPosition: ${adapterPosition()}")
    }

    override fun onViewAttachedToWindow(holder: ChildViewHolder) {
        super.onViewAttachedToWindow(holder)

        Log.d(LOG_TAG_CHILD, "onViewAttachedToWindow adapterPosition: ${adapterPosition()} holderPosition: ${holder.adapterPosition}")
    }

    override fun onViewRecycled(holder: ChildViewHolder) {
        super.onViewRecycled(holder)

        Log.d(LOG_TAG_CHILD, "onViewRecycled adapterPosition: ${adapterPosition()} holderPosition: ${holder.adapterPosition}")
    }

    override fun onViewDetachedFromWindow(holder: ChildViewHolder) {
        super.onViewDetachedFromWindow(holder)

        Log.d(LOG_TAG_CHILD, "onViewDetachedFromWindow adapterPosition: ${adapterPosition()} holderPosition: ${holder.adapterPosition}")
    }

    override fun getItemCount(): Int =
        urls.size

    class ChildViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val textView = view as TextView

        fun bind(parentPosition: Int) {
            val text = "adapter position: $parentPosition\n" +
                    "position: $adapterPosition"

            textView.text = text
        }
    }
}

class NoLimitRecycledViewPool : RecyclerView.RecycledViewPool() {

    private val scrapCount = SparseIntArray()
    private val maxScrap = SparseIntArray()

    override fun setMaxRecycledViews(viewType: Int, max: Int) {
        maxScrap.put(viewType, max)
        super.setMaxRecycledViews(viewType, max)
    }

    override fun getRecycledView(viewType: Int): RecyclerView.ViewHolder? {
        return super.getRecycledView(viewType)?.also {
            val count = scrapCount[viewType, -1]
            check(count > 0) { "Not expected here. The #put call must be before" }
            scrapCount.put(viewType, count - 1)
        }
    }

    override fun putRecycledView(scrap: RecyclerView.ViewHolder) {
        val viewType = scrap.itemViewType
        val count = scrapCount[viewType, 0]

        scrapCount.put(viewType, count + 1)

        var max = maxScrap[viewType, -1]

        if (max == -1) {
            max = DEFAULT_MAX_SIZE
            setMaxRecycledViews(viewType, max)
        }
        if (count + 1 > max) {
            setMaxRecycledViews(viewType, count + 1)
        }
        super.putRecycledView(scrap)
    }

    override fun clear() {
        scrapCount.clear()
        maxScrap.clear()
        super.clear()
    }

    companion object {

        private const val DEFAULT_MAX_SIZE = 5
    }
}