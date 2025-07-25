package library

import java.util.UUID
import scala.util.{Try, Success, Failure}
import scala.annotation.targetName

/**
 * type system to make sure we dont mix up everything
 * 
 * using scala 3 opaque types to wrap primitive types
 * so we cant accidentally pass an ISBN where we need a UserID
 * 
 * each type has:
 * - a safe() constructor that validates the data
 * - a direct apply() constructor when we know its good
 * - extension methods for useful stuff
 * 
 * opaque types cost nothing at runtime, just for
 * compile-time checking
 * 
 * we have: ISBN, UserID, BookTitle, AuthorName, Genre
 * each with their specific validation rules
 */
object Types:
  // opaque types so we dont mix up identifiers
  opaque type ISBN = String
  opaque type UserID = UUID
  opaque type BookTitle = String
  opaque type AuthorName = String
  opaque type Genre = String

  // companion objects with validation
  object ISBN:
    def apply(value: String): ISBN = value
    def safe(value: String): Either[String, ISBN] =
      if value.trim.isEmpty then
        Left("ISBN cannot be empty")
      else if value.length < 10 then
        Left("ISBN must be at least 10 characters")
      else if !value.matches("^[0-9\\-X]+$") then
        Left("ISBN can only contain digits, hyphens, and X")
      else
        Right(value.trim)
    
    def unwrap(isbn: ISBN): String = isbn

  object UserID:
    def apply(value: UUID): UserID = value
    def safe(value: String): Either[String, UserID] =
      Try(UUID.fromString(value)) match
        case Success(uuid) => Right(uuid)
        case Failure(_) => Left(s"Invalid UUID format: $value")
    
    def random(): UserID = UUID.randomUUID()
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

  // Extension methods for opaque types
  extension (isbn: ISBN)
    @targetName("isbnValue")
    def value: String = isbn
    def isValid: Boolean = ISBN.safe(isbn).isRight
    def checkDigit: Option[Char] = isbn.lastOption
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
