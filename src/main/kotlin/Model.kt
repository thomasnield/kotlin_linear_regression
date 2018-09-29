

enum class Solver {

    OLS {
        override fun solve(points: List<Point>): LineSolution {

            return LineSolution(12.0, 2.0)
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

                if (epoch % 10000 == 0)
                    currentLine.set(LineSolution(m,b))
            }
            currentLine.set(LineSolution(m,b))


            return LineSolution(m, b)
        }
    };

    abstract fun solve(points: List<Point>): LineSolution
}
class Point(val x: Double, val y: Double) {
    constructor(x: Int, y: Int): this(x.toDouble(), y.toDouble())
}
class LineSolution(val m: Double, val b: Double) {
    fun evaluate(x: Double) = (m*x) + b
}