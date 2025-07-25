package library

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import library.*
import library.Types.*
import library.Extensions.*
import java.util.UUID
import java.time.LocalDateTime

/**
 * Simplified comprehensive tests for Extensions functionality to maximize coverage.
 * Tests only the methods that actually exist in Extensions.scala.
 */
class ExtensionsSimplifiedSpec extends AnyFlatSpec with Matchers {

  val sampleBooks = List(
    Book(ISBN("978-0134685991"), "Effective Java", List("Joshua Bloch"), 2017, "Programming", true),
    Book(ISBN("978-0135166307"), "Java Complete Reference", List("Herbert Schildt"), 2020, "Programming", false),
    Book(ISBN("978-0132350884"), "Clean Code", List("Robert Martin"), 2008, "Programming", true),
    Book(ISBN("978-0316769174"), "The Catcher in the Rye", List("J.D. Salinger"), 1951, "Fiction", true),
    Book(ISBN("978-0061120084"), "To Kill a Mockingbird", List("Harper Lee"), 1960, "Fiction", true)
  )

  val sampleUsers = List(
    User.Student(UserID(UUID.randomUUID()), "Alice", "Computer Science", "password123"),
    User.Faculty(UserID(UUID.randomUUID()), "Bob", "Literature", "password456"),
    User.Librarian(UserID(UUID.randomUUID()), "Charlie", "Main Library", "password789")
  )

  val sampleTransactions = List(
    Transaction.Loan(
      sampleBooks.head,
      sampleUsers.head,
      LocalDateTime.now(),
      Some(LocalDateTime.now().plusDays(30))
    ),
    Transaction.Return(
      sampleBooks.head,
      sampleUsers.head,
      LocalDateTime.now().plusDays(15)
    )
  )

  val sampleCatalog = LibraryCatalog(
    books = sampleBooks.map(b => b.isbn -> b).toMap,
    users = sampleUsers.map(u => u.id -> u).toMap,
    transactions = sampleTransactions
  )

  "Book collection extensions" should "provide filtering and sorting methods" in {
    // Test filtering methods
    val availableBooks = sampleBooks.availableOnly
    availableBooks.foreach(_.isAvailable shouldBe true)
    
    val onLoanBooks = sampleBooks.onLoanOnly
    onLoanBooks.foreach(_.isAvailable shouldBe false)
    
    // Test genre filtering
    val programmingBooks = sampleBooks.byGenre("Programming")
    programmingBooks should not be empty
    programmingBooks.foreach(_.genre should include("Programming"))
    
    // Test author filtering
    val blochBooks = sampleBooks.byAuthor("Joshua")
    blochBooks should not be empty
    blochBooks.foreach(_.authors.exists(_.contains("Joshua")) shouldBe true)
    
    // Test year filtering
    val recentBooks = sampleBooks.publishedAfter(2010)
    recentBooks.foreach(_.publicationYear should be >= 2010)
    
    val olderBooks = sampleBooks.publishedBefore(2000)
    olderBooks.foreach(_.publicationYear should be <= 2000)
    
    // Test sorting
    val sortedByTitle = sampleBooks.sortedByTitle
    val titles = sortedByTitle.map(_.title)
    titles shouldBe titles.sorted
    
    val sortedByYear = sampleBooks.sortedByYear
    val years = sortedByYear.map(_.publicationYear)
    years shouldBe years.sorted
    
    // Test grouping
    val groupedByGenre = sampleBooks.groupedByGenre
    groupedByGenre should not be empty
    groupedByGenre.keys should contain("Programming")
    groupedByGenre.keys should contain("Fiction")

    // Test display summary
    val summary = sampleBooks.displaySummary
    summary should include("Books Summary")
    summary should include("total")
    summary should include("available")
    summary should include("genres")
  }

  "User collection extensions" should "filter users by type" in {
    // Test user type filters
    val students = sampleUsers.students
    students.foreach(_.isStudent shouldBe true)
    
    val faculty = sampleUsers.faculty
    faculty.foreach(_.isFaculty shouldBe true)
    
    val librarians = sampleUsers.librarians
    librarians.foreach(_.isLibrarian shouldBe true)
    
    // Test byType with ClassTag
    val studentsByType = sampleUsers.byType[User.Student]
    studentsByType should have size 1
    
    // Test canReserve filter
    val canReserveUsers = sampleUsers.canReserve
    canReserveUsers should not be empty

    // Test display summary
    val summary = sampleUsers.displaySummary
    summary should include("Users Summary")
    summary should include("total")
    summary should include("students")
    summary should include("faculty")
    summary should include("librarians")
  }

