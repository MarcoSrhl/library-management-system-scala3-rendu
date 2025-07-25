package library

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import Types.{ISBN, UserID}

/**
 * TypeClasses.scala - Type Class Pattern Implementation
 * 
 * This file implements the type class pattern using Scala 3's given/using syntax,
 * providing polymorphic behavior for different domain types in the library system.
 * Type classes enable ad-hoc polymorphism and allow adding functionality to existing
 * types without modifying their source code.
 * 
 * Key Features:
 * - Type Classes: Displayable, Validatable, Serializable, Similarity traits
 * - Given Instances: Concrete implementations for Book, User, Transaction types
 * - Extension Methods: Convenient syntax for using type class functionality
 * - Polymorphic Operations: Same interface for different types
 * - Functional Programming: Clean separation of data and behavior
 * 
 * The file demonstrates advanced Scala 3 features:
 * - Type class pattern with traits and given instances
 * - Given/using syntax for implicit resolution
 * - Extension methods that depend on given instances
 * - Context bounds and using clauses
 * - Ad-hoc polymorphism for domain types
 * 
 * Type Classes Implemented:
 * - Displayable[T]: Provides display and displayDetailed methods
 * - Validatable[T]: Provides validation logic for domain objects
 * - Serializable[T]: Provides serialization/deserialization (if needed)
 * - Similarity[T]: Provides similarity comparison between objects
 * 
 * Usage:
 * - Import given instances to enable type class functionality
 * - Use extension methods: book.display, user.validate, etc.
 * - Write generic functions that work with any type having a type class
 * - Extend functionality by providing new given instances
 */
