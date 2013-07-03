package cjdns.cjdns_music.music

import org.apache.lucene.index._
import org.apache.lucene.search.IndexSearcher
import cjdns.util.collection.SyncRef
import java.io.{IOException, File}
import org.apache.lucene.store.NIOFSDirectory
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.core.SimpleAnalyzer
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import java.util.concurrent.atomic.AtomicReference
import java.util.{TimerTask, Timer}
import scala.concurrent.duration._
import org.slf4j.LoggerFactory
import util.Try

/**
 * User: willzyx
 * Date: 01.07.13 - 0:43
 */
object DirtyIndex {
  val log = LoggerFactory.getLogger("dirty-index")

  private val PATH = new File("/tmp/music_index")
  if (!PATH.exists && !PATH.mkdirs) {
    throw new IOException
  }

  val DIRTY_WRITER =
    new IndexWriter(
      new NIOFSDirectory(PATH),
      new IndexWriterConfig(Version.LUCENE_43, new SimpleAnalyzer(Version.LUCENE_43)).
        setOpenMode(OpenMode.CREATE).
        setRAMBufferSizeMB(4)
    )

  private val DIRTY_READER = new AtomicReference[DirectoryReader](DirectoryReader.open(DIRTY_WRITER, true))

  private def searcher =
    new IndexSearcher(
      new MultiReader(DIRTY_READER.get)
    )

  val SEARCHER_REF = new SyncRef[IndexSearcher](searcher)

  new Timer("dirty-indexer", true).schedule(
    new TimerTask {
      def run() {
        Try {
          val readers =
            List(
              Option(DirectoryReader.openIfChanged(DIRTY_READER.get)).map(DIRTY_READER.getAndSet)
            ) collect {
              case Some(reader) => reader
            }
          if (!readers.isEmpty) {
            SEARCHER_REF.getAndSet(searcher)
            readers.foreach(_.close())
          }
        } recover {
          case e: Exception => log.error("failure", e)
        }
      }
    },
    5.seconds.toMillis, 5.seconds.toMillis
  )

}
