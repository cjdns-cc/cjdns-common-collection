package cjdns.util.fs

import java.io.File
import annotation.tailrec
import util.Random

/**
 * User: willzyx
 * Date: 26.06.13 - 23:38
 */
class FSIterator(root: File) extends Iterator[File] {
  private var list = Iterator(root) :: List.empty

  @tailrec
  private def getNext: File = {
    if (list.isEmpty) null
    else if (list.head.hasNext) {
      val file = list.head.next()
      if (file.isDirectory) {
        list = Random.shuffle(file.listFiles.toList).toIterator :: list
        getNext
      } else if (file.isFile) {
        file
      } else getNext
    } else {
      list = list.tail
      getNext
    }
  }

  private var file = getNext

  def hasNext = this.file != null

  def next() = {
    val out = this.file
    this.file = getNext
    out
  }
}
