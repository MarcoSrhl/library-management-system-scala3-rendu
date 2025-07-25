package library

import java.time.LocalDateTime
import Types.{ISBN, UserID}
import TypeClasses.{given, *}
import scala.annotation.targetName
import scala.reflect.ClassTag

/**
 * Extensions.scala - Collection and Domain-Specific Extension Methods
 * 
 * This file provides extension methods that enhance collection types and domain objects
 * with library-specific functionality. Extensions allow adding methods to existing types
 * without modifying their source code, providing a clean and readable API.
 * 
 * Key Features:
 * - Collection Extensions: Enhanced operations for Lists of Books, Users, Transactions
 * - Filtering Operations: Domain-specific filtering methods (availableOnly, byGenre, etc.)
 * - Grouping Operations: Methods to group and categorize domain objects
 * - Summary Operations: Methods to generate summaries and statistics
 * - Type-Safe Operations: Using ClassTag for runtime type checking
 * 
 * The file demonstrates advanced Scala 3 features:
 * - Extension methods for enhanced APIs
 * - Target name annotations for method overloading
 * - Integration with type classes through given instances
 * - Generic programming with ClassTag
 * - Functional collection operations
 * 
 * Extensions Provided:
 * - List[Book]: Filtering, sorting, grouping, and summary operations
 * - List[User]: User type filtering and statistics
 * - List[Transaction]: Transaction analysis and filtering
 * - Individual domain objects: Enhanced display and manipulation
 * 
 * Usage:
 * - Import Extensions.* to enable all extension methods
 * - Use natural syntax: books.availableOnly, users.students, etc.
 * - Chain operations for complex queries
 * - Generate summaries with displaySummary methods
 */
