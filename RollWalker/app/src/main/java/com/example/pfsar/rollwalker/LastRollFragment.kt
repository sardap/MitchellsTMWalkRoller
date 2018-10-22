package com.example.pfsar.rollwalker

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ChildEventListener





class LastRollFragment : Fragment() {

    companion object {
        fun newInstance(): LastRollFragment {
            return LastRollFragment()
        }
    }

    private class ViewHolder(view: View)
    {
        val recyclerView = view.findViewById<RecyclerView>(R.id.last_results)
    }

    private lateinit var mViewHolder: ViewHolder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.last_rolls_fragment, container, false)

        mViewHolder = ViewHolder(view)


        mViewHolder.recyclerView.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }


        return view
    }

    private fun setUpRecyclerView()
    {
        val query = FirebaseDatabase.getInstance()
            .reference
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child(Main.ROLL_CHILD)
            .limitToLast(50)

        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // ...
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // ...
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                // ...
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // ...
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // ...
            }
        }

        query.addChildEventListener(childEventListener)



    }
}