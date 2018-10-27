package mitchideas.club.psarda.rollwalkers

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView

class EntryButton(name: String, val srcID: Int, val action: () -> Boolean) : Entry(name){
    override fun Type() : Int{
        return OptionsFragment.EntryTypes.CLEAR.value
    }
}

class EntrySwitch(name: String, val startingValue: Boolean, val action: () -> Boolean) : Entry(name) {
    override fun Type() : Int {
        return OptionsFragment.EntryTypes.SWTICH.value
    }
}

open class Entry(val name: String) {
    open fun Type() : Int
    {
        return OptionsFragment.EntryTypes.BASIC.value
    }
}

class ListAdapter(private val myDataset: Array<Entry>) : RecyclerView.Adapter<ListAdapter.EntryHolder>() {

    private class ButtonHolder(view: View): EntryHolder(view){
        val Image = view.findViewById<ImageView>(R.id.image)
    }

    private class EntrySwitchHolder(view: View): EntryHolder(view)
    {
        val switch: Switch = view.findViewById(R.id.entry_switch)
    }

    open class EntryHolder(view: View): RecyclerView.ViewHolder(view) {
        val title = view.findViewById<TextView>(R.id.title)
    }

    override fun getItemViewType(position: Int): Int {
        //Implement your logic here
        return myDataset[position].Type()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListAdapter.EntryHolder {
        when(viewType)
        {
            OptionsFragment.EntryTypes.BASIC.value -> {
                throw NotImplementedError()
            }
            OptionsFragment.EntryTypes.SWTICH.value -> {
                return EntrySwitchHolder(LayoutInflater.from(parent.context).inflate(R.layout.option_entry_switch, parent, false))
            }
            OptionsFragment.EntryTypes.CLEAR.value -> {
                return ButtonHolder(LayoutInflater.from(parent.context).inflate(R.layout.options_clear_entry, parent, false))
            }
            else ->{
                throw NotImplementedError()
            }
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: EntryHolder, position: Int) {
        val selectedEntry = myDataset[position]
        holder.title.text = selectedEntry.name

        if(holder is EntrySwitchHolder) {
            holder.switch.setOnClickListener { (selectedEntry as EntrySwitch).action() }
            holder.switch.isChecked = (selectedEntry as EntrySwitch).startingValue
        }

        if(holder is ButtonHolder){
            holder.Image.setOnClickListener {
                (selectedEntry as EntryButton).action()
            }

            holder.Image.setImageResource((selectedEntry as EntryButton).srcID)
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}
