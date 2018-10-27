package mitchideas.club.psarda.rollwalkers

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LeaderboardsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LeaderboardsFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class LeaderboardsFragment : Fragment() {

    companion object {
        fun newInstance(main: Main): LeaderboardsFragment{
            val result = LeaderboardsFragment()

            result.mLeaderBoards  = poplauteEntryArray(result, main)

            return result
        }

        private fun poplauteEntryArray(leaderboards: LeaderboardsFragment, activity: Activity): Array<Entry> {
            val entArray = ArrayList<Entry>(3)
            val data = Data.instance()

            entArray.add(EntryButton(activity.getString(R.string.option_leaderboard_target), R.drawable.ledearboard_temp, leaderboards::targetAction))
            entArray.add(EntryButton(activity.getString(R.string.option_leaderboard_combo), R.drawable.ledearboard_temp, leaderboards::comboAction))

            val result = Array<Entry?>(entArray.size){null}

            for(i in 0 until entArray.size)
            {
                result[i] = entArray[i]
            }

            return result as Array<Entry>
        }

    }

    private class ViewHolder(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
    }

    private lateinit var mViewHolder: ViewHolder
    private lateinit var mLeaderBoards: Array<Entry>
    private val mData = Data.instance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.leaderboards_fragment, container, false)

        mViewHolder = ViewHolder(view)

        mViewHolder.recyclerView.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = LinearLayoutManager(context)

            // specify an viewAdapter (see also next example)
            adapter = ListAdapter(mLeaderBoards)

        }

        // Inflate the layout for this fragment
        return view
    }

    private fun targetAction(): Boolean{
        showLeaderboard(R.string.leaderboard_highest_target)

        return true
    }

    private fun comboAction(): Boolean{
        showLeaderboard(R.string.leaderboard_combo)

        return true
    }


    private fun showLeaderboard(id: Int) {
        val main = activity as Main

        Games.getLeaderboardsClient(main, GoogleSignIn.getLastSignedInAccount(main)!!)
            .getLeaderboardIntent(getString(id))
            .addOnSuccessListener { intent -> startActivityForResult(intent, Main.RC_LEADERBOARD_UI); }
    }

}
