package library

import java.time.LocalDateTime

/**
 * transaction system to keep track of everything that happens
 * 
 * using scala 3 enums to have different transaction types
 * (loan, return, reservation) in a clean hierarchy
 * 
 * each transaction is an immutable event with a timestamp,
 * so we can rebuild the system state at any point in time
 * 
 * transaction types:
 * - Loan: when someone borrows a book (with optional due date)
 * - Return: when a book gets returned 
 * - Reservation: when someone reserves a book for later
 * 
 * everything is immutable so no risk of data corruption,
 * and we can easily add new transaction types if needed
 */
enum Transaction:

  // book loan with optional due date
  case Loan(book: Book, user: User, timestamp: LocalDateTime, dueDate: Option[LocalDateTime])

  // book return
  case Return(book: Book, user: User, timestamp: LocalDateTime)

  // reservation with start and end period
  case Reservation(book: Book, user: User, timestamp: LocalDateTime, startDate: LocalDateTime, endDate: LocalDateTime)

object Transaction:

  extension (tx: Transaction)
    def book: Book = tx match
      case Transaction.Loan(book, _, _, _)         => book
      case Transaction.Return(book, _, _)          => book
      case Transaction.Reservation(book, _, _, _, _) => book

    def user: User = tx match
      case Transaction.Loan(_, user, _, _)        => user
      case Transaction.Return(_, user, _)         => user
      case Transaction.Reservation(_, user, _, _, _) => user

    def timestamp: LocalDateTime = tx match
      case Transaction.Loan(_, _, timestamp, _)      => timestamp
      case Transaction.Return(_, _, timestamp)       => timestamp
      case Transaction.Reservation(_, _, timestamp, _, _) => timestamp

    def dueDate: Option[LocalDateTime] = tx match
      case Transaction.Loan(_, _, _, dueDate) => dueDate
      case _                                  => None
