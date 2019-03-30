package uzh.scenere.helpers

import android.content.Context
import android.util.Log
import uzh.scenere.const.Constants.Companion.COLOR_BOUND
import java.util.*

class CppHelper {

    external fun evaluateAverage(grid: Array<IntArray>): Double

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }

        fun evaluate(context: Context){
            val cpp = CppHelper()
            val gridSize = 500
            val runs = 20
            val bitmapGrid = array2dOfInt(gridSize,gridSize)
            for (w in 0 until gridSize){
                for (h in 0 until gridSize){
                    bitmapGrid[w][h] = Random().nextInt(COLOR_BOUND)
                }
            }
            for (i in 1 .. runs) {
                val startCpp = System.currentTimeMillis()
                val cppResult = cpp.evaluateAverage(bitmapGrid)
                val startKotlin = System.currentTimeMillis()
                val kotlinResult = evaluate(bitmapGrid)
                val startKotlinStream = System.currentTimeMillis()
                val kotlinStreamResult = evaluateStream(bitmapGrid)
                val endTime = System.currentTimeMillis()
                Log.d("JNI Performance", "To process ${gridSize * gridSize} values, \n" +
                        "C++ took ${startKotlin - startCpp} ms, outcome: $cppResult;\n" +
                        "Kotlin took ${startKotlinStream - startKotlin} ms, outcome $kotlinResult;\n" +
                        "Kotlin Streams took ${endTime - startKotlinStream} ms, outcome $kotlinStreamResult;")
            }
        }

        private fun evaluate(grid: Array<IntArray>): Double {
            var totalAverage = 0.0
            for (array in grid){
                for (value in array){
                    totalAverage += value.toDouble().div(grid.size*array.size)
                }
            }
            return totalAverage
        }

        private fun evaluateStream(grid: Array<IntArray>): Double {
            var totalAverage = 0.0
            grid.forEach { a -> a.forEach { v -> totalAverage += v.toDouble().div(grid.size*a.size)}}
            return totalAverage
        }
    }
}