object Extensions:

  // Extension methods for Book collections
  extension (books: List[Book])
    def availableOnly: List[Book] = books.filter(_.isAvailable)
    def onLoanOnly: List[Book] = books.filterNot(_.isAvailable)
    def byGenre(genre: String): List[Book] = books.filter(_.genre.toLowerCase.contains(genre.toLowerCase))
    def byAuthor(author: String): List[Book] = books.filter(_.authors.exists(_.toLowerCase.contains(author.toLowerCase)))
    def publishedAfter(year: Int): List[Book] = books.filter(_.publicationYear >= year)
    def publishedBefore(year: Int): List[Book] = books.filter(_.publicationYear <= year)
    def sortedByTitle: List[Book] = books.sortBy(_.title)
    def sortedByYear: List[Book] = books.sortBy(_.publicationYear)
    def groupedByGenre: Map[String, List[Book]] = books.groupBy(_.genre)
    @targetName("bookDisplaySummary")
    def displaySummary: String =
      val total = books.length
      val available = books.count(_.isAvailable)
      val onLoan = total - available
      val genres = books.map(_.genre).distinct.size
      s"Books Summary: $total total, $available available, $onLoan on loan, $genres genres"

  // Extension methods for User collections
  extension (users: List[User])
    def students: List[User] = users.filter(_.isStudent)
    def faculty: List[User] = users.filter(_.isFaculty)
    def librarians: List[User] = users.filter(_.isLibrarian)
    def byType[T <: User](using ClassTag[T]): List[T] = users.collect { case u: T => u }
    def canReserve: List[User] = users.filter(_.canReserve)
    @targetName("userDisplaySummary")
    def displaySummary: String =
      val studentCount = users.count(_.isStudent)
      val facultyCount = users.count(_.isFaculty)
      val librarianCount = users.count(_.isLibrarian)
      s"Users Summary: ${users.length} total ($studentCount students, $facultyCount faculty, $librarianCount librarians)"

  // Extension methods for Transaction collections
  extension (transactions: List[Transaction])
    def loans: List[Transaction.Loan] = transactions.collect { case l: Transaction.Loan => l }
    def returns: List[Transaction.Return] = transactions.collect { case r: Transaction.Return => r }
    def reservations: List[Transaction.Reservation] = transactions.collect { case r: Transaction.Reservation => r }
    def forUser(userId: UserID): List[Transaction] = transactions.filter(_.user.id == userId)
    def forBook(isbn: ISBN): List[Transaction] = transactions.filter(_.book.isbn == isbn)
    def recent(days: Int): List[Transaction] = 
      val cutoff = LocalDateTime.now.minusDays(days)
      transactions.filter(_.timestamp.isAfter(cutoff))
    @targetName("transactionDisplaySummary")
    def displaySummary: String =
      val loanCount = loans.length
      val returnCount = returns.length
      val reservationCount = reservations.length
      s"Transactions Summary: ${transactions.length} total ($loanCount loans, $returnCount returns, $reservationCount reservations)"

  // Extension methods for LibraryCatalog
  extension (catalog: LibraryCatalog)
    def totalBooks: Int = catalog.books.size
    def totalUsers: Int = catalog.users.size
    def totalTransactions: Int = catalog.transactions.length
    def availableBooks: List[Book] = catalog.books.values.toList.availableOnly
    def booksOnLoan: List[Book] = catalog.books.values.toList.onLoanOnly
    def activeUsers: List[User] = 
      val activeUserIds = catalog.transactions.recent(30).map(_.user.id).distinct
      catalog.users.values.filter(user => activeUserIds.contains(user.id)).toList
    def displayStats: String =
      s"""Library Statistics:
         |${catalog.books.values.toList.displaySummary}
         |${catalog.users.values.toList.displaySummary}
         |${catalog.transactions.displaySummary}
         |Active users (last 30 days): ${activeUsers.length}""".stripMargin

  // Extension methods for validation and error handling
  extension (isbn: String)
    def toValidISBN: Either[String, ISBN] = Types.createValidatedISBN(isbn)
    def isValidISBN: Boolean = toValidISBN.isRight

  extension (userId: String)
    def toValidUserID: Either[String, UserID] = Types.createValidatedUserID(userId)
    def isValidUserID: Boolean = toValidUserID.isRight

  extension (title: String)
    def toValidTitle: Either[String, Types.BookTitle] = Types.createValidatedTitle(title)
    def isValidTitle: Boolean = toValidTitle.isRight

  // Extension methods for Option and Either types (functional error handling)
  extension [T](option: Option[T])
    def orElseThrow(message: String): T = option.getOrElse(throw new RuntimeException(message))
    def toEither(error: String): Either[String, T] = option.toRight(error)

  extension [L, R](either: Either[L, R])
    def getOrElseThrow: R = either.fold(
      left => throw new RuntimeException(s"Operation failed: $left"),
      right => right
    )
    def logError: Either[L, R] = either match
      case Left(error) => 
        println(s"Error: $error")
        either
      case right => right

  // Extension methods for displaying collections using type classes
  extension (books: List[Book])(using displayable: Displayable[Book])
    @targetName("booksDisplayAll")
    def displayAll: Unit = books.foreach(book => println(displayable.display(book)))
    @targetName("booksDisplayDetailed")
    def displayDetailed: Unit = books.foreach(book => println(displayable.displayDetailed(book)))

  extension (users: List[User])(using displayable: Displayable[User])
    @targetName("usersDisplayAll")
    def displayAll: Unit = users.foreach(user => println(displayable.display(user)))
    @targetName("usersDisplayDetailed")
    def displayDetailed: Unit = users.foreach(user => println(displayable.displayDetailed(user)))

  extension (transactions: List[Transaction])(using displayable: Displayable[Transaction])
    @targetName("transactionsDisplayAll")
    def displayAll: Unit = transactions.foreach(tx => println(displayable.display(tx)))
    @targetName("transactionsDisplayDetailed")
    def displayDetailed: Unit = transactions.foreach(tx => println(displayable.displayDetailed(tx)))

  // Extension methods for search and filtering with similarity
  extension (books: List[Book])(using similarity: Similarity[Book])
    def findSimilarTo(target: Book, threshold: Double = 0.7): List[Book] =
      books.filter(book => similarity.similarity(book, target) >= threshold).sortBy(book => similarity.similarity(book, target)).reverse
    
    def mostSimilarTo(target: Book): Option[Book] =
      if books.isEmpty then None
      else Some(books.maxBy(book => similarity.similarity(book, target)))

  // Extension methods for validation
  extension (book: Book)(using validatable: Validatable[Book])
    def validateAndLog: Book = validatable.validate(book) match
      case Right(validBook) => validBook
      case Left(error) =>
        println(s"Book validation error: $error")
        book

  extension (user: User)(using validatable: Validatable[User])
    def validateAndLog: User = validatable.validate(user) match
      case Right(validUser) => validUser
      case Left(error) =>
        println(s"User validation error: $error")
        user

  // Utility functions for creating instances with validation
  def createBookSafely(isbn: String, title: String, authors: List[String], year: Int, genre: String): Either[String, Book] =
    for
      validISBN <- isbn.toValidISBN
      validTitle <- title.toValidTitle
      validGenre <- genre.toValidTitle.map(_.value) // Reuse title validation for genre
    yield Book(validISBN, validTitle.value, authors, year, validGenre, isAvailable = true)

  def createUserSafely(name: String, password: String, userType: String, extraInfo: String): Either[String, User] =
    if name.trim.isEmpty then Left("Name cannot be empty")
    else if password.length < 6 then Left("Password must be at least 6 characters")
    else
      val userId = Types.UserID.random()
      userType.toLowerCase match
        case "student" => Right(User.Student(userId, name, extraInfo, password))
        case "faculty" => Right(User.Faculty(userId, name, extraInfo, password))
        case "librarian" => Right(User.Librarian(userId, name, extraInfo, password))
        case _ => Left(s"Invalid user type: $userType")

  // Union type helpers
  type SearchResult = List[Book] | String  // Either books found or error message
  
  def searchBooksUnion(catalog: LibraryCatalog, query: String): SearchResult =
    if query.trim.isEmpty then
      "Search query cannot be empty"
    else
      val results = catalog.books.values.toList.filter(book =>
        book.title.toLowerCase.contains(query.toLowerCase) ||
        book.authors.exists(_.toLowerCase.contains(query.toLowerCase)) ||
        book.genre.toLowerCase.contains(query.toLowerCase)
      )
      if results.nonEmpty then results else "No books found matching your search"
  
  // Pattern matching helper for SearchResult
  def handleSearchResult(result: SearchResult): Unit = result match
    case books: List[Book] => 
      println(s"Found ${books.length} books:")
      books.take(10).foreach(book => println(s"  - ${book.display}"))
      if books.length > 10 then println(s"  ... and ${books.length - 10} more")
    case error: String => 
      println(s"Search failed: $error")
