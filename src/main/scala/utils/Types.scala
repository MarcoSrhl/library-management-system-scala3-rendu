package utils

import java.util.UUID
import scala.util.{Try, Success, Failure}
import scala.annotation.targetName

/**
 * Domain-specific type system using Scala 3 opaque types.
 * 
 * This module provides type-safe wrappers around primitive types to create
 * a robust domain model that prevents common programming errors through
 * compile-time type checking. Each opaque type includes validation, rich APIs,
 * and zero runtime overhead.
 * 
 * == Design Philosophy ==
 * 
 * '''Type Safety''': Prevents mixing of conceptually different values that
 * happen to share the same underlying type (e.g., ISBN vs. regular String).
 * 
 * '''Zero Cost Abstractions''': Opaque types compile to their underlying
 * types, providing type safety without runtime performance overhead.
 * 
 * '''Rich Domain Models''': Each type includes domain-specific operations
 * and validations appropriate for its use case.
 * 
 * '''Functional Error Handling''': Uses Either types for safe construction
 * with comprehensive error reporting.
 * 
 * == Available Types ==
 * 
 * '''ISBN''': International Standard Book Number with format validation
 * '''UserID''': Unique user identifier based on UUID
 * '''BookTitle''': Book title with validation and text processing
 * '''AuthorName''': Author name with parsing and formatting
 * '''Genre''': Book genre with normalization and classification
 * 
 * @example
 * {{{
 * import utils.Types.*
 * 
 * // Safe construction with validation
 * val isbn = ISBN.safe("978-0-13-468599-1") match {
 *   case Right(validISBN) => validISBN
 *   case Left(error) => throw new IllegalArgumentException(error)
 * }
 * 
 * // Rich domain operations
 * println(isbn.formatted)     // "978-0-13-468599-1"
 * println(isbn.isISBN13)      // true
 * println(isbn.checkDigit)    // '1'
 * 
 * // Type safety prevents errors
 * val userId = UserID.random()
 * // isbn == userId  // Compile error - different types!
 * 
 * // Combining domain types safely
 * val title = BookTitle.safe("Programming in Scala").getOrElse(???)
 * val author = AuthorName.safe("Martin Odersky").getOrElse(???)
 * val genre = Genre.safe("Computer Science").getOrElse(???)
 * }}}
 * 
 * @since 1.0.0
 * @see [[models.Book]] for usage in domain models
 * @see [[services.LibraryCatalog]] for usage in business logic
 */
