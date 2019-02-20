import koma.pow
import org.apache.commons.math3.distribution.TDistribution
import org.nield.kotlinstatistics.weightedCoinFlip
import kotlin.math.exp

enum class Solver {

    OLS {

        // ported from https://github.com/chethangn/SimpleLinearRegression/blob/master/SimpleLinearRegression.py
        override fun solve(points: List<Point>): LineSolution {

            // number of samples
            val n = points.size.toDouble()

            // averages of x and y
            val meanX = points.asSequence().map { it.x }.average()
            val meanY = points.asSequence().map { it.y }.average()

            // calculating cross-deviation and deviation about x
            val ssXy = points.asSequence().map { (x,y) -> (x*y) - (n*meanY*meanX) }.sum()
            val ssXx = points.asSequence().map { (x,_) -> (x*x) - (n*meanX*meanX) }.sum()

            // calculating regression coefficients
            val b1 = ssXy / ssXx
            val b0 = meanY - (b1*meanX)

            return LineSolution(b1,b0, points).also { currentLine.set(it) }
        }
    },
    SIMULATED_ANNEALING{

        // https://stats.stackexchange.com/questions/340626/simulated-annealing-for-least-squares-linear-regression
        // https://stats.stackexchange.com/questions/28946/what-does-the-rta-b-function-do-in-r
        override fun solve(points: List<Point>): LineSolution {

            val scale = 0.1

            val tDistribution = TDistribution(3.0)
            var currentFit = LineSolution(0.0,0.0, points)
            var bestFit = LineSolution(0.0,0.0, points)
            var bestSumOfSquaredError = Double.MAX_VALUE


            // kick off temperature from 120.0 down to 0.0 at step -.005
            generateSequence(120.0) { it - .005 }.takeWhile { it >= 0.0 }
                    .withIndex()
                    .forEach { (index,temp) ->

                        val proposedM = currentFit.m + scale * tDistribution.sample()
                        val proposedB = currentFit.b + scale * tDistribution.sample()

                        val yPredictions = points.map { (proposedM * it.x) + proposedB }
                        val sumOfSquaredError = points.map { it.y }.zip(yPredictions).map { (yActual, yPredicted) -> (yPredicted-yActual).pow(2) }.sum()

                        if (sumOfSquaredError < bestSumOfSquaredError ||
                                weightedCoinFlip(exp((-(sumOfSquaredError - bestSumOfSquaredError)) / temp))) {

                            currentFit = LineSolution(proposedM, proposedB, points)
                        }
                        if (index % 500 == 0 && currentLine.get () != bestFit)
                            currentLine.set(bestFit)

                        if (sumOfSquaredError < bestSumOfSquaredError) {
                            bestSumOfSquaredError = sumOfSquaredError
                            bestFit = currentFit
                        }    
                    }

            currentLine.set(bestFit)
            return currentFit
        }
    },

    SIMULATED_ANNEALING_WITH_TARGET{

        // https://stats.stackexchange.com/questions/340626/simulated-annealing-for-least-squares-linear-regression
        // https://stats.stackexchange.com/questions/28946/what-does-the-rta-b-function-do-in-r
        override fun solve(points: List<Point>): LineSolution {

            val scale = 0.1
            val underCurveTarget = .80

            val tDistribution = TDistribution(3.0)
            var currentFit = LineSolution(0.0,0.0, points)
            var bestFit = LineSolution(0.0,0.0, points)

            var bestFitLoss = Double.MAX_VALUE
            var bestPctUnderCurve = Double.MAX_VALUE

            // kick off temperature from 120.0 down to 0.0 at step -.005
            generateSequence(120.0) { it - .005 }.takeWhile { it >= 0.0 }
                    .withIndex()
                    .forEach { (index,temp) ->

                        val proposedM = currentFit.m + scale * tDistribution.sample()
                        val proposedB = currentFit.b + scale * tDistribution.sample()

                        val yPredictions = points.map { (proposedM * it.x) + proposedB }

                        val pctUnderCurve = points.map { it.y }.zip(yPredictions).count { (yActual, yPredicted) -> yPredicted >= yActual }.toDouble() / points.count().toDouble()

                        val fitLoss = points.map { it.y }.zip(yPredictions).map { (yActual, yPredicted) -> (yPredicted-yActual).pow(2) }.sum()

                        val takeMove = when {

                            bestPctUnderCurve < underCurveTarget && pctUnderCurve > bestPctUnderCurve -> {
                                bestPctUnderCurve = pctUnderCurve
                                bestFit = currentFit
                                true
                            }
                            bestPctUnderCurve >= underCurveTarget && pctUnderCurve >= underCurveTarget && fitLoss < bestFitLoss -> {
                                bestFitLoss = fitLoss
                                bestFit = currentFit
                                true
                            }
                            bestPctUnderCurve >= underCurveTarget && pctUnderCurve >= underCurveTarget && fitLoss > bestFitLoss -> weightedCoinFlip(exp((-(fitLoss - bestFitLoss)) / temp))
                            else -> false
                        }

                        if (takeMove) {
                            currentFit = LineSolution(proposedM, proposedB, points)
                        }
                        if (index % 500 == 0 && currentLine.get () != bestFit)
                            currentLine.set(bestFit)
                    }

            currentLine.set(bestFit)
            bestFit.apply {
                println("${ pts.count { evaluate(it.x) >= it.y }}/${pts.count()}")
                println(bestFit.pctUnderCurve)
            }

            return currentFit
        }
    },

    GRADIENT_DESCENT {

        override fun solve(points: List<Point>): LineSolution {


            val n = points.count().toDouble()

            // partial derivative with respect to M
            fun dM(expectedYs: List<Double>) =
                    (-2.0 / n) * points.mapIndexed { i, p -> p.x * (p.y - expectedYs[i]) }.sum()

            // partial derivative with respect to B
            fun dB(expectedYs: List<Double>) =
                    (-2.0 / n) * points.mapIndexed { i, p -> p.y - expectedYs[i] }.sum()

            val learningRate = .00001

            var m = 0.0
            var b = 0.0

            val epochs = 1000000 // epochs is a fancy name for # of iterations

            repeat(epochs) { epoch ->
                val yPredictions = points.map { (m * it.x) + b }

                val dM = dM(yPredictions)
                val dB = dB(yPredictions)

                m -= learningRate * dM
                b -= learningRate * dB

                // only animate once every 30K iterations
                if (epoch % 30000 == 0)
                    currentLine.set(LineSolution(m,b, points))
            }
            currentLine.set(LineSolution(m,b, points))


            return LineSolution(m, b, points)
        }
    };

    abstract fun solve(points: List<Point>): LineSolution
}
data class Point(val x: Double, val y: Double) {
    constructor(x: Int, y: Int): this(x.toDouble(), y.toDouble())
}
class LineSolution(val m: Double, val b: Double, val pts: List<Point>) {

    fun evaluate(x: Double) = (m*x) + b

    val pctUnderCurve get() = pts.count { evaluate(it.x) >= it.y }.toDouble() / pts.count().toDouble()

    val loss get() =
        points.map { (evaluate(it.x) - it.y).pow(2) }.sum()
}
