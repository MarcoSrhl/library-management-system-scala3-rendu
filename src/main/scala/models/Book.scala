package models

import utils.Types.ISBN

/**
 * Represents a book in the library management system.
 * 
 * The Book case class is the core domain model for representing books in the library catalog.
 * It provides a complete, immutable representation of book metadata with type-safe identifiers
 * and rich domain semantics.
 * 
 * == Type Safety ==
 * Uses opaque ISBN type for compile-time safety, preventing mixing of ISBNs with other strings.
 * 
 * == Immutability ==
 * All fields are immutable, ensuring thread safety and functional programming principles.
 * 
 * == Integration ==
 * Seamlessly integrates with:
 *  - JSON serialization (via JsonIO)
 *  - Type classes (Displayable, Validatable)
 *  - Collection operations and search algorithms
 * 
 * @example
 * {{{
 * import utils.Types.ISBN
 * 
 * val isbn = ISBN.safe("978-0134685991").getOrElse(throw new IllegalArgumentException("Invalid ISBN"))
 * val book = Book(
 *   isbn = isbn,
 *   title = "Programming in Scala",
 *   authors = List("Martin Odersky", "Lex Spoon", "Bill Venners"),
 *   publicationYear = 2021,
 *   genre = "Computer Science",
 *   isAvailable = true
 * )
 * 
 * // Use in operations
 * val catalog = List(book)
 * val available = catalog.filter(_.isAvailable)
 * }}}
 * 
 * @param isbn The unique International Standard Book Number identifying this book.
 *             Must be a valid ISBN-10 or ISBN-13 format.
 * @param title The full title of the book. Cannot be empty or null.
 * @param authors List of authors who wrote this book. Must contain at least one author.
 * @param publicationYear The year this book was published. Should be a reasonable year (e.g., 1000-2030).
 * @param genre The genre/category of the book (e.g., "Fiction", "Computer Science", "Biography").
 * @param isAvailable Whether this book is currently available for loan (true) or checked out (false).
 * 
 * @since 1.0.0
 * @see [[utils.Types.ISBN]] for ISBN type safety
 * @see [[services.LibraryCatalog]] for catalog operations
 */
case class Book(
  isbn: ISBN,
  title: String,
  authors: List[String],
  publicationYear: Int,
  genre: String,
  isAvailable: Boolean
)
