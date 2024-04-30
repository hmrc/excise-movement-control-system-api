package uk.gov.hmrc.excisemovementcontrolsystemapi.repository

import org.apache.pekko.Done
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import play.api.Logging
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ErnRetrievalRepository.mongoIndexes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ErnRetrieval
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErnRetrievalRepository @Inject()
(mongo: MongoComponent,
 appConfig: AppConfig,
 timeService: DateTimeService)(implicit ec: ExecutionContext) extends
  PlayMongoRepository[ErnRetrieval](
    collectionName = "ern-retrievals",
    mongoComponent = mongo,
    domainFormat = ErnRetrieval.format,
    indexes = mongoIndexes(appConfig.ernRetrievalTTL),
    replaceIndexes = true
  ) with Logging {

  def retrieve(ern: String): Future[Option[ErnRetrieval]] = ???

  def save(ern: String): Future[Done] = ???
}

object ErnRetrievalRepository {
  def mongoIndexes(ttl: Duration): Seq[IndexModel] = {
    Seq(
      IndexModel(
        Indexes.ascending("lastRetrieved"),
        IndexOptions()
          .name("lastRetrieved_ttl_index")
          .expireAfter(ttl.toSeconds, TimeUnit.SECONDS)
      ),
      IndexModel(
        Indexes.ascending("ern"),
        IndexOptions()
          .name("ern_index")
          .unique(true)
      )
    )
  }
}
