package be.planty.assistant.app.actions.adapter

import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import be.planty.assistant.app.R

import java.util.ArrayList

/**
 * @author will on 5/30/2016.
 */

class ActionFragmentAdapter(items: List<ActionFragmentItem>) : RecyclerView.Adapter<ActionFragmentAdapter.ActionFragmentViewHolder>() {

    internal var items: List<ActionFragmentItem> = ArrayList()

    init {
        this.items = items
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionFragmentViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_action_fragment, parent, false)
        return ActionFragmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActionFragmentViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.icon.setImageResource(item.iconResource)
        holder.itemView.setOnClickListener(item.clickListener)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ActionFragmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var title: TextView
        internal var icon: ImageView

        init {
            title = itemView.findViewById(R.id.title) as TextView
            icon = itemView.findViewById(R.id.icon) as ImageView
        }
    }

    class ActionFragmentItem(title: String, @DrawableRes iconResource: Int, clickListener: View.OnClickListener) {
        var title: String
            internal set
        var iconResource: Int = 0
            internal set
        var clickListener: View.OnClickListener
            internal set

        init {
            this.title = title
            this.iconResource = iconResource
            this.clickListener = clickListener
        }
    }
}
