import java.net.URL

val points = URL("https://tinyurl.com/y58sesrr")
        .readText().split(Regex("\\r?\\n"))
        .asSequence()
        .drop(1)
        .filter { it.isNotEmpty() }
        .map { it.split(",") }
        .map { (x,y) ->  Point(x.toDouble(), y.toDouble()) }
        .toList()