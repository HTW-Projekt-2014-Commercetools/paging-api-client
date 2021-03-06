package common.elasticsearch

import common.domain.{TypeName, IndexName}
import common.helper.Configloader
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.{ActionListener, ListenableActionFuture}
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.node.NodeBuilder
import org.elasticsearch.node.Node
import play.api.libs.json.JsValue

import scala.concurrent.{Promise, Future}

sealed trait ElasticsearchClient {
  implicit def convertListenableActionFutureToScalaFuture[T](x: ListenableActionFuture[T]): Future[T] = {
    val p = Promise[T]()
    x.addListener(new ActionListener[T] {
      def onFailure(e: Throwable) = p.failure(e)
      def onResponse(response: T) = p.success(response)
    })
    p.future
  }

  lazy val client = createElasticsearchClient()
  def close(): Unit
  def createElasticsearchClient(): Client

  def indexDocument(esIndex: IndexName, esType: TypeName, doc: JsValue): Future[IndexResponse] =
    client.prepareIndex(esIndex.value, esType.value).setSource(doc.toString()).execute()
  def search(esIndex: IndexName, esType: TypeName, query: QueryBuilder): Future[SearchResponse] =
    client.prepareSearch(esIndex.value).setTypes(esType.value).setQuery(query).execute()
}


object LocalEsClient extends ElasticsearchClient {
  lazy val node: Node = NodeBuilder.nodeBuilder().local(true).node()
  override def createElasticsearchClient(): Client = node.client()
  override def close() = node.close()
}

object RemoteEsClient extends ElasticsearchClient {
  val clustername = Configloader.getStringOpt("elasticsearch.clustername").getOrElse("elasticsearch")
  val hosts = readHostsFromConfig(Configloader.getStringSeq("elasticsearch.hosts").getOrElse(Nil))
  lazy val node: Node = NodeBuilder.nodeBuilder()
    .clusterName(clustername)
    .client(true)
    .settings(ImmutableSettings.settingsBuilder()
      .put("discovery.zen.ping.unicast.hosts", hosts.map(hp => s"${hp.host}:${hp.port}").mkString(",")))
    .node()

  override def createElasticsearchClient(): Client = node.client()
  override def close() = node.close()

  def readHostsFromConfig(configString: List[String]): List[HostWithPort] = {
    configString.map {
      s =>
        val hostAndPortArray = s.split(":")
        HostWithPort(hostAndPortArray(0), hostAndPortArray(1).toInt)
    }
  }
}

case class HostWithPort(host: String, port: Int)