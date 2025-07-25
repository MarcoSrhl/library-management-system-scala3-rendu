package library

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import Types.ISBN
import SearchOperations.* // Import all SearchOperations including extension methods

/**
 * Property-based tests for SearchOperations using ScalaCheck.
 * 
 * These tests verify that the search operations and predicate combinators
 * maintain important mathematical and logical properties across many
 * randomly generated inputs.
 */
class SearchOperationsPropertySpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  // Generators for test data
  implicit val arbISBN: Arbitrary[ISBN] = Arbitrary {
    Gen.alphaNumStr.suchThat(_.nonEmpty).map(ISBN(_))
  }

  implicit val arbBook: Arbitrary[Book] = Arbitrary {
    for {
      isbn <- Arbitrary.arbitrary[ISBN]
      title <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      author <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      year <- Gen.choose(1000, 2024)
      genre <- Gen.oneOf("Fiction", "Non-fiction", "Science", "Biography", "Programming", "History", "Art")
      available <- Gen.oneOf(true, false)
    } yield Book(isbn, title, List(author), year, genre, available)
  }

  // Generator for non-empty book lists
  val nonEmptyBookListGen: Gen[List[Book]] = Gen.nonEmptyListOf(Arbitrary.arbitrary[Book])

  "SearchOperations.Predicates" should "satisfy logical consistency properties" in {
    forAll { (books: List[Book]) =>
      // Law of excluded middle: every book is either available or not available
      books.foreach { book =>
        SearchOperations.Predicates.isAvailable(book) || SearchOperations.Predicates.isUnavailable(book) shouldBe true
      }
      
      // Law of non-contradiction: no book can be both available and unavailable
      books.foreach { book =>
        !(SearchOperations.Predicates.isAvailable(book) && SearchOperations.Predicates.isUnavailable(book)) shouldBe true
      }
    }
  }

  "SearchOperations.Predicates.all" should "always return true" in {
    forAll { (book: Book) =>
      SearchOperations.Predicates.all(book) shouldBe true
    }
  }

  "SearchOperations.Predicates.byTitle" should "be case-insensitive" in {
    forAll { (books: List[Book], searchTerm: String) =>
      whenever(searchTerm.nonEmpty) {
        val lowerCaseResults = books.filter(SearchOperations.Predicates.byTitle(searchTerm.toLowerCase))
        val upperCaseResults = books.filter(SearchOperations.Predicates.byTitle(searchTerm.toUpperCase))
        val mixedCaseResults = books.filter(SearchOperations.Predicates.byTitle(searchTerm))
        
        lowerCaseResults shouldEqual upperCaseResults
        lowerCaseResults shouldEqual mixedCaseResults
      }
    }
  }

  "SearchOperations.Predicates.byAuthor" should "find books by partial author names" in {
    forAll { (book: Book) =>
      whenever(book.authors.nonEmpty && book.authors.head.length > 2) {
        val authorSubstring = book.authors.head.substring(0, 2)
        SearchOperations.Predicates.byAuthor(authorSubstring)(book) shouldBe true
      }
    }
  }

  "SearchOperations.Predicates.byGenre" should "be case-insensitive and support partial matching" in {
    forAll { (book: Book) =>
      whenever(book.genre.length > 1) {
        val genreSubstring = book.genre.substring(0, math.min(book.genre.length, 3))
        
        // Should match with original case
        SearchOperations.Predicates.byGenre(genreSubstring)(book) shouldBe true
        
        // Should match with different case
        SearchOperations.Predicates.byGenre(genreSubstring.toUpperCase)(book) shouldBe true
        SearchOperations.Predicates.byGenre(genreSubstring.toLowerCase)(book) shouldBe true
      }
    }
  }

  "SearchOperations.Predicates.byYear" should "match exact years only" in {
    forAll { (book: Book, testYear: Int) =>
      val result = SearchOperations.Predicates.byYear(testYear)(book)
      result shouldEqual (book.publicationYear == testYear)
    }
  }

  "SearchOperations.Predicates.byYearRange" should "satisfy range properties" in {
    forAll { (book: Book, minYear: Int, maxYear: Int) =>
      whenever(minYear <= maxYear) {
        val inRange = SearchOperations.Predicates.byYearRange(minYear, maxYear)(book)
        val expectedInRange = book.publicationYear >= minYear && book.publicationYear <= maxYear
        
        inRange shouldEqual expectedInRange
      }
    }
  }

  "Predicate combinators" should "satisfy Boolean algebra laws" in {
    forAll { (books: List[Book]) =>
      val p1 = SearchOperations.Predicates.isAvailable
      val p2 = SearchOperations.Predicates.byGenre("Fiction")
      val p3 = SearchOperations.Predicates.byYearRange(2000, 2024)

      books.foreach { book =>
        // Commutativity: p1 AND p2 = p2 AND p1
        p1.and(p2)(book) shouldEqual p2.and(p1)(book)
        p1.or(p2)(book) shouldEqual p2.or(p1)(book)

        // Associativity: (p1 AND p2) AND p3 = p1 AND (p2 AND p3)
        p1.and(p2).and(p3)(book) shouldEqual p1.and(p2.and(p3))(book)
        p1.or(p2).or(p3)(book) shouldEqual p1.or(p2.or(p3))(book)

        // Identity: p1 AND true = p1
        p1.and(SearchOperations.Predicates.all)(book) shouldEqual p1(book)

        // Annihilation: p1 AND false = false  
        p1.and(_ => false)(book) shouldBe false

        // Double negation: NOT (NOT p1) = p1
        p1.not.not(book) shouldEqual p1(book)

        // De Morgan's laws: NOT (p1 AND p2) = (NOT p1) OR (NOT p2)
        p1.and(p2).not(book) shouldEqual p1.not.or(p2.not)(book)
        p1.or(p2).not(book) shouldEqual p1.not.and(p2.not)(book)
      }
    }
  }

  "SearchOperations.searchBy" should "preserve input list order" in {
    forAll(nonEmptyBookListGen) { (books: List[Book]) =>
      val predicate = SearchOperations.Predicates.all
      val results = SearchOperations.searchBy(predicate)(books)
      
      // All books should be present since we use the 'all' predicate
      results shouldEqual books
      
      // Order should be preserved
      results.zip(books).foreach { case (result, original) =>
        result shouldEqual original
      }
    }
  }

  it should "only return books that satisfy the predicate" in {
    forAll { (books: List[Book]) =>
      val predicate = SearchOperations.Predicates.isAvailable
      val results = SearchOperations.searchBy(predicate)(books)
      
      // All results should satisfy the predicate
      results.foreach { book =>
        predicate(book) shouldBe true
      }
      
      // All books that satisfy the predicate should be in results
      val expectedResults = books.filter(predicate)
      results shouldEqual expectedResults
    }
  }

  it should "return empty list when no books match" in {
    forAll { (books: List[Book]) =>
      val impossiblePredicate: SearchOperations.BookPredicate = _ => false
      val results = SearchOperations.searchBy(impossiblePredicate)(books)
      
      results shouldBe empty
    }
  }
}
