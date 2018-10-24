package com.mitchellsideas.pfsar.rollwalker

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
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
    private var mVisable: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.main_fragment, container, false)

        mViewHolder = ViewHolder(view)

        return view
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        mVisable = true
		initlise(activity as Main)
	}

    override fun onPause() {
        super.onPause()
        mVisable = false
    }

    fun updateComboText(combo: Long) {
        mViewHolder.rollCombo.text = getString(R.string.combo_title, combo)
    }

    fun updateTargetText(target: Long) {
        mViewHolder.rollTarget.text = getString(R.string.target_title, target)
    }

	fun updateRollResult(main: Main){
		if(main.settings.rollAnimation) {
			startRollAnimation(main)
		}
		else{
			var nextRoll: Long = 0

			while (!main.animeRollStack.empty())
				nextRoll = main.animeRollStack.pop()

			mViewHolder.rollResult.text = nextRoll.toString()
			mViewHolder.animeRollResult.text = nextRoll.toString()
			mViewHolder.animeRollResult.setTextColor(Color.RED)
		}
	}

    fun initlise(main: Main)
    {
        updateComboText(main.comboNum)
        updateTargetText(main.maxRoll)
        mViewHolder.animeRollResult.text = main.lastRoll.toString()
        mViewHolder.rollResult.text = main.lastRoll.toString()
    }

	private fun startRollAnimation(main: Main) {
		mViewHolder.rollLayout.clearAnimation()

		val animShake = AnimationUtils.loadAnimation(activity, R.anim.shake)

		animShake.duration = 30

		animShake.setAnimationListener(object : Animation.AnimationListener{
			private var mPrevColour: Int = 0

			override fun onAnimationStart(animation: Animation?) {
			}

			override fun onAnimationEnd(animation: Animation?) {
				if(main.animeRollStack.size > 0){
					val next = main.animeRollStack.pop().toString()

					var nextColour: Int

					do
					{
						nextColour = Utils.randomValue(COLOURS_LIST)
					} while (mPrevColour == nextColour)

					mPrevColour = nextColour

					if(main.animeRollStack.size == 0) {
						nextColour = Color.RED
						mViewHolder.rollLayout.clearAnimation()
					}

					mViewHolder.rollResult.text = next
					mViewHolder.animeRollResult.text = next

					mViewHolder.animeRollResult.setTextColor(nextColour)

					when {
						main.animeRollStack.size > 25 -> animShake.duration += 1
						main.animeRollStack.size > 15 -> animShake.duration = 50
						main.animeRollStack.size > 5 -> animShake.duration += 40
						else -> animShake.duration += 100
					}

					if(main.animeRollStack.size > 0) {
						mViewHolder.rollLayout.startAnimation(animShake)
					}
				}
			}

			override fun onAnimationRepeat(animation: Animation?) {
			}

		})
		mViewHolder.rollLayout.startAnimation(animShake)

	}
}
