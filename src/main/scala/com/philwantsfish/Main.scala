package com.philwantsfish

import java.io.File

object Main extends App {
  def p(msg: String): Unit = println(s"[+] $msg")

  val defaultFpRate = 0.001

  case class Config(out: File = new File("filter.object"),
                    rate: Double = defaultFpRate,
                    data: File = new File("."),
                    num: Long = -1
                   )

  val parser = new scopt.OptionParser[Config]("BloomFilter Generator") {
//    head("scopt", "3.x")
    help("help").text("prints this usage text")

    opt[File]('o', "output").action((x, c) =>
      c.copy(out = x) ).text("The path to store the bloom filter. Defaults to a file named filter.object")

    opt[Double]('r', "rate").action((x, c) =>
      c.copy(rate = x)).text(s"The false positive rate for the bloom filter. Default is $defaultFpRate")

    opt[File]('d', "data").required.action((x, c) =>
      c.copy(data = x)).text(s"A path to a file containing sha1 hashes")

    opt[Long]('n', "num").action((x, c) =>
      c.copy(num = x)).text("Create the bloom filter with the N hashes in the file")

  }

  parser.parse(args, Config()) match {
    case Some(config) =>
      import BloomFilterExtension._
      p("Generating bloom filter ...")

      // I don't think scopt has Option arguments, a default must be provided? Using -1 to represent none provided
      // and transforming that to None
      val numOpt = if (config.num == -1) None else Some(config.num)

      val start = System.nanoTime()
      val filter = PwnedPasswordFactory.create(config.data, config.rate, numOpt)
      p(s"Bloom filter generated, storing result to ${config.out}")
      filter.store(config.out)
      val end = System.nanoTime()

      val elapsedTimeSeconds = (end - start) / 1000 / 1000 / 1000
      val fileSize = config.out.length()
      p(s"Generation took $elapsedTimeSeconds seconds, the filter is $fileSize bytes on disk")
    case None =>
    // arguments are bad, error message will have been displayed
  }
}
