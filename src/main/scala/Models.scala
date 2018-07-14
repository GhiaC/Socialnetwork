import scalikejdbc.interpolation.SQLSyntax

final case class Post(username: String, tweet: String, created_at: String)

final case class users(username: String, password: String)

final case class LoginRequest(username: String, password: String)

final case class RegisterRequest(username: String, password: String)

final case class SendTweetRequest(cookie: String, text: String)

final case class tweets(user_id: Int, tweet: String, created_at: SQLSyntax)

final case class Posts(items: List[Post]) {}