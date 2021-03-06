package mitchideas.club.psarda.rollwalkers

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class LastRollFragment : Fragment() {

    companion object {
        fun newInstance(rollData: ArrayList<RollData>, activity: Activity): LastRollFragment {
            val result = LastRollFragment()
            result.mDataSet = result.convertRollDataToEntyies(rollData, activity)
            return result
        }
    }

    private class Entry(rollData: RollData, activity: Activity)
    {
        val distance = activity.getString(R.string.result_distance_travled_title, distanceToString(rollData.distance))
        val numberOfRolls = activity.getString(R.string.result_num_of_rolls, rollData.rolls)
        val target = activity.getString(R.string.result_target_title, rollData.target)
        val combo = activity.getString(R.string.result_combo, rollData.bestCombo)
        val shakes = activity.getString(R.string.result_shakes, (rollData.shakes / 2).toString())

        fun distanceToString(pDistance: Double) : String
        {
            val extension: String
            val value: Double

            if(pDistance > 1000)
            {
                extension = "KM"
                value = pDistance / 1000
            }
            else
            {
                extension = "M"
                value = pDistance
            }

            return String.format("%.2f %s", value, extension)
        }
    }

    private class MyAdapter(private val myDataset: Array<Entry>) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>()
    {
        class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
        {
            val targetTitle = view.findViewById<TextView>(R.id.target_title)
            val numRolls = view.findViewById<TextView>(R.id.num_of_rolls)
            val distanceTraveled = view.findViewById<TextView>(R.id.distance_traveled)
            val combo = view.findViewById<TextView>(R.id.combo)
            val shakes = view.findViewById<TextView>(R.id.shakes)
        }


        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter.MyViewHolder {
            // create a new view
            val view = LayoutInflater.from(parent.context).inflate(R.layout.roll_results_entry, parent, false)

            return MyViewHolder(view)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val selectedEntry = myDataset[position]
            holder.targetTitle.text = selectedEntry.target
            holder.distanceTraveled.text = selectedEntry.distance
            holder.numRolls.text = selectedEntry.numberOfRolls
            holder.combo.text = selectedEntry.combo
            holder.shakes.text = selectedEntry.shakes
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = myDataset.size
    }


    private class ViewHolder(view: View)
    {
        val recyclerView = view.findViewById<RecyclerView>(R.id.last_results)
    }

    private lateinit var mViewHolder: ViewHolder
    private lateinit var mDataSet: Array<Entry>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.last_rolls_fragment, container, false)

        mViewHolder = ViewHolder(view)


        mViewHolder.recyclerView.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = LinearLayoutManager(context)

            // specify an viewAdapter (see also next example)
            adapter = MyAdapter(mDataSet)

        }


        return view
    }

    private fun convertRollDataToEntyies(rollDataArray: ArrayList<RollData>, activity: Activity) : Array<Entry>
    {
        val result = Array<Entry?>(rollDataArray.size){null}

        for (i in 0 until rollDataArray.size)
        {
            result[i] = Entry(rollDataArray[i], activity)
        }

        return result as Array<Entry>
    }

}