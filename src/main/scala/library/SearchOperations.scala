package library

import Types.{ISBN, UserID}
import scala.util.{Try, Success, Failure}

/**
 * Functional operations for searching and filtering books using advanced Scala 3 features.
 * 
 * This object provides a comprehensive set of functional programming tools for book search
 * operations, including higher-order functions, function composition, partial application,
 * and predicate combinators. All operations are pure functions that maintain immutability
 * and referential transparency.
 * 
 * Key Features:
 * - Type-safe predicate composition using extension methods
 * - Fuzzy search with configurable edit distance
 * - Performance-optimized search algorithms
 * - Functional composition patterns
 * 
 * @example
 * {{{
 * // Basic usage with predicate composition
 * val books = List(
 *   Book(ISBN("123"), "Scala Programming", List("Odersky"), 2021, "Programming", true),
 *   Book(ISBN("456"), "Java Essentials", List("Gosling"), 2020, "Programming", false)
 * )
 * 
 * // Find available programming books from 2020 onwards
 * val predicate = SearchOperations.Predicates.isAvailable
 *   .and(SearchOperations.Predicates.byGenre("Programming"))
 *   .and(SearchOperations.Predicates.byYearRange(2020, 2024))
 * 
 * val results = SearchOperations.searchBy(books, predicate)
 * // Returns: List(Book(ISBN("123"), "Scala Programming", ...))
 * 
 * // Fuzzy search with typo tolerance
 * val fuzzyResults = SearchOperations.fuzzySearchWithThreshold(books, "Scla", 2)
 * // Returns books matching "Scala" even with typo
 * }}}
 * 
 * @since 1.0.0
 * @author Library Management System Team
 */
