package models

import java.time.LocalDateTime

/**
 * Transaction.scala - Transaction Domain Model and Event System
 * 
 * This file defines the transaction system for the library management application
 * using Scala 3's enum feature. It represents all types of transactions that can
 * occur in the library system, providing a complete audit trail of library operations.
 * 
 * Key Features:
 * - Enum-Based Design: Uses Scala 3 enums for clean transaction type hierarchy
 * - Event Sourcing: Each transaction represents an immutable event in the system
 * - Temporal Data: All transactions include timestamp information
 * - Rich Domain Model: Captures all necessary information for each transaction type
 * - Type Safety: Leverages the type system to prevent invalid operations
 * 
 * The file demonstrates advanced Scala 3 features:
 * - Enum definitions with different case parameters
 * - Pattern matching on enum cases
 * - Integration with other domain models (Book, User)
 * - Optional values for conditional data (due dates)
 * 
 * Transaction Types:
 * - Loan: Records when a book is borrowed by a user
 * - Return: Records when a book is returned to the library
 * - Reservation: Records when a user reserves a book for future pickup
 * 
 * Business Rules:
 * - Loans may have due dates (for students) or be unlimited (for faculty/librarians)
 * - Returns complete the loan cycle and make books available again
 * - Reservations allow users to claim books when they become available
 * 
 * Design Principles:
 * - Immutability: All transactions are immutable events
 * - Completeness: Each transaction captures all relevant information
 * - Traceability: Full audit trail of all library operations
 * - Extensibility: Easy to add new transaction types
 * 
 * Usage:
 * - Create transactions when library operations occur
 * - Store in the catalog for historical tracking
 * - Query for reporting and analytics
 * - Pattern match for type-specific behavior
 */
enum Transaction:

  /**
   * Represents a book loan transaction.
   *
   * @param book The book that was loaned.
   * @param user The user who borrowed the book.
   * @param timestamp The date and time the loan was recorded.
   * @param dueDate The date the book must be returned (only for students).
   */
  case Loan(book: Book, user: User, timestamp: LocalDateTime, dueDate: Option[LocalDateTime])

  /**
   * Represents a book return transaction.
   *
   * @param book The book that was returned.
   * @param user The user who returned the book.
   * @param timestamp The date and time the return was recorded.
   */
  case Return(book: Book, user: User, timestamp: LocalDateTime)

  /**
   * Represents a reservation transaction for a book.
   *
   * @param book The reserved book.
   * @param user The user who made the reservation.
   * @param timestamp The date and time the reservation was made.
   * @param startDate When the reservation period starts.
   * @param endDate When the reservation period ends.
   */
  case Reservation(book: Book, user: User, timestamp: LocalDateTime, startDate: LocalDateTime, endDate: LocalDateTime)

object Transaction:

  /**
   * Extension methods for Transaction operations
   */
  extension (transaction: Transaction)
    
    /**
     * Gets the book involved in this transaction.
     */
    def book: Book = transaction match
      case Loan(book, _, _, _) => book
      case Return(book, _, _) => book
      case Reservation(book, _, _, _, _) => book

    /**
     * Gets the user involved in this transaction.
     */
    def user: User = transaction match
      case Loan(_, user, _, _) => user
      case Return(_, user, _) => user
      case Reservation(_, user, _, _, _) => user

    /**
     * Gets the timestamp of this transaction.
     */
    def timestamp: LocalDateTime = transaction match
      case Loan(_, _, timestamp, _) => timestamp
      case Return(_, _, timestamp) => timestamp
      case Reservation(_, _, timestamp, _, _) => timestamp

    /**
     * Checks if this transaction is a loan.
     */
    def isLoan: Boolean = transaction match
      case _: Loan => true
      case _ => false

    /**
     * Checks if this transaction is a return.
     */
    def isReturn: Boolean = transaction match
      case _: Return => true
      case _ => false

    /**
     * Checks if this transaction is a reservation.
     */
    def isReservation: Boolean = transaction match
      case _: Reservation => true
      case _ => false

  /**
   * Helper methods for creating transactions
   */
  def createLoan(book: Book, user: User, timestamp: LocalDateTime = LocalDateTime.now()): Transaction =
    val dueDate = if user.isStudent then Some(user.calculateDueDate(timestamp)) else None
    Loan(book, user, timestamp, dueDate)

  def createReturn(book: Book, user: User, timestamp: LocalDateTime = LocalDateTime.now()): Transaction =
    Return(book, user, timestamp)

  def createReservation(book: Book, user: User, startDate: LocalDateTime, endDate: LocalDateTime, 
                       timestamp: LocalDateTime = LocalDateTime.now()): Transaction =
    Reservation(book, user, timestamp, startDate, endDate)
