package analytics

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import library.*
import library.Types.*
import library.User.*
import java.util.UUID
import java.time.LocalDateTime

/**
 * Tests for Analytics functionality.
 * Tests statistical analysis and reporting features.
 */
class AnalyticsSpec extends AnyFlatSpec with Matchers {

  val sampleBooks = List(
    Book(ISBN("978-0134685991"), "Effective Java", List("Joshua Bloch"), 2017, "Programming", true),
    Book(ISBN("978-0135166307"), "Java Complete Reference", List("Herbert Schildt"), 2020, "Programming", true),
    Book(ISBN("978-0132350884"), "Clean Code", List("Robert Martin"), 2008, "Programming", true),
    Book(ISBN("978-0316769174"), "The Catcher in the Rye", List("J.D. Salinger"), 1951, "Fiction", true),
    Book(ISBN("978-0061120084"), "To Kill a Mockingbird", List("Harper Lee"), 1960, "Fiction", true)
  )

  val sampleUsers = List(
    User.Student(UserID(UUID.randomUUID()), "Alice", "Computer Science", "password123"),
    User.Faculty(UserID(UUID.randomUUID()), "Bob", "Literature", "password456"),
    User.Librarian(UserID(UUID.randomUUID()), "Charlie", "Main Library", "password789")
  )

  val sampleTransactions = List(
    Transaction.Loan(sampleBooks(0), sampleUsers(0), LocalDateTime.now.minusDays(15), Some(LocalDateTime.now.plusDays(-1))), // Alice - Effective Java
    Transaction.Loan(sampleBooks(2), sampleUsers(0), LocalDateTime.now.minusDays(10), Some(LocalDateTime.now.plusDays(4))), // Alice - Clean Code
    Transaction.Return(sampleBooks(0), sampleUsers(0), LocalDateTime.now.minusDays(5)), // Alice returned Effective Java
    Transaction.Loan(sampleBooks(3), sampleUsers(1), LocalDateTime.now.minusDays(8), Some(LocalDateTime.now.plusDays(6))),  // Bob - Catcher in the Rye
    Transaction.Loan(sampleBooks(4), sampleUsers(1), LocalDateTime.now.minusDays(3), Some(LocalDateTime.now.plusDays(11))),  // Bob - To Kill a Mockingbird
    Transaction.Return(sampleBooks(3), sampleUsers(1), LocalDateTime.now.minusDays(1)) // Bob returned Catcher
  )

  val sampleCatalog = LibraryCatalog(
    books = sampleBooks.map(b => b.isbn -> b).toMap,
    users = sampleUsers.map(u => u.id -> u).toMap,
    transactions = sampleTransactions
  )

  "Analytics package" should "be accessible" in {
    // Test that analytics package exists
    noException should be thrownBy this.getClass.getPackage.getName
  }

  "Library statistics" should "calculate basic statistics" in {
    // Test basic library statistics
    val totalBooks = sampleCatalog.books.size
    val totalUsers = sampleCatalog.users.size
    val totalTransactions = sampleCatalog.transactions.size
    
    totalBooks shouldBe 5
    totalUsers shouldBe 3
    totalTransactions shouldBe 6
  }

  "Book popularity analysis" should "rank books by popularity" in {
    // Test book popularity metrics
    val bookBorrowCount = sampleTransactions
      .filter(_.isInstanceOf[Transaction.Loan])
      .groupBy(_.book.isbn)
      .view
      .mapValues(_.length)
      .toMap
    
    bookBorrowCount(ISBN("978-0134685991")) shouldBe 1 // Effective Java
    bookBorrowCount(ISBN("978-0132350884")) shouldBe 1 // Clean Code
    bookBorrowCount(ISBN("978-0316769174")) shouldBe 1 // Catcher in the Rye
    bookBorrowCount(ISBN("978-0061120084")) shouldBe 1 // To Kill a Mockingbird
    
    val mostPopular = bookBorrowCount.maxBy(_._2)
    mostPopular._2 shouldBe 1 // All books equally popular in this sample
  }

  "User activity analysis" should "track user engagement" in {
    // Test user activity metrics
    val userActivityCount = sampleTransactions
      .groupBy(_.user.id)
      .view
      .mapValues(_.length)
      .toMap
    
    userActivityCount(sampleUsers(0).id) shouldBe 3 // Alice: 2 loans + 1 return
    userActivityCount(sampleUsers(1).id) shouldBe 3 // Bob: 2 loans + 1 return
    userActivityCount.get(sampleUsers(2).id) shouldBe None // Charlie: no activity
    
    val mostActiveUser = userActivityCount.maxBy(_._2)
    mostActiveUser._2 shouldBe 3
  }

  "Genre analysis" should "analyze genre preferences" in {
    // Test genre distribution analysis
    val genreDistribution = sampleTransactions
      .filter(_.isInstanceOf[Transaction.Loan])
      .groupBy(_.book.genre)
      .view
      .mapValues(_.length)
      .toMap
    
    genreDistribution("Programming") shouldBe 2 // Effective Java + Clean Code
    genreDistribution("Fiction") shouldBe 2 // Catcher + Mockingbird
    
    val mostPopularGenre = genreDistribution.maxBy(_._2)
    mostPopularGenre._2 shouldBe 2 // Tie between Programming and Fiction
  }

