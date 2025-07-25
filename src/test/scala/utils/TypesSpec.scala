package utils

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen

import Types.*
import java.util.UUID

class TypesSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks:

  "ISBN" should "create valid instances" in {
    val isbn = ISBN("978-3-16-148410-0")
    isbn.value shouldBe "978-3-16-148410-0"
  }

  it should "be opaque and type-safe" in {
    val isbn1 = ISBN("978-3-16-148410-0")
    val isbn2 = ISBN("978-3-16-148410-0")
    val isbn3 = ISBN("978-0-123-45678-9")
    
    isbn1 shouldBe isbn2
    isbn1 should not be isbn3
  }

  it should "work in collections" in {
    val isbns = Set(
      ISBN("978-3-16-148410-0"),
      ISBN("978-0-123-45678-9"),
      ISBN("978-3-16-148410-0") // duplicate
    )
    
    isbns should have size 2
  }

  "UserID" should "create unique instances" in {
    val uuid1 = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()
    
    val userId1 = UserID(uuid1)
    val userId2 = UserID(uuid2)
    val userId3 = UserID(uuid1) // same UUID
    
    userId1 should not be userId2
    userId1 shouldBe userId3
  }

  it should "maintain UUID properties" in {
    forAll { (uuid: UUID) =>
      val userId = UserID(uuid)
      userId.value shouldBe uuid
    }
  }

  "BookTitle" should "handle various string inputs" in {
    val title1 = BookTitle("The Great Gatsby")
    val title2 = BookTitle("1984")
    val title3 = BookTitle("Scala Programming: A Comprehensive Guide")
    
    title1.value shouldBe "The Great Gatsby"
    title2.value shouldBe "1984"
    title3.value shouldBe "Scala Programming: A Comprehensive Guide"
  }

  it should "be case-sensitive" in {
    val title1 = BookTitle("scala")
    val title2 = BookTitle("Scala")
    
    title1 should not be title2
  }

  "AuthorName" should "handle author names correctly" in {
    val author1 = AuthorName("Martin Odersky")
    val author2 = AuthorName("Joshua Bloch")
    val author3 = AuthorName("Martin Odersky") // duplicate
    
    author1.value shouldBe "Martin Odersky"
    author2.value shouldBe "Joshua Bloch"
    author1 shouldBe author3
  }

  "Genre" should "categorize books properly" in {
    val genres = List(
      Genre("Fiction"),
      Genre("Non-Fiction"),
      Genre("Science Fiction"),
      Genre("Programming"),
      Genre("History")
    )
    
    genres should have size 5
    genres.map(_.value) should contain allOf ("Fiction", "Programming", "History")
  }

  it should "support property-based testing" in {
    forAll(Gen.alphaNumStr.suchThat(_.nonEmpty)) { genreStr =>
      val genre = Genre(genreStr)
      genre.value shouldBe genreStr
    }
  }

  "Type safety" should "prevent mixing opaque types" in {
    val isbn = ISBN("978-3-16-148410-0")
    val title = BookTitle("Test Book")
    val author = AuthorName("Test Author")
    val genre = Genre("Test Genre")
    
    // These should all be different types, even though they wrap strings
    // We can't directly compare them - this is ensured by the type system
    isbn.value shouldBe a[String]
    title.value shouldBe a[String]
    author.value shouldBe a[String]
    genre.value shouldBe a[String]
  }

  "Collections with opaque types" should "work correctly" in {
    val books = Map(
      ISBN("978-3-16-148410-0") -> BookTitle("Book 1"),
      ISBN("978-0-123-45678-9") -> BookTitle("Book 2")
    )
    
    books should have size 2
    books(ISBN("978-3-16-148410-0")) shouldBe BookTitle("Book 1")
  }

  it should "support filtering and mapping" in {
    val isbns = List(
      ISBN("978-3-16-148410-0"),
      ISBN("978-0-123-45678-9"),
      ISBN("978-1-234-56789-0")
    )
    
    val filtered = isbns.filter(_.value.startsWith("978-3"))
    filtered should have size 1
    filtered.head shouldBe ISBN("978-3-16-148410-0")
    
    val mapped = isbns.map(_.value.length)
    mapped should contain only 17
  }

  "Property-based tests" should "validate type construction" in {
    forAll(Gen.alphaNumStr.suchThat(_.nonEmpty)) { str =>
      val isbn = ISBN(str)
      val title = BookTitle(str)
      val author = AuthorName(str)
      val genre = Genre(str)
      
      isbn.value shouldBe str
      title.value shouldBe str
      author.value shouldBe str
      genre.value shouldBe str
    }
  }

  it should "maintain equality semantics" in {
    forAll(Gen.alphaNumStr) { str =>
      val isbn1 = ISBN(str)
      val isbn2 = ISBN(str)
      
      isbn1 shouldBe isbn2
      isbn1.hashCode shouldBe isbn2.hashCode
    }
  }

  "Pattern matching" should "work with opaque types" in {
    val isbn = ISBN("978-3-16-148410-0")
    
    val result = isbn.value match {
      case value if value.startsWith("978") => "Valid ISBN-13"
      case _ => "Invalid ISBN"
    }
    
    result shouldBe "Valid ISBN-13"
  }

  it should "support extraction in collections" in {
    val data = List(
      (ISBN("978-3-16-148410-0"), BookTitle("Book 1")),
      (ISBN("978-0-123-45678-9"), BookTitle("Book 2"))
    )
    
    val titles = data.collect {
      case (isbn, title) if isbn.value.contains("148410") => title.value
    }
    
    titles shouldBe List("Book 1")
  }
