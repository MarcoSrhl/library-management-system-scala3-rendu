package library

import java.util.UUID
import java.time.{LocalDateTime, Duration}
import java.time.temporal.ChronoUnit
import Types.UserID

/**
 * user system for the library app
 * 
 * using scala 3 enums instead of sealed traits because its cleaner,
 * represents different types of library users with their permissions
 * 
 * basically we have:
 * - students: can borrow 5 books max for 30 days
 * - faculty: 10 books max for 90 days 
 * - librarians: unlimited everything
 * 
 * using extension methods so we dont repeat code everywhere,
 * all users get the same basic methods
 * 
 * type safe with opaque UserIDs and immutable so no surprises
 */
enum User:

  // student with their major
  case Student(id: UserID, name: String, major: String, password: String) extends User

  // faculty member with their department
  case Faculty(id: UserID, name: String, department: String, password: String) extends User

  // librarian with employee ID
  case Librarian(id: UserID, name: String, employeeId: String, password: String) extends User

object User:

  // extension methods to get common stuff from all user types
  extension (u: User)
    def id: UserID = u match
      case Student(id, _, _, _)     => id
      case Faculty(id, _, _, _)     => id
      case Librarian(id, _, _, _)   => id

    def name: String = u match
      case Student(_, name, _, _)     => name
      case Faculty(_, name, _, _)     => name
      case Librarian(_, name, _, _)   => name

    def password: String = u match
      case Student(_, _, _, password)     => password
      case Faculty(_, _, _, password)     => password
      case Librarian(_, _, _, password)   => password

    def isStudent: Boolean = u match
      case Student(_, _, _, _) => true
      case _                   => false

    def isFaculty: Boolean = u match
      case Faculty(_, _, _, _) => true
      case _                   => false

    def isLibrarian: Boolean = u match
      case Librarian(_, _, _, _) => true
      case _                     => false

    // max books they can borrow based on user type
    def maxLoans: Int = u match
      case Student(_, _, _, _)   => 5
      case Faculty(_, _, _, _)   => 10
      case Librarian(_, _, _, _) => Int.MaxValue // unlimited for librarians

    // how long they can keep a book
    def loanPeriod: Duration = u match
      case Student(_, _, _, _)   => Duration.ofDays(30)  // 1 month
      case Faculty(_, _, _, _)   => Duration.ofDays(90)  // 3 months
      case Librarian(_, _, _, _) => Duration.ofDays(365 * 100) // basically unlimited

    // whether they can reserve books
    def canReserve: Boolean = u match
      case Student(_, _, _, _)   => true
      case Faculty(_, _, _, _)   => true
      case Librarian(_, _, _, _) => false // librarians dont need to reserve

    // calculate return date based on user type
    def calculateDueDate(loanDate: LocalDateTime): LocalDateTime = u match
      case Student(_, _, _, _)   => loanDate.plus(30, ChronoUnit.DAYS)
      case Faculty(_, _, _, _)   => loanDate.plus(90, ChronoUnit.DAYS)
      case Librarian(_, _, _, _) => loanDate.plus(365 * 100, ChronoUnit.DAYS) // basically no due date

    // late fees per day
    def overdueFeeRate: Double = u match
      case Student(_, _, _, _)   => 0.50  // standard rate
      case Faculty(_, _, _, _)   => 0.25  // discount rate
      case Librarian(_, _, _, _) => 0.0   // no fees for staff

  // authentication stuff
  def authenticate(userId: UserID, password: String, users: Map[UserID, User]): Option[User] =
    users.get(userId).filter(_.password == password)

  def authenticateByName(name: String, password: String, users: Map[UserID, User]): Option[User] =
    users.values.find(user => user.name.equalsIgnoreCase(name) && user.password == password)
