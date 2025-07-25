package models

import utils.Types.{ISBN, UserID}

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
 * The file demonstrates advanced Scala 3 features:
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
  type SearchResult[T] = Success[T] | LibraryError
  type BookOperationResult = Book | BookError
  type UserOperationResult = User | UserError
  type TransactionResult = Transaction | TransactionError
  
  // Success wrapper for explicit success values
  case class Success[T](value: T)
  
  // Error hierarchy using enums
  enum LibraryError:
    case BookNotFound(isbn: String)
    case UserNotFound(userId: String)
    case BookAlreadyLoaned(isbn: ISBN, currentBorrower: String)
    case BookNotLoaned(isbn: ISBN)
    case BookUnavailable(title: String)
    case UserLimitExceeded(userId: UserID, currentLoans: Int, maxLoans: Int)
    case LoanLimitExceeded(userName: String, limit: Int)
    case ReservationNotAllowed(userId: UserID, reason: String)
    case PermissionDenied(msg: String)
    case InvalidOperation(msg: String)
    case IOError(msg: String)
    case ValidationError(msg: String)
    
    def message: String = this match
      case BookNotFound(isbn) => s"Book not found: $isbn"
      case UserNotFound(userId) => s"User not found: $userId"
      case BookAlreadyLoaned(isbn, borrower) => s"Book ${isbn.value} already loaned to $borrower"
      case BookNotLoaned(isbn) => s"Book ${isbn.value} is not currently loaned"
      case BookUnavailable(title) => s"Book unavailable: $title"
      case UserLimitExceeded(userId, current, max) => s"User ${userId.value} has $current loans, maximum is $max"
      case LoanLimitExceeded(userName, limit) => s"Loan limit exceeded for $userName: $limit"
      case ReservationNotAllowed(userId, reason) => s"Reservation not allowed for ${userId.value}: $reason"
      case PermissionDenied(msg) => s"Permission denied: $msg"
      case InvalidOperation(msg) => s"Invalid operation: $msg"
      case IOError(msg) => s"I/O error: $msg"
      case ValidationError(msg) => s"Validation error: $msg"
  
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

  // Extension methods for SearchResult (union type with Success wrapper)
  extension [T](result: SearchResult[T])
    def isSuccess: Boolean = result match
      case _: Success[T] => true
      case _: LibraryError => false
    
    def isError: Boolean = !isSuccess
    
    def map[U](f: T => U): SearchResult[U] = result match
      case s: Success[T] => Success(f(s.value))
      case error: LibraryError => error
    
    def flatMap[U](f: T => SearchResult[U]): SearchResult[U] = result match
      case s: Success[T] => f(s.value)
      case error: LibraryError => error
    
    def getOrElse[U >: T](default: U): U = result match
      case s: Success[T] => s.value
      case _: LibraryError => default
    
    def fold[U](onError: LibraryError => U, onSuccess: T => U): U = result match
      case s: Success[T] => onSuccess(s.value)
      case error: LibraryError => onError(error)
    
    def recover(pf: PartialFunction[LibraryError, T]): SearchResult[T] = result match
      case error: LibraryError if pf.isDefinedAt(error) => Success(pf(error))
      case other => other
    
    def mapError(f: LibraryError => LibraryError): SearchResult[T] = result match
      case s: Success[T] => s
      case error: LibraryError => f(error)
  
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
