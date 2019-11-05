/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.instrumentation.cassandra.client

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import com.datastax.driver.core.{ExecutorQueueMetricsExtractor, Session}
import kamon.Kamon
import kamon.instrumentation.cassandra.Cassandra.samplingIntervalMillis
import kamon.metric._
import kamon.tag.TagSet

import scala.util.Try


object TargetResolver {
  def getTarget(address: InetAddress): String = address.getHostAddress
}

object ClientMetrics {

  val DmlStatementPrefixes = Set("select", "insert", "update", "delete")

  def poolBorrow(host: String): Histogram =
    Kamon.histogram("cassandra.client.pool-borrow-time", MeasurementUnit.time.nanoseconds)
      .withTag("target", host)

  def connections(host: String): Histogram =
    Kamon.histogram("cassandra.connection-pool.size").withTag("target", host)

  def trashedConnections(host: String): Histogram =
    Kamon.histogram("cassandra.trashed-connections").withTag("target", host)

  def inflightPerConnection: Histogram =
    Kamon.histogram("cassandra.client.inflight-per-connection").withoutTags()

  def inflightDriver(host: String): Histogram =
    Kamon.histogram("cassandra.client.inflight-per-target").withTag("target", host)




  def queryDuration: Histogram =
    Kamon.histogram("cassandra.client.query.duration", MeasurementUnit.time.nanoseconds).withoutTags()

  def queryCount: Counter =
    Kamon.counter("cassandra.client.query.count").withoutTags()

  def queryInflight(host: String): RangeSampler =
    Kamon.rangeSampler("cassandra.client.inflight").withTag("target", host)




  def errors(host: String): Counter =
    Kamon.counter("cassandra.query.errors").withTag("target", host)

  def timeouts(host: String): Counter =
    Kamon.counter("cassandra.query.timeouts").withTag("target", host)

  /*Here it would be more valuable to tag with host that's being retried or speculated on than
  * one defined by a policy so we are dropping it altogether */
  def retries: Counter =
    Kamon.counter("cassandra.query.retries").withoutTags()

  def speculative: Counter =
    Kamon.counter("cassandra.query.speculative").withoutTags()

  def cancelled: Counter =
    Kamon.counter("cassandra.query.cancelled").withoutTags()





  def recordQueryDuration(start: Long, end: Long, statementKind: Option[String]): Unit = {
    val statementTags = TagSet.of("statement.kind", statementKind.getOrElse("other"))
    queryDuration.withTags(statementTags).record(end - start)
    queryCount.withTags(statementTags).increment()
    queryInflight("ALL").decrement()
  }

  def from(session: Session): Unit = {
    import scala.collection.JavaConverters._

    Kamon.scheduler().scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        val state = session.getState

        ExecutorQueueMetricsExtractor.from(session, ExecutorQueueMetrics())

        state.getConnectedHosts.asScala.foreach { host =>
          val hostId = TargetResolver.getTarget(host.getAddress)
          val trashed = state.getTrashedConnections(host)
          val openConnections = state.getOpenConnections(host)
          val inflightCount = state.getInFlightQueries(host)

          trashedConnections(hostId).record(trashed)
          inflightDriver(hostId).record(inflightCount)
          connections(hostId).record(openConnections)
        }
      }
    }, samplingIntervalMillis, samplingIntervalMillis, TimeUnit.MILLISECONDS)
  }

  case class ExecutorQueueMetrics(executorQueueDepth: Gauge,
                                  blockingQueueDepth: Gauge,
                                  reconnectionTaskCount: Gauge,
                                  taskSchedulerTaskCount: Gauge)

  object ExecutorQueueMetrics {
    def apply(): ExecutorQueueMetrics = {
      val generalTags = TagSet.from(Map("component" -> "cassandra-client"))
      new ExecutorQueueMetrics(
        Kamon.gauge("cassandra.queue.executor").withTags(generalTags),
        Kamon.gauge("cassandra.queue.blocking").withTags(generalTags),
        Kamon.gauge("cassandra.queue.reconnection").withTags(generalTags),
        Kamon.gauge("cassandra.scheduled-tasks").withTags(generalTags))
    }
  }
}