package cjdns.cjdns_music.music.index

import cjdns.cjdns_music.Model.{MusicArtist, MusicAlbum, MusicRecord}
import org.apache.lucene.document.{TextField, StringField, Document}
import org.apache.lucene.document.Field.Store
import org.apache.commons.codec.binary.Hex
import org.apache.lucene.index.{Term, IndexWriter}
import scala.collection.JavaConversions._

/**
 * User: willzyx
 * Date: 30.06.13 - 16:14
 */
object Tools {
  private def getUniqueId(record: MusicRecord)(implicit partition: String) = {
    if (partition == null)
      record.getId
    else
      partition + "_" + record.getId
  }

  private def write(artist: MusicArtist,
                    album: MusicAlbum,
                    record: MusicRecord)(implicit writer: IndexWriter, partition: String) {
    val id = getUniqueId(record)
    val document = new Document
    document.add(new StringField(Constant.UNIQUE_ID, id, Store.NO))
    document.add(new StringField(Constant.HASH, new String(Hex.encodeHex(record.getHash.toByteArray)), Store.YES))
    document.add(new StringField(Constant.RECORD_ID, record.getId, Store.YES))
    document.add(new StringField(Constant.ALBUM_ID, album.getId, Store.YES))
    document.add(new StringField(Constant.ARTIST_ID, artist.getId, Store.YES))
    document.add(new TextField(Constant.SUGGEST, record.getTitle, Store.NO))
    document.add(new TextField(Constant.SUGGEST, album.getTitle, Store.NO))
    document.add(new TextField(Constant.SUGGEST, artist.getTitle, Store.NO))
    writer.updateDocument(new Term(Constant.UNIQUE_ID, id), document)
  }

  def writeRecord(record: MusicRecord)(implicit writer: IndexWriter, partition: String = null) {
    if (record.hasAlbum) {
      for (artist <- record.getArtistList) {
        write(artist, record.getAlbum, record)
      }
      for (artist <- record.getAlbum.getArtistList) {
        write(artist, record.getAlbum, record)
      }
    }
  }

  def removeRecord(record: MusicRecord)(implicit writer: IndexWriter, partition: String = null) {
    writer.deleteDocuments(new Term(Constant.UNIQUE_ID, getUniqueId(record)))
  }

  def writeArtist(artist: MusicArtist)(implicit writer: IndexWriter, partition: String = null) {
    for (album <- artist.getAlbumList; record <- album.getRecordList) {
      write(artist, album, record)
    }
  }

}
