package library

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import java.util.UUID
import Types.*

/**
 * Comprehensive test suite for opaque types.
 * Tests type safety, validation, and extension methods.
 */
class TypesSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  // Property-based test generators
  implicit val arbISBN: Arbitrary[ISBN] = Arbitrary {
    Gen.oneOf(
      Gen.const("978-0134685991"),
      Gen.const("978-1234567890"),
      Gen.alphaNumStr.suchThat(_.nonEmpty)
    ).map(ISBN(_))
  }

  implicit val arbUserID: Arbitrary[UserID] = Arbitrary {
    Gen.const(UserID(UUID.randomUUID()))
  }

  implicit val arbBookTitle: Arbitrary[BookTitle] = Arbitrary {
    Gen.alphaNumStr.suchThat(_.nonEmpty).map(BookTitle(_))
  }

  implicit val arbAuthorName: Arbitrary[AuthorName] = Arbitrary {
    Gen.alphaNumStr.suchThat(_.nonEmpty).map(AuthorName(_))
  }

  implicit val arbGenre: Arbitrary[Genre] = Arbitrary {
    Gen.oneOf("Fiction", "Non-fiction", "Science", "Biography", "Programming").map(Genre(_))
  }

  "ISBN" should "create valid instances" in {
    val isbn = ISBN("978-0134685991")
    isbn.value shouldBe "978-0134685991"
  }

  it should "support safe construction" in {
    val validISBN = ISBN.safe("978-0134685991")
    validISBN match {
      case Right(isbn) => isbn.value shouldBe "978-0134685991"
      case Left(error) => fail(s"Expected valid ISBN but got error: $error")
    }
    
    val invalidISBN = ISBN.safe("")
    invalidISBN shouldBe a[Left[String, ISBN]]
  }

  it should "maintain type safety" in {
    val isbn = ISBN("123")
    val userID = UserID(UUID.randomUUID())
    
    // This should not compile (different types)
    // isbn should Be userID - Would cause compilation error
    isbn.value shouldBe "123"
  }

  it should "support property-based testing" in {
    forAll { (isbn: ISBN) =>
      isbn.value should not be empty
    }
  }

  "UserID" should "create valid instances with UUID" in {
    val uuid = UUID.randomUUID()
    val userID = UserID(uuid)
    userID.value shouldBe uuid
  }

  it should "be unique for each generation" in {
    val userID1 = UserID(UUID.randomUUID())
    val userID2 = UserID(UUID.randomUUID())
    userID1.value should not equal userID2.value
  }

  it should "support property-based testing" in {
    forAll { (userID: UserID) =>
      userID.value.toString should not be empty
    }
  }

  "BookTitle" should "create valid instances" in {
    val title = BookTitle("Effective Java")
    title.value shouldBe "Effective Java"
  }

  it should "support safe construction with validation" in {
    val validTitle = BookTitle.safe("Valid Title")
    validTitle match {
      case Right(title) => title.value shouldBe "Valid Title"
      case Left(error) => fail(s"Expected valid title but got error: $error")
    }
  }

  it should "handle empty titles safely" in {
    val emptyTitle = BookTitle.safe("")
    emptyTitle shouldBe a[Left[String, BookTitle]]
  }

  it should "support property-based testing" in {
    forAll { (title: BookTitle) =>
      title.value should not be null
    }
  }

  "AuthorName" should "create valid instances" in {
    val author = AuthorName("Joshua Bloch")
    author.value shouldBe "Joshua Bloch"
  }

  it should "support safe construction" in {
    val validAuthor = AuthorName.safe("Valid Author")
    validAuthor match {
      case Right(author) => author.value shouldBe "Valid Author"
      case Left(error) => fail(s"Expected valid author but got error: $error")
    }
  }

  it should "support property-based testing" in {
    forAll { (author: AuthorName) =>
      author.value should not be null
    }
  }

  "Genre" should "create valid instances" in {
    val genre = Genre("Programming")
    genre.value shouldBe "Programming"
  }

  it should "support safe construction" in {
    val validGenre = Genre.safe("Science Fiction")
    validGenre match {
      case Right(genre) => genre.value shouldBe "Science Fiction"
      case Left(error) => fail(s"Expected valid genre but got error: $error")
    }
  }

  it should "support property-based testing" in {
    forAll { (genre: Genre) =>
      genre.value should not be null
    }
  }

  "Opaque types" should "maintain type safety across operations" in {
    val isbn = ISBN("978-0134685991")
    val title = BookTitle("Effective Java")
    val author = AuthorName("Joshua Bloch")
    val genre = Genre("Programming")

    // All should be distinct types
    isbn.value shouldBe "978-0134685991"
    title.value shouldBe "Effective Java"
    author.value shouldBe "Joshua Bloch"
    genre.value shouldBe "Programming"
  }

  it should "work with collections safely" in {
    val isbns = List(ISBN("123"), ISBN("456"), ISBN("789"))
    val titles = List(BookTitle("Book 1"), BookTitle("Book 2"))
    
    isbns should have size 3
    titles should have size 2
    
    isbns.map(_.value) should contain allOf ("123", "456", "789")
    titles.map(_.value) should contain allOf ("Book 1", "Book 2")
  }

  it should "support functional operations" in {
    val isbns = List(ISBN("978-1"), ISBN("978-2"), ISBN("978-3"))
    
    val filtered = isbns.filter(_.value.contains("978"))
    filtered should have size 3
    
    val mapped = isbns.map(isbn => s"ISBN: ${isbn.value}")
    mapped should contain allOf ("ISBN: 978-1", "ISBN: 978-2", "ISBN: 978-3")
  }

  "Extension methods" should "work correctly" in {
    val isbn = ISBN("978-0134685991")
    
    // Test if extension methods exist and work
    isbn.value should not be empty
    isbn.toString should include("978-0134685991")
  }

  "Type equality" should "work correctly" in {
    val isbn1 = ISBN("123")
    val isbn2 = ISBN("123")
    val isbn3 = ISBN("456")
    
    isbn1 shouldBe isbn2
    isbn1 should not equal isbn3
  }

  "Companion object methods" should "work correctly" in {
    // Test safe constructors that return Either
    val isbn = ISBN.safe("978-0134685991")
    val title = BookTitle.safe("Test Title")
    val author = AuthorName.safe("Test Author")
    val genre = Genre.safe("Test Genre")
    
    isbn match {
      case Right(value) => value.value shouldBe "978-0134685991"
      case Left(error) => fail(s"Expected valid ISBN: $error")
    }
    
    title match {
      case Right(value) => value.value shouldBe "Test Title"
      case Left(error) => fail(s"Expected valid title: $error")
    }
    
    author match {
      case Right(value) => value.value shouldBe "Test Author"
      case Left(error) => fail(s"Expected valid author: $error")
    }
    
    genre match {
      case Right(value) => value.value shouldBe "Test Genre"
      case Left(error) => fail(s"Expected valid genre: $error")
    }
  }
}
