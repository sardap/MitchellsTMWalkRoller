package com.example.pfsar.rollwalker

import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.*

class MainFragment : Fragment() {

    companion object {
        fun newInstance(): MainFragment {
            return MainFragment()
        }

        val COLOURS_LIST: MutableList<Int> = Arrays.asList(
            (0xFF00FF00).toInt(),
            (0xFF0000FF).toInt(),
            (0xFFFFC0CB).toInt(),
            (0xFFffa500).toInt(),
            (0xFFEEE8AA).toInt(),
            (0xFFFFFFF0).toInt(),
            (0xFF228B22).toInt(),
            (0xFF9400D3).toInt()
        )!!
    }

    private class ViewHolder(view: View)
    {
        val rollTarget: TextView = view.findViewById(R.id.roll_target)
        val rollResult: TextView = view.findViewById(R.id.rollResult)
        val rollCombo: TextView = view.findViewById(R.id.roll_combo)
        val animeRollResult: TextView = view.findViewById(R.id.roll_animation_result)
    }

    private lateinit var mViewHolder: ViewHolder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.main_fragment, container, false)

        mViewHolder = ViewHolder(view)

        return view
    }

    override fun onStart() {
        super.onStart()
        initlise(activity as Main)
    }

    fun updateComboText(combo: Int)
    {
        mViewHolder.rollCombo.text = getString(R.string.combo_title, combo)
    }

    fun updateTargetText(target: Long)
    {
        mViewHolder.rollTarget.text = getString(R.string.target_title, target)
    }


    fun initlise(main: Main)
    {
        updateComboText(main.comboNum)
        updateTargetText(main.maxRoll)
        mViewHolder.animeRollResult.text = main.lastRoll.toString()
        mViewHolder.rollResult.text = main.lastRoll.toString()

        val handler = Handler()

        val updater = object : Runnable {

            private var mNextAnimeUpdate = 0L
            private var mAnimeNextIncremnet = 0.1

            override fun run()
            {
                val now = System.currentTimeMillis();

                if (!main.animeRollStack.empty()) {
                    if (now > mNextAnimeUpdate) {

                        val next = main.animeRollStack.pop().toString()
                        mViewHolder.animeRollResult.text = next

                        if(main.animeRollStack.size > 0)
                        {
                            mViewHolder.animeRollResult.setTextColor(Utils.randomValue<Int>(COLOURS_LIST))
                        }
                        else
                        {
                            mViewHolder.animeRollResult.setTextColor(0xFFFF0000.toInt())
                        }


                        mViewHolder.rollResult.text = next

                        mNextAnimeUpdate = now + mAnimeNextIncremnet.toLong()


                        if(main.animeRollStack.size > 25)
                        {
                            mAnimeNextIncremnet += 0.001
                        }
                        else if(main.animeRollStack.size > 15)
                        {
                            mAnimeNextIncremnet += 20
                        }
                        else if(mAnimeNextIncremnet > 5)
                        {
                            mAnimeNextIncremnet += 40
                        }
                        else
                        {
                            mAnimeNextIncremnet += 100
                        }

                        Log.w(Main.TAG, "Now:$now NextTime:$mAnimeNextIncremnet StackSize:${main.animeRollStack.size} NextVaule:$next")
                    }
                }
                else
                {
                    mAnimeNextIncremnet = 0.0
                }

                handler.postDelayed(this, 5)
            }
        }

        handler.post(updater)

    }
}
