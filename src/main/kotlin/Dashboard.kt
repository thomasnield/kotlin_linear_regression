import javafx.animation.SequentialTransition
import javafx.application.Application
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import tornadofx.*
import java.text.DecimalFormat


fun main(args: Array<String>) = Application.launch(DemoApp::class.java, *args)

class DemoApp: App(DemoView::class)

val currentLine = SimpleObjectProperty<LineSolution?>()

class DemoView: View() {

    override val root = borderpane {

        val xAxis = NumberAxis(0.0,100.0, 10.0)
        val yAxis = NumberAxis(0.0,200.0, 10.0)

        val mLabelText = SimpleStringProperty()
        val bLabelText = SimpleStringProperty()

        val selectedSolver = SimpleObjectProperty<Solver>()

        left = form {
            fieldset {
                field("m-value") {
                    label(mLabelText)
                }
                field("b-value") {
                    label(bLabelText)
                }
            }
            fieldset {
                field("Solver") {
                    combobox(selectedSolver, Solver.values().asList().observable())
                }
            }
        }
        center = stackpane {

            scatterchart(null, xAxis, yAxis) {
                series("") {
                    points.forEach {
                        data(it.x, it.y)
                    }
                }
            }

            linechart(null, xAxis, yAxis) {

                val animationQueue = SequentialTransition()

                opacity = 0.5
                series("") {

                    val minX = points.asSequence().map { it.x }.min()!!
                    val maxX = points.asSequence().map { it.x }.max()!!

                    val startPoint = XYChart.Data<Number,Number>(minX, 0)
                    val endPoint = XYChart.Data<Number,Number>(maxX, 0)

                    data.setAll(startPoint, endPoint)

                    currentLine.onChange {
                        animationQueue.children += timeline(play = false) {
                            keyframe(1000.millis) {
                                keyvalue(mLabelText, DecimalFormat("#.0000").format(it!!.m))
                                keyvalue(bLabelText, DecimalFormat("#.0000").format(it.b))
                                keyvalue(startPoint.YValueProperty(), it.evaluate(minX))
                                keyvalue(endPoint.YValueProperty(), it.evaluate(maxX))
                            }
                        }
                    }

                    selectedSolver.onChange {
                        animationQueue.children.clear()
                        it?.solve(points)?.also {
                            animationQueue.play()
                        }
                    }
                }
            }
        }
    }
}