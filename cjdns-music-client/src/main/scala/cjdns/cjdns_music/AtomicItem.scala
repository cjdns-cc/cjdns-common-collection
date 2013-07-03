package cjdns.cjdns_music

/**
 * User: willzyx
 * Date: 03.07.13 - 3:15
 */
trait AtomicItem {
  def id: String
}

object AtomicItem {

  class WMusicRecord(val base: Model.MusicRecord) extends AtomicItem with Comparable[WMusicRecord] {
    def id: String = base.getId

    def compareTo(r: WMusicRecord): Int = {
      if (r.id == this.id) 0
      else if (r.base.getWeight < this.base.getWeight) -1 else 1
    }

    override def hashCode = id.hashCode

    override def equals(obj: Any): Boolean =
      obj != null &&
        obj.isInstanceOf[WMusicRecord] &&
        obj.asInstanceOf[WMusicRecord].id == this.id
  }

  object WMusicRecord {
    def apply(base: Model.MusicRecord) = new WMusicRecord(base)
  }

}
