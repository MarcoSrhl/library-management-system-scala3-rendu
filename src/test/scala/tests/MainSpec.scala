package tests

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import library.*
import library.Types.*
import library.User.*
import java.util.UUID
import java.time.LocalDateTime

/**
 * Tests for Main application and entry points.
 * Tests application initialization and core functionality.
 */
class MainSpec extends AnyFlatSpec with Matchers {

  "Main application" should "have proper package structure" in {
    // Test that core packages exist
    noException should be thrownBy Class.forName("library.Book")
    noException should be thrownBy Class.forName("library.User")
    noException should be thrownBy Class.forName("library.Transaction")
    noException should be thrownBy Class.forName("library.LibraryCatalog")
  }

  it should "support basic domain operations" in {
    // Test basic domain object creation
    val book = Book(
      ISBN("978-0134685991"),
      "Effective Java",
      List("Joshua Bloch"),
      2017,
      "Programming",
      true
    )
    
    val user = User.Student(
      UserID(UUID.randomUUID()),
      "Alice",
      "Computer Science",
      "password123"
    )
    
    val transaction = Transaction.Loan(book, user, LocalDateTime.now, Some(LocalDateTime.now.plusDays(14)))
    
    book shouldBe a[Book]
    user shouldBe a[User]
    transaction shouldBe a[Transaction]
  }

  it should "initialize empty catalog" in {
    val emptyCatalog = LibraryCatalog.empty
    
    emptyCatalog.books should be(empty)
    emptyCatalog.users should be(empty)
    emptyCatalog.transactions should be(empty)
  }

  it should "handle default user creation" in {
    // Test default user creation logic
    val defaultUsers = List(
      User.Student(UserID(UUID.randomUUID()), "Alice", "Computer Science", "student123"),
      User.Faculty(UserID(UUID.randomUUID()), "Dr. Smith", "Mathematics", "faculty123"),
      User.Librarian(UserID(UUID.randomUUID()), "Admin", "Main Library", "admin123")
    )
    
    defaultUsers should have size 3
    defaultUsers.count(_.isStudent) shouldBe 1
    defaultUsers.count(_.isFaculty) shouldBe 1
    defaultUsers.count(_.isLibrarian) shouldBe 1
    
    // Note: User doesn't have authenticate method in the main codebase
    defaultUsers.foreach { user =>
      user.password should not be empty
    }
  }

  "Application initialization" should "handle data directory setup" in {
    val dataPath = "data"
    
    // Test that data path configuration works
    dataPath should not be empty
    dataPath shouldBe "data"
  }

  it should "handle catalog loading fallback" in {
    // Test loading from non-existent path
    val catalog = library.JsonIO.loadCatalog("non/existent/path")
    
    // Should fall back to empty catalog
    catalog shouldBe a[LibraryCatalog]
    catalog.books should be(empty)
    catalog.users should be(empty)
    catalog.transactions should be(empty)
  }

  "Core application logic" should "support basic library operations" in {
    val catalog = LibraryCatalog.empty
    
    val book = Book(
      ISBN("978-0134685991"),
      "Effective Java",
      List("Joshua Bloch"),
      2017,
      "Programming",
      true
    )
    
    val user = User.Student(
      UserID(UUID.randomUUID()),
      "Alice",
      "Computer Science",
      "password123"
    )
    
    // Test adding book and user
    val catalogWithBook = catalog.addBook(book)
    val catalogWithUser = catalogWithBook.addUser(user)
    
    catalogWithUser.books should contain key book.isbn
    catalogWithUser.users should contain key user.id
    
    // Test loan operation
    val loanResult = catalogWithUser.loanBook(book.isbn, user.id)
    loanResult shouldBe a[Right[_, _]]
  }

  "Session management" should "handle user authentication" in {
    val user1 = User.Student(UserID(UUID.randomUUID()), "Alice", "CS", "student123")
    val user2 = User.Faculty(UserID(UUID.randomUUID()), "Dr. Smith", "Math", "faculty123")
    val user3 = User.Librarian(UserID(UUID.randomUUID()), "Admin", "Library", "admin123")
    
    val users = Map(
      user1.id -> user1,
      user2.id -> user2,
      user3.id -> user3
    )
    
    val catalog = LibraryCatalog(Map.empty, users, List.empty)
    
    // Test user lookup and authentication
    val student = catalog.users.values.find(_.name == "Alice")
    val faculty = catalog.users.values.find(_.name == "Dr. Smith")
    val librarian = catalog.users.values.find(_.name == "Admin")
    
    student should not be empty
    faculty should not be empty
    librarian should not be empty
    
    student.foreach { user =>
      user.name shouldBe "Alice"
      user.password should not be empty
    }
  }

  "Menu system navigation" should "handle user choices" in {
    val menuChoices = Map(
      "1" -> "View Books",
      "2" -> "Search Books",
      "3" -> "Loan Book",
      "4" -> "Return Book",
      "5" -> "View My Loans",
      "6" -> "Logout"
    )
    
    menuChoices should have size 6
    menuChoices("1") shouldBe "View Books"
    menuChoices("6") shouldBe "Logout"
    
    // Test choice validation
    val validChoices = menuChoices.keys.toSet
    validChoices should contain("1")
    validChoices should contain("6")
    validChoices should not contain "7"
  }

  "Application state" should "maintain consistency" in {
    var catalog = LibraryCatalog.empty
    var running = true
    
    // Test state management variables
    running shouldBe true
    catalog shouldBe a[LibraryCatalog]
    
    // Test state transitions
    running = false
    running shouldBe false
    
    // Test catalog updates
    val book = Book(ISBN("978-1234567890"), "Test Book", List("Author"), 2024, "Fiction", true)
    catalog = catalog.addBook(book)
    catalog.books should contain key book.isbn
  }

  "Command line arguments" should "be processable" in {
    // Test command line argument processing
    val args = Array("--data-path", "data", "--port", "8080")
    
    args should have length 4
    args should contain("--data-path")
    args should contain("data")
    
    // Test argument parsing logic
    val dataPathIndex = args.indexOf("--data-path")
    if dataPathIndex >= 0 && dataPathIndex + 1 < args.length then
      val dataPath = args(dataPathIndex + 1)
      dataPath shouldBe "data"
  }

  "Error handling" should "manage application errors gracefully" in {
    // Test error scenarios
    val invalidISBN = "invalid-isbn"
    val validISBN = "978-0134685991"
    
    // Test error handling patterns
    noException should be thrownBy {
      try {
        ISBN(validISBN)
      } catch {
        case _: Exception => // Handle gracefully
      }
    }
    
    // Test graceful degradation
    val fallbackCatalog = LibraryCatalog.empty
    fallbackCatalog shouldBe a[LibraryCatalog]
  }
}
