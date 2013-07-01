package cjdns.cjdns_music.music.storage

import cjdns.cjdns_music.{Model, Storage}
import java.io.File
import com.sleepycat.je.{DiskOrderedCursorConfig, OperationStatus, DatabaseEntry}
import cjdns.util.SHA1
import cjdns.cjdns_music.music.Mp3Read

/**
 * User: willzyx
 * Date: 30.06.13 - 18:18
 */
object LocalDB extends (File => Model.MusicRecordLocal) {
  val REF = Storage.database("local-cache-music")

  private def key(file: File) = new DatabaseEntry(SHA1.hash(file.getAbsolutePath))

  def delete(file: File) {
    REF.delete(null, key(file))
  }

  def cache(file: File): Option[Model.MusicRecordLocal] = {
    val entry = new DatabaseEntry
    REF.get(null, key(file), entry, null) match {
      case OperationStatus.SUCCESS =>
        val record = Model.MusicRecordLocal.parseFrom(entry.getData)
        def a1 = record.getLastModified == file.lastModified
        def a2 = record.getFileSize == file.length
        if (a1 && a2) Option(record) else Option.empty
      case OperationStatus.NOTFOUND =>
        Option.empty
    }
  }

  def update(file: File): Model.MusicRecordLocal = {
    val builder = Model.MusicRecordLocal.newBuilder
    builder.setFilename(file.getAbsolutePath)
    builder.setFileSize(file.length)
    builder.setLastModified(file.lastModified)
    Mp3Read(file).foreach(builder.setMusicRecord(_))
    val record = builder.build
    REF.put(null, key(file), new DatabaseEntry(record.toByteArray))
    record
  }

  def patch(file: File) =
    if (cache(file).isEmpty)
      Option(update(file))
    else
      Option.empty

  def apply(file: File): Model.MusicRecordLocal =
    cache(file) getOrElse update(file)

  def readAll(handler: Model.MusicRecordLocal => Unit) {
    val cursor = REF.openCursor(DiskOrderedCursorConfig.DEFAULT)
    try {
      val key = new DatabaseEntry
      val value = new DatabaseEntry
      while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS) {
        handler(Model.MusicRecordLocal.parseFrom(value.getData))
      }
    } finally {
      cursor.close()
    }
  }

}
