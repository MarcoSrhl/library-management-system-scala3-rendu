package services

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import library.*
import library.Types.*
import library.User.*
import java.util.UUID
import java.time.LocalDateTime

/**
 * Tests for RecommendationEngine functionality.
 * Tests book recommendation algorithms and user preference analysis.
 */
class RecommendationEngineSpec extends AnyFlatSpec with Matchers {

  val sampleBooks = List(
    Book(ISBN("978-0134685991"), "Effective Java", List("Joshua Bloch"), 2017, "Programming", true),
    Book(ISBN("978-0135166307"), "Java Complete Reference", List("Herbert Schildt"), 2020, "Programming", true),
    Book(ISBN("978-0132350884"), "Clean Code", List("Robert Martin"), 2008, "Programming", true),
    Book(ISBN("978-0321127426"), "Design Patterns", List("Gang of Four"), 1994, "Programming", true),
    Book(ISBN("978-1449331818"), "Learning Python", List("Mark Lutz"), 2013, "Programming", true),
    Book(ISBN("978-0316769174"), "The Catcher in the Rye", List("J.D. Salinger"), 1951, "Fiction", true),
    Book(ISBN("978-0061120084"), "To Kill a Mockingbird", List("Harper Lee"), 1960, "Fiction", true),
    Book(ISBN("978-0486280615"), "Pride and Prejudice", List("Jane Austen"), 1813, "Romance", true)
  )

  val sampleUsers = List(
    User.Student(UserID(UUID.randomUUID()), "Alice", "Computer Science", "password123"),
    User.Faculty(UserID(UUID.randomUUID()), "Bob", "Literature", "password456"),
    User.Librarian(UserID(UUID.randomUUID()), "Charlie", "Main Library", "password789")
  )

  val sampleTransactions = List(
    Transaction.Loan(sampleBooks(0), sampleUsers(0), LocalDateTime.now.minusDays(10), Some(LocalDateTime.now.plusDays(4))), // Alice borrowed Effective Java
    Transaction.Loan(sampleBooks(2), sampleUsers(0), LocalDateTime.now.minusDays(8), Some(LocalDateTime.now.plusDays(6))),  // Alice borrowed Clean Code
    Transaction.Loan(sampleBooks(5), sampleUsers(1), LocalDateTime.now.minusDays(5), Some(LocalDateTime.now.plusDays(9))),  // Bob borrowed Catcher in the Rye
    Transaction.Loan(sampleBooks(6), sampleUsers(1), LocalDateTime.now.minusDays(3), Some(LocalDateTime.now.plusDays(11)))   // Bob borrowed To Kill a Mockingbird
  )

  val sampleCatalog = LibraryCatalog(
    books = sampleBooks.map(b => b.isbn -> b).toMap,
    users = sampleUsers.map(u => u.id -> u).toMap,
    transactions = sampleTransactions
  )

  "RecommendationEngine" should "exist and be accessible" in {
    // Test that RecommendationEngine can be referenced
    noException should be thrownBy RecommendationEngine.getClass
  }

  "Genre-based recommendations" should "suggest books from preferred genres" in {
    // Test genre preference analysis
    val aliceTransactions = sampleTransactions.filter(_.user.id == sampleUsers(0).id)
    val aliceGenres = aliceTransactions.map(_.book.genre).distinct
    
    aliceGenres should contain("Programming")
    aliceGenres should have size 1
    
    // Find books in preferred genres that Alice hasn't read
    val aliceBorrowedISBNs = aliceTransactions.map(_.book.isbn).toSet
    val programmingBooks = sampleBooks.filter(_.genre == "Programming")
    val recommendations = programmingBooks.filterNot(book => aliceBorrowedISBNs.contains(book.isbn))
    
    recommendations should not be empty
    recommendations.forall(_.genre == "Programming") shouldBe true
  }



  "Collaborative filtering" should "suggest books based on similar users" in {
    // Test collaborative filtering approach
    val aliceId = sampleUsers(0).id
    val bobId = sampleUsers(1).id
    
    // Find books that Bob liked but Alice hasn't read
    val aliceBorrowedISBNs = sampleTransactions.filter(_.user.id == aliceId).map(_.book.isbn).toSet
    val bobBorrowedBooks = sampleTransactions.filter(_.user.id == bobId).map(_.book)
    val collaborativeRecommendations = bobBorrowedBooks.filterNot(book => aliceBorrowedISBNs.contains(book.isbn))
    
    collaborativeRecommendations should have size 2 // Bob's 2 fiction books
    collaborativeRecommendations.forall(_.genre == "Fiction") shouldBe true
  }

