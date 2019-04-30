package com.philwantsfish

import java.io._
import java.nio.charset.StandardCharsets
import com.google.common.hash.{BloomFilter, Funnels}
import scala.io.Source
import scala.util.Try

object PwnedPasswordFactory {
  // The v4 version of the pwned password data contains this many entries
  val PwnedPasswordCount: Long = 551509767L

  // Wrappers around the create method below, one with num and one without
  def create(file: File, fpp: Double, num: Long): BloomFilter[String] = create(file, fpp, Some(num))
  def create(file: File, fpp: Double): BloomFilter[String] = create(file, fpp, None)

  /**
    * Create a BloomFilter with a specific false positive rate
    *
    * @param file A file containing hashes
    * @param fpp The false positive rate for the bloom filter
    * @param num Create the filter using the N hashes, -1 results in all
    *
    * @return A new BloomFilter instance
    */
  def create(file: File, fpp: Double, num: Option[Long]): BloomFilter[String] = create(Source.fromFile(file).getLines, fpp, num)


  // Wrappers around the create method below, one with num and one without
  def create(iter: Iterator[String], fpp: Double, num: Long): BloomFilter[String] = create(iter, fpp, Some(num))
  def create(iter: Iterator[String], fpp: Double): BloomFilter[String] = create(iter, fpp, None)

  /**
    * Create a BloomFilter with a specific false positive rate
    *
    * @param iter An iterator over the hashes
    * @param fpp The false positive rate for the bloom filter
    * @param numOpt Create the filter using the N hashes, -1 results in all
    *
    * @return A new BloomFilter instance
    */
  def create(iter: Iterator[String], fpp: Double, numOpt: Option[Long]): BloomFilter[String] = {
    // If the number of hashes is not defined, default to the size of the pwned password data set. Ideally the code would
    // check the number of hashes in the data set, then create the bloom filter, but this requires iterating over the
    // data set twice.
    val num = numOpt.getOrElse(PwnedPasswordCount)
    val filter = BloomFilter.create[String](
      Funnels.stringFunnel(StandardCharsets.US_ASCII),
      num,
      fpp
    )

    // Check the first hash looks correct
    val firstHash = iter.take(1).next()
    if (isExpectedHashFormat(firstHash)) {
      // Put the first hash in the bloom filter
      filter.put(firstHash)

      var index: Long = 1
      while (index < num) {
        if (iter.hasNext) {
          filter.put(iter.next())
          index += 1
        } else {
          println(s"[+] Data set containeed $index entries, but you asked to take $num hashes from it. Created filter with $index entries")
        }
      }
    } else {
      throw new RuntimeException(s"Expected a 40 character alphanumeric hash. What is this: $firstHash")
    }

    filter
  }

  def isExpectedHashFormat(line: String): Boolean = line.length == 40 && line.forall(c => c.isLetterOrDigit)

  /**
    * Create a BloomFilter[String] instance from a serialized object
    *
    * @param path A path to the serialized object
    * @return The new deserialized BloomFilter[String] instance
    */
  def read(path: String): Try[BloomFilter[String]] = {
    Try {
      val stream = new ObjectInputStream(new FileInputStream(path))
      stream.readObject().asInstanceOf[BloomFilter[String]]
    }
  }
}

object BloomFilterExtension {
  implicit class BloomFilterExtensionMethods(filter: BloomFilter[String]) {
    /**
      * The same function as mightContain, but with a more context specific name
      */
    def maybePwned(sha1: String): Boolean = filter.mightContain(sha1)

    /**
      * Serialize the BloomFilter and store it to disk
      *
      * @param path The file to store the BloomFilter
      */
    def store(path: String): Unit = {
      val stream = new ObjectOutputStream(new FileOutputStream(path))
      stream.writeObject(filter)
    }


    /**
      * Serialize the BloomFilter and store it to disk
      *
      * @param file The file to store the BloomFilter
      */
    def store(file: File): Unit = {
      val stream = new ObjectOutputStream(new FileOutputStream(file))
      stream.writeObject(filter)
    }
  }
}
