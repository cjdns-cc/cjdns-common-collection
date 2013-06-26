package cjdns.cjdns_music.scanner

import java.io.File
import cjdns.cjdns_music._
import org.jaudiotagger.audio.AudioFileIO
import util.Try
import org.apache.commons.io.FileUtils
import org.jaudiotagger.tag.FieldKey
import com.google.protobuf.ByteString
import cjdns.util.{number, SHA1}
import cjdns.util.fs.FSIterator

/**
 * User: willzyx
 * Date: 26.06.13 - 3:52
 */
object LocalScanner {
  val MAX_FILE_SIZE = 30 * FileUtils.ONE_MB

  case class Item(file: File, record: Model.Record)

  def scan(file: File): Iterator[Item] = {
    val rootHash = SHA1.hashString(file.getAbsolutePath)
    for {
      file <- new FSIterator(file)
      if file.getName.endsWith(".mp3")
      if file.length <= MAX_FILE_SIZE
      audioFile <- Try(AudioFileIO.read(file)).toOption
      header = audioFile.getAudioHeader
      if header.getEncodingType == "mp3"
      tag <- Option(audioFile.getTag)
      artistTitle = tag.getFirst(FieldKey.ARTIST)
      albumTitle = tag.getFirst(FieldKey.ALBUM)
      recordTitle = tag.getFirst(FieldKey.TITLE)
    } yield {
      val artist =
        Model.Artist.newBuilder.
          setId(SHA1.hashString(rootHash + artistTitle)).
          setTitle(artistTitle)
      val album =
        Model.Album.newBuilder.
          setId(SHA1.hashString(artist.getId + albumTitle)).
          setArtist(artist).
          setTitle(albumTitle)
      Option(tag.getFirst(FieldKey.YEAR)).
        flatMap(number.parseInt).
        foreach(album.setYear(_))
      val record =
        Model.Record.newBuilder.
          setAlbum(album).
          setId(SHA1.hashString(album.getId + recordTitle)).
          setTitle(recordTitle).
          setBitRate(header.getBitRateAsNumber).
          setIsVariableBitRate(header.isVariableBitRate).
          setLength(header.getTrackLength).
          setSize(file.length).
          setHash(ByteString.copyFrom(SHA1.hash(file))).
          build
      Item(file = file, record = record)
    }
  }

}
