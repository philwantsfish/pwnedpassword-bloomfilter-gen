# pwnedpassword-bloomfilter-gen

A small program to process the PwnedPassword data set into a BloomFilter object

## Prerequisites

Download the [PwnedPassword data set](https://haveibeenpwned.com/Passwords) and remove the occurrences column:

```zsh
➜  time sed -i '' -e 's/:.*$//g' pwned-passwords-sha1-ordered-by-hash-v4.txt
sed -i '' -e 's/:.*$//g' pwned-passwords-sha1-ordered-by-hash-v4.txt  1497.24s user 110.03s system 93% cpu 28:40.04 total
➜
```

## Usage

Generating larger bloom filters requires increasing the JVM heap space. Increasing the heap space to 3 GB is sufficient for the entire data set

```bash
export "SBT_OPTS=-Xmx3G"
```

The program is executed using [sbt](https://www.scala-sbt.org/). Passing the `--help` argument prints the program usage:

```zsh
➜ sbt -warn "run --help"
Usage: BloomFilter Generator [options]

  --help                prints this usage text
  -o, --output <value>  The path to store the bloom filter. Defaults to a file named filter.object
  -r, --rate <value>    The false positive rate for the bloom filter. Default is 0.001
  -d, --data <value>    A path to a file containing sha1 hashes
  -n, --num <value>     Create the bloom filter with the N hashes in the file
➜
```

## Generating a bloom filter

The following command creates a bloom filter with the first 2000 hashes and a false positive rate of 0.1%. The bloom filter object is serialized and stored to disk in the `bloomfilter.object` file.

```zsh
➜ sbt -warn "run -d pwned-passwords-sha1-ordered-by-count-v4.txt  -r 0.001 -n 2000 -o bloomfilter.object"
[+] Generating bloom filter ...
[+] Bloom filter generated, storing result to bloomfilter.object
[+] Generation took 0 seconds, the filter is 4073 bytes on disk
➜
```

## Using the bloom filter

Add a dependency for Google Guava in your project, deserialize the object, and use it as normal:

```scala
val stream = new ObjectInputStream(new FileInputStream(path))
val bloomfilter = stream.readObject().asInstanceOf[BloomFilter[String]]
val pwnedpassword = bloomfilter.mightContain(hash)
```






