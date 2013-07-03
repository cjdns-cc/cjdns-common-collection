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
import cjdns.cjdns_music.AtomicItem.WMusicRecord

/**
 * User: willzyx
 * Date: 26.06.13 - 3:52
 */
object Mp3Scanner {
  val log = LoggerFactory.getLogger("local-scanner")

  import DirtyPoolShepherd.LOCAL_POOL

  private implicit val writer = DirtyIndex.DIRTY_WRITER

  def scan(file: File)(implicit writer: IndexWriter) {
    def items =
      new FSIterator(file).
        filter(Mp3Read.isSuspect).
        flatMap(LocalStorage.patch).
        filter(_.hasMusicRecord).
        map(_.getMusicRecord)
    items.foreach {
      case record =>
        LOCAL_POOL.addItem(WMusicRecord(record))
        index.Tools.writeRecord(record)
    }
  }

  def scanAll(implicit writer: IndexWriter) {
    for {
      filename <- properties.getStringList("local-collection")
      file = new File(filename)
      if file.exists
    } scan(file)
  }

  def reverseScan(implicit writer: IndexWriter) {
    music.LocalStorage.readAll(
      record => {
        val file = new File(record.getFilename)
        if (!Mp3Read.isSuspect(file)) {
          music.LocalStorage.delete(file)
          if (record.hasMusicRecord) {
            LOCAL_POOL.removeItem(WMusicRecord(record.getMusicRecord))
            index.Tools.removeRecord(record.getMusicRecord)
          }
        }
      }
    )
  }

  LocalStorage.readAll(
    record =>
      if (record.hasMusicRecord) {
        LOCAL_POOL.addItem(WMusicRecord(record.getMusicRecord))
        index.Tools.removeRecord(record.getMusicRecord)
      }
  )

  def initialize() {
    new Timer("local-collection-scanner", true).schedule(
      new TimerTask {
        def run() {
          Try {
            scanAll(writer)
            reverseScan(writer)
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
