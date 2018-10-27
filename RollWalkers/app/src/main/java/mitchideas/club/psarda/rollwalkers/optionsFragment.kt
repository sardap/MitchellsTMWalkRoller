package mitchideas.club.psarda.rollwalkers

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class OptionsFragment : Fragment() {

	companion object {
		fun newInstance(activity: Activity, main: Main): OptionsFragment {
			val result = OptionsFragment()
			result.mOptions = poplauteEntryArray(result, activity)
			return result
		}

		private fun poplauteEntryArray(optionsFragment: OptionsFragment, activity: Activity): Array<Entry> {
			val entArray = ArrayList<Entry>(3)
			val data = Data.instance()

			entArray.add(EntrySwitch(activity.getString(R.string.options_disable_animation), data.settings.rollAnimation, optionsFragment::switchRollAnimation))
			entArray.add(EntrySwitch(activity.getString(R.string.options_notifacation_every_roll), data.settings.notifcationEveryRoll, optionsFragment::switchRollNotifaction))
			entArray.add(EntrySwitch(activity.getString(R.string.options_enable_vibration), data.settings.vibrateOnRoll, optionsFragment::switchVibrationMode))
			entArray.add(EntryButton(activity.getString(R.string.options_clear_entries), R.drawable.bin, optionsFragment::clearDatabase))

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

	private class ViewHolder(view: View) {
		val recyclerView = view.findViewById<RecyclerView>(R.id.options_recycler_view)
	}

	private lateinit var mViewHolder: ViewHolder
	private lateinit var mOptions: Array<Entry>
	private val mData = Data.instance()

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
			adapter = ListAdapter(mOptions)

		}


		return view
	}

	private fun switchRollAnimation(): Boolean {
		mData.settings.rollAnimation = !mData.settings.rollAnimation
		return true
	}

	private fun switchRollNotifaction(): Boolean {
		mData.settings.notifcationEveryRoll = !mData.settings.notifcationEveryRoll
		return true
	}

	private fun switchVibrationMode(): Boolean {
		mData.settings.vibrateOnRoll = !mData.settings.vibrateOnRoll
		return true
	}

	private fun clearDatabase() : Boolean{
		(activity as Main).clearDatabase()
		return true
	}
}

