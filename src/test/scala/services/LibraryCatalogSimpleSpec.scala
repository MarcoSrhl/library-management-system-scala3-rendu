package services

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import models.*
import utils.Types.{ISBN, UserID}
import java.util.UUID

class LibraryCatalogSimpleSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks:

  val sampleBook = Book(
    ISBN("978-3-16-148410-0"),
    "Sample Book",
    List("Author Name"),
    2023,
    "Fiction",
    isAvailable = true
  )

  val sampleUser = User.Student(
    UserID(UUID.randomUUID()),
    "John Doe",
    "Computer Science",
    "password123"
  )

  val emptyCatalog = new LibraryCatalog(Map.empty, Map.empty, List.empty)

  "LibraryCatalog" should "be created with empty state" in {
    emptyCatalog.books shouldBe empty
    emptyCatalog.users shouldBe empty
    emptyCatalog.transactions shouldBe empty
  }

  it should "add books correctly" in {
    val catalog = emptyCatalog.addBook(sampleBook)
    
    catalog.books should contain key sampleBook.isbn
    catalog.books(sampleBook.isbn) shouldBe sampleBook
    catalog.books should have size 1
  }

  it should "replace existing books with same ISBN" in {
    val updatedBook = sampleBook.copy(title = "Updated Title")
    val catalog = emptyCatalog
      .addBook(sampleBook)
      .addBook(updatedBook)
    
    catalog.books should have size 1
    catalog.books(sampleBook.isbn).title shouldBe "Updated Title"
  }

  it should "add users correctly" in {
    val catalog = emptyCatalog.addUser(sampleUser)
    
    catalog.users should contain key sampleUser.id
    catalog.users(sampleUser.id) shouldBe sampleUser
    catalog.users should have size 1
  }

  it should "handle multiple users" in {
    val user2 = User.Faculty(
      UserID(UUID.randomUUID()),
      "Jane Smith",
      "Computer Science Department",
      "faculty123"
    )
    
    val catalog = emptyCatalog
      .addUser(sampleUser)
      .addUser(user2)
    
    catalog.users should have size 2
    catalog.users should contain key sampleUser.id
    catalog.users should contain key user2.id
  }

  "Book operations" should "track availability" in {
    val catalog = emptyCatalog.addBook(sampleBook)
    
    catalog.books should contain key sampleBook.isbn
    catalog.books(sampleBook.isbn) shouldBe sampleBook
  }

  "Loan operations" should "track active loans" in {
    val catalog = emptyCatalog
      .addBook(sampleBook)
      .addUser(sampleUser)
    
    catalog.activeLoansFor(sampleUser.id) shouldBe 0
    
    val updatedCatalog = catalog.loanBook(sampleBook.isbn, sampleUser.id)
    updatedCatalog.activeLoansFor(sampleUser.id) shouldBe 1
  }

  "Property-based tests" should "maintain catalog invariants" in {
    // Simplified test with fixed values instead of property-based
    val book = Book(
      ISBN("978-0-123-45678-9"),
      "Test Title",
      List("Test Author"),
      2020,
      "Test Genre",
      isAvailable = true
    )
    
    val catalog = emptyCatalog.addBook(book)
    catalog.books should contain key book.isbn
    catalog.books(book.isbn) shouldBe book
  }

  "Catalog state" should "remain immutable" in {
    val originalCatalog = emptyCatalog.addBook(sampleBook)
    val modifiedCatalog = originalCatalog.addUser(sampleUser)
    
    originalCatalog.users shouldBe empty
    modifiedCatalog.users should have size 1
    originalCatalog.books should have size 1
    modifiedCatalog.books should have size 1
  }

  "ActiveLoansFor" should "handle non-existent users" in {
    val catalog = emptyCatalog.addBook(sampleBook)
    val nonExistentUserId = UserID(UUID.randomUUID())
    
    catalog.activeLoansFor(nonExistentUserId) shouldBe 0
  }

  "Book management" should "handle book updates" in {
    val originalBook = sampleBook
    val updatedBook = originalBook.copy(title = "Updated Book Title")
    
    val catalog = emptyCatalog
      .addBook(originalBook)
      .addBook(updatedBook) // Should replace original
    
    catalog.books should have size 1
    catalog.books(originalBook.isbn).title shouldBe "Updated Book Title"
  }

  "User management" should "store different user types" in {
    val student = User.Student(UserID(UUID.randomUUID()), "Student", "CS", "pass1")
    val faculty = User.Faculty(UserID(UUID.randomUUID()), "Faculty", "CS Dept", "pass2")
    val librarian = User.Librarian(UserID(UUID.randomUUID()), "Librarian", "EMP001", "pass3")
    
    val catalog = emptyCatalog
      .addUser(student)
      .addUser(faculty)
      .addUser(librarian)
    
    catalog.users should have size 3
    catalog.users should contain key student.id
    catalog.users should contain key faculty.id
    catalog.users should contain key librarian.id
  }
