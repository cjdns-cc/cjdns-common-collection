package cjdns.cjdns_music.music

import java.io.File
import cjdns.cjdns_music._
import util.Try
import cjdns.util.fs.FSIterator
import scala.collection.JavaConversions._
import java.util.{TimerTask, Timer}
import scala.concurrent.duration._
import org.slf4j.LoggerFactory
import org.apache.lucene.index.IndexWriter

/**
 * User: willzyx
 * Date: 26.06.13 - 3:52
 */
object Mp3Scanner {
  val log = LoggerFactory.getLogger("local-scanner")

  def scan(file: File)(implicit writer: IndexWriter) {
    new FSIterator(file).
      filter(Mp3Read.isSuspect).
      flatMap(storage.LocalDB.patch).
      filter(_.hasMusicRecord).
      map(_.getMusicRecord).
      foreach(record => index.Tools.writeRecord(record))
  }

  def scanAll(implicit writer: IndexWriter) {
    for {
      filename <- properties.getStringList("local-collection")
      file = new File(filename)
      if file.exists
    } scan(file)
  }

  def reverseScan(implicit writer: IndexWriter) {
    storage.LocalDB.readAll(
      record => {
        val file = new File(record.getFilename)
        if (!Mp3Read.isSuspect(file)) {
          storage.LocalDB.delete(file)
          if (record.hasMusicRecord) {
            index.Tools.removeRecord(record.getMusicRecord)
          }
        }
      }
    )
  }

  def initialize() {
    new Timer("local-collection-scanner", true).schedule(
      new TimerTask {
        def run() {
          Try {
            Index.LOCAL_COLLECTOR.write(
              writer => {
                scanAll(writer)
                reverseScan(writer)
              }
            )
          } recover {
            case e: Exception =>
              log.error("error while scanning", e)
          }
        }
      },
      0, 5.minutes.toMillis
    )
    LoggerFactory.getLogger("initializer").info("local collection scanner")
  }

}
