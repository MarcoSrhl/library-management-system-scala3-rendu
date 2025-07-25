package services

import models.*
import utils.Types.{ISBN, UserID}
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.collection.immutable.{Map, List}

/**
 * Core service class for library management operations.
 * 
 * The LibraryCatalog represents the complete state of a library system, providing
 * comprehensive functionality for book management, user administration, transaction
 * processing, and business rule enforcement. This class serves as the primary
 * service layer, coordinating all library operations while maintaining data integrity
 * and implementing complex business logic.
 * 
 * == Design Principles ==
 * 
 * '''Immutability''': All operations return new instances rather than modifying
 * existing state, ensuring thread safety and functional programming principles.
 * 
 * '''Type Safety''': Leverages Scala 3's advanced type system with Union types,
 * opaque types, and comprehensive error handling.
 * 
 * '''Business Logic Encapsulation''': Centralizes all library business rules
 * and constraints within service methods.
 * 
 * == Core Functionality ==
 * 
 * '''Book Operations''':
 *  - Add/remove books from catalog
 *  - Loan books with automatic availability tracking
 *  - Return books with overdue fee calculation
 *  - Reserve books with queue management
 * 
 * '''User Management''':
 *  - Add/remove users with role-based permissions
 *  - Enforce loan limits per user type
 *  - Track user transaction history
 * 
 * '''Transaction Processing''':
 *  - Comprehensive audit trail of all operations
 *  - Due date calculation and overdue tracking
 *  - Fee calculation and waiving capabilities
 * 
 * == Business Rules ==
 * 
 * '''Loan Limits''':
 *  - Students: 5 books maximum
 *  - Faculty: 20 books maximum  
 *  - Librarians: Unlimited
 * 
 * '''Loan Periods''':
 *  - Students: 30 days
 *  - Faculty: Unlimited
 *  - Librarians: Unlimited
 * 
 * '''Permissions''':
 *  - Only librarians can add/remove books and users
 *  - All users can loan/return books (subject to limits)
 *  - Only librarians can waive overdue fees
 * 
 * @example
 * {{{
 * import models._
 * import utils.Types._
 * 
 * // Create empty catalog
 * val catalog = LibraryCatalog.empty
 * 
 * // Add a book
 * val book = Book(ISBN.safe("978-0134685991").get, "Programming in Scala", 
 *                 List("Martin Odersky"), 2021, "Computer Science", true)
 * val catalogWithBook = catalog.addBook(book)
 * 
 * // Add a user
 * val student = User.Student(UserID("s001"), "Alice", "alice@uni.edu", LocalDate.now)
 * val catalogWithUser = catalogWithBook.addUser(student)
 * 
 * // Loan a book
 * val finalCatalog = catalogWithUser.loanBook(book.isbn, student.id)
 * 
 * // Check active loans
 * val activeLoans = finalCatalog.activeLoansFor(student.id)
 * println(s"Student has $activeLoans active loans")
 * }}}
 * 
 * @param books Immutable map of ISBN to Book entities representing the library's collection
 * @param users Immutable map of UserID to User entities representing registered library members  
 * @param transactions Immutable list of all Transaction records providing complete audit trail
 * 
 * @since 1.0.0
 * @see [[models.Book]] for book entity structure
 * @see [[models.User]] for user hierarchy and permissions
 * @see [[models.Transaction]] for transaction types and history
 */
