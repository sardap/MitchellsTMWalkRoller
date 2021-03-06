package mitchideas.club.psarda.rollwalkers

import java.util.*

class Utils {
    companion object {
        private val RANDOM = Random()

        fun<T> randomValue(array : List<T>) : T
        {
            return array[RANDOM.nextInt(array.size - 1)]
        }

    }
}