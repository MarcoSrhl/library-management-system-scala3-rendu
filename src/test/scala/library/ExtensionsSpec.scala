package library

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import Types.*
import User.*
import Extensions.*
import utils.TypeClasses.given
import java.util.UUID
import java.time.LocalDateTime

/**
 * Tests for extension methods.
 * Tests additional functionality added to core types.
 */
class ExtensionsSpec extends AnyFlatSpec with Matchers {

  // Sample data
  val sampleBooks = List(
    Book(ISBN("978-0134685991"), "Effective Java", List("Joshua Bloch"), 2017, "Programming", true),
    Book(ISBN("978-0135166307"), "Java: The Complete Reference", List("Herbert Schildt"), 2020, "Programming", false),
    Book(ISBN("978-0134494166"), "Clean Code", List("Robert Martin"), 2008, "Programming", true),
    Book(ISBN("978-0132350884"), "Effective C++", List("Scott Meyers"), 1998, "Programming", true),
    Book(ISBN("978-0321127426"), "Effective Modern C++", List("Scott Meyers"), 2014, "Programming", false)
  )
  
  val sampleUsers = List(
    Student(UserID(UUID.randomUUID()), "Alice", "Computer Science", "pass123"),
    Faculty(UserID(UUID.randomUUID()), "Bob", "Mathematics", "pass456"),
    Librarian(UserID(UUID.randomUUID()), "Charlie", "Main Library", "pass789")
  )

  "Book collection extensions" should "filter by availability" in {
    val available = sampleBooks.availableOnly
    available should have size 3
    available.forall(_.isAvailable) shouldBe true
    
    val onLoan = sampleBooks.onLoanOnly
    onLoan should have size 2
    onLoan.forall(!_.isAvailable) shouldBe true
  }

  it should "filter by publication year" in {
    val after2010 = sampleBooks.publishedAfter(2010)
    after2010 should have size 3
    after2010.forall(_.publicationYear > 2010) shouldBe true

    val before2010 = sampleBooks.publishedBefore(2010)
    before2010 should have size 2
    before2010.forall(_.publicationYear <= 2010) shouldBe true
  }

  it should "sort collections" in {
    val sortedByTitle = sampleBooks.sortedByTitle
    sortedByTitle.map(_.title) shouldBe sampleBooks.map(_.title).sorted

    val sortedByYear = sampleBooks.sortedByYear
    sortedByYear.map(_.publicationYear) shouldBe sampleBooks.map(_.publicationYear).sorted
  }

  it should "group by genre" in {
    val grouped = sampleBooks.groupedByGenre
    grouped("Programming") should have size 5
    grouped.keys should contain only "Programming"
  }

  it should "display summary" in {
    val summary = sampleBooks.displaySummary
    summary should include("5 total")
    summary should include("3 available")
    summary should include("2 on loan")
    summary should include("1 genres")
  }

  "User collection extensions" should "filter by role" in {
    val students = sampleUsers.students
    students should have size 1
    students.head.isStudent shouldBe true

    val faculty = sampleUsers.faculty
    faculty should have size 1
    faculty.head.isFaculty shouldBe true

    val librarians = sampleUsers.librarians
    librarians should have size 1
    librarians.head.isLibrarian shouldBe true
  }

  it should "filter users who can reserve" in {
    val canReserveUsers = sampleUsers.filter(u => u.isStudent || u.isFaculty)
    canReserveUsers should have size 2 // students and faculty can reserve
  }

  it should "display summary" in {
    val summary = sampleUsers.displaySummary
    summary should include("3 total")
    summary should include("1 students")
    summary should include("1 faculty")
    summary should include("1 librarians")
  }

  "Transaction collection extensions" should "filter by type" in {
    val user = sampleUsers.head
    val book = sampleBooks.head
    val now = LocalDateTime.now

    val transactions = List(
      Transaction.Loan(book, user, now, Some(now.plusDays(14))),
      Transaction.Return(book, user, now),
      Transaction.Reservation(book, user, now, now, now.plusDays(7))
    )

    val loans = transactions.loans
    loans should have size 1
    loans.head shouldBe a[Transaction.Loan]

    val returns = transactions.returns
    returns should have size 1
    returns.head shouldBe a[Transaction.Return]

    val reservations = transactions.reservations
    reservations should have size 1
    reservations.head shouldBe a[Transaction.Reservation]
  }

