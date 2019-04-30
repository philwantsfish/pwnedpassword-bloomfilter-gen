package com.philwantsfish

import java.io.File
import org.scalatest.{FlatSpec, Matchers}
import scala.io.Source

object TestUtils {
  def p(str: String) = println(s"[+] $str")

  def timeIt[T](f: => T): T = {
    val start = System.nanoTime()
    val res = f
    val end = System.nanoTime()
    p(s"Function took ${end - start} ns or ${(end - start) / 1000} us")
    res
  }
}

class BloomFilterTest extends FlatSpec with Matchers {
  import TestUtils._
  import com.philwantsfish.BloomFilterExtension._

  val fpRate = 0.001

  "PwnedPaswordFactory.create" should "create a bloom file from a file of hashes" in {
    val iter = Source.fromResource("hashes.txt").getLines()
    val filter = PwnedPasswordFactory.create(iter, fpRate)
    filter.maybePwned("7C4A8D09CA3762AF61E59520943DC26494F8941B") shouldBe true
    filter.maybePwned("F7C3BC1D808E04732ADF679965CCC34CA7AE3441") shouldBe true
    filter.maybePwned("B1B3773A05C0ED0176787A4F1574FF0075F7521E") shouldBe true
  }

  it should "fail if the first line of the file is not a sha1 hash" in {
    val iter = Source.fromResource("pwnedpassword-head.txt").getLines
    an [RuntimeException] should be thrownBy PwnedPasswordFactory.create(iter, fpRate)
  }

  "BloomFilterExtensions" should "provide serialization methods" in {
    val iter = Source.fromResource("hashes.txt").getLines()
    val filter = PwnedPasswordFactory.create(iter, fpRate)

    // Store the filter
    val f = File.createTempFile("bloomfilter", "object")
    filter.store(f)

    // Create a file from that file
    val newFilter = PwnedPasswordFactory.read(f.getAbsolutePath).get

    // Confirm it works
    newFilter.maybePwned("7C4A8D09CA3762AF61E59520943DC26494F8941B") shouldBe true
    newFilter.maybePwned("F7C3BC1D808E04732ADF679965CCC34CA7AE3441") shouldBe true
    newFilter.maybePwned("B1B3773A05C0ED0176787A4F1574FF0075F7521E") shouldBe true
  }

  // ------------------------------------------------------------

  // I used this function to get the total occurrences for the data set.
  // I created the file using sed:
  // $ sed -i '' -e 's/^.*://g' occurrences.txt
  def getOccurrences(iter: Iterator[String]): BigInt = {
    var bigInt = 0
    iter.foreach(line => bigInt += line.toInt)
    bigInt
  }

  "getOccurrences" should "sum a file that contains a single number per line" in {
    val iter = Source.fromResource("occurrences.txt").getLines()
    getOccurrences(iter) shouldBe 54337892L
  }


  "Find occurrences for each 5%" should "sum" ignore {
    // I'm using this test to find the total occurrences per 5% of hashes in the pwned passwords data.
    // Obtain a copy of the the data set an run the following sed command:
    // $ sed -i '' -e 's/^.*://g' occurrences.txt
    // Then run this test!
    val totalOccurrences: BigDecimal = 3344070078L

    var occurrences: BigDecimal = 0
    var offset: BigInt = 0
    var percentage: Int = 0

    Source.fromFile("data/occurrences.txt").getLines().foreach { line =>
      val num = line.toInt
      occurrences += num
      offset += 1

      if (offset % 27575488 == 0) {
        percentage += 5
        val percentOfOccurrences = occurrences / totalOccurrences
        p(s"Percentage $percentage. Offset: $offset. Occurrences: $occurrences. PercentOfOccurrences: $percentOfOccurrences ")
      }
    }
    p(s"Total occurrences $occurrences")
  }
}
