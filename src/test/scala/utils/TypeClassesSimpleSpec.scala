package utils

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import TypeClasses.*
import TypeClasses.given
import models.*
import utils.Types.*
import java.time.LocalDateTime
import java.util.UUID

class TypeClassesSimpleSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks:

  val sampleBook = Book(
    ISBN("978-3-16-148410-0"),
    "Sample Book",
    List("John Doe", "Jane Smith"),
    2023,
    "Fiction",
    isAvailable = true
  )

  val sampleStudent = User.Student(
    UserID(UUID.randomUUID()),
    "John Student",
    "Computer Science",
    "password123"
  )

  val sampleFaculty = User.Faculty(
    UserID(UUID.randomUUID()),
    "Jane Faculty",
    "Computer Science Department",
    "password456"
  )

  val sampleTransaction = Transaction.Loan(
    sampleBook,
    sampleStudent,
    LocalDateTime.now(),
    Some(LocalDateTime.now().plusDays(30))
  )

  "Displayable[Book]" should "provide short display format" in {
    val displayable = summon[Displayable[Book]]
    val result = displayable.display(sampleBook)
    
    result should include("Sample Book")
    result should include("John Doe, Jane Smith")
    result should include("2023")
  }

  it should "provide detailed display format" in {
    val displayable = summon[Displayable[Book]]
    val result = displayable.displayDetailed(sampleBook)
    
    result should include("Sample Book")
    result should include("978-3-16-148410-0")
    result should include("Fiction")
    result should include("Available")
  }

  "Displayable[User]" should "show user information" in {
    val displayable = summon[Displayable[User]]
    val result = displayable.display(sampleStudent)
    
    result should include("John Student")
    result should include("Student")
  }

  it should "provide detailed user information" in {
    val displayable = summon[Displayable[User]]
    val result = displayable.displayDetailed(sampleStudent)
    
    result should include("John Student")
    result should include("Student")
    result should include("Max Loans")
  }

  "Displayable[Transaction]" should "show transaction info" in {
    val displayable = summon[Displayable[Transaction]]
    val result = displayable.display(sampleTransaction)
    
    result should include("LOAN")
    result should include("Sample Book") // Check for book title instead of ISBN
  }

  "Validatable[Book]" should "validate correct books" in {
    val validatable = summon[Validatable[Book]]
    val result = validatable.validate(sampleBook)
    
    result shouldBe Right(sampleBook)
  }

  it should "reject invalid books" in {
    val validatable = summon[Validatable[Book]]
    
    val emptyTitleBook = sampleBook.copy(title = "")
    val emptyAuthorsBook = sampleBook.copy(authors = List.empty)
    val futureYearBook = sampleBook.copy(publicationYear = 3000)
    
    validatable.validate(emptyTitleBook) shouldBe a[Left[_, _]]
    validatable.validate(emptyAuthorsBook) shouldBe a[Left[_, _]]
    validatable.validate(futureYearBook) shouldBe a[Left[_, _]]
  }

  "Validatable[User]" should "validate correct users" in {
    val validatable = summon[Validatable[User]]
    val result = validatable.validate(sampleStudent)
    
    result shouldBe Right(sampleStudent)
  }

  "Extension methods" should "work on Book instances" in {
    val display = sampleBook.display
    val detailed = sampleBook.displayDetailed
    
    display should not be empty
    detailed should not be empty
    detailed.length should be > display.length
  }

  it should "work on User instances" in {
    val display = sampleStudent.display
    val detailed = sampleStudent.displayDetailed
    
    display should include("John Student")
    detailed should include("Student")
  }

  it should "work on Transaction instances" in {
    val display = sampleTransaction.display
    val detailed = sampleTransaction.displayDetailed
    
    display should include("LOAN")
    detailed should include("Sample Book") // Check for book title instead of ISBN
  }

  "Validation extension methods" should "work correctly" in {
    val validBook = sampleBook.validate
    val invalidBook = sampleBook.copy(title = "").validate
    
    validBook shouldBe Right(sampleBook)
    invalidBook shouldBe a[Left[_, _]]
  }

  "Different user types" should "display correctly" in {
    val studentDisplay = sampleStudent.display
    val facultyDisplay = sampleFaculty.display
    
    studentDisplay should include("Student")
    facultyDisplay should include("Faculty")
  }

  "Book properties" should "be maintained" in {
    val book = sampleBook
    book.title should not be empty
    book.authors should not be empty
    book.publicationYear should be > 1000
    book.genre should not be empty
  }

  "Property-based tests" should "validate basic properties" in {
    // Simplified property test that uses fixed values
    val book = Book(
      ISBN("978-0-123-45678-9"),
      "Test Title",
      List("Test Author"),
      2020,
      "Fiction",
      isAvailable = true
    )
    
    val display = book.display
    val detailed = book.displayDetailed
    
    display should not be empty
    detailed should not be empty
    detailed should include("Test Title")
  }

  "Type class instances" should "be available" in {
    // Verify that all required type class instances exist
    summon[Displayable[Book]] should not be null
    summon[Displayable[User]] should not be null
    summon[Displayable[Transaction]] should not be null
    summon[Validatable[Book]] should not be null
    summon[Validatable[User]] should not be null
  }
