package library

import Types.{ISBN, UserID}

/**
 * Results.scala - Error Handling and Result Types System
 * 
 * This file implements a comprehensive error handling system for the library management
 * system using Scala 3's union types and enums. It provides a functional approach to
 * error handling that ensures all possible failure cases are handled at compile time.
 * 
 * Key Features:
 * - Union Types: Flexible return types that can be either success values or errors
 * - Error Hierarchy: Organized error types using Scala 3 enums for different domains
 * - Functional API: Extension methods for working with results in a functional style
 * - Type Safety: Compile-time guarantees that errors are handled
 * - Pattern Matching: Helper objects for easy pattern matching on results
 * 
 * The file demonstrates  the following features:
 * - Union types (T | Error) for flexible API design
 * - Enum-based error hierarchies replacing sealed trait patterns
 * - Extension methods for result manipulation
 * - Functional error handling patterns (map, flatMap, getOrElse)
 * 
 * Error Categories:
 * - LibraryError: General library operation errors
 * - BookError: Book-specific validation and operation errors
 * - UserError: User-specific validation and authentication errors
 * - TransactionError: Transaction-specific operation errors
 * 
 * Usage:
 * - Functions return LibraryResult[T] instead of throwing exceptions
 * - Use pattern matching with Success/Failure extractors
 * - Chain operations with map/flatMap for error propagation
 * - Handle errors gracefully with getOrElse for default values
 */
object Results:
  
  // Union types for operation results
  type LibraryResult[T] = T | LibraryError
  type BookOperationResult = Book | BookError
  type UserOperationResult = User | UserError
  type TransactionResult = Transaction | TransactionError
  
  // Error hierarchy using enums
  enum LibraryError:
    case BookNotFound(isbn: ISBN)
    case UserNotFound(userId: UserID)
    case BookAlreadyLoaned(isbn: ISBN, currentBorrower: String)
    case BookNotLoaned(isbn: ISBN)
    case UserLimitExceeded(userId: UserID, currentLoans: Int, maxLoans: Int)
    case ReservationNotAllowed(userId: UserID, reason: String)
    case InvalidOperation(message: String)
    case IOError(message: String)
    case ValidationError(field: String, message: String)
  
  enum BookError:
    case InvalidISBN(isbn: String)
    case InvalidTitle(title: String)
    case InvalidAuthors(authors: List[String])
    case InvalidYear(year: Int)
    case DuplicateBook(isbn: ISBN)
  
  enum UserError:
    case InvalidUserId(id: String)
    case InvalidName(name: String)
    case InvalidPassword(reason: String)
    case DuplicateUser(userId: UserID)
    case AuthenticationFailed(identifier: String)
  
  enum TransactionError:
    case InvalidLoanPeriod(days: Int)
    case BookAlreadyReturned(isbn: ISBN, userId: UserID)
    case ReturnDatePassed(isbn: ISBN, dueDate: java.time.LocalDateTime)
    case TransactionNotFound(transactionId: String)
  
  // Extension methods for error handling
  extension [T](result: LibraryResult[T])
    def isSuccess: Boolean = result match
      case _: LibraryError => false
      case _ => true
    
    def isError: Boolean = !isSuccess
    
    def getOrThrow: T = result match
      case error: LibraryError => throw new RuntimeException(s"Library operation failed: $error")
      case value => value.asInstanceOf[T]
    
    def getOrElse[U >: T](default: U): U = result match
      case _: LibraryError => default
      case value => value.asInstanceOf[T]
    
    def map[U](f: T => U): LibraryResult[U] = result match
      case error: LibraryError => error
      case value => f(value.asInstanceOf[T])
    
    def flatMap[U](f: T => LibraryResult[U]): LibraryResult[U] = result match
      case error: LibraryError => error
      case value => f(value.asInstanceOf[T])
  
  // Helper methods for creating results
  def success[T](value: T): LibraryResult[T] = value
  def failure[T](error: LibraryError): LibraryResult[T] = error
  
  // Pattern matching helpers
  object Success:
    def unapply[T](result: LibraryResult[T]): Option[T] = result match
      case _: LibraryError => None
      case value => Some(value.asInstanceOf[T])
  
  object Failure:
    def unapply[T](result: LibraryResult[T]): Option[LibraryError] = result match
      case error: LibraryError => Some(error)
      case _ => None
