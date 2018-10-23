package com.mitchellsideas.pfsar.rollwalker

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import android.widget.TextView
import java.util.*

class MainFragment : Fragment() {

    companion object {
        fun newInstance(): MainFragment {
            return MainFragment()
        }

        val COLOURS_LIST: MutableList<Int> = Arrays.asList(
            Color.RED,
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.YELLOW
        )!!
    }

    private class ViewHolder(view: View)
    {
        val rollTarget: TextView = view.findViewById(R.id.roll_target)
        val rollResult: TextView = view.findViewById(R.id.rollResult)
        val rollCombo: TextView = view.findViewById(R.id.roll_combo)
        val animeRollResult: TextView = view.findViewById(R.id.roll_animation_result)
        val rollLayout: RelativeLayout = view.findViewById(R.id.roll_layout)
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

            private var mAnimeUpdate = 0L
            private var mNextIncremnet = 0.1
            private var mPrevColour: Int = 0

            override fun run()
            {
                val now = System.currentTimeMillis();

                if (!main.animeRollStack.empty()) {
                    if(main.animeRollStack.size == Main.ANIMATION_COUNT)
                    {
                        val animShake = AnimationUtils.loadAnimation(activity, R.anim.shake)
                        mViewHolder.rollLayout.startAnimation(animShake)
                    }

                    if (now > mAnimeUpdate) {

                        val next = main.animeRollStack.pop().toString()
                        mViewHolder.animeRollResult.text = next

                        var nextColour: Int

                        do
                        {
                            nextColour = Utils.randomValue(COLOURS_LIST)
                        } while (mPrevColour == nextColour)

                        mPrevColour = nextColour

                        mViewHolder.animeRollResult.setTextColor(nextColour)


                        mViewHolder.rollResult.text = next

                        mAnimeUpdate = now + mNextIncremnet.toLong()


                        if(main.animeRollStack.size > 25)
                        {
                            mNextIncremnet += 0.10
                        }
                        else if(main.animeRollStack.size > 15)
                        {
                            mNextIncremnet = 50.0
                        }
                        else if(mNextIncremnet > 5)
                        {
                            mNextIncremnet += 40
                        }
                        else
                        {
                            mNextIncremnet += 100
                        }

                        Log.w(Main.TAG, "Now:$now NextTime:$mNextIncremnet StackSize:${main.animeRollStack.size} NextVaule:$next")
                    }

                    if(main.animeRollStack.size == 0)
                    {
                        mNextIncremnet = 0.0
                        mAnimeUpdate = 0
                        mViewHolder.animeRollResult.setTextColor(Color.RED)
                        mViewHolder.rollLayout.clearAnimation()
                    }
                }

                handler.postDelayed(this, 30)
            }
        }

        handler.post(updater)

    }
}