  it should "filter by user and book" in {
    val user1 = sampleUsers(0)
    val user2 = sampleUsers(1)
    val book1 = sampleBooks(0)
    val book2 = sampleBooks(1)
    val now = LocalDateTime.now

    val transactions = List(
      Transaction.Loan(book1, user1, now, Some(now.plusDays(14))),
      Transaction.Loan(book2, user2, now, Some(now.plusDays(14))),
      Transaction.Return(book2, user1, now)
    )

    val user1Transactions = transactions.forUser(user1.id)
    user1Transactions should have size 2

    val book1Transactions = transactions.forBook(book1.isbn)
    book1Transactions should have size 1
  }

  it should "filter recent transactions" in {
    val user = sampleUsers.head
    val book = sampleBooks.head

    val transactions = List(
      Transaction.Loan(book, user, LocalDateTime.now.minusDays(1), Some(LocalDateTime.now.plusDays(13))),
      Transaction.Loan(book, user, LocalDateTime.now.minusDays(10), Some(LocalDateTime.now.plusDays(4)))
    )

    val recent = transactions.recent(5)
    recent should have size 1
  }

  it should "display summary" in {
    val user = sampleUsers.head
    val book = sampleBooks.head
    val now = LocalDateTime.now

    val transactions = List(
      Transaction.Loan(book, user, now, Some(now.plusDays(14))),
      Transaction.Return(book, user, now),
      Transaction.Reservation(book, user, now, now, now.plusDays(7))
    )

    val summary = transactions.displaySummary
    summary should include("3 total")
    summary should include("1 loans")
    summary should include("1 returns")
    summary should include("1 reservations")
  }

  "String validation extensions" should "validate ISBN" in {
    "978-0134685991".isValidISBN shouldBe true
    "invalid".isValidISBN shouldBe false

    val validResult = "978-0134685991".toValidISBN
    validResult.isRight shouldBe true

    val invalidResult = "invalid".toValidISBN
    invalidResult.isLeft shouldBe true
  }

  it should "validate Title" in {
    "Valid Title".isValidTitle shouldBe true
    "".isValidTitle shouldBe false

    val validResult = "Valid Title".toValidTitle
    validResult.isRight shouldBe true

    val invalidResult = "".toValidTitle
    invalidResult.isLeft shouldBe true
  }

  "LibraryCatalog extensions" should "provide statistics" in {
    val catalog = LibraryCatalog(
      books = sampleBooks.map(b => b.isbn -> b).toMap,
      users = sampleUsers.map(u => u.id -> u).toMap,
      transactions = List.empty
    )

    catalog.totalBooks shouldBe 5
    catalog.totalUsers shouldBe 3
    catalog.totalTransactions shouldBe 0
    catalog.availableBooks should have size 3
    catalog.booksOnLoan should have size 2

    val stats = catalog.displayStats
    stats should include("5 total")
    stats should include("3 available")
  }

  "Union type search" should "handle search operations" in {
    val catalog = LibraryCatalog(
      books = sampleBooks.map(b => b.isbn -> b).toMap,
      users = Map.empty,
      transactions = List.empty
    )

    val result = searchBooksUnion(catalog, "Java")
    result match
      case books: List[Book] => 
        books should not be empty
        books.exists(_.title.contains("Java")) shouldBe true
      case error: String => fail(s"Expected books but got error: $error")

    val emptyResult = searchBooksUnion(catalog, "")
    emptyResult match
      case books: List[Book] => fail(s"Expected error but got ${books.length} books")
      case error: String => error should include("cannot be empty")
  }

  "Safe creation functions" should "create books safely" in {
    val result = createBookSafely(
      "978-0134685991",
      "Test Book",
      List("Author"),
      2023,
      "Fiction"
    )

    result.isRight shouldBe true
    result.foreach { book =>
      book.title shouldBe "Test Book"
      book.authors should contain("Author")
    }

    val invalidResult = createBookSafely("invalid", "", List(), 2023, "")
    invalidResult.isLeft shouldBe true
  }

  it should "create users safely" in {
    val result = createUserSafely("Alice", "password123", "student", "CS")
    result.isRight shouldBe true
    result.foreach { user =>
      user.name shouldBe "Alice"
      user.isStudent shouldBe true
    }

    val invalidResult = createUserSafely("", "123", "invalid", "")
    invalidResult.isLeft shouldBe true
  }

  "Option and Either extensions" should "provide utility methods" in {
    val someValue = Some("test")
    val noneValue: Option[String] = None

    someValue.toEither("error") shouldBe Right("test")
    noneValue.toEither("error") shouldBe Left("error")

    val rightValue: Either[String, Int] = Right(42)
    val leftValue: Either[String, Int] = Left("error")

    rightValue.getOrElseThrow shouldBe 42
    an[RuntimeException] should be thrownBy leftValue.getOrElseThrow

    // Test logError (should return the same either but log)
    rightValue.logError shouldBe rightValue
    leftValue.logError shouldBe leftValue
  }
}
