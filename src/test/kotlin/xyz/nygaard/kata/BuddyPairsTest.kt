package xyz.nygaard.kata

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BuddyPairsTest {

    @Test
    fun test1() {
        assertEquals(48 to 75, BuddyPairs.buddy(10,50))
    }

    @Test
    fun test2() {
        assertEquals(48 to 75, BuddyPairs.buddy(48,50))
    }

    @Test
    fun test3() {
        assertEquals(1081184 to 1331967, BuddyPairs.buddy(1071625,1103735))
    }

    @Test
    fun test4() {
        assertEquals(null, BuddyPairs.buddy(2382,3679))
    }

    @Test
    fun test5() {
        assertEquals(1050 to 1925, BuddyPairs.buddy(381,4318))
    }

    @Test
    fun devisors() {
        assertEquals(8, 10.sumOfDivisors())
        assertEquals(22, 20.sumOfDivisors())
        assertEquals(10, 14.sumOfDivisors())
    }

    @Test
    fun storeTall() {
        repeat(30000) { i ->
            if(i % 100 == 0) println(i)
            (1071625+i).sumOfDivisors() }
    }
}
