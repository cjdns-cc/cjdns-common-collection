package cjdns.cjdns_music.music

import java.io.File
import cjdns.cjdns_music.Model
import cjdns.cjdns_music.Model.MusicRecord
import util.Try
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import cjdns.util.{number, SHA1}
import com.google.protobuf.ByteString
import org.apache.commons.io.FileUtils
import scala.concurrent.duration._

/**
 * User: willzyx
 * Date: 30.06.13 - 21:56
 */
object Mp3Read extends (File => Option[Model.MusicRecord]) {
  val MAX_FILE_SIZE = 30 * FileUtils.ONE_MB

  private object NonEmptyString {
    def unapply(s: String) = Option(s).map(_.trim).filterNot(_.isEmpty)
  }

  def isSuspect(file: File) =
    file.exists &&
      file.getName.endsWith(".mp3") &&
      file.length <= MAX_FILE_SIZE

  def apply(file: File): Option[MusicRecord] = {
    for {
      file <- Option(file)
      if isSuspect(file)
      audioFile <- Try(AudioFileIO.read(file)).toOption
      header = audioFile.getAudioHeader
      if header.getEncodingType == "mp3"
      tag <- Option(audioFile.getTag)
      NonEmptyString(artistTitle) <- Option(tag.getFirst(FieldKey.ARTIST))
      NonEmptyString(albumTitle) <- Option(tag.getFirst(FieldKey.ALBUM))
      NonEmptyString(recordTitle) <- Option(tag.getFirst(FieldKey.TITLE))
    } yield {
      val artist =
        Model.MusicArtist.newBuilder.
          setId(SHA1.hashString(artistTitle)).
          setTitle(artistTitle)
      val album =
        Model.MusicAlbum.newBuilder.
          setId(SHA1.hashString(artist.getId + albumTitle)).
          addArtist(artist).
          setTitle(albumTitle)
      Option(tag.getFirst(FieldKey.YEAR)).
        flatMap(number.parseInt).
        foreach(album.setYear(_))
      val record =
        Model.MusicRecord.newBuilder.
          setAlbum(album).
          setId(SHA1.hashString(album.getId + recordTitle)).
          setTitle(recordTitle).
          setBitRate(header.getBitRateAsNumber).
          setIsVariableBitRate(header.isVariableBitRate).
          setLength(header.getTrackLength).
          setFileSize(file.length).
          setFilePath(file.getAbsolutePath).
          setHash(ByteString.copyFrom(SHA1.hash(file))).
          setWeight(file.lastModified / 1.minute.toMillis).
          build
      record
    }
  }
}
