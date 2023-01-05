import com.yfc.cool.umd.UMD
import java.io.File
import java.nio.charset.StandardCharsets

fun main(args: Array<String>?) {
//    println("Hello World!")
//
//    // Try adding program arguments via Run/Debug configuration.
//    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
//    println("Program arguments: ${args?.joinToString()}")

//    test("C:\\Users\\admin\\Desktop\\测试文件\\umd\\天涯-青春疼痛小说：用左手爱你.umd")
    test("C:\\Users\\admin\\Desktop\\测试文件\\umd\\明朝那些事儿（1-7全套）终极版.umd")
}

private fun test(filePath: String) {
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

                chapter?.apply {
                    val file = File("C:\\Users\\admin\\Desktop\\明朝那些事儿（1-7全套）终极版")
                    file.mkdirs()

                    val fileData = File(file, file.nameWithoutExtension + ".txt")
                    fileData.delete()
                    this.chaptersData?.forEachIndexed { index, umdChapterData ->
                        fileData.appendText(
                            "\n" +
                                    "\n" +
                                    "\n" +
                                    "\n" +
                                    "\n" +
                                    "\n" +
                                    "\n" +
                                    "\n" +
                                    "\n" +
                                    "\n" +
                                    "\n" +
                                    "\n" +
                                    "\n" + "${index}${index}${index}${index}\n\n\n\n\n\n\n\n\n\n\n\n\n${umdChapterData.data}", StandardCharsets.UTF_8
                        )
                    }
                }
            }
        }
    }
}