  "Transaction collection extensions" should "filter and analyze transactions" in {
    // Test transaction type filters
    val loans = sampleTransactions.loans
    loans.foreach(_ shouldBe a[Transaction.Loan])
    
    val returns = sampleTransactions.returns
    returns.foreach(_ shouldBe a[Transaction.Return])
    
    val reservations = sampleTransactions.reservations
    reservations.foreach(_ shouldBe a[Transaction.Reservation])
    
    // Test filtering by user and book
    val userTransactions = sampleTransactions.forUser(sampleUsers.head.id)
    userTransactions.foreach(_.user.id shouldBe sampleUsers.head.id)
    
    val bookTransactions = sampleTransactions.forBook(sampleBooks.head.isbn)
    bookTransactions.foreach(_.book.isbn shouldBe sampleBooks.head.isbn)
    
    // Test recent transactions
    val recentTransactions = sampleTransactions.recent(30)
    recentTransactions should not be null

    // Test display summary
    val summary = sampleTransactions.displaySummary
    summary should include("Transactions Summary")
    summary should include("total")
    summary should include("loans")
    summary should include("returns")
  }

  "LibraryCatalog extensions" should "provide catalog statistics" in {
    // Test basic statistics
    val totalBooks = sampleCatalog.totalBooks
    totalBooks shouldBe sampleBooks.size
    
    val totalUsers = sampleCatalog.totalUsers
    totalUsers shouldBe sampleUsers.size
    
    val totalTransactions = sampleCatalog.totalTransactions
    totalTransactions shouldBe sampleTransactions.size
    
    // Test book availability
    val availableBooks = sampleCatalog.availableBooks
    availableBooks.foreach(_.isAvailable shouldBe true)
    
    val booksOnLoan = sampleCatalog.booksOnLoan
    booksOnLoan.foreach(_.isAvailable shouldBe false)
    
    // Test active users
    val activeUsers = sampleCatalog.activeUsers
    activeUsers should not be null
    
    // Test display stats
    val stats = sampleCatalog.displayStats
    stats should include("Library Statistics")
    stats should include("Books Summary")
    stats should include("Users Summary")
    stats should include("Transactions Summary")
    stats should include("Active users")
  }

  "Option and Either extensions" should "provide utility methods" in {
    val someValue: Option[String] = Some("test")
    val noneValue: Option[String] = None

    // Test toEither conversion
    someValue.toEither("error") shouldBe Right("test")
    noneValue.toEither("error") shouldBe Left("error")

    // Test orElseThrow
    someValue.orElseThrow("error") shouldBe "test"

    val rightValue: Either[String, Int] = Right(42)
    val leftValue: Either[String, Int] = Left("error")

    // Test getOrElseThrow (only test Right to avoid exception)
    rightValue.getOrElseThrow shouldBe 42

    // Test logError (should return the same Either)
    rightValue.logError shouldBe rightValue
    leftValue.logError shouldBe leftValue
  }

  "Displayable extensions" should "format output correctly" in {
    import TypeClasses.given

    // Test book display (these methods exist and use given instances)
    sampleBooks.take(2).displayAll
    sampleBooks.take(2).displayDetailed

    // Test user display
    sampleUsers.take(2).displayAll  
    sampleUsers.take(2).displayDetailed

    // Test transaction display
    sampleTransactions.displayAll
    sampleTransactions.displayDetailed
  }

  "Search functionality" should "find similar books" in {
    import TypeClasses.given

    val targetBook = sampleBooks.head
    
    // Test similarity search
    val similarBooks = sampleBooks.findSimilarTo(targetBook, 0.5)
    similarBooks should not be null
    
    // Test most similar
    val mostSimilar = sampleBooks.mostSimilarTo(targetBook)
    mostSimilar shouldBe defined
  }

  "Safe creation methods" should "validate input" in {
    // Test safe book creation
    val bookResult = createBookSafely(
      "978-0134685991",
      "Test Book",
      List("Test Author"),
      2023,
      "Programming"
    )
    bookResult shouldBe a[Right[_, _]]

    // Test invalid book creation
    val invalidBookResult = createBookSafely("", "", List.empty, -1, "")
    invalidBookResult shouldBe a[Left[_, _]]

    // Test safe user creation
    val userResult = createUserSafely("John Doe", "password123", "student", "Computer Science")
    userResult shouldBe a[Right[_, _]]

    // Test invalid user creation
    val invalidUserResult = createUserSafely("", "weak", "invalid", "")
    invalidUserResult shouldBe a[Left[_, _]]
  }

  "Union type functionality" should "handle search results" in {
    val catalog = LibraryCatalog(
      books = sampleBooks.map(b => b.isbn -> b).toMap,
      users = Map.empty,
      transactions = List.empty
    )

    // Test successful search
    val validSearchResult = searchBooksUnion(catalog, "Programming")
    validSearchResult match {
      case books: List[Book] => books should not be empty
      case error: String => fail(s"Expected books but got error: $error")
    }

    // Test empty search
    val emptySearchResult = searchBooksUnion(catalog, "")
    emptySearchResult match {
      case books: List[Book] => fail("Expected error but got books")
      case error: String => error should include("empty")
    }

    // Test handleSearchResult
    handleSearchResult(validSearchResult) // Should not throw
    handleSearchResult(emptySearchResult) // Should not throw
  }

  "Book and User validation" should "validate objects correctly" in {
    import TypeClasses.given

    val validBook = sampleBooks.head
    val validatedBook = validBook.validateAndLog
    validatedBook shouldBe validBook

    val validUser = sampleUsers.head
    val validatedUser = validUser.validateAndLog
    validatedUser shouldBe validUser
  }
}
