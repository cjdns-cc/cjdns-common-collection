package cjdns.cjdns_music

import com.sleepycat.je.{Durability, EnvironmentConfig, Environment}
import java.io.File

/**
 * User: willzyx
 * Date: 27.06.13 - 0:24
 */
object Storage {
  val config = properties.getConfig("storage")
  val root = new File(config.getString(config.getString("path")))
  val configuration = new EnvironmentConfig
  configuration.setAllowCreate(true)
  configuration.setTransactional(false)
  configuration.setCachePercent(5)
  configuration.setDurability(Durability.COMMIT_SYNC)
  val environment = new Environment(root, configuration)
}
