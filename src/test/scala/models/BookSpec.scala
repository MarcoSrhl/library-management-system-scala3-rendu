package models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import utils.Types.*

/**
 * Comprehensive test suite for the Book model.
 * Tests the Book case class and its integration with opaque types.
 */
class BookSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  // Test data
  val validISBN = ISBN("9780134685991")
  val sampleBook = Book(
    validISBN,
    "Effective Java",
    List("Joshua Bloch"),
    2017,
    "Programming",
    true
  )

  // Property-based test generators
  implicit val arbBook: Arbitrary[Book] = Arbitrary {
    for {
      isbn <- Gen.alphaNumStr.map(ISBN(_))
      title <- Gen.alphaNumStr.suchThat(_.nonEmpty).map(_.take(100))
      author <- Gen.alphaNumStr.suchThat(_.nonEmpty).map(_.take(50))
      year <- Gen.choose(1000, 2024)
      genre <- Gen.oneOf("Fiction", "Non-fiction", "Science", "Biography", "Programming")
      available <- Gen.oneOf(true, false)
    } yield Book(isbn, title, List(author), year, genre, available)
  }

  "Book" should "create instances with valid data" in {
    sampleBook.isbn shouldBe validISBN
    sampleBook.title shouldBe "Effective Java"
    sampleBook.authors should contain("Joshua Bloch")
    sampleBook.publicationYear shouldBe 2017
    sampleBook.genre shouldBe "Programming"
    sampleBook.isAvailable shouldBe true
  }

  it should "be immutable" in {
    val originalBook = sampleBook
    val modifiedBook = sampleBook.copy(title = "Modified Title")
    
    originalBook.title shouldBe "Effective Java"
    modifiedBook.title shouldBe "Modified Title"
    originalBook should not equal modifiedBook
  }

  it should "support multiple authors" in {
    val multiAuthorBook = sampleBook.copy(
      authors = List("Joshua Bloch", "Brian Goetz", "Tim Peierls")
    )
    
    multiAuthorBook.authors should have size 3
    multiAuthorBook.authors should contain allOf ("Joshua Bloch", "Brian Goetz", "Tim Peierls")
  }

  it should "handle availability status correctly" in {
    val availableBook = sampleBook.copy(isAvailable = true)
    val unavailableBook = sampleBook.copy(isAvailable = false)
    
    availableBook.isAvailable shouldBe true
    unavailableBook.isAvailable shouldBe false
  }

  it should "integrate with ISBN opaque type" in {
    val isbn = ISBN("9781234567890")
    val book = sampleBook.copy(isbn = isbn)
    
    book.isbn shouldBe isbn
    book.isbn.value shouldBe "9781234567890"
  }

  it should "support case class operations" in {
    // Test case class equality
    val book1 = Book(validISBN, "Title", List("Author"), 2020, "Fiction", true)
    val book2 = Book(validISBN, "Title", List("Author"), 2020, "Fiction", true)
    val book3 = Book(validISBN, "Different Title", List("Author"), 2020, "Fiction", true)
    
    book1 shouldBe book2
    book1 should not equal book3
    
    // Test case class copy
    val copiedBook = book1.copy(publicationYear = 2021)
    copiedBook.publicationYear shouldBe 2021
    copiedBook.title shouldBe book1.title
  }

  it should "provide meaningful toString representation" in {
    val bookString = sampleBook.toString
    bookString should include("Book")
    bookString should include("Effective Java")
    bookString should include("Joshua Bloch")
  }

  // Property-based tests
  "Book property-based tests" should "maintain immutability" in {
    forAll { (book: Book) =>
      val originalTitle = book.title
      val copiedBook = book.copy(title = "New Title")
      
      book.title shouldBe originalTitle
      copiedBook.title shouldBe "New Title"
    }
  }

  it should "preserve non-modified fields in copy operations" in {
    forAll { (book: Book) =>
      val copiedBook = book.copy(isAvailable = !book.isAvailable)
      
      copiedBook.isbn shouldBe book.isbn
      copiedBook.title shouldBe book.title
      copiedBook.authors shouldBe book.authors
      copiedBook.publicationYear shouldBe book.publicationYear
      copiedBook.genre shouldBe book.genre
      copiedBook.isAvailable should not equal book.isAvailable
    }
  }

  it should "handle various author list sizes" in {
    forAll { (authors: List[String]) =>
      whenever(authors.nonEmpty && authors.forall(_.nonEmpty)) {
        val book = sampleBook.copy(authors = authors)
        book.authors shouldBe authors
        book.authors should have size authors.size
      }
    }
  }

  it should "handle valid publication years" in {
    forAll(Gen.choose(1000, 3000)) { year =>
      val book = sampleBook.copy(publicationYear = year)
      book.publicationYear shouldBe year
    }
  }

  it should "work with different genres" in {
    val genres = List("Fiction", "Non-fiction", "Science Fiction", "Biography", 
                     "History", "Programming", "Mathematics", "Philosophy")
    
    forAll(Gen.oneOf(genres)) { genre =>
      val book = sampleBook.copy(genre = genre)
      book.genre shouldBe genre
    }
  }

  "Book integration" should "work with ISBN validation" in {
    // Test with valid ISBN
    ISBN.safe("9780134685991") match {
      case Right(validIsbn) =>
        val book = Book(validIsbn, "Title", List("Author"), 2020, "Fiction", true)
        book.isbn.isValid shouldBe true
      case Left(error) => fail(s"Expected valid ISBN: $error")
    }
    
    // Test with invalid ISBN (still creates book, but ISBN is invalid)
    val invalidIsbn = ISBN("invalid")
    val book = Book(invalidIsbn, "Title", List("Author"), 2020, "Fiction", true)
    book.isbn.isValid shouldBe false
  }

  it should "work in collections" in {
    val books = List(
      Book(ISBN("1"), "Book 1", List("Author 1"), 2020, "Fiction", true),
      Book(ISBN("2"), "Book 2", List("Author 2"), 2021, "Science", false),
      Book(ISBN("3"), "Book 3", List("Author 3"), 2022, "History", true)
    )
    
    books should have size 3
    books.map(_.title) should contain allOf ("Book 1", "Book 2", "Book 3")
    books.filter(_.isAvailable) should have size 2
    books.map(_.publicationYear).sum shouldBe 6063
  }

  it should "support complex queries" in {
    val books = List(
      Book(ISBN("1"), "Scala Programming", List("Martin"), 2020, "Programming", true),
      Book(ISBN("2"), "Java Programming", List("Gosling"), 2021, "Programming", false),
      Book(ISBN("3"), "History of Computing", List("Ceruzzi"), 2019, "History", true),
      Book(ISBN("4"), "Advanced Scala", List("Martin", "Odersky"), 2022, "Programming", true)
    )
    
    // Filter by genre
    val programmingBooks = books.filter(_.genre == "Programming")
    programmingBooks should have size 3
    
    // Filter by availability
    val availableBooks = books.filter(_.isAvailable)
    availableBooks should have size 3
    
    // Filter by multiple authors
    val multiAuthorBooks = books.filter(_.authors.size > 1)
    multiAuthorBooks should have size 1
    multiAuthorBooks.head.title shouldBe "Advanced Scala"
    
    // Filter by publication year range
    val recentBooks = books.filter(book => book.publicationYear >= 2020)
    recentBooks should have size 3
  }

  "Book edge cases" should "handle empty author lists" in {
    // While not recommended, the model allows empty author lists
    val bookWithNoAuthors = Book(validISBN, "Title", List.empty, 2020, "Fiction", true)
    bookWithNoAuthors.authors shouldBe empty
  }

  it should "handle very long titles" in {
    val longTitle = "A" * 1000
    val book = Book(validISBN, longTitle, List("Author"), 2020, "Fiction", true)
    book.title should have length 1000
  }

  it should "handle special characters in titles and authors" in {
    val specialTitle = "Ñiño's Guide to Programming: A £100 Adventure!"
    val specialAuthor = "José María García-López"
    
    val book = Book(validISBN, specialTitle, List(specialAuthor), 2020, "Fiction", true)
    book.title shouldBe specialTitle
    book.authors should contain(specialAuthor)
  }

  it should "handle extreme publication years" in {
    val oldBook = Book(validISBN, "Ancient Text", List("Unknown"), 1, "History", true)
    val futureBook = Book(validISBN, "Future Predictions", List("Prophet"), 3000, "Science", true)
    
    oldBook.publicationYear shouldBe 1
    futureBook.publicationYear shouldBe 3000
  }
}
