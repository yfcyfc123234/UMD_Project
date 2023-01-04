import com.yfc.cool.umd.UMD

fun main(args: Array<String>?) {
//    println("Hello World!")
//
//    // Try adding program arguments via Run/Debug configuration.
//    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
//    println("Program arguments: ${args?.joinToString()}")

    test()
}

private fun test() {
//    val filePath = "C:\\Users\\admin\\Desktop\\测试文件\\umd\\天涯-青春疼痛小说：用左手爱你.umd"
    val filePath = "C:\\Users\\admin\\Desktop\\测试文件\\umd\\明朝那些事儿（1-7全套）终极版.umd"
    UMD().apply {
        parseFile(filePath, true) { success ->
            if (success) {
                println()
                println(header)
                println()
                println(property)
                println()
                println(chapter)
                println()
                println(cover)
            }
        }
    }
}