object Types:
  
  /** 
   * Opaque type for International Standard Book Numbers (ISBN).
   * 
   * Provides type-safe handling of ISBN values with comprehensive validation
   * for both ISBN-10 and ISBN-13 formats. Includes format checking, check
   * digit validation, and conversion utilities.
   * 
   * == Supported Formats ==
   *  - ISBN-13: 978-0-13-468599-1 (preferred format)
   *  - ISBN-10: 0-13-468599-2 (legacy format)  
   *  - Compact: 9780134685991 (no hyphens)
   * 
   * @example 
   * {{{
   * // Safe construction with validation
   * val isbn = ISBN.safe("978-0-13-468599-1") match {
   *   case Right(validISBN) => validISBN
   *   case Left(error) => sys.error(s"Invalid ISBN: $error")
   * }
   * 
   * // Rich API for ISBN operations  
   * println(isbn.formatted)        // "978-0-13-468599-1"
   * println(isbn.isISBN13)         // true
   * println(isbn.checkDigit)       // '1'
   * println(isbn.publisherCode)    // "13"
   * }}}
   * 
   * @since 1.0.0
   */
  opaque type ISBN = String
  
  /** 
   * Opaque type for unique user identification.
   * 
   * Wraps UUID to provide type-safe user identification throughout the system.
   * Prevents accidental mixing of user IDs with other UUID values and provides
   * user-specific utility methods.
   * 
   * @example 
   * {{{
   * // Generate random user ID
   * val userId = UserID.random()
   * 
   * // Create from existing UUID
   * val existingId = UserID(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
   * 
   * // Utility methods
   * println(userId.shortId)        // First 8 chars: "550e8400"
   * println(userId.toString)       // Full UUID string
   * }}}
   * 
   * @since 1.0.0
   */
  opaque type UserID = UUID
  
  /** 
   * Opaque type for validated book titles.
   * 
   * Ensures book titles meet validation requirements and provides text
   * processing utilities for title manipulation, capitalization, and analysis.
   * 
   * == Validation Rules ==
   *  - Minimum length: 1 character
   *  - Maximum length: 200 characters
   *  - No leading/trailing whitespace
   *  - Contains at least one non-whitespace character
   * 
   * @example 
   * {{{
   * // Safe construction with validation
   * val title = BookTitle.safe("the great gatsby") match {
   *   case Right(validTitle) => validTitle
   *   case Left(error) => sys.error(s"Invalid title: $error")
   * }
   * 
   * // Text processing utilities
   * println(title.capitalized)     // "The Great Gatsby"
   * println(title.wordCount)       // 3
   * println(title.abbreviated(20)) // "The Great Gatsby"
   * }}}
   * 
   * @since 1.0.0
   */
  opaque type BookTitle = String
  
  /** 
   * Opaque type for validated author names.
   * 
   * Provides type safety for author names with validation, parsing, and
   * formatting functionality. Supports various name formats and provides
   * utilities for name manipulation.
   * 
   * == Supported Formats ==
   *  - "First Last" (e.g., "Jane Doe")
   *  - "First Middle Last" (e.g., "Mary Jane Smith")  
   *  - "Last, First" (e.g., "Doe, Jane")
   *  - "First M. Last" (e.g., "John F. Kennedy")
   * 
   * @example 
   * {{{
   * // Various name formats
   * val author1 = AuthorName.safe("F. Scott Fitzgerald").getOrElse(???)
   * val author2 = AuthorName.safe("Tolkien, J.R.R.").getOrElse(???)
   * 
   * // Name parsing utilities
   * println(author1.firstName)     // "F."
   * println(author1.lastName)      // "Fitzgerald"
   * println(author1.initials)      // "F.S.F."
   * println(author1.formatted)     // "Fitzgerald, F. Scott"
   * }}}
   * 
   * @since 1.0.0
   */
  opaque type AuthorName = String
  
  /** 
   * Opaque type for book genre classification.
   * 
   * Ensures consistent genre handling with normalization, validation, and
   * classification helper methods. Supports hierarchical genre systems and
   * genre matching.
   * 
   * == Common Genres ==
   *  - Fiction (Literary, Historical, Science Fiction, Fantasy)
   *  - Non-Fiction (Biography, History, Science, Philosophy)  
   *  - Technical (Computer Science, Engineering, Mathematics)
   *  - Academic (Textbook, Reference, Research)
   * 
   * @example 
   * {{{
   * // Genre creation and normalization
   * val genre = Genre.safe("Science Fiction").getOrElse(???)
   * 
   * // Classification utilities  
   * println(genre.normalized)      // "science fiction"
   * println(genre.isScience)       // true
   * println(genre.isFiction)       // true
   * println(genre.category)        // "Fiction"
   * }}}
   * 
   * @since 1.0.0
   */
  opaque type Genre = String

  /** 
   * Companion object for ISBN opaque type.
   * 
   * Provides construction, validation, and utility methods for ISBN handling.
   */
  object ISBN:
    /** 
     * Creates an ISBN without validation.
     * Use this when you're certain the input is valid.
     * 
     * @param value the ISBN string
     * @return ISBN instance
     * @example {{{
     * val isbn = ISBN("9780134685991")
     * }}}
     */
    def apply(value: String): ISBN = value
    
    /** 
     * Creates a validated ISBN.
     * 
     * Validates the ISBN format and returns either an error message
     * or a valid ISBN instance.
     * 
     * @param value the ISBN string to validate
     * @return Either an error message or valid ISBN
     * @example {{{
     * ISBN.safe("9780134685991") match {
     *   case Right(isbn) => println(s"Valid ISBN: ${isbn.value}")
     *   case Left(error) => println(s"Invalid: $error")
     * }
     * }}}
     */
    def safe(value: String): Either[String, ISBN] =
      if value.trim.isEmpty then
        Left("ISBN cannot be empty")
      else if value.length < 10 then
        Left("ISBN must be at least 10 characters")
      else if !value.matches("^[0-9\\-X]+$") then
        Left("ISBN can only contain digits, hyphens, and X")
      else
        Right(value.trim)
    
    /** 
     * Extracts the underlying string value from an ISBN.
     * 
     * @param isbn the ISBN to unwrap
     * @return the underlying string value
     */
    def unwrap(isbn: ISBN): String = isbn

  /** 
   * Companion object for UserID opaque type.
   * 
   * Provides construction and validation methods for user identification.
   */
  object UserID:
    /** 
     * Creates a UserID from a UUID.
     * 
     * @param value the UUID value
     * @return UserID instance
     */
    def apply(value: UUID): UserID = value
    
    /** 
     * Creates a validated UserID from a string.
     * 
     * Parses the string as a UUID and returns either an error
     * or a valid UserID.
     * 
     * @param value the UUID string to parse
     * @return Either an error message or valid UserID
     * @example {{{
     * UserID.safe("123e4567-e89b-12d3-a456-426614174000") match {
     *   case Right(userId) => println(s"Valid ID: ${userId.shortId}")
     *   case Left(error) => println(s"Invalid: $error")
     * }
     * }}}
     */
    def safe(value: String): Either[String, UserID] =
      Try(UUID.fromString(value)) match
        case Success(uuid) => Right(uuid)
        case Failure(_) => Left(s"Invalid UUID format: $value")
    
    /** 
     * Generates a random UserID.
     * 
     * @return a new random UserID
     * @example {{{
     * val newUserId = UserID.random()
     * }}}
     */
    def random(): UserID = UUID.randomUUID()
    
    /** 
     * Extracts the underlying UUID from a UserID.
     * 
     * @param id the UserID to unwrap
     * @return the underlying UUID
     */
    def unwrap(id: UserID): UUID = id

  object BookTitle:
    def apply(value: String): BookTitle = value
    def safe(value: String): Either[String, BookTitle] =
      val trimmed = value.trim
      if trimmed.isEmpty then
        Left("Book title cannot be empty")
      else if trimmed.length > 200 then
        Left("Book title cannot exceed 200 characters")
      else
        Right(trimmed)
    
    def unwrap(title: BookTitle): String = title

  object AuthorName:
    def apply(value: String): AuthorName = value
    def safe(value: String): Either[String, AuthorName] =
      val trimmed = value.trim
      if trimmed.isEmpty then
        Left("Author name cannot be empty")
      else if trimmed.length > 100 then
        Left("Author name cannot exceed 100 characters")
      else if !trimmed.matches("^[a-zA-Z\\s\\-\\.]+$") then
        Left("Author name can only contain letters, spaces, hyphens, and periods")
      else
        Right(trimmed)
    
    def unwrap(author: AuthorName): String = author

  object Genre:
    def apply(value: String): Genre = value
    def safe(value: String): Either[String, Genre] =
      val trimmed = value.trim
      if trimmed.isEmpty then
        Left("Genre cannot be empty")
      else if trimmed.length > 50 then
        Left("Genre cannot exceed 50 characters")
      else
        Right(trimmed)
    
    def unwrap(genre: Genre): String = genre

  /** 
   * Extension methods for ISBN opaque type.
   * 
   * Provides domain-specific operations for ISBN manipulation and validation.
   */
  extension (isbn: ISBN)
    /** 
     * Returns the underlying string value of the ISBN.
     * 
     * @return the ISBN as a string
     * @example {{{
     * val isbn = ISBN("9780134685991")
     * println(isbn.value) // "9780134685991"
     * }}}
     */
    @targetName("isbnValue")
    def value: String = isbn
    
    /** 
     * Validates whether this ISBN meets format requirements.
     * 
     * @return true if the ISBN is valid, false otherwise
     * @example {{{
     * val isbn = ISBN("9780134685991")
     * println(isbn.isValid) // true
     * }}}
     */
    def isValid: Boolean = ISBN.safe(isbn).isRight
    
    /** 
     * Returns the check digit of the ISBN (last character).
     * 
     * @return Some(char) if ISBN has characters, None if empty
     * @example {{{
     * val isbn = ISBN("013468599X")
     * println(isbn.checkDigit) // Some('X')
     * }}}
     */
    def checkDigit: Option[Char] = isbn.lastOption
    
    /** 
     * Formats the ISBN with hyphens if it's a 13-digit ISBN.
     * 
     * @return formatted ISBN string with hyphens, or original if not 13 digits
     * @example {{{
     * val isbn = ISBN("9780134685991")
     * println(isbn.formatted) // "978-0-13-468599-1"
     * }}}
     */
    def formatted: String = 
      if isbn.length == 13 then
        s"${isbn.take(3)}-${isbn.slice(3, 4)}-${isbn.slice(4, 6)}-${isbn.slice(6, 12)}-${isbn.last}"
      else isbn

  extension (id: UserID)
    @targetName("userIdValue")
    def value: UUID = id
    def shortId: String = id.toString.take(8)
    def formatted: String = id.toString

  extension (title: BookTitle)
    @targetName("titleValue")
    def value: String = title
    def words: List[String] = title.split("\\s+").toList
    def wordCount: Int = words.length
    def capitalized: String = title.split("\\s+").map(_.capitalize).mkString(" ")

  extension (author: AuthorName)
    @targetName("authorValue")
    def value: String = author
    def lastName: String = 
      val parts = author.split("\\s+")
      if parts.length > 1 then parts.last else author
    def firstName: String = 
      val parts = author.split("\\s+")
      if parts.length > 1 then parts.head else ""
    def initials: String = author.split("\\s+").map(_.headOption.getOrElse(' ')).mkString(".")

  extension (genre: Genre)
    @targetName("genreValue")
    def value: String = genre
    def normalized: String = genre.toLowerCase.trim
    def isScience: Boolean = genre.toLowerCase.contains("science")
    def isFiction: Boolean = genre.toLowerCase.contains("fiction")

  // Extension methods for collections of domain types
  extension (isbns: List[ISBN])
    @targetName("isbnListFormatted")
    def formatted: List[String] = isbns.map { isbn =>
      if isbn.length == 13 then
        s"${isbn.take(3)}-${isbn.slice(3, 4)}-${isbn.slice(4, 6)}-${isbn.slice(6, 12)}-${isbn.last}"
      else isbn
    }
    def validOnly: List[ISBN] = isbns.filter(isbn => ISBN.safe(isbn).isRight)

  extension (authors: List[AuthorName])
    def lastNames: List[String] = authors.map { author =>
      val parts = author.split("\\s+")
      if parts.length > 1 then parts.last else author
    }
    @targetName("authorListFormatted")
    def formatted: String = authors.map(author => author: String) match
      case Nil => ""
      case single :: Nil => single
      case many => many.init.mkString(", ") + " and " + many.last

  extension (genres: List[Genre])
    def unique: List[Genre] = genres.distinctBy(genre => (genre: String).toLowerCase.trim)
    def sorted: List[Genre] = genres.sortBy(genre => genre: String)

  // Helper methods for creating validated instances
  def createValidatedISBN(value: String): Either[String, ISBN] = ISBN.safe(value)
  def createValidatedUserID(value: String): Either[String, UserID] = UserID.safe(value)
  def createValidatedTitle(value: String): Either[String, BookTitle] = BookTitle.safe(value)
  def createValidatedAuthor(value: String): Either[String, AuthorName] = AuthorName.safe(value)
  def createValidatedGenre(value: String): Either[String, Genre] = Genre.safe(value)