object TypeClasses:
  
  // Type class for objects that can be displayed in a formatted way
  trait Displayable[T]:
    def display(value: T): String
    def displayDetailed(value: T): String
  
  // Type class for objects that can be validated
  trait Validatable[T]:
    def validate(value: T): Either[String, T]
  
  // Type class for objects that can be serialized
  trait Serializable[T]:
    def serialize(value: T): String
    def deserialize(data: String): Either[String, T]
  
  // Type class for objects that can be compared for similarity
  trait Similarity[T]:
    def similarity(a: T, b: T): Double // Returns 0.0 to 1.0
  
  // Given instances for Book
  given Displayable[Book] with
    def display(book: Book): String =
      s"${book.title} by ${book.authors.mkString(", ")} (${book.publicationYear})"
    
    def displayDetailed(book: Book): String =
      s"""Book Details:
         |  ISBN: ${book.isbn.value}
         |  Title: ${book.title}
         |  Authors: ${book.authors.mkString(", ")}
         |  Genre: ${book.genre}
         |  Year: ${book.publicationYear}
         |  Status: ${if book.isAvailable then "Available" else "On Loan"}""".stripMargin
  
  given Validatable[Book] with
    def validate(book: Book): Either[String, Book] =
      if book.title.trim.isEmpty then
        Left("Book title cannot be empty")
      else if book.authors.isEmpty then
        Left("Book must have at least one author")
      else if book.publicationYear < 1000 || book.publicationYear > LocalDateTime.now().getYear then
        Left(s"Invalid publication year: ${book.publicationYear}")
      else if book.isbn.value.trim.isEmpty then
        Left("ISBN cannot be empty")
      else
        Right(book)
  
  given Similarity[Book] with
    def similarity(a: Book, b: Book): Double =
      val titleSim = calculateStringSimilarity(a.title.toLowerCase, b.title.toLowerCase)
      val authorSim = calculateAuthorSimilarity(a.authors, b.authors)
      val genreSim = if a.genre.toLowerCase == b.genre.toLowerCase then 1.0 else 0.0
      
      // Weighted average: title 40%, authors 40%, genre 20%
      (titleSim * 0.4) + (authorSim * 0.4) + (genreSim * 0.2)
  
  // Given instances for User
  given Displayable[User] with
    def display(user: User): String = user match
      case User.Student(_, name, major, _) => s"$name (Student - $major)"
      case User.Faculty(_, name, dept, _) => s"$name (Faculty - $dept)"
      case User.Librarian(_, name, empId, _) => s"$name (Librarian - $empId)"
    
    def displayDetailed(user: User): String = user match
      case User.Student(id, name, major, _) =>
        s"""Student Details:
           |  ID: ${id.value}
           |  Name: $name
           |  Major: $major
           |  Type: Student
           |  Max Loans: ${user.maxLoans}
           |  Loan Period: ${user.loanPeriod.toDays} days""".stripMargin
      
      case User.Faculty(id, name, dept, _) =>
        s"""Faculty Details:
           |  ID: ${id.value}
           |  Name: $name
           |  Department: $dept
           |  Type: Faculty
           |  Max Loans: ${user.maxLoans}
           |  Loan Period: ${user.loanPeriod.toDays} days""".stripMargin
      
      case User.Librarian(id, name, empId, _) =>
        s"""Librarian Details:
           |  ID: ${id.value}
           |  Name: $name
           |  Employee ID: $empId
           |  Type: Librarian
           |  Max Loans: ${user.maxLoans}
           |  Permissions: Full Access""".stripMargin
  
  given Validatable[User] with
    def validate(user: User): Either[String, User] =
      if user.name.trim.isEmpty then
        Left("User name cannot be empty")
      else if user.password.length < 6 then
        Left("Password must be at least 6 characters")
      else user match
        case User.Student(_, _, major, _) if major.trim.isEmpty =>
          Left("Student major cannot be empty")
        case User.Faculty(_, _, dept, _) if dept.trim.isEmpty =>
          Left("Faculty department cannot be empty")
        case User.Librarian(_, _, empId, _) if empId.trim.isEmpty =>
          Left("Librarian employee ID cannot be empty")
        case _ => Right(user)
  
  // Given instances for Transaction
  given Displayable[Transaction] with
    def display(transaction: Transaction): String = transaction match
      case Transaction.Loan(book, user, timestamp, dueDate) =>
        val due = dueDate.map(d => s", due ${d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}").getOrElse("")
        s"LOAN: ${book.title} to ${user.name} on ${timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}$due"
      
      case Transaction.Return(book, user, timestamp) =>
        s"RETURN: ${book.title} from ${user.name} on ${timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}"
      
      case Transaction.Reservation(book, user, timestamp, startDate, endDate) =>
        s"RESERVATION: ${book.title} by ${user.name} (${startDate.toLocalDate} to ${endDate.toLocalDate})"
    
    def displayDetailed(transaction: Transaction): String = transaction match
      case Transaction.Loan(book, user, timestamp, dueDate) =>
        s"""Loan Transaction:
           |  Book: ${book.title}
           |  Borrower: ${user.name}
           |  Loan Date: ${timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}
           |  Due Date: ${dueDate.map(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).getOrElse("No limit")}""".stripMargin
      
      case Transaction.Return(book, user, timestamp) =>
        s"""Return Transaction:
           |  Book: ${book.title}
           |  Returner: ${user.name}
           |  Return Date: ${timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}""".stripMargin
      
      case Transaction.Reservation(book, user, timestamp, startDate, endDate) =>
        s"""Reservation Transaction:
           |  Book: ${book.title}
           |  User: ${user.name}
           |  Reserved On: ${timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}
           |  Period: ${startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} to ${endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}""".stripMargin
  
  // Extension methods that use type classes
  extension [T](value: T)(using displayable: Displayable[T])
    def display: String = displayable.display(value)
    def displayDetailed: String = displayable.displayDetailed(value)
  
  extension [T](value: T)(using validatable: Validatable[T])
    def validate: Either[String, T] = validatable.validate(value)
    def isValid: Boolean = validatable.validate(value).isRight
  
  extension [T](a: T)(using similarity: Similarity[T])
    def similarityTo(b: T): Double = similarity.similarity(a, b)
    def isSimilarTo(b: T, threshold: Double = 0.7): Boolean = similarity.similarity(a, b) >= threshold
  
  // Helper functions
  private def calculateStringSimilarity(s1: String, s2: String): Double =
    if s1 == s2 then 1.0
    else if s1.isEmpty || s2.isEmpty then 0.0
    else
      val longer = if s1.length > s2.length then s1 else s2
      val shorter = if s1.length > s2.length then s2 else s1
      val editDistance = levenshteinDistance(longer, shorter)
      (longer.length - editDistance).toDouble / longer.length
  
  private def calculateAuthorSimilarity(authors1: List[String], authors2: List[String]): Double =
    if authors1.isEmpty && authors2.isEmpty then 1.0
    else if authors1.isEmpty || authors2.isEmpty then 0.0
    else
      val commonAuthors = authors1.intersect(authors2).size
      val totalAuthors = (authors1 ++ authors2).distinct.size
      commonAuthors.toDouble / totalAuthors
  
  private def levenshteinDistance(s1: String, s2: String): Int =
    val dist = Array.tabulate(s2.length + 1, s1.length + 1) { (j, i) =>
      if j == 0 then i
      else if i == 0 then j
      else 0
    }
    
    for
      j <- s2.indices
      i <- s1.indices
    do
      dist(j + 1)(i + 1) =
        if s2(j) == s1(i) then dist(j)(i)
        else (dist(j)(i) + dist(j + 1)(i) + dist(j)(i + 1)) min 3
    
    dist(s2.length)(s1.length)
