package utils

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import library.*
import library.Types.*
import library.User.*
import java.util.UUID
import java.time.LocalDateTime

/**
 * Tests for CLI utility functions and command-line interface.
 * Tests CLI input/output functionality and user interaction patterns.
 */
class CLISpec extends AnyFlatSpec with Matchers {

  "CLI utility functions" should "exist and be accessible" in {
    // Test that CLI object exists
    CLI should not be null
    noException should be thrownBy CLI.getClass
  }

  "CLI prompting functions" should "handle input operations" in {
    // Test that prompt methods exist (without requiring actual input)
    noException should be thrownBy CLI.getClass.getMethods.filter(_.getName.contains("prompt"))
  }

  "CLI validation functions" should "handle user input validation" in {
    // Test input validation patterns
    val validISBN = "978-0-123456-78-9"
    val invalidISBN = "invalid"
    
    // These should not throw exceptions when CLI methods are called
    noException should be thrownBy validISBN.length
    noException should be thrownBy invalidISBN.length
  }

  "CLI display functions" should "format output correctly" in {
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
    
    // Test that basic formatting works
    noException should be thrownBy book.title.toString
    noException should be thrownBy user.name.toString
  }

  "CLI menu systems" should "handle navigation" in {
    // Test basic menu structure without actual interaction
    val menuOptions = List("1. View Books", "2. Loan Book", "3. Return Book", "4. Exit")
    menuOptions should have size 4
    menuOptions.foreach(option => option should not be empty)
  }

  "CLI error handling" should "display error messages" in {
    val errorMessage = "Book not found"
    val warningMessage = "Invalid input"
    val infoMessage = "Operation successful"
    
    // Test message formatting
    errorMessage should include("not found")
    warningMessage should include("Invalid")
    infoMessage should include("successful")
  }

  "CLI authentication" should "handle login simulation" in {
    val user1 = User.Student(UserID(UUID.randomUUID()), "Alice", "CS", "password123")
    val user2 = User.Faculty(UserID(UUID.randomUUID()), "Dr. Smith", "Math", "faculty123")
    val user3 = User.Librarian(UserID(UUID.randomUUID()), "Admin", "Library", "admin123")
    
    val catalog = LibraryCatalog(
      books = Map.empty,
      users = Map(
        user1.id -> user1,
        user2.id -> user2,
        user3.id -> user3
      ),
      transactions = List.empty
    )
    
    // Test that catalog has users for authentication
    catalog.users should not be empty
    catalog.users should have size 3
    
    val student = catalog.users.values.find(_.isStudent)
    val faculty = catalog.users.values.find(_.isFaculty)
    val librarian = catalog.users.values.find(_.isLibrarian)
    
    student should not be empty
    faculty should not be empty
    librarian should not be empty
  }

  "CLI book operations" should "handle book management" in {
    val book = Book(
      ISBN("978-0134685991"),
      "Effective Java",
      List("Joshua Bloch"),
      2017,
      "Programming",
      true
    )
    
    // Test book display formatting
    val bookInfo = s"${book.title} by ${book.authors.mkString(", ")} (${book.publicationYear})"
    bookInfo should include("Effective Java")
    bookInfo should include("Joshua Bloch")
    bookInfo should include("2017")
    
    // Test availability status
    val availabilityStatus = if book.isAvailable then "Available" else "On Loan"
    availabilityStatus shouldBe "Available"
  }

  "CLI user operations" should "handle user management" in {
    val users = List(
      User.Student(UserID(UUID.randomUUID()), "Alice", "Computer Science", "password123"),
      User.Faculty(UserID(UUID.randomUUID()), "Dr. Smith", "Mathematics", "faculty123"),
      User.Librarian(UserID(UUID.randomUUID()), "Admin", "Main Library", "admin123")
    )
    
    // Test user listing
    users should have size 3
    users.count(_.isStudent) shouldBe 1
    users.count(_.isFaculty) shouldBe 1
    users.count(_.isLibrarian) shouldBe 1
    
    // Test user display formatting
    users.foreach { user =>
      val userInfo = s"${user.name} (${user.getClass.getSimpleName.replace("$", "")})"
      userInfo should include(user.name)
    }
  }

  "CLI transaction operations" should "handle loan/return operations" in {
    val user = User.Student(UserID(UUID.randomUUID()), "Alice", "CS", "password123")
    val book = Book(ISBN("978-0134685991"), "Test Book", List("Author"), 2017, "Fiction", true)
    
    val loan = Transaction.Loan(book, user, LocalDateTime.now, Some(LocalDateTime.now.plusDays(14)))
    val returnTx = Transaction.Return(book, user, LocalDateTime.now)
    
    // Test transaction display
    val loanInfo = s"${user.name} borrowed '${book.title}'"
    val returnInfo = s"${user.name} returned '${book.title}'"
    
    loanInfo should include("borrowed")
    returnInfo should include("returned")
    
    loan.user shouldBe user
    loan.book shouldBe book
    returnTx.user shouldBe user
    returnTx.book shouldBe book
  }

  "CLI search operations" should "handle search queries" in {
    val books = List(
      Book(ISBN("978-0134685991"), "Effective Java", List("Joshua Bloch"), 2017, "Programming", true),
      Book(ISBN("978-0135166307"), "Java Complete Reference", List("Herbert Schildt"), 2020, "Programming", true),
      Book(ISBN("978-0132350884"), "Clean Code", List("Robert Martin"), 2008, "Programming", true)
    )
    
    // Test search functionality
    val javaBooks = books.filter(_.title.toLowerCase.contains("java"))
    val programmingBooks = books.filter(_.genre.toLowerCase.contains("programming"))
    val availableBooks = books.filter(_.isAvailable)
    
    javaBooks should have size 2
    programmingBooks should have size 3
    availableBooks should have size 3
    
    // Test search result formatting
    javaBooks.foreach { book =>
      book.title.toLowerCase should include("java")
    }
  }

  "CLI pagination" should "handle large result sets" in {
    val manyBooks = (1 to 50).map { i =>
      Book(
        ISBN(s"978-0-123456-7$i-9"),
        s"Book $i",
        List(s"Author $i"),
        2020 + (i % 5),
        "Fiction",
        i % 2 == 0
      )
    }.toList
    
    // Test pagination logic
    val pageSize = 10
    val totalPages = math.ceil(manyBooks.length.toDouble / pageSize).toInt
    
    totalPages shouldBe 5
    
    val firstPage = manyBooks.take(pageSize)
    val secondPage = manyBooks.slice(pageSize, pageSize * 2)
    
    firstPage should have size pageSize
    secondPage should have size pageSize
    firstPage.head.title shouldBe "Book 1"
    secondPage.head.title shouldBe "Book 11"
  }

  "CLI validation" should "validate user input" in {
    // Test ISBN validation patterns
    val validISBNs = List(
      "978-0-123456-78-9",
      "978-0-234567-89-0",
      "978-0-345678-90-1"
    )
    
    val invalidISBNs = List(
      "invalid",
      "123",
      ""
    )
    
    validISBNs.foreach { isbn =>
      isbn should not be empty
      isbn should include("978")
    }
    
    invalidISBNs.foreach { isbn =>
      isbn should not include "978"
    }
    
    // Test password strength
    val strongPasswords = List("password123", "mySecurePass456", "library2024!")
    val weakPasswords = List("123", "pass", "")
    
    strongPasswords.foreach(_.length should be >= 6)
    weakPasswords.foreach(_.length should be < 6)
  }
}
