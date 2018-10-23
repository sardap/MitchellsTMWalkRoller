package com.mitchellsideas.pfsar.rollwalker

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import java.lang.Exception

class optionsFragment : Fragment() {

	companion object {
		fun newInstance(activity: Activity, main: Main): optionsFragment {
			val result = optionsFragment()
			result.mMain = main
			result.mOptions = poplauteEntryArray(result, activity)
			return result
		}

		private fun poplauteEntryArray(optionsFragment: optionsFragment, activity: Activity): Array<Entry> {
			val result = Array<Entry?>(1){null}

			result[0] = EntrySwitch(activity.getString(R.string.options_disable_animation), (activity as Main).settings.rollAnimation, optionsFragment::switchRollAnimation)

			return result as Array<Entry>
		}
	}

	enum class EntryTypes(val value: Int)
	{
		BASIC(0), SWTICH(1)
	}

	private class EntrySwitch(name: String, val startingValue: Boolean, val action: () -> Boolean) : Entry(name)
	{
		override fun Type() : Int
		{
			return EntryTypes.SWTICH.value
		}
	}

	private open class Entry(val name: String)
	{
		open fun Type() : Int
		{
			return EntryTypes.BASIC.value
		}
	}

	private class MyAdapter(private val myDataset: Array<Entry>) : RecyclerView.Adapter<MyAdapter.EntryHolder>() {

		class EntrySwitchHolder(view: View) : EntryHolder(view)
		{
			val switch: Switch = view.findViewById(R.id.entry_switch)
		}

		open class EntryHolder(view: View) : RecyclerView.ViewHolder(view) {
			val title = view.findViewById<TextView>(R.id.title)
		}

		override fun getItemViewType(position: Int): Int {
			//Implement your logic here
			return myDataset[position].Type()
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter.EntryHolder {
			when(viewType)
			{
				EntryTypes.BASIC.value -> {
					throw NotImplementedError()
				}
				EntryTypes.SWTICH.value -> {
					return EntrySwitchHolder(LayoutInflater.from(parent.context).inflate(R.layout.option_entry_switch, parent, false))
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

			if(holder is EntrySwitchHolder)
			{
				holder.switch.setOnClickListener { (selectedEntry as EntrySwitch).action() }
				holder.switch.isChecked = (selectedEntry as EntrySwitch).startingValue
			}

		}

		// Return the size of your dataset (invoked by the layout manager)
		override fun getItemCount() = myDataset.size
	}


	private class ViewHolder(view: View) {
		val recyclerView = view.findViewById<RecyclerView>(R.id.options_recycler_view)
	}

	private lateinit var mViewHolder: ViewHolder
	private lateinit var mOptions: Array<Entry>
	private lateinit var mMain: Main

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.options_fragment, container, false)

		mViewHolder = ViewHolder(view)

		mViewHolder.recyclerView.apply {
			// use this setting to improve performance if you know that changes
			// in content do not change the layout size of the RecyclerView
			setHasFixedSize(true)

			// use a linear layout manager
			layoutManager = LinearLayoutManager(context)

			// specify an viewAdapter (see also next example)
			adapter = MyAdapter(mOptions)

		}


		return view
	}

	private fun switchRollAnimation(): Boolean {
		mMain.settings.rollAnimation = !mMain.settings.rollAnimation

		return true
	}
}

