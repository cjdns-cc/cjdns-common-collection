package cjdns.cjdns_music.music

import org.apache.lucene.index.{IndexReader, MultiReader}
import org.apache.lucene.search.IndexSearcher
import cjdns.util.collection.SyncRef
import cjdns.cjdns_music.index.Collector
import java.io.{IOException, File}
import org.apache.lucene.store.NIOFSDirectory

/**
 * User: willzyx
 * Date: 01.07.13 - 0:43
 */
object Index {
  val REF = new SyncRef[IndexSearcher](new IndexSearcher(new MultiReader))

  private val LOCK = new Object
  private var LOCAL_R: IndexReader = null

  private def reopen() {
    REF.getAndSet {
      new IndexSearcher(
        new MultiReader(
          Array(LOCAL_R).
            filter(_ != null),
          false
        )
      )
    }
  }

  /* LOCAL */

  val LOCAL_COLLECTOR = {
    val PATH = new File("/tmp/music_index")
    if (!PATH.exists && !PATH.mkdirs) {
      throw new IOException
    }
    new Collector(new NIOFSDirectory(PATH)) {
      protected def reopen(ir: IndexReader) {
        LOCK.synchronized {
          LOCAL_R = ir
          Index.reopen()
        }
      }
    }
  }

}
