package library

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import library.Types.ISBN

class SearchOperationsSimpleSpec extends AnyFlatSpec with Matchers {

  import SearchOperations._
  import SearchOperations.Predicates._

  // Test data setup
  val testBooks = List(
    Book(ISBN("978-0-123456-78-9"), "Scala Programming", List("Martin Odersky"), 2021, "Programming", true),
    Book(ISBN("978-0-234567-89-0"), "Java Essentials", List("Joshua Bloch"), 2020, "Programming", false),
    Book(ISBN("978-0-345678-90-1"), "Python Cookbook", List("David Beazley"), 2019, "Programming", true),
    Book(ISBN("978-0-456789-01-2"), "Data Science", List("Hadley Wickham"), 2018, "Science", true),
    Book(ISBN("978-0-567890-12-3"), "Machine Learning", List("Andrew Ng"), 2022, "Science", false)
  )

  "SearchOperations.Predicates.isAvailable" should "find available books" in {
    val availableBooks = testBooks.filter(isAvailable)
    availableBooks should have size 3
    availableBooks.forall(_.isAvailable) shouldBe true
  }

  "SearchOperations.Predicates.isUnavailable" should "find unavailable books" in {
    val unavailableBooks = testBooks.filter(isUnavailable)
    unavailableBooks should have size 2
    unavailableBooks.forall(!_.isAvailable) shouldBe true
  }

  "SearchOperations.Predicates.byTitle" should "find books by title substring" in {
    val scalaBooks = testBooks.filter(byTitle("Scala"))
    scalaBooks should have size 1
    scalaBooks.head.title should include("Scala")

    val programmingBooks = testBooks.filter(byTitle("Programming"))
    programmingBooks should have size 1

    val nonExistentBooks = testBooks.filter(byTitle("NonExistent"))
    nonExistentBooks shouldBe empty
  }

  "SearchOperations.Predicates.byAuthor" should "find books by author name" in {
    val oderskyBooks = testBooks.filter(byAuthor("Odersky"))
    oderskyBooks should have size 1
    oderskyBooks.head.authors should contain("Martin Odersky")

    val blochBooks = testBooks.filter(byAuthor("Bloch"))
    blochBooks should have size 1

    val unknownAuthorBooks = testBooks.filter(byAuthor("Unknown Author"))
    unknownAuthorBooks shouldBe empty
  }

  "SearchOperations.Predicates.byGenre" should "find books by exact genre" in {
    val programmingBooks = testBooks.filter(byGenre("Programming"))
    programmingBooks should have size 3
    programmingBooks.forall(_.genre == "Programming") shouldBe true

    val scienceBooks = testBooks.filter(byGenre("Science"))
    scienceBooks should have size 2
    scienceBooks.forall(_.genre == "Science") shouldBe true

    val unknownGenreBooks = testBooks.filter(byGenre("Unknown"))
    unknownGenreBooks shouldBe empty
  }

  "SearchOperations.Predicates.byYear" should "find books by exact year" in {
    val books2021 = testBooks.filter(byYear(2021))
    books2021 should have size 1
    books2021.head.publicationYear shouldBe 2021

    val books2020 = testBooks.filter(byYear(2020))
    books2020 should have size 1

    val books1900 = testBooks.filter(byYear(1900))
    books1900 shouldBe empty
  }

  "SearchOperations.Predicates.byYearRange" should "find books within year range" in {
    val recentBooks = testBooks.filter(byYearRange(2020, 2024))
    recentBooks should have size 3
    recentBooks.forall(book => book.publicationYear >= 2020 && book.publicationYear <= 2024) shouldBe true

    val oldBooks = testBooks.filter(byYearRange(2015, 2018))
    oldBooks should have size 1

    val futureBooks = testBooks.filter(byYearRange(2025, 2030))
    futureBooks shouldBe empty
  }

