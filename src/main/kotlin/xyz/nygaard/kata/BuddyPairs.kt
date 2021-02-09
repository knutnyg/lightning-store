package xyz.nygaard.kata

internal class BuddyPairs {

    companion object {
        internal fun buddy(start:Int, limit: Int): Pair<Int, Int>? {
            (start..limit).forEach { n ->
                println(n)
                val m = n.sumOfDivisors() - 1
                if (m > n && m.sumOfDivisors() == n + 1) return n to m
            }
            return null
        }
    }
}

private val cache = mutableMapOf<Int, Int>()
internal fun Int.sumOfDivisors(): Int = (1..this/2 step 2).filter { this % it == 0 }.sum()
