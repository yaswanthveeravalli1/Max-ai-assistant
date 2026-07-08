import rikka.shizuku.Shizuku
fun test() {
    // let's dump all methods
    Shizuku::class.java.methods.forEach { println(it.name) }
}
