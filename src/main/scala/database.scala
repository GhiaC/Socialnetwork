import scalikejdbc.{AutoSession, ConnectionPool, DB, using}
import scalikejdbc._
import scala.collection.mutable
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

class database {
  Class.forName("org.postgresql.Driver")
  ConnectionPool.singleton("jdbc:postgresql://localhost:5432/learn", "postgres", "masoud")
  private implicit val session = AutoSession

  val settings = ConnectionPoolSettings(
    initialSize = 50,
    maxSize = 200,
    connectionTimeoutMillis = 3000L,
    validationQuery = "select 1 from users")

  object tweets extends SQLSyntaxSupport[tweets] {}

  object users extends SQLSyntaxSupport[users] {}

  def initialTables(): Unit = {
    using(ConnectionPool.borrow()) { conn: java.sql.Connection =>
      val db: DB = DB(conn)
      db.autoClose(false)

      db.localTx { implicit session =>
        sql"""
              create table IF NOT EXISTS users (
                   id serial not null primary key,
                   username varchar(64),
                   password varchar(64),
                   cookie varchar(64),
                   unique(cookie),
                   unique(username)
              )
             """.execute.apply()

        sql"""
             create table IF NOT EXISTS tweets (
             id serial not null primary key,
             user_id serial not null,
             tweet text,
             FOREIGN KEY (user_id) REFERENCES users(id),
              created_at timestamp not null);
    """.execute.apply()
      } // localTx won't close the current Connection
    }
  }

  private def insertData(data: users)(implicit session: DBSession = AutoSession) = withSQL {
    val t = users.column
    insert.into(users).namedValues(
      t.username -> data.username,
      t.password -> data.password
    )
  }.update().apply()

  def login(loginRequest: LoginRequest): String = {
    using(ConnectionPool.borrow()) { conn: java.sql.Connection =>
      var A: mutable.Set[Map[String, String]] = mutable.Set()
      val db: DB = DB(conn)
      db.autoClose(true)
      val cookie = new scala.util.Random(new java.security.SecureRandom()).toString

      //      val cookie = "hashstir13212423ng"
      val idOnly = (rs: WrappedResultSet) => rs.string("id")
      val isUserExist = DB readOnly { implicit session =>
        sql"""
                   (SELECT id FROM users WHERE username = ${loginRequest.username} and password = ${loginRequest.password})
              """.map(idOnly).single().apply()
      }
      if (isUserExist.isEmpty) {
        A = A.++(Set(Map("result" -> "true", "error" -> "sername or password is false")))
      }
      else {
        sql"""
          UPDATE users set cookie = ${cookie} where username = ${loginRequest.username} and password = ${loginRequest.password}
          """.update.apply()
        A = A.++(Set(Map("result" -> "true", "username" -> loginRequest.username, "cookie" -> cookie)))
      }
      A.asJson.noSpaces
    }
  }

  def register(registerRequest: RegisterRequest): String = { //register
    var A: mutable.Set[Map[String, String]] = mutable.Set()
    using(ConnectionPool.borrow()) { conn: java.sql.Connection =>
      val db: DB = DB(conn)
      db.autoClose(true)

      // define a class to map the result
      val idOnly = (rs: WrappedResultSet) => rs.string("id")

      val isUserExist = DB readOnly { implicit session =>
        sql"select id from users where username = ${registerRequest.username}"
          .map(idOnly).single.apply()
      }
      if (isUserExist.isEmpty) {
        insertData(new users(registerRequest.username, registerRequest.password))
        A = A.++(Set(Map("result" -> "true")))
      }
      else
        A = A.++(Set(Map("result" -> "false", "error" -> "نام کاربری موجد است")))
    }
    A.asJson.noSpaces
  }

  def tweet(sendTweetRequest: SendTweetRequest): String = {
    using(ConnectionPool.borrow()) { conn: java.sql.Connection =>
      var A: mutable.Set[Map[String, String]] = mutable.Set()

      val db: DB = DB(conn)
      db.autoClose(true)

      // define a class to map the result
      val idOnly = (rs: WrappedResultSet) => rs.int("id")

      val isUserExist = DB readOnly { implicit session =>
        sql"SELECT id FROM users WHERE cookie = ${sendTweetRequest.cookie}"
          .map(idOnly).single.apply()
      }
      if (!isUserExist.isEmpty) {
        insertTweet(new tweets(isUserExist.get, sendTweetRequest.text, sqls.currentTimestamp))
        A = A.++(Set(Map("result" -> "true")))
      }
      else
        A = A.++(Set(Map("result" -> "false", "error" -> "دوباره لاگین کنید")))
      A.asJson.noSpaces
    }
  }

  private def insertTweet(data: tweets)(implicit session: DBSession = AutoSession)
  = withSQL {
    val t = tweets.column
    insert.into(tweets).namedValues(
      t.user_id -> data.user_id,
      t.tweet -> data.tweet,
      t.created_at -> sqls.currentTimestamp
    )
  }.update().apply()

  def getTweets(id: Long): String = using(ConnectionPool.borrow()) { conn: java.sql.Connection =>
    var A: mutable.Set[Map[String, String]] = mutable.Set()
    val db: DB = DB(conn)
    db.autoClose(true)
    val posts = (rs: WrappedResultSet) => {
      Post(rs.string("username"), rs.string("tweet"), rs.string("created_at"))
    }
    DB readOnly { implicit session =>
      sql"select username , tweet, created_at from tweets tw inner join users us on us.id = tw.user_id  ORDER BY created_at DESC;"
        .foreach {
          rs =>
            val e = posts(rs)
            val set = mutable.Set(Map(
              "username" -> e.username
              , "tweet" -> e.tweet
              , "date" -> e.created_at)
            )
            A = A.++(set)
        }
    }
    return A.asJson.noSpaces
  }
}