object SearchOperations:

  // Type aliases for better readability
  type BookPredicate = Book => Boolean
  type BookTransform[T] = Book => T
  type BookComparator = (Book, Book) => Int

  /**
   * Pre-defined predicates for common book filtering operations.
   * 
   * This object contains a collection of commonly used book predicates that can be
   * composed together using the predicate combinators to create complex search queries.
   * All predicates are pure functions that return Boolean values.
   * 
   * @example
   * {{{
   * // Using basic predicates
   * val availableBooks = books.filter(Predicates.isAvailable)
   * val programmingBooks = books.filter(Predicates.byGenre("Programming"))
   * val recentBooks = books.filter(Predicates.byYearRange(2020, 2024))
   * 
   * // Composing predicates
   * val complexPredicate = Predicates.isAvailable
   *   .and(Predicates.byGenre("Science"))
   *   .and(Predicates.byYearRange(2015, 2024))
   * val filteredBooks = books.filter(complexPredicate)
   * }}}
   */
  object Predicates:
    /** Predicate that matches books currently available for loan */  
    val isAvailable: BookPredicate = _.isAvailable
    
    /** Predicate that matches books currently on loan */ 
    val isUnavailable: BookPredicate = !_.isAvailable
    
    /** Universal predicate that matches all books */
    val all: BookPredicate = _ => true
    
    /**
     * Creates a predicate that matches books containing the specified title substring.
     * 
     * @param title The title substring to search for (case-insensitive)
     * @return A predicate function that matches books with titles containing the substring
     * 
     * @example
     * {{{
     * val javaBooks = books.filter(Predicates.byTitle("Java"))
     * val effectiveBooks = books.filter(Predicates.byTitle("Effective"))
     * }}}
     */
    def byTitle(title: String): BookPredicate = 
      book => book.title.toLowerCase.contains(title.toLowerCase)
    
    /**
     * Creates a predicate that matches books by any author containing the specified name.
     * 
     * @param author The author name substring to search for (case-insensitive)
     * @return A predicate function that matches books with authors containing the substring
     * 
     * @example
     * {{{
     * val blochBooks = books.filter(Predicates.byAuthor("Bloch"))
     * val martinBooks = books.filter(Predicates.byAuthor("Martin"))
     * }}}
     */
    def byAuthor(author: String): BookPredicate = 
      book => book.authors.exists(_.toLowerCase.contains(author.toLowerCase))
    
    /**
     * Creates a predicate that matches books in the specified genre.
     * 
     * @param genre The genre to search for (case-insensitive)
     * @return A predicate function that matches books in the specified genre
     * 
     * @example
     * {{{
     * val fictionBooks = books.filter(Predicates.byGenre("Fiction"))
     * val programmingBooks = books.filter(Predicates.byGenre("Programming"))
     * }}}
     */
    def byGenre(genre: String): BookPredicate = 
      book => book.genre.toLowerCase.contains(genre.toLowerCase)
    
    /**
     * Creates a predicate that matches books published in the specified year.
     * 
     * @param year The publication year to match exactly
     * @return A predicate function that matches books from the specified year
     * 
     * @example
     * {{{
     * val books2023 = books.filter(Predicates.byYear(2023))
     * }}}
     */
    def byYear(year: Int): BookPredicate = 
      book => book.publicationYear == year
    
    /**
     * Creates a predicate that matches books published within the specified year range.
     * 
     * @param minYear The minimum publication year (inclusive)
     * @param maxYear The maximum publication year (inclusive)
     * @return A predicate function that matches books within the year range
     * 
     * @example
     * {{{
     * val recentBooks = books.filter(Predicates.byYearRange(2020, 2024))
     * val classicBooks = books.filter(Predicates.byYearRange(1950, 2000))
     * }}}
     */
    def byYearRange(minYear: Int, maxYear: Int): BookPredicate = 
      book => book.publicationYear >= minYear && book.publicationYear <= maxYear
    
    /**
     * Creates a predicate that matches books with ISBN containing the specified string.
     * 
     * @param isbn The ISBN substring to search for
     * @return A predicate function that matches books with ISBNs containing the substring
     * 
     * @example
     * {{{
     * val isbn13Books = books.filter(Predicates.byISBN("978"))
     * val specificBook = books.filter(Predicates.byISBN("0134685991"))
     * }}}
     */
    def byISBN(isbn: String): BookPredicate = 
      book => book.isbn.value.contains(isbn)

  /**
   * Extension methods for composing BookPredicate functions using logical combinators.
   * 
   * These extension methods enable fluent predicate composition using standard logical
   * operations. They follow the principles of Boolean algebra and maintain referential
   * transparency for reliable functional composition.
   * 
   * @example
   * {{{
   * // Combining predicates with logical AND
   * val availableProgrammingBooks = Predicates.isAvailable
   *   .and(Predicates.byGenre("Programming"))
   * 
   * // Combining with logical OR
   * val javaOrScalaBooks = Predicates.byTitle("Java")
   *   .or(Predicates.byTitle("Scala"))
   * 
   * // Negating a predicate
   * val nonFictionBooks = Predicates.byGenre("Fiction").not
   * 
   * // Complex composition
   * val complexFilter = Predicates.isAvailable
   *   .and(Predicates.byYearRange(2020, 2024))
   *   .and(Predicates.byGenre("Programming").or(Predicates.byGenre("Science")))
   *   .and(Predicates.byTitle("Scala").not)
   * }}}
   */
  extension (p1: BookPredicate)
    /**
     * Logical AND combinator for predicates.
     * 
     * @param p2 The second predicate to combine with logical AND
     * @return A new predicate that returns true only if both predicates return true
     */
    def and(p2: BookPredicate): BookPredicate = book => p1(book) && p2(book)
    
    /**
     * Logical OR combinator for predicates.
     * 
     * @param p2 The second predicate to combine with logical OR  
     * @return A new predicate that returns true if either predicate returns true
     */
    def or(p2: BookPredicate): BookPredicate = book => p1(book) || p2(book)
    
    /**
     * Logical NOT combinator for predicates.
     * 
     * @return A new predicate that returns the negation of the original predicate
     */
    def not: BookPredicate = book => !p1(book)

  /**
   * Higher-order search function with partial application support.
   * 
   * This function demonstrates functional programming principles by returning
   * a partially applied function that can be reused with different predicates.
   * It's particularly useful when you have a fixed collection of books and want
   * to apply multiple different search criteria.
   * 
   * @param books The collection of books to search through
   * @return A function that takes a predicate and returns filtered books
   * 
   * @example
   * {{{
   * val bookSearcher = SearchOperations.searchBooks(libraryBooks)
   * val availableBooks = bookSearcher(Predicates.isAvailable)
   * val programmingBooks = bookSearcher(Predicates.byGenre("Programming"))
   * val availableProgramming = bookSearcher(Predicates.isAvailable.and(Predicates.byGenre("Programming")))
   * }}}
   * 
   * @since 1.0.0
   */
  def searchBooks(books: Iterable[Book]): BookPredicate => List[Book] = 
    predicate => books.filter(predicate).toList

  /**
   * Curried search function for flexible predicate application.
   * 
   * This function uses currying to provide a more functional interface where
   * the predicate can be defined first and then applied to different book collections.
   * This is useful when you have a fixed search criteria but different data sets.
   * 
   * @param predicate The search predicate to apply
   * @return A function that takes a book collection and returns filtered results
   * 
   * @example
   * {{{
   * val availableBookFilter = SearchOperations.searchBy(Predicates.isAvailable)
   * val availableInLibraryA = availableBookFilter(libraryABooks)
   * val availableInLibraryB = availableBookFilter(libraryBBooks)
   * }}}
   * 
   * @since 1.0.0
   */
  def searchBy(predicate: BookPredicate)(books: Iterable[Book]): List[Book] = 
    books.filter(predicate).toList

  /**
   * Composes multiple predicates into a single predicate using logical AND.
   * 
   * This function provides a convenient way to combine multiple search criteria
   * into a single predicate using functional composition. All predicates must
   * be satisfied for a book to match.
   * 
   * @param predicates Variable number of predicates to combine with AND logic
   * @return A single predicate that represents the logical AND of all input predicates
   * 
   * @example
   * {{{
   * val complexSearch = SearchOperations.composeSearch(
   *   Predicates.isAvailable,
   *   Predicates.byGenre("Programming"),
   *   Predicates.byYearRange(2020, 2024)
   * )
   * val results = books.filter(complexSearch)
   * }}}
   * 
   * @throws IllegalArgumentException if no predicates are provided
   * @since 1.0.0
   */
  def composeSearch(predicates: BookPredicate*): BookPredicate = 
    if (predicates.isEmpty) throw new IllegalArgumentException("At least one predicate must be provided")
    predicates.reduce(_ and _)

  /**
   * Predefined comparator functions for sorting book search results.
   * 
   * This object contains commonly used sorting functions that can be applied
   * to search results to present them in a meaningful order. All comparators
   * follow the standard comparison contract and can be used with sorting functions.
   * 
   * @example
   * {{{
   * val searchResults = SearchOperations.searchBy(Predicates.byGenre("Fiction"))(books)
   * val sortedByTitle = searchResults.sortWith(Sorters.byTitle(_, _) < 0)
   * val sortedByYear = searchResults.sortWith(Sorters.byYearDesc(_, _) < 0)
   * }}}
   */
  object Sorters:
    /** Comparator for sorting books alphabetically by title (case-insensitive) */
    val byTitle: BookComparator = (b1, b2) => b1.title.compareToIgnoreCase(b2.title)
    
    /** Comparator for sorting books by publication year (ascending) */
    val byYear: BookComparator = (b1, b2) => b1.publicationYear.compare(b2.publicationYear)
    
    /** Comparator for sorting books by publication year (descending) */
    val byYearDesc: BookComparator = (b1, b2) => b2.publicationYear.compare(b1.publicationYear)
    
    /** Comparator for sorting books alphabetically by first author (case-insensitive) */
    val byAuthor: BookComparator = (b1, b2) => 
      b1.authors.headOption.getOrElse("").compareToIgnoreCase(b2.authors.headOption.getOrElse(""))

  def searchAndSort(books: Iterable[Book])(predicate: BookPredicate)(comparator: BookComparator): List[Book] = 
    books.filter(predicate).toList.sortWith((a, b) => comparator(a, b) < 0)

  /**
   * Advanced search with multiple criteria
   */
  case class SearchCriteria(
    title: Option[String] = None,
    author: Option[String] = None,
    genre: Option[String] = None,
    yearFrom: Option[Int] = None,
    yearTo: Option[Int] = None,
    availableOnly: Boolean = false
  )

  def advancedSearch(books: Iterable[Book])(criteria: SearchCriteria): List[Book] =
    val predicates = List(
      criteria.title.map(Predicates.byTitle),
      criteria.author.map(Predicates.byAuthor),
      criteria.genre.map(Predicates.byGenre),
      criteria.yearFrom.map(year => Predicates.byYearRange(year, criteria.yearTo.getOrElse(Int.MaxValue))),
      if criteria.availableOnly then Some(Predicates.isAvailable) else None
    ).flatten

    if predicates.isEmpty then books.toList
    else books.filter(composeSearch(predicates*)).toList

  /**
   * Fuzzy search using Levenshtein distance
   */
  def fuzzySearch(books: Iterable[Book])(query: String, threshold: Int = 2): List[Book] =
    def levenshtein(s1: String, s2: String): Int =
      val s1Lower = s1.toLowerCase
      val s2Lower = s2.toLowerCase
      if s1Lower == s2Lower then 0
      else if s1Lower.isEmpty then s2Lower.length
      else if s2Lower.isEmpty then s1Lower.length
      else
        val costs = Array.tabulate(s2Lower.length + 1)(identity)
        for i <- 1 to s1Lower.length do
          costs(0) = i
          var nw = i - 1
          for j <- 1 to s2Lower.length do
            val cj = Math.min(
              1 + Math.min(costs(j), costs(j - 1)),
              if s1Lower(i - 1) == s2Lower(j - 1) then nw else nw + 1
            )
            nw = costs(j)
            costs(j) = cj
        costs(s2Lower.length)

    books.filter { book =>
      levenshtein(book.title, query) <= threshold ||
      book.authors.exists(author => levenshtein(author, query) <= threshold)
    }.toList

  /**
   * Alternative fuzzy search function for compatibility
   */
  def fuzzySearchWithThreshold(books: Iterable[Book], term: String, maxDistance: Int): List[Book] =
    fuzzySearch(books)(term, maxDistance)

  /**
   * Partial application examples
   */
  val searchAvailableBooks: Iterable[Book] => List[Book] = searchBy(Predicates.isAvailable)
  val searchProgrammingBooks: Iterable[Book] => List[Book] = searchBy(Predicates.byGenre("Programming"))
  val searchRecentBooks: Iterable[Book] => List[Book] = searchBy(Predicates.byYearRange(2020, 2025))

  // Convenience functions for CLI
  def searchByGenre(books: List[Book], genre: String): List[Book] = 
    searchBy(Predicates.byGenre(genre))(books)
  
  def searchByAuthor(books: List[Book], author: String): List[Book] = 
    searchBy(Predicates.byAuthor(author))(books)
  
  def searchByYearRange(books: List[Book], startYear: Int, endYear: Int): List[Book] = 
    searchBy(Predicates.byYearRange(startYear, endYear))(books)
  
  def searchAvailable(books: List[Book]): List[Book] = 
    searchBy(Predicates.isAvailable)(books)

  def searchBy(books: List[Book], predicate: BookPredicate): List[Book] = 
    books.filter(predicate)
