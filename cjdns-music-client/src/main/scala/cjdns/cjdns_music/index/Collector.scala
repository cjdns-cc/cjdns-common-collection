package cjdns.cjdns_music.index

import org.apache.lucene.index.{DirectoryReader, IndexReader, IndexWriterConfig, IndexWriter}
import org.apache.lucene.store.Directory
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.core.SimpleAnalyzer
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import cjdns.util.concurrent.ReadWriteLockWrapper

/**
 * User: willzyx
 * Date: 01.07.13 - 2:14
 */
abstract class Collector(directory: Directory) {
  private val writer =
    new IndexWriter(
      directory,
      new IndexWriterConfig(Version.LUCENE_43, new SimpleAnalyzer(Version.LUCENE_43)).
        setOpenMode(OpenMode.CREATE).
        setRAMBufferSizeMB(4)
    )

  private var reader = DirectoryReader.open(writer, true)
  private val lock = ReadWriteLockWrapper.allocate

  def write(handler: IndexWriter => Unit) {
    lock.read {
      handler(writer)
    }
    lock.write {
      val oldReader = reader
      Option(DirectoryReader.openIfChanged(oldReader)).foreach(
        newReader => {
          this.reader = newReader
          reopen(newReader)
          oldReader.close()
        }
      )
    }
  }

  protected def reopen(ir: IndexReader)

  reopen(reader)
}
