import java.net.URL

val points = URL("https://tinyurl.com/yaxgfjzt")
        .readText().split(Regex("\\r?\\n"))
        .asSequence()
        .filter { it.isNotEmpty() }
        .map { it.split(",") }
        .map { (x,y) ->  Point(x.toDouble(), y.toDouble()) }
        .toList()