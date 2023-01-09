import com.yfc.cool.umd.UMD
import java.io.File

fun main(args: Array<String>?) {
//    test("C:/Users/admin/Desktop/电子书测试文件/umd/天涯-青春疼痛小说：用左手爱你.umd")
    test("C:/Users/admin/Desktop/电子书测试文件/umd/明朝那些事儿（1-7全套）终极版.umd")
}

private fun test(filePath: String) {
    UMD().parseFile(filePath) { umd, success ->
        if (success) {
            println("${umd.header}\n")
            println("${umd.property}\n")
            println("${umd.chapter}\n")
            println("${umd.cover}\n")

            umd.chapter?.apply {
                val fileBook = File(filePath)
                val file = File(fileBook.parentFile, fileBook.nameWithoutExtension)
                if (!file.exists()) {
                    file.mkdirs()
                }
                chaptersTitleAndContent?.forEach {
                    val fileChapter = File(file, "${if (it.title.isNullOrEmpty()) "unknown_title" else it.title!!.trim().replace("*", "-")}.txt")
                    if (!fileChapter.exists()) {
                        fileChapter.createNewFile()
                    }
                    fileChapter.writeText(it.content ?: "")
                }
            }
        }
    }
}
