
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.io.{Codec, Source, StdIn}
import scala.concurrent.Future

object WebServer {

  // needed to run the route
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext = system.dispatcher

  // formats for unmarshalling and marshalling
  implicit val loginRequest = jsonFormat2(LoginRequest)
  implicit val registerRequest = jsonFormat2(RegisterRequest)
  implicit val sendTweetRequest = jsonFormat2(SendTweetRequest)

  val db = new database

  def fetchItem(itemId: Long): Future[Option[String]] = Future {
    Some(db.getTweets(0))
  }

  def Login(loginRequest: LoginRequest): Future[Option[String]] = Future {
    Some(db.login(loginRequest))
  }

  def Register(registerRequest: RegisterRequest): Future[Option[String]] = Future {
    Some(db.register(registerRequest))
  }

  def SendTweet(sendTweetRequest: SendTweetRequest): Future[Option[String]] = Future {
    Some(db.tweet(sendTweetRequest))
  }

  def main(args: Array[String]) {
    java.nio.charset.Charset.availableCharsets()
    Codec("UTF8")
    db.initialTables()
    val route: Route =
    //        get {
    //          entity(as[HttpRequest]) { requestData =>
    //            var result: String = ""
    //            //          (path("index.html") & respondWithHeader(RawHeader("myheader", "myvalue"))) {
    //            val uri = requestData.uri.toString().split("http://localhost:8080/")
    //            val filename = "src/main/resource/".concat(uri(1))
    //            val err404 = new java.io.File(filename).exists
    //            if (err404 && !filename.contains("..")) {
    //              for (line <- Source.fromFile(filename, enc="UTF8").getLines) {
    //                result = result.concat(line).concat("\n")
    //              }
    //              complete(HttpResponse(entity = HttpEntity(
    //                ContentTypes.`text/html(UTF-8)`,
    //                result)))
    //            }
    //            else {
    //              complete(StatusCodes.NotFound)
    //            }
    //          }
    //        } ~
      get {
        pathEndOrSingleSlash {
          getFromResource("index.html")
        } ~ {
          getFromResourceDirectory("")
        }
      }~
      pathPrefix("api") {
        get {
          path("getTweets") {
            // there might be no item for a given id

            val maybeItem: Future[Option[String]] = fetchItem(2)
            onSuccess(maybeItem) {
              case Some(item) => complete(item)
              case _ => complete(StatusCodes.NotFound)
            }
          }
        } ~ post {
          path("login") {
            entity(as[LoginRequest]) { order =>
              val login: Future[Option[String]] = Login(order)
              onSuccess(login) {
                case Some(item) => complete(item)
                case _ => complete(StatusCodes.NotFound)
              }
            }
          }
        } ~
          post {
            path("register") {
              entity(as[RegisterRequest]) { order =>
                val login: Future[Option[String]] = Register(order)
                onSuccess(login) {
                  case Some(item) => complete(item)
                  case _ => complete(StatusCodes.NotFound)
                }
              }
            }

          } ~
          post {
            path("sendTweet") {
              entity(as[SendTweetRequest]) { order =>
                val login: Future[Option[String]] = SendTweet(order)
                onSuccess(login) {
                  case Some(item) => complete(item)
                  case _ => complete(StatusCodes.NotFound)
                }
              }
            }
          }
      }


    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done

  }
}