import java.net.URL

val points = URL("https://tinyurl.com/yaxgfjzt")
        .readText().split(Regex("\\r?\\n"))
        .asSequence()
        .filter { it.isNotEmpty() }
        .map { it.split(",") }
        .map { Point(it[0].toDouble(), it[1].toDouble()) }
        .toList()