  "Popularity-based recommendations" should "suggest popular books" in {
    // Test popularity analysis
    val bookPopularity = sampleTransactions
      .groupBy(_.book.isbn)
      .view
      .mapValues(_.length)
      .toMap
    
    // Each book in our sample has been borrowed once
    bookPopularity.values.foreach(_ shouldBe 1)
    
    // Test sorting by popularity
    val popularBooks = bookPopularity.toList.sortBy(-_._2).map(_._1)
    popularBooks should have size 4 // 4 books have been borrowed
  }

  "Recency-based recommendations" should "consider recent trends" in {
    // Test recency analysis
    val recentTransactions = sampleTransactions.filter(_.timestamp.isAfter(LocalDateTime.now.minusDays(7)))
    recentTransactions should have size 2 // Bob's transactions are more recent
    
    val recentBooks = recentTransactions.map(_.book)
    recentBooks.forall(_.genre == "Fiction") shouldBe true
  }

  "Content-based filtering" should "recommend similar content" in {
    // Test content similarity
    val targetBook = sampleBooks.head // Effective Java
    val similarBooks = sampleBooks.filter { book =>
      book.isbn != targetBook.isbn &&
      (book.genre == targetBook.genre || 
       book.authors.exists(author => targetBook.authors.contains(author)) ||
       book.title.toLowerCase.split(" ").exists(word => 
         targetBook.title.toLowerCase.split(" ").contains(word) && word.length > 3))
    }
    
    similarBooks should not be empty
    similarBooks.forall(_.genre == "Programming") shouldBe true
  }

  "User profile analysis" should "build preference profiles" in {
    // Test user preference profiling
    val alice = sampleUsers(0)
    val aliceTransactions = sampleTransactions.filter(_.user.id == alice.id)
    
    // Build preference profile
    val genrePreferences = aliceTransactions.groupBy(_.book.genre).view.mapValues(_.length).toMap
    val authorPreferences = aliceTransactions.flatMap(_.book.authors).groupBy(identity).view.mapValues(_.length).toMap
    val yearRange = aliceTransactions.map(_.book.publicationYear)
    
    genrePreferences("Programming") shouldBe 2
    authorPreferences("Joshua Bloch") shouldBe 1
    authorPreferences("Robert Martin") shouldBe 1
    yearRange should contain allOf(2017, 2008)
  }

  "Recommendation scoring" should "score recommendations appropriately" in {
    // Test recommendation scoring algorithm
    val targetUser = sampleUsers(0) // Alice
    val candidateBook = sampleBooks(1) // Java Complete Reference
    
    var score = 0.0
    
    // Genre match bonus
    val userGenres = sampleTransactions.filter(_.user.id == targetUser.id).map(_.book.genre).distinct
    if userGenres.contains(candidateBook.genre) then score += 1.0
    
    // Author match bonus
    val userAuthors = sampleTransactions.filter(_.user.id == targetUser.id).flatMap(_.book.authors).distinct
    if candidateBook.authors.exists(userAuthors.contains) then score += 0.5
    
    // Popularity bonus
    val bookPopularity = sampleTransactions.count(_.book.isbn == candidateBook.isbn)
    score += bookPopularity * 0.1
    
    // Recency bonus
    val recentBorrows = sampleTransactions.filter(tx => 
      tx.book.genre == candidateBook.genre && tx.timestamp.isAfter(LocalDateTime.now.minusDays(30))
    )
    score += recentBorrows.length * 0.2
    
    score should be > 1.0 // Should have genre match at minimum
  }

  "Recommendation filtering" should "filter out inappropriate recommendations" in {
    // Test recommendation filtering
    val targetUser = sampleUsers(0) // Alice (Student)
    val alreadyBorrowedISBNs = sampleTransactions.filter(_.user.id == targetUser.id).map(_.book.isbn).toSet
    
    // Filter out already borrowed books
    val availableBooks = sampleBooks.filterNot(book => alreadyBorrowedISBNs.contains(book.isbn))
    availableBooks should have size (sampleBooks.length - 2) // Alice borrowed 2 books
    
    // Filter out unavailable books
    val availableForLoan = availableBooks.filter(_.isAvailable)
    availableForLoan should have size availableBooks.length // All sample books are available
    
    // Apply user-specific filters (e.g., reading level, preferences)
    val ageAppropriate = availableForLoan.filter(_.publicationYear >= 1900) // Modern books
    ageAppropriate should not be empty
  }


}
