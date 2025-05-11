package app.ultradev.divineprison.util

class TestClass(
    val name: String,
) {
    val test: String = "test"
}

val GLOBAL_TEST = "test"

object Test {
    val test: String = "test"
}

fun test() {
    val testList = listOf(1, 2, 3, 4, 5)
    val testClass = TestClass("test")

    testList.forEach {
        println(it)
    }



    println(testClass.name)

    for (i in 1..10) {
        println(i)
    }
}