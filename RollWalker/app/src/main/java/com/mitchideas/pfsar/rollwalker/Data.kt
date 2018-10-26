package com.mitchideas.pfsar.rollwalker

import java.util.*

class Data private constructor() {

	companion object {
		private var singleton: Data? = null

		fun instance(): Data {
			if(singleton == null){
				singleton = Data()
			}

			return singleton as Data
		}
	}

	var maxRoll = 0L
	var comboNum = 0L
	var maxCombo = 0L
	var lastRoll = 0L
	var settings = Settings()
	var animeRollStack = Stack<Long>()
	var distanceTraveledSinceRoll = 0e0
	var rollData = ArrayList<RollData>()
}