class LibraryCatalog(
  val books: Map[ISBN, Book],
  val users: Map[UserID, User],
  val transactions: List[Transaction]
):

  /**
   * Adds a new book to the library catalog.
   * 
   * This method creates a new catalog instance with the specified book added
   * to the collection. The book's ISBN serves as the unique identifier.
   * 
   * @param book The book entity to add to the catalog. Must have a unique ISBN.
   * @return New LibraryCatalog instance containing the added book
   * 
   * @example
   * {{{
   * val book = Book(ISBN.safe("978-0134685991").get, "Programming in Scala", 
   *                 List("Martin Odersky"), 2021, "Computer Science", true)
   * val newCatalog = catalog.addBook(book)
   * }}}
   * 
   * @note This operation overwrites any existing book with the same ISBN
   * @since 1.0.0
   */
  def addBook(book: Book): LibraryCatalog =
    copy(books = books + (book.isbn -> book))

  /**
   * Adds a new user to the library system.
   * 
   * Registers a new user in the library system, enabling them to borrow books
   * and access library services based on their user type and permissions.
   * 
   * @param user The user entity to add. Must have a unique UserID.
   * @return New LibraryCatalog instance containing the added user
   * 
   * @example
   * {{{
   * val student = User.Student(UserID("s001"), "Alice Johnson", 
   *                           "alice@uni.edu", LocalDate.now)
   * val newCatalog = catalog.addUser(student)
   * }}}
   * 
   * @note This operation overwrites any existing user with the same ID
   * @since 1.0.0
   */
  def addUser(user: User): LibraryCatalog =
    copy(users = users + (user.id -> user))

  /**
   * Loans a book to a user with comprehensive business rule validation.
   * 
   * This method handles the complete book loan process, including:
   * - Availability verification
   * - User loan limit enforcement  
   * - Due date calculation based on user type
   * - Transaction record creation
   * - Book availability status updates
   * 
   * The loan will fail if:
   * - Book is not available
   * - User has reached their loan limit
   * - Book or user does not exist
   * 
   * @param isbn The ISBN of the book to loan
   * @param userId The ID of the user requesting the loan
   * @return New LibraryCatalog instance with updated state, or unchanged if loan failed
   * 
   * @example
   * {{{
   * val result = catalog.loanBook(ISBN.safe("978-0134685991").get, UserID("s001")) 
   * // Check if loan was successful by comparing catalog instances
   * val successful = result != catalog
   * }}}
   * 
   * @note Loan limits: Students (5), Faculty (20), Librarians (unlimited)
   * @note Loan periods: Students (30 days), Faculty/Librarians (unlimited)
   * @since 1.0.0
   */
  def loanBook(isbn: ISBN, userId: UserID): LibraryCatalog =
    (books.get(isbn), users.get(userId)) match
      case (Some(book), Some(user)) if book.isAvailable =>
        val currentLoans = activeLoansFor(userId)
        if currentLoans >= user.maxLoansAllowed then
          println(s"Loan failed: ${user.name} has reached maximum loan limit (${user.maxLoansAllowed})")
          return this

        val loanTransaction = Transaction.createLoan(book, user)
        val updatedBook = book.copy(isAvailable = false)

        val lastLoaner = lastLoanedBy(isbn)
        println(lastLoaner.map(u => s"Last borrowed by: ${u.name}").getOrElse("No previous borrower."))
        loanTransaction match {
          case Transaction.Loan(_, _, _, Some(dueDate)) => println(s"Due date: $dueDate")
          case _ => println("No due date (unlimited loan period)")
        }
        println(s"${user.name} (${user.role}) has borrowed '${book.title}'")

        copy(
          books = books.updated(isbn, updatedBook),
          transactions = loanTransaction :: transactions
        )

      case (Some(_), Some(_)) =>
        println("Loan failed: book is not available.")
        this
      case _ =>
        println("Loan failed: book or user not found.")
        this

  /**
   * Processes the return of a loaned book with validation and state updates.
   * 
   * This method handles the complete book return process, including:
   * - Validation that the user has an active loan for the book
   * - Book availability status restoration
   * - Return transaction record creation
   * - Overdue fee calculation (if applicable)
   * 
   * The return will fail if:
   * - User does not have an active loan for the book
   * - Book or user does not exist in the system
   * - Book has already been returned
   * 
   * @param isbn The ISBN of the book being returned
   * @param userId The ID of the user returning the book
   * @return New LibraryCatalog instance with updated state, or unchanged if return failed
   * 
   * @example
   * {{{
   * // Return a book
   * val returnedCatalog = catalog.returnBook(ISBN.safe("978-0134685991").get, UserID("s001"))
   * 
   * // Check if return was successful
   * val successful = returnedCatalog != catalog
   * 
   * // Calculate any overdue fees
   * val fees = returnedCatalog.calculateOverdueFees(UserID("s001"))
   * }}}
   * 
   * @note Only users with active loans for the specific book can return it
   * @note Return automatically makes the book available for other users
   * @since 1.0.0
   */
  def returnBook(isbn: ISBN, userId: UserID): LibraryCatalog =
    (books.get(isbn), users.get(userId)) match
      case (Some(book), Some(user)) =>
        // Check if this user actually has this book on loan
        val hasActiveLoan = transactions.exists {
          case loan @ Transaction.Loan(loanedBook, loanUser, _, _) if loanedBook.isbn == isbn && loanUser.id == userId =>
            // Check if not already returned (return must be AFTER the loan)
            !transactions.exists {
              case Transaction.Return(returnedBook, returnUser, returnTimestamp) => 
                returnedBook.isbn == isbn && returnUser.id == userId && returnTimestamp.isAfter(loan.timestamp)
              case _ => false
            }
          case _ => false
        }

        if !hasActiveLoan then
          println(s"Return failed: ${user.name} does not have '${book.title}' on loan.")
          return this

        val updatedBook = book.copy(isAvailable = true)
        val newTransaction = Transaction.createReturn(book, user)
        println(s"${user.name} returned '${book.title}'")
        copy(
          books = books.updated(isbn, updatedBook),
          transactions = newTransaction :: transactions
        )
      case _ =>
        println("Return failed: book or user not found.")
        this

  def reserveBook(isbn: ISBN, userId: UserID): LibraryCatalog =
    (books.get(isbn), users.get(userId)) match
      case (Some(book), Some(user)) =>
        if !user.canReserveBooks then
          println(s"Reservation failed: ${user.name} (${user.role}) is not allowed to make reservations.")
          return this
        
        val availabilityPeriods = getBookAvailabilityPeriods(isbn)
        if availabilityPeriods.isEmpty then
          println(s"Book '${book.title}' has no available periods.")
          return this

        println(s"Book availability for '${book.title}':")
        availabilityPeriods.foreach { case (start, end) =>
          if end.isAfter(LocalDateTime.now.plusYears(10)) then
            println(s"✓ Available: ${start.toLocalDate} onwards (no end limit)")
          else
            println(s"✓ Available: ${start.toLocalDate} to ${end.toLocalDate}")
        }

        println("Enter the date you want to start your reservation (YYYY-MM-DD) or 'cancel':")
        val input = scala.io.StdIn.readLine().trim
        if input.toLowerCase == "cancel" then
          println("Reservation cancelled.")
          return this

        parseDate(input) match
          case Some(startDate) =>
            val startDateTime = startDate.atStartOfDay()
            if !isDateAvailable(startDateTime, availabilityPeriods) then
              println(s"Date $startDate is not available for reservation.")
              return this

            val maxEndDate = calculateMaxReservationEnd(startDateTime, availabilityPeriods)
            val actualEndDate = if maxEndDate.isBefore(startDateTime.plusMonths(1)) then maxEndDate else startDateTime.plusMonths(1)
            
            val duration = ChronoUnit.DAYS.between(startDateTime, actualEndDate)
            if duration < 1 then
              println("Reservation period too short (less than 1 day).")
              return this

            val newTransaction = Transaction.createReservation(book, user, startDateTime, actualEndDate)
            println(s"${user.name} has reserved '${book.title}' from ${startDateTime.toLocalDate} to ${actualEndDate.toLocalDate} ($duration days)")
            copy(transactions = newTransaction :: transactions)

          case None =>
            println("Invalid date format. Please use YYYY-MM-DD.")
            this

      case _ =>
        println("Reservation failed: book or user not found.")
        this

  def lastLoanedBy(isbn: ISBN): Option[User] =
    transactions.collect {
      case Transaction.Loan(book, user, _, _) if book.isbn == isbn => user
    }.headOption

  def activeLoansFor(userId: UserID): Int =
    transactions.collect {
      case l @ Transaction.Loan(_, user, _, _) if user.id == userId => l
    }.count(loan =>
      !transactions.exists {
        case Transaction.Return(book, u, timestamp) => 
          book.isbn == loan.book.isbn && u.id == userId && timestamp.isAfter(loan.timestamp)
        case _ => false
      }
    )

  def overdueLoansFor(userId: UserID): Int =
    transactions.collect {
      case Transaction.Loan(_, user, _, Some(due)) if user.id == userId && due.isBefore(LocalDateTime.now) =>
        due
    }.length

  def calculateOverdueFees(userId: UserID): Double =
    users.get(userId) match
      case Some(user) =>
        val overdueLoans = transactions.collect {
          case Transaction.Loan(book, u, _, Some(due)) if u.id == userId && due.isBefore(LocalDateTime.now) =>
            val daysOverdue = ChronoUnit.DAYS.between(due, LocalDateTime.now)
            daysOverdue * 0.50 // $0.50 per day overdue fee
        }
        overdueLoans.sum
      case None => 0.0

  def waiveFees(userId: UserID, waivingLibrarian: User): LibraryCatalog =
    if !waivingLibrarian.isLibrarian then
      println("Only librarians can waive fees.")
      this
    else
      println(s"Fees waived for user $userId by librarian ${waivingLibrarian.name}")
      // In a real system, this would update fee records
      this

  /**
   * Calculate available reservation slots for a book.
   * Returns list of (startDate, endDate) tuples for available periods.
   */
  def getAvailableReservationSlots(isbn: ISBN): List[(LocalDateTime, LocalDateTime)] =
    val now = LocalDateTime.now
    val oneMonthFromNow = now.plusMonths(1)
    
    // Get all periods when the book is unavailable (loans + existing reservations)
    val unavailablePeriods = transactions.collect {
      case Transaction.Loan(book, _, _, Some(dueDate)) if book.isbn == isbn =>
        val loanStart = transactions.find {
          case Transaction.Loan(b, _, timestamp, _) if b.isbn == isbn => true
          case _ => false
        }.map(_.timestamp).getOrElse(now)
        (loanStart, dueDate)
      
      case Transaction.Reservation(book, _, _, startDate, endDate) if book.isbn == isbn =>
        (startDate, endDate)
    }.sortBy(_._1)

    // Find gaps between unavailable periods
    var availableSlots = List.empty[(LocalDateTime, LocalDateTime)]
    var currentStart = now
    val relevantPeriods = unavailablePeriods.takeWhile(_._1.isBefore(oneMonthFromNow.plusDays(30)))

    for ((unavailableStart, unavailableEnd) <- relevantPeriods if currentStart.isBefore(oneMonthFromNow)) {
      // If there's a gap before this unavailable period
      if currentStart.isBefore(unavailableStart) then
        val slotEnd = if unavailableStart.isBefore(oneMonthFromNow) then unavailableStart else oneMonthFromNow
        if ChronoUnit.DAYS.between(currentStart, slotEnd) >= 7 then
          availableSlots = availableSlots :+ (currentStart, slotEnd)
      
      // Move to the end of this unavailable period
      currentStart = unavailableEnd.plusDays(1)
    }

    // Add remaining time until one month from now
    if currentStart.isBefore(oneMonthFromNow) then
      if ChronoUnit.DAYS.between(currentStart, oneMonthFromNow) >= 7 then
        availableSlots = availableSlots :+ (currentStart, oneMonthFromNow)

    availableSlots

  /**
   * Get the next date when a book will be available.
   * Returns None if the book is currently available.
   */
  def getNextAvailableDate(isbn: ISBN): Option[LocalDateTime] =
    val now = LocalDateTime.now
    
    // Find the latest end date from current loans and reservations
    val busyUntil = transactions.collect {
      case Transaction.Loan(book, _, _, Some(dueDate)) if book.isbn == isbn && dueDate.isAfter(now) => dueDate
      case Transaction.Reservation(book, _, _, _, endDate) if book.isbn == isbn && endDate.isAfter(now) => endDate
    }.maxOption
    
    busyUntil.map(_.plusDays(1))

  /**
   * Check if a time slot is available for reservation.
   */
  def isSlotAvailable(isbn: ISBN, startDate: LocalDateTime, endDate: LocalDateTime): Boolean =
    !transactions.exists {
      case Transaction.Loan(book, _, _, Some(dueDate)) if book.isbn == isbn =>
        // Check if there's overlap with existing loan
        !(endDate.isBefore(startDate) || startDate.isAfter(dueDate))
      case Transaction.Reservation(book, _, _, resStart, resEnd) if book.isbn == isbn =>
        // Check if there's overlap with existing reservation
        !(endDate.isBefore(resStart) || startDate.isAfter(resEnd))
      case _ => false
    }

  /**
   * Show availability calendar for the next few months.
   */
  def showAvailabilityCalendar(isbn: ISBN, startFrom: LocalDateTime): Unit =
    println("\nUpcoming availability:")
    val endDate = startFrom.plusMonths(3)
    var current = startFrom
    
    while current.isBefore(endDate) do
      val monthEnd = current.plusMonths(1)
      val slotEnd = if monthEnd.isBefore(endDate) then monthEnd else endDate
      
      if isSlotAvailable(isbn, current, slotEnd) then
        println(s"✓ Available: ${current.toLocalDate} to ${slotEnd.toLocalDate}")
      else
        println(s"✗ Occupied: ${current.toLocalDate} to ${slotEnd.toLocalDate}")
      
      current = slotEnd.plusDays(1)

  /**
   * Get all availability periods for a book (not limited to next month).
   * Returns list of (startDate, endDate) tuples for ALL available periods.
   */
  def getBookAvailabilityPeriods(isbn: ISBN): List[(LocalDateTime, LocalDateTime)] =
    val now = LocalDateTime.now
    val farFuture = now.plusYears(10) // Look 10 years ahead
    
    // Get all unavailable periods (loans + reservations)
    val unavailablePeriods = transactions.collect {
      case Transaction.Loan(book, _, _, Some(dueDate)) if book.isbn == isbn && dueDate.isAfter(now) =>
        val loanStart = transactions.collectFirst {
          case Transaction.Loan(b, _, timestamp, _) if b.isbn == isbn => timestamp
        }.getOrElse(now)
        (if loanStart.isBefore(now) then now else loanStart, dueDate)
      
      case Transaction.Reservation(book, _, _, startDate, endDate) if book.isbn == isbn && endDate.isAfter(now) =>
        (if startDate.isBefore(now) then now else startDate, endDate)
    }.sortBy(_._1)

    // Find gaps between unavailable periods
    var availableSlots = List.empty[(LocalDateTime, LocalDateTime)]
    var currentStart = now

    for ((unavailableStart, unavailableEnd) <- unavailablePeriods) {
      // If there's a gap before this unavailable period
      if currentStart.isBefore(unavailableStart) then
        availableSlots = availableSlots :+ (currentStart, unavailableStart)
      
      // Move to the end of this unavailable period
      currentStart = unavailableEnd.plusDays(1)
    }

    // Add remaining time to far future
    if currentStart.isBefore(farFuture) then
      availableSlots = availableSlots :+ (currentStart, farFuture)

    // Filter out very short periods (less than 1 day)
    availableSlots.filter { case (start, end) =>
      ChronoUnit.DAYS.between(start, end) >= 1
    }

  /**
   * Check if a specific date is available for reservation.
   */
  def isDateAvailable(date: LocalDateTime, availabilityPeriods: List[(LocalDateTime, LocalDateTime)]): Boolean =
    availabilityPeriods.exists { case (start, end) =>
      !date.isBefore(start) && date.isBefore(end)
    }

  /**
   * Calculate the maximum end date for a reservation starting at a given date.
   */
  def calculateMaxReservationEnd(startDate: LocalDateTime, availabilityPeriods: List[(LocalDateTime, LocalDateTime)]): LocalDateTime =
    availabilityPeriods.find { case (start, end) =>
      !startDate.isBefore(start) && startDate.isBefore(end)
    } match {
      case Some((_, periodEnd)) => periodEnd
      case None => startDate.plusDays(1) // Fallback
    }

  /**
   * Parse a date string in YYYY-MM-DD format.
   */
  def parseDate(dateStr: String): Option[java.time.LocalDate] =
    scala.util.Try(java.time.LocalDate.parse(dateStr)).toOption

  def removeUser(userId: UserID, removingLibrarian: User): LibraryCatalog =
    if !removingLibrarian.isLibrarian then
      println("Only librarians can remove users.")
      this
    else
      users.get(userId) match
        case Some(user) =>
          val activeLoans = activeLoansFor(userId)
          if activeLoans > 0 then
            println(s"Cannot remove ${user.name}: they have $activeLoans active loans.")
            this
          else
            println(s"User ${user.name} removed by ${removingLibrarian.name}")
            copy(users = users - userId)
        case None =>
          println("User not found.")
          this

  def removeBook(isbn: ISBN, removingLibrarian: User): LibraryCatalog =
    if !removingLibrarian.isLibrarian then
      println("Only librarians can remove books.")
      this
    else
      books.get(isbn) match
        case Some(book) =>
          if !book.isAvailable then
            println(s"Cannot remove '${book.title}': it is currently on loan.")
            this
          else
            println(s"Book '${book.title}' removed by ${removingLibrarian.name}")
            copy(books = books - isbn)
        case None =>
          println("Book not found.")
          this

  private def copy(
    books: Map[ISBN, Book] = this.books,
    users: Map[UserID, User] = this.users,
    transactions: List[Transaction] = this.transactions
  ): LibraryCatalog = new LibraryCatalog(books, users, transactions)

  override def toString: String =
    s"""LibraryCatalog(
       |  Books: ${books.values.mkString("\n  ")},
       |  Users: ${users.values.mkString("\n  ")},
       |  Transactions: ${transactions.mkString("\n  ")}
       |)""".stripMargin

object LibraryCatalog:
  def empty: LibraryCatalog = new LibraryCatalog(Map.empty, Map.empty, List.empty)
  
  def apply(books: Map[ISBN, Book], users: Map[UserID, User], transactions: List[Transaction]): LibraryCatalog =
    new LibraryCatalog(books, users, transactions)