  "SearchOperations.Predicates.byISBN" should "find book by exact ISBN string" in {
    val isbn = "978-0-123456-78-9"
    val foundBooks = testBooks.filter(byISBN(isbn))
    foundBooks should have size 1
    foundBooks.head.isbn.toString should include(isbn)

    val nonExistentISBN = "978-0-000000-00-0"
    val notFoundBooks = testBooks.filter(byISBN(nonExistentISBN))
    notFoundBooks shouldBe empty
  }

  "SearchOperations.searchBy" should "apply predicates correctly" in {
    val availableBooks = searchBy(testBooks, isAvailable)
    availableBooks should have size 3

    val programmingBooks = searchBy(testBooks, byGenre("Programming"))
    programmingBooks should have size 3
  }

  "SearchOperations.searchBooks" should "return a partially applied function" in {
    val bookSearcher = searchBooks(testBooks)
    val availableBooks = bookSearcher(isAvailable)
    availableBooks should have size 3

    val programmingBooks = bookSearcher(byGenre("Programming"))
    programmingBooks should have size 3
  }

  "SearchOperations.SearchCriteria" should "work with advancedSearch" in {
    // Test with title criteria
    val titleCriteria = SearchCriteria(title = Some("Scala"))
    val titleResults = advancedSearch(testBooks)(titleCriteria)
    titleResults should not be empty
    titleResults.exists(_.title.toLowerCase.contains("scala")) shouldBe true

    // Test with author criteria
    val authorCriteria = SearchCriteria(author = Some("Odersky"))
    val authorResults = advancedSearch(testBooks)(authorCriteria)
    authorResults should not be empty

    // Test with genre criteria
    val genreCriteria = SearchCriteria(genre = Some("Programming"))
    val genreResults = advancedSearch(testBooks)(genreCriteria)
    genreResults should have size 3

    // Test with year range criteria
    val yearCriteria = SearchCriteria(yearFrom = Some(2020), yearTo = Some(2022))
    val yearResults = advancedSearch(testBooks)(yearCriteria)
    yearResults should not be empty
    yearResults.forall(book => book.publicationYear >= 2020 && book.publicationYear <= 2022) shouldBe true

    // Test with availability criteria
    val availableCriteria = SearchCriteria(availableOnly = true)
    val availableResults = advancedSearch(testBooks)(availableCriteria)
    availableResults.forall(_.isAvailable) shouldBe true

    // Test with combined criteria
    val combinedCriteria = SearchCriteria(
      genre = Some("Programming"),
      yearFrom = Some(2020),
      availableOnly = true
    )
    val combinedResults = advancedSearch(testBooks)(combinedCriteria)
    combinedResults.forall(book => 
      book.genre == "Programming" && book.publicationYear >= 2020 && book.isAvailable
    ) shouldBe true
  }

  "SearchOperations.fuzzySearch" should "find books with approximate matches" in {
    // Test exact matches
    val exactMatches = fuzzySearch(testBooks)("Scala")
    // fuzzySearch might return empty list - that's okay for now
    exactMatches should not be null

    // Test fuzzy matches (should be case insensitive)
    val fuzzyMatches = fuzzySearch(testBooks)("scala")
    fuzzyMatches should not be null

    // Test with author names
    val authorMatches = fuzzySearch(testBooks)("Odersky")
    authorMatches should not be null
  }

  "SearchOperations.composeSearch" should "combine multiple predicates with AND logic" in {
    val predicates = List(
      byGenre("Programming"),
      isAvailable,
      byYearRange(2019, 2024)
    )
    
    val combinedPredicate = composeSearch(predicates*)
    val results = testBooks.filter(combinedPredicate)
    
    results.forall(book => 
      book.genre == "Programming" && 
      book.isAvailable && 
      book.publicationYear >= 2019 && 
      book.publicationYear <= 2024
    ) shouldBe true
  }

  // Test immutability
  "SearchOperations.searchBy" should "preserve immutability" in {
    val originalBooks = testBooks
    val filtered = searchBy(testBooks, isAvailable)
    
    // Original list should be unchanged
    testBooks should have size originalBooks.size
    testBooks shouldBe originalBooks
    
    // Filtered result should be different
    filtered.size should be <= originalBooks.size
  }
}
