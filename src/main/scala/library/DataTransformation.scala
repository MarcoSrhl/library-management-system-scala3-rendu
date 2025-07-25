package library

import library.Types.*
import cats.syntax.all.*
import io.circe.*
import io.circe.syntax.*

// report data structure for library stats
case class LibraryReport(
  totalBooks: Int,
  availableBooks: Int,
  loanedBooks: Int,
  totalUsers: Int,
  activeLoans: Int,
  totalReservations: Int,
  booksByGenre: Map[String, Int],
  usersByType: Map[String, Int]
)

// transforms data for reports and analytics stuff
object DataTransformation:

  // generates a comprehensive library report
  def generateLibraryReport(catalog: LibraryCatalog): LibraryReport =
    val books = catalog.books.values.toList
    val users = catalog.users.values.toList
    
    val availableCount = books.count(_.isAvailable)
    val loanedCount = books.length - availableCount
    
    // simple book categorization by first letter
    val booksByGenre = books.groupBy(_.title.take(1)).view.mapValues(_.length).toMap
    
    // group users by their type
    val usersByType = users.groupBy {
      case User.Student(_, _, _, _) => "Student" 
      case User.Faculty(_, _, _, _) => "Faculty"
      case User.Librarian(_, _, _, _) => "Librarian"
    }.view.mapValues(_.length).toMap
    
    LibraryReport(
      totalBooks = books.length,
      availableBooks = availableCount,
      loanedBooks = loanedCount,
      totalUsers = users.length,
      activeLoans = loanedCount, // simple approximation
      totalReservations = 0, // no reservation system yet
      booksByGenre = booksByGenre,
      usersByType = usersByType
    )

  // gets the most popular books based on current loans
  def getMostPopularBooks(catalog: LibraryCatalog, limit: Int): List[(String, Int)] =
    val books = catalog.books.values.toList
    
    // for now just return borrowed books as "popular"
    books
      .filterNot(_.isAvailable)
      .groupBy(_.title)
      .view
      .mapValues(_.length)
      .toList
      .sortBy(-_._2)
      .take(limit)

  // gets the most active users based on borrowed books
  def getMostActiveUsers(catalog: LibraryCatalog, limit: Int): List[(String, Int)] =
    val users = catalog.users.values.toList
    val transactions = catalog.transactions
    
    // count loan transactions per user
    val borrowCounts = transactions
      .collect { case Transaction.Loan(_, user, _, _) => user }
      .groupBy(_.id)
      .view
      .mapValues(_.length)
      .toMap
    
    users
      .map(user => (user.name, borrowCounts.getOrElse(user.id, 0)))
      .sortBy(-_._2)
      .take(limit)
