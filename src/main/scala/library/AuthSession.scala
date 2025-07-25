package library

import java.time.LocalDateTime
import Types.UserID

/**
 * represents a user session when someone's logged into the system
 *
 * @param user the user who logged in
 * @param loginTime when they logged in
 */
case class AuthSession(user: User, loginTime: LocalDateTime):
  def userId: UserID = user.id
  def userName: String = user.name
  def userType: String = user match
    case User.Student(_, _, _, _)   => "Student"
    case User.Faculty(_, _, _, _)   => "Faculty"
    case User.Librarian(_, _, _, _) => "Librarian"

  def hasPermission(operation: String): Boolean = operation match
    case "loan" | "return" | "search" | "view_books" => true
    case "reserve" => user.canReserve
    case "add_book" | "add_user" | "waive_fees" | "list_users" | "view_transactions" | "remove_user" | "remove_book" | "view_statistics" => user.isLibrarian
    case _ => false

object AuthSession:
  def login(identifier: String, password: String, catalog: LibraryCatalog): Option[AuthSession] =
    // try to parse as UUID first (login by ID)
    val userOpt = scala.util.Try(java.util.UUID.fromString(identifier))
      .map(uuid => User.authenticate(UserID(uuid), password, catalog.users))
      .getOrElse(User.authenticateByName(identifier, password, catalog.users))

    userOpt.map(user => AuthSession(user, LocalDateTime.now))
