package com.mitchellsideas.pfsar.rollwalker

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView

class optionsFragment : Fragment() {

	companion object {
		fun newInstance(activity: Activity, main: Main): optionsFragment {
			val result = optionsFragment()
			result.mMain = main
			result.mOptions = poplauteEntryArray(result, activity)
			return result
		}

		private fun poplauteEntryArray(optionsFragment: optionsFragment, activity: Activity): Array<Entry> {
			val entArray = ArrayList<Entry>(3)
			val main = (activity as Main)

			entArray.add(EntrySwitch(activity.getString(R.string.options_disable_animation), main.settings.rollAnimation, optionsFragment::switchRollAnimation))
			entArray.add(EntrySwitch(activity.getString(R.string.options_notifacation_every_roll), main.settings.notifcationEveryRoll, optionsFragment::switchRollNotifaction))
			entArray.add(EntrySwitch(activity.getString(R.string.options_enable_vibration), main.settings.vibrateOnRoll, optionsFragment::switchVibrationMode))
			entArray.add(ClearEntry(activity.getString(R.string.options_clear_entries), optionsFragment::clearDatabase))

			val result = Array<Entry?>(entArray.size){null}

			for(i in 0 until entArray.size)
			{
				result[i] = entArray[i]
			}

			return result as Array<Entry>
		}
	}

	enum class EntryTypes(val value: Int)
	{
		BASIC(0), SWTICH(1), CLEAR(2)
	}

	private class ClearEntry(name: String, val action: () -> Boolean) : Entry(name){
		override fun Type() : Int{
			return EntryTypes.CLEAR.value
		}
	}

	private class EntrySwitch(name: String, val startingValue: Boolean, val action: () -> Boolean) : Entry(name) {
		override fun Type() : Int {
			return EntryTypes.SWTICH.value
		}
	}

	private open class Entry(val name: String) {
		open fun Type() : Int
		{
			return EntryTypes.BASIC.value
		}
	}

	private class MyAdapter(private val myDataset: Array<Entry>) : RecyclerView.Adapter<MyAdapter.EntryHolder>() {

		private class ClearHolder(view: View): EntryHolder(view){
			val Image = view.findViewById<ImageView>(R.id.image)
		}

		private class EntrySwitchHolder(view: View): EntryHolder(view)
		{
			val switch: Switch = view.findViewById(R.id.entry_switch)
		}

		private open class EntryHolder(view: View): RecyclerView.ViewHolder(view) {
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
				EntryTypes.CLEAR.value -> {
					return ClearHolder(LayoutInflater.from(parent.context).inflate(R.layout.options_clear_entry, parent, false))
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

			if(holder is ClearHolder){
				holder.Image.setOnClickListener {
					(selectedEntry as ClearEntry).action()
				}
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

	private fun switchRollNotifaction(): Boolean {
		mMain.settings.notifcationEveryRoll = !mMain.settings.notifcationEveryRoll
		return true
	}

	private fun switchVibrationMode(): Boolean {
		mMain.settings.vibrateOnRoll = !mMain.settings.vibrateOnRoll
		return true
	}

	private fun clearDatabase() : Boolean{
		mMain.clearDatabase()
		return true
	}
}

