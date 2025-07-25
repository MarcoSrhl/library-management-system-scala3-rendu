package library

import Types.{ISBN, UserID}
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Recommendation engine using functional composition to suggest books
 * based on user reading history and behavior patterns.
 */
object RecommendationEngine:

  // Type aliases for recommendation functions
  type RecommendationStrategy[T] = (User, LibraryCatalog) => List[T]
  type BookRecommendation = RecommendationStrategy[Book]
  type ScoredRecommendation = (Book, Double)
  type RecommendationFilter = Book => Boolean
  type RecommendationScorer = (User, Book, LibraryCatalog) => Double

  /**
   * User reading profile extracted from transaction history
   */
  case class ReadingProfile(
    favoriteGenres: Map[String, Double],
    favoriteAuthors: Map[String, Double],
    averageBookAge: Double,
    readingFrequency: Double,
    preferredBookComplexity: String,
    totalBooksRead: Int,
    recentGenres: Set[String],
    genreDiversity: Double
  )

  /**
   * Recommendation context and configuration
   */
  case class RecommendationConfig(
    maxRecommendations: Int = 10,
    includeRead: Boolean = false,
    diversityWeight: Double = 0.3,
    popularityWeight: Double = 0.2,
    similarityWeight: Double = 0.5,
    recencyBoost: Boolean = true
  )

  /**
   * Extract reading profile from user's transaction history
   */
  def extractReadingProfile(user: User, catalog: LibraryCatalog): ReadingProfile =
    val userTransactions = catalog.transactions.filter(_.user.id == user.id)
    val booksRead = userTransactions.collect {
      case Transaction.Loan(book, _, _, _) => book
      case Transaction.Return(book, _, _) => book
    }.distinct

    val genreCounts = booksRead.groupBy(_.genre).view.mapValues(_.length.toDouble).toMap
    val totalBooks = booksRead.length.toDouble
    val normalizedGenres = genreCounts.view.mapValues(_ / totalBooks).toMap

    val authorCounts = booksRead.flatMap(_.authors).groupBy(identity).view.mapValues(_.length.toDouble).toMap
    val normalizedAuthors = authorCounts.view.mapValues(_ / totalBooks).toMap

    val avgYear = if booksRead.nonEmpty then booksRead.map(_.publicationYear).sum.toDouble / booksRead.length else 0.0
    val avgAge = LocalDateTime.now.getYear - avgYear

    val recentBooks = userTransactions.filter(_.timestamp.isAfter(LocalDateTime.now.minusMonths(6)))
                                     .collect { case Transaction.Loan(book, _, _, _) => book }.distinct
    val recentGenres = recentBooks.map(_.genre).toSet

    val genreDiversity = if genreCounts.nonEmpty then 
      -genreCounts.values.map(count => (count / totalBooks) * math.log(count / totalBooks)).sum
    else 0.0

    ReadingProfile(
      favoriteGenres = normalizedGenres,
      favoriteAuthors = normalizedAuthors,
      averageBookAge = avgAge,
      readingFrequency = calculateReadingFrequency(userTransactions),
      preferredBookComplexity = inferComplexity(booksRead),
      totalBooksRead = booksRead.length,
      recentGenres = recentGenres,
      genreDiversity = genreDiversity
    )

  private def calculateReadingFrequency(transactions: List[Transaction]): Double =
    val loans = transactions.collect { case l: Transaction.Loan => l }
    if loans.length < 2 then 0.0
    else
      val timeSpan = ChronoUnit.DAYS.between(loans.last.timestamp, loans.head.timestamp)
      if timeSpan > 0 then loans.length.toDouble / timeSpan * 30 else 0.0 // books per month

  private def inferComplexity(books: List[Book]): String =
    val avgTitleLength = books.map(_.title.split("\\s+").length).sum.toDouble / books.length
    if avgTitleLength > 6 then "Academic" else if avgTitleLength > 4 then "Intermediate" else "Popular"

  /**
   * Basic recommendation strategies
   */
  object Strategies:

    /**
     * Recommend based on genre preferences
     */
    val genreBased: BookRecommendation = (user, catalog) =>
      val profile = extractReadingProfile(user, catalog)
      val availableBooks = catalog.books.values.filter(_.isAvailable).toList
      
      availableBooks.map { book =>
        val genreScore = profile.favoriteGenres.getOrElse(book.genre, 0.0)
        (book, genreScore)
      }.sortBy(-_._2).take(10).map(_._1)

    /**
     * Recommend based on author preferences
     */
    val authorBased: BookRecommendation = (user, catalog) =>
      val profile = extractReadingProfile(user, catalog)
      val availableBooks = catalog.books.values.filter(_.isAvailable).toList
      
      availableBooks.map { book =>
        val authorScore = book.authors.map(author => 
          profile.favoriteAuthors.getOrElse(author, 0.0)
        ).maxOption.getOrElse(0.0)
        (book, authorScore)
      }.sortBy(-_._2).take(10).map(_._1)

    /**
     * Recommend popular books in user's favorite genres
     */
    val popularityBased: BookRecommendation = (user, catalog) =>
      val profile = extractReadingProfile(user, catalog)
      val bookPopularity = catalog.transactions.collect {
        case Transaction.Loan(book, _, _, _) => book.isbn
      }.groupBy(identity).view.mapValues(_.length).toMap

      catalog.books.values.filter(_.isAvailable)
                          .filter(book => profile.favoriteGenres.contains(book.genre))
                          .toList
                          .sortBy(book => -bookPopularity.getOrElse(book.isbn, 0))
                          .take(10)

    /**
     * Recommend based on what similar users liked
     */
    val collaborativeFiltering: BookRecommendation = (user, catalog) =>
      val userProfile = extractReadingProfile(user, catalog)
      val allUsers = catalog.users.values.toList
      
      // Find similar users based on genre preferences
      val similarUsers = allUsers.filter(_ != user).map { otherUser =>
        val otherProfile = extractReadingProfile(otherUser, catalog)
        val similarity = calculateGenreSimilarity(userProfile, otherProfile)
        (otherUser, similarity)
      }.filter(_._2 > 0.3).sortBy(-_._2).take(5)

      // Get books that similar users liked but current user hasn't read
      val userBooks = catalog.transactions.filter(_.user.id == user.id)
                                         .collect { case Transaction.Loan(book, _, _, _) => book.isbn }
                                         .toSet

      similarUsers.flatMap { case (similarUser, _) =>
        catalog.transactions.filter(_.user.id == similarUser.id)
                           .collect { case Transaction.Loan(book, _, _, _) => book }
                           .filter(book => !userBooks.contains(book.isbn) && book.isAvailable)
      }.distinct.take(10)

    /**
     * Recommend books to increase diversity
     */
    val diversityBased: BookRecommendation = (user, catalog) =>
      val profile = extractReadingProfile(user, catalog)
      val availableBooks = catalog.books.values.filter(_.isAvailable).toList
      val readGenres = profile.favoriteGenres.keySet
      
      // Prioritize books from genres the user hasn't explored much
      availableBooks.groupBy(_.genre)
                   .toList
                   .sortBy { case (genre, _) => profile.favoriteGenres.getOrElse(genre, 0.0) }
                   .flatMap(_._2)
                   .take(10)

    /**
     * Recommend recent releases in user's interests
     */
    val recencyBased: BookRecommendation = (user, catalog) =>
      val profile = extractReadingProfile(user, catalog)
      val currentYear = LocalDateTime.now.getYear
      val recentBooks = catalog.books.values.filter(book => 
        book.isAvailable && 
        book.publicationYear >= currentYear - 2 &&
        profile.favoriteGenres.contains(book.genre)
      ).toList.sortBy(-_.publicationYear).take(10)
      
      recentBooks

  /**
   * Composite recommendation strategies using function composition
   */
  object CompositeStrategies:

    /**
     * Weighted combination of multiple strategies
     */
    def weighted(strategies: List[(BookRecommendation, Double)]): BookRecommendation = 
      (user, catalog) =>
        val allRecommendations = strategies.flatMap { case (strategy, weight) =>
          strategy(user, catalog).map(book => (book, weight))
        }

        allRecommendations.groupBy(_._1)
                         .view.mapValues(_.map(_._2).sum)
                         .toList
                         .sortBy(-_._2)
                         .map(_._1)
                         .take(10)

    /**
     * Hybrid strategy combining content-based and collaborative filtering
     */
    val hybrid: BookRecommendation = weighted(List(
      (Strategies.genreBased, 0.3),
      (Strategies.authorBased, 0.2),
      (Strategies.collaborativeFiltering, 0.3),
      (Strategies.popularityBased, 0.1),
      (Strategies.diversityBased, 0.1)
    ))

    /**
     * Adaptive strategy that changes based on user's reading profile
     */
    val adaptive: BookRecommendation = (user, catalog) =>
      val profile = extractReadingProfile(user, catalog)
      
      if profile.totalBooksRead < 3 then
        // New users get popular books
        Strategies.popularityBased(user, catalog)
      else if profile.genreDiversity < 1.0 then
        // Users with low diversity get diversity recommendations
        Strategies.diversityBased(user, catalog)
      else if profile.readingFrequency > 2.0 then
        // Frequent readers get sophisticated recommendations
        hybrid(user, catalog)
      else
        // Regular users get genre-based recommendations
        Strategies.genreBased(user, catalog)

  /**
   * Utility functions for similarity calculations
   */
  private def calculateGenreSimilarity(profile1: ReadingProfile, profile2: ReadingProfile): Double =
    val allGenres = profile1.favoriteGenres.keySet ++ profile2.favoriteGenres.keySet
    val similarities = allGenres.map { genre =>
      val score1 = profile1.favoriteGenres.getOrElse(genre, 0.0)
      val score2 = profile2.favoriteGenres.getOrElse(genre, 0.0)
      1.0 - math.abs(score1 - score2)
    }
    if similarities.nonEmpty then similarities.sum / similarities.size else 0.0

  /**
   * Main recommendation function with configuration
   */
  def recommend(user: User, catalog: LibraryCatalog, config: RecommendationConfig = RecommendationConfig()): List[Book] =
    val strategy = CompositeStrategies.adaptive
    val rawRecommendations = strategy(user, catalog)
    
    val filtered = if config.includeRead then rawRecommendations
                   else filterUnreadBooks(user, catalog, rawRecommendations)
    
    filtered.take(config.maxRecommendations)

  // Alias for CLI compatibility
  def generateRecommendations(user: User, catalog: LibraryCatalog): List[Book] =
    recommend(user, catalog)

  private def filterUnreadBooks(user: User, catalog: LibraryCatalog, books: List[Book]): List[Book] =
    val readBooks = catalog.transactions.filter(_.user.id == user.id)
                                       .collect { case Transaction.Loan(book, _, _, _) => book.isbn }
                                       .toSet
    books.filter(book => !readBooks.contains(book.isbn))

  /**
   * Get explanation for why a book was recommended
   */
  def explainRecommendation(user: User, book: Book, catalog: LibraryCatalog): String =
    val profile = extractReadingProfile(user, catalog)
    val reasons = scala.collection.mutable.ListBuffer[String]()

    if profile.favoriteGenres.getOrElse(book.genre, 0.0) > 0.2 then
      reasons += s"You enjoy ${book.genre} books"

    if book.authors.exists(author => profile.favoriteAuthors.contains(author)) then
      val commonAuthors = book.authors.filter(profile.favoriteAuthors.contains)
      reasons += s"You've read books by ${commonAuthors.mkString(", ")}"

    val bookAge = LocalDateTime.now.getYear - book.publicationYear
    if math.abs(bookAge - profile.averageBookAge) < 5 then
      reasons += "Publication year matches your reading preferences"

    if reasons.isEmpty then "This book is popular among users with similar reading patterns"
    else reasons.mkString("; ")

  /**
   * Get trending books for general recommendations
   */
  def getTrendingBooks(catalog: LibraryCatalog, days: Int = 30): List[Book] =
    val cutoff = LocalDateTime.now.minusDays(days)
    val recentLoans = catalog.transactions.collect {
      case Transaction.Loan(book, _, timestamp, _) if timestamp.isAfter(cutoff) => book.isbn
    }

    val trendingISBNs = recentLoans.groupBy(identity)
                                  .view.mapValues(_.length)
                                  .toList
                                  .sortBy(-_._2)
                                  .take(10)
                                  .map(_._1)

    trendingISBNs.flatMap(isbn => catalog.books.get(isbn))
                 .filter(_.isAvailable)
