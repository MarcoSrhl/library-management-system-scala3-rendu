package library

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.collection.immutable.{Map, List}
import Types.{ISBN, UserID}
import Transaction.*
import User.*

/**
 * main class that manages the whole library system state
 * 
 * this is the heart of the system, keeps track of all books,
 * users and transactions. everything is immutable so no surprises
 * when we modify stuff
 * 
 * parameters:
 * - books: all books with their ISBN as key
 * - users: all users with their ID
 * - transactions: list of everything that happened in chronological order
 * 
 * basic example:
 * val catalog = LibraryCatalog.empty
 * val book = Book(ISBN("978-0134685991"), "Effective Java", List("Joshua Bloch"), 2017, "Programming", true)
 * val user = User.Student(UserID(UUID.randomUUID()), "Alice", "Computer Science", "password123")
 * val result = catalog.addBook(book).addUser(user).loanBook(book.isbn, user.id)
 */
class LibraryCatalog(
  val books: Map[ISBN, Book],
  val users: Map[UserID, User],
  val transactions: List[Transaction]
):

  // ajoute un livre au catalogue, si l'ISBN existe déjà il sera remplacé
  def addBook(book: Book): LibraryCatalog =
    copy(books = books + (book.isbn -> book))

  // add a user to the system, if ID already exists it gets replaced
  def addUser(user: User): LibraryCatalog =
    copy(users = users + (user.id -> user))

  // borrow a book - checks everything is ok before doing the transaction
  // looks if book and user exist, if book is available,
  // if user hasnt exceeded their loan limit, etc
  def loanBook(isbn: ISBN, userId: UserID): LibraryCatalog =
    (books.get(isbn), users.get(userId)) match
      case (Some(book), Some(user)) if book.isAvailable =>
        val currentLoans = activeLoansFor(userId)
        if currentLoans >= user.maxLoans then
          println(s"Loan failed: ${user.name} has reached maximum loan limit (${user.maxLoans})")
          return this

        val dueDate = user.calculateDueDate(LocalDateTime.now)
        val updatedBook = book.copy(isAvailable = false)
        val newTransaction = Loan(updatedBook, user, LocalDateTime.now, Some(dueDate))

        val lastLoaner = lastLoanedBy(isbn)
        println(lastLoaner.map(u => s"Last borrowed by: ${u.name}").getOrElse("No previous borrower."))
        println(s"Due date: $dueDate")
        println(s"${user.name} (${if user.isStudent then "Student" else if user.isFaculty then "Faculty" else "Librarian"}) has borrowed '${book.title}'")

        copy(
          books = books.updated(isbn, updatedBook),
          transactions = newTransaction :: transactions
        )

      case (Some(_), Some(_)) =>
        println("Loan failed: book is not available.")
        this
      case _ =>
        println("Loan failed: book or user not found.")
        this

  def returnBook(isbn: ISBN, userId: UserID): LibraryCatalog =
    (books.get(isbn), users.get(userId)) match
      case (Some(book), Some(user)) =>
        // check if this user actually has this book on loan
        val hasActiveLoan = transactions.exists {
          case loan @ Loan(loanedBook, loanUser, _, _) if loanedBook.isbn == isbn && loanUser.id == userId =>
            // check its not already returned (return must be AFTER the loan)
            !transactions.exists {
              case Return(returnedBook, returnUser, returnTimestamp) => 
                returnedBook.isbn == isbn && returnUser.id == userId && returnTimestamp.isAfter(loan.timestamp)
              case _ => false
            }
          case _ => false
        }

        if !hasActiveLoan then
          println(s"Return failed: ${user.name} does not have '${book.title}' on loan.")
          return this

        val updatedBook = book.copy(isAvailable = true)
        val newTransaction = Return(updatedBook, user, LocalDateTime.now)
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
        if !user.canReserve then
          println(s"Reservation failed: ${user.name} (${if user.isLibrarian then "Librarian" else "Unknown"}) is not allowed to make reservations.")
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

            val newTransaction = Reservation(book, user, LocalDateTime.now, startDateTime, actualEndDate)
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
      case Loan(book, user, _, _) if book.isbn == isbn => user
    }.headOption

  def activeLoansFor(userId: UserID): Int =
    transactions.collect {
      case l @ Loan(_, user, _, _) if user.id == userId => l
    }.count(loan =>
      !transactions.exists {
        case Return(book, u, timestamp) => 
          book.isbn == loan.book.isbn && u.id == userId && timestamp.isAfter(loan.timestamp)
        case _ => false
      }
    )

  def overdueLoansFor(userId: UserID): Int =
    transactions.collect {
      case Loan(_, user, _, Some(due)) if user.id == userId && due.isBefore(LocalDateTime.now) =>
        due
    }.length

  def calculateOverdueFees(userId: UserID): Double =
    users.get(userId) match
      case Some(user) =>
        val overdueLoans = transactions.collect {
          case Loan(book, u, _, Some(due)) if u.id == userId && due.isBefore(LocalDateTime.now) =>
            val daysOverdue = ChronoUnit.DAYS.between(due, LocalDateTime.now)
            daysOverdue * user.overdueFeeRate
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
      case Loan(book, _, _, Some(dueDate)) if book.isbn == isbn =>
        val loanStart = transactions.find {
          case Loan(b, _, timestamp, _) if b.isbn == isbn => true
          case _ => false
        }.map(_.timestamp).getOrElse(now)
        (loanStart, dueDate)
      
      case Reservation(book, _, _, startDate, endDate) if book.isbn == isbn =>
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
      case Loan(book, _, _, Some(dueDate)) if book.isbn == isbn && dueDate.isAfter(now) => dueDate
      case Reservation(book, _, _, _, endDate) if book.isbn == isbn && endDate.isAfter(now) => endDate
    }.maxOption
    
    busyUntil.map(_.plusDays(1))

  /**
   * Check if a time slot is available for reservation.
   */
  def isSlotAvailable(isbn: ISBN, startDate: LocalDateTime, endDate: LocalDateTime): Boolean =
    !transactions.exists {
      case Loan(book, _, _, Some(dueDate)) if book.isbn == isbn =>
        // Check if there's overlap with existing loan
        !(endDate.isBefore(startDate) || startDate.isAfter(dueDate))
      case Reservation(book, _, _, resStart, resEnd) if book.isbn == isbn =>
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
      case Loan(book, _, _, Some(dueDate)) if book.isbn == isbn && dueDate.isAfter(now) =>
        val loanStart = transactions.collectFirst {
          case Loan(b, _, timestamp, _) if b.isbn == isbn => timestamp
        }.getOrElse(now)
        (if loanStart.isBefore(now) then now else loanStart, dueDate)
      
      case Reservation(book, _, _, startDate, endDate) if book.isbn == isbn && endDate.isAfter(now) =>
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

/**
 * Companion object for LibraryCatalog providing factory methods and utilities.
 * 
 * This object contains convenient methods to create new LibraryCatalog instances
 * and provides common catalog configurations for different use cases.
 * 
 * @since 1.0.0
 */
object LibraryCatalog:
  /**
   * Creates an empty LibraryCatalog with no books, users, or transactions.
   * 
   * This factory method is the recommended way to create a new catalog instance
   * when starting with an empty library system.
   * 
   * @return A new empty LibraryCatalog instance
   * 
   * @example
   * {{{
   * val newCatalog = LibraryCatalog.empty
   * assert(newCatalog.books.isEmpty)
   * assert(newCatalog.users.isEmpty)
   * assert(newCatalog.transactions.isEmpty)
   * }}}
   * 
   * @since 1.0.0
   */
  def empty: LibraryCatalog = new LibraryCatalog(Map.empty, Map.empty, List.empty)
  
  /**
   * Creates a LibraryCatalog with the specified initial data.
   * 
   * This factory method allows creating a catalog with pre-existing books,
   * users, and transaction history. Useful for loading saved state or
   * initializing with test data.
   * 
   * @param books Initial collection of books indexed by ISBN
   * @param users Initial collection of users indexed by UserID  
   * @param transactions Initial transaction history in chronological order
   * @return A new LibraryCatalog instance with the specified data
   * 
   * @example
   * {{{
   * val books = Map(isbn -> book)
   * val users = Map(userId -> user)
   * val transactions = List(loanTransaction, returnTransaction)
   * val catalog = LibraryCatalog(books, users, transactions)
   * }}}
   * 
   * @since 1.0.0
   */
  def apply(books: Map[ISBN, Book], users: Map[UserID, User], transactions: List[Transaction]): LibraryCatalog =
    new LibraryCatalog(books, users, transactions)