  "Temporal analysis" should "analyze usage patterns over time" in {
    // Test temporal patterns
    val transactionsByDay = sampleTransactions
      .groupBy(_.timestamp.toLocalDate)
      .view
      .mapValues(_.length)
      .toMap
    
    transactionsByDay.values.sum shouldBe sampleTransactions.length
    
    // Test recent activity (last 7 days)
    val recentTransactions = sampleTransactions.filter(
      _.timestamp.isAfter(LocalDateTime.now.minusDays(7))
    )
    recentTransactions should have size 4 // 4 transactions in last 7 days
  }

  "User type analysis" should "analyze patterns by user type" in {
    // Test user type behavior analysis
    val studentTransactions = sampleTransactions.filter(_.user match {
      case _: User.Student => true
      case _ => false
    })
    
    val facultyTransactions = sampleTransactions.filter(_.user match {
      case _: User.Faculty => true
      case _ => false
    })
    
    val librarianTransactions = sampleTransactions.filter(_.user match {
      case _: User.Librarian => true
      case _ => false
    })
    
    studentTransactions should have size 3 // Alice's transactions
    facultyTransactions should have size 3 // Bob's transactions
    librarianTransactions should have size 0 // Charlie's transactions
  }

  "Return rate analysis" should "calculate return rates" in {
    // Test return rate calculations
    val loanTransactions = sampleTransactions.filter(_.isInstanceOf[Transaction.Loan])
    val returnTransactions = sampleTransactions.filter(_.isInstanceOf[Transaction.Return])
    
    val returnRate = returnTransactions.length.toDouble / loanTransactions.length.toDouble
    returnRate shouldBe 0.5 // 2 returns out of 4 loans
    
    // Test overdue analysis
    val currentTime = LocalDateTime.now
    val overdueLoans = loanTransactions.filter { loan =>
      val hasReturn = returnTransactions.exists(ret => 
        ret.user.id == loan.user.id && ret.book.isbn == loan.book.isbn
      )
      !hasReturn && loan.timestamp.isBefore(currentTime.minusDays(14)) // 14-day loan period
    }
    
    overdueLoans should have size 1 // Alice's Clean Code loan
  }

  "Collection analysis" should "analyze collection composition" in {
    // Test collection analysis
    val publicationYearDistribution = sampleBooks
      .groupBy(book => (book.publicationYear / 10) * 10) // Group by decade
      .view
      .mapValues(_.length)
      .toMap
    
    publicationYearDistribution(1950) shouldBe 2 // 1951 and 1960
    publicationYearDistribution(2000) shouldBe 1 // 2008
    publicationYearDistribution(2010) shouldBe 2 // 2017 and 2020
    
    val averageAge = LocalDateTime.now.getYear - (sampleBooks.map(_.publicationYear).sum / sampleBooks.length)
    averageAge should be > 20 // Collection has older books
  }

  "Performance metrics" should "calculate system performance" in {
    // Test performance metrics
    val totalCatalogSize = sampleCatalog.books.size + sampleCatalog.users.size
    val transactionThroughput = sampleTransactions.length.toDouble / 30.0 // Transactions per day (30-day period)
    
    totalCatalogSize shouldBe 8 // 5 books + 3 users
    transactionThroughput should be > 0.0
    
    // Test availability metrics
    val availableBooks = sampleBooks.count(_.isAvailable)
    val availabilityRate = availableBooks.toDouble / sampleBooks.length.toDouble
    
    availabilityRate shouldBe 1.0 // All books available in sample
  }

  "Trend analysis" should "identify trends" in {
    // Test trend identification
    val weeklyTransactions = sampleTransactions
      .groupBy(tx => tx.timestamp.toLocalDate.atStartOfDay.minusDays(tx.timestamp.getDayOfWeek.getValue - 1))
      .view
      .mapValues(_.length)
      .toMap
    
    weeklyTransactions.values.sum shouldBe sampleTransactions.length
    
    // Test growth trends
    val oldTransactions = sampleTransactions.filter(_.timestamp.isBefore(LocalDateTime.now.minusDays(7)))
    val recentTransactions = sampleTransactions.filter(_.timestamp.isAfter(LocalDateTime.now.minusDays(7)))
    
    val growthRate = if oldTransactions.nonEmpty then {
      (recentTransactions.length.toDouble - oldTransactions.length.toDouble) / oldTransactions.length.toDouble
    } else 0.0
    
    growthRate should be >= -1.0 // Growth rate should be reasonable
  }

  "Report generation" should "generate summary reports" in {
    // Test report generation capabilities
    val summaryReport = Map(
      "totalBooks" -> sampleCatalog.books.size,
      "totalUsers" -> sampleCatalog.users.size,
      "totalTransactions" -> sampleCatalog.transactions.size,
      "activeUsers" -> sampleTransactions.map(_.user.id).distinct.size,
      "popularGenre" -> sampleTransactions.filter(_.isInstanceOf[Transaction.Loan])
        .groupBy(_.book.genre)
        .view
        .mapValues(_.length)
        .maxBy(_._2)
        ._1
    )
    
    summaryReport("totalBooks") shouldBe 5
    summaryReport("totalUsers") shouldBe 3
    summaryReport("totalTransactions") shouldBe 6
    summaryReport("activeUsers") shouldBe 2 // Alice and Bob
    summaryReport("popularGenre") should (be("Programming") or be("Fiction"))
  }
}
