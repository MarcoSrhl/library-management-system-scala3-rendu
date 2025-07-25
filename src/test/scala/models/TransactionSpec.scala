package models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import utils.Types.*
import java.time.LocalDateTime

/**
 * Comprehensive test suite for the Transaction model.
 * Tests the Transaction enum and its various cases.
 */
class TransactionSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  // Test data
  val sampleISBN = ISBN("9780134685991")
  val sampleUserID = UserID.random()
  val sampleBook = Book(sampleISBN, "Test Book", List("Test Author"), 2023, "Fiction", true)
  val sampleUser = User.Student(sampleUserID, "John Doe", "Computer Science", "password123")
  val now = LocalDateTime.now()

  val sampleLoan = Transaction.Loan(sampleBook, sampleUser, now, Some(now.plusDays(30)))
  val sampleReturn = Transaction.Return(sampleBook, sampleUser, now)
  val sampleReservation = Transaction.Reservation(
    sampleBook, sampleUser, now, now.plusDays(1), now.plusDays(7)
  )

  // Property-based test generators
  implicit val arbTransaction: Arbitrary[Transaction] = Arbitrary {
    Gen.oneOf(
      Gen.const(sampleLoan),
      Gen.const(sampleReturn),
      Gen.const(sampleReservation)
    )
  }

  "Transaction.Loan" should "create loan transactions correctly" in {
    val loan = Transaction.Loan(sampleBook, sampleUser, now, Some(now.plusDays(30)))
    
    loan.book shouldBe sampleBook
    loan.user shouldBe sampleUser
    loan.timestamp shouldBe now
    loan match {
      case Transaction.Loan(_, _, _, dueDate) => dueDate shouldBe Some(now.plusDays(30))
      case _ => fail("Expected Loan transaction")
    }
  }

  it should "support loans without due dates" in {
    val unlimitedLoan = Transaction.Loan(sampleBook, sampleUser, now, None)
    
    unlimitedLoan match {
      case Transaction.Loan(_, _, _, dueDate) => dueDate shouldBe None
      case _ => fail("Expected Loan transaction")
    }
  }

  it should "identify as loan transaction" in {
    sampleLoan.isLoan shouldBe true
    sampleLoan.isReturn shouldBe false
    sampleLoan.isReservation shouldBe false
  }

  "Transaction.Return" should "create return transactions correctly" in {
    val returnTx = Transaction.Return(sampleBook, sampleUser, now)
    
    returnTx.book shouldBe sampleBook
    returnTx.user shouldBe sampleUser
    returnTx.timestamp shouldBe now
  }

  it should "identify as return transaction" in {
    sampleReturn.isReturn shouldBe true
    sampleReturn.isLoan shouldBe false
    sampleReturn.isReservation shouldBe false
  }

  "Transaction.Reservation" should "create reservation transactions correctly" in {
    val startDate = now.plusDays(1)
    val endDate = now.plusDays(7)
    val reservation = Transaction.Reservation(sampleBook, sampleUser, now, startDate, endDate)
    
    reservation.book shouldBe sampleBook
    reservation.user shouldBe sampleUser
    reservation.timestamp shouldBe now
    reservation match {
      case Transaction.Reservation(_, _, _, start, end) =>
        start shouldBe startDate
        end shouldBe endDate
      case _ => fail("Expected Reservation")
    }
  }

  it should "identify as reservation transaction" in {
    sampleReservation.isReservation shouldBe true
    sampleReservation.isLoan shouldBe false
    sampleReservation.isReturn shouldBe false
  }

  "Transaction extension methods" should "extract book correctly" in {
    sampleLoan.book shouldBe sampleBook
    sampleReturn.book shouldBe sampleBook
    sampleReservation.book shouldBe sampleBook
  }

  it should "extract user correctly" in {
    sampleLoan.user shouldBe sampleUser
    sampleReturn.user shouldBe sampleUser
    sampleReservation.user shouldBe sampleUser
  }

  it should "extract timestamp correctly" in {
    sampleLoan.timestamp shouldBe now
    sampleReturn.timestamp shouldBe now
    sampleReservation.timestamp shouldBe now
  }

  "Transaction helper methods" should "create loans with automatic due dates for students" in {
    val loan = Transaction.createLoan(sampleBook, sampleUser)
    
    loan shouldBe a[Transaction.Loan]
    loan match {
      case Transaction.Loan(book, user, _, dueDate) =>
        book shouldBe sampleBook
        user shouldBe sampleUser
        dueDate shouldBe defined // Students should have due dates
      case _ => fail("Expected Loan")
    }
  }

  it should "create loans without due dates for faculty and librarians" in {
    val faculty = User.Faculty(UserID.random(), "Dr. Smith", "Computer Science", "password")
    val librarian = User.Librarian(UserID.random(), "Jane Doe", "EMP001", "password")
    
    val facultyLoan = Transaction.createLoan(sampleBook, faculty)
    val librarianLoan = Transaction.createLoan(sampleBook, librarian)
    
    facultyLoan match {
      case Transaction.Loan(_, _, _, dueDate) => dueDate shouldBe None
      case _ => fail("Expected Loan")
    }
    
    librarianLoan match {
      case Transaction.Loan(_, _, _, dueDate) => dueDate shouldBe None
      case _ => fail("Expected Loan")
    }
  }

  it should "create returns with current timestamp" in {
    val returnTx = Transaction.createReturn(sampleBook, sampleUser)
    
    returnTx shouldBe a[Transaction.Return]
    returnTx match {
      case Transaction.Return(book, user, timestamp) =>
        book shouldBe sampleBook
        user shouldBe sampleUser
        // Timestamp should be close to now (within 1 second)
        timestamp.isAfter(LocalDateTime.now().minusSeconds(1)) shouldBe true
        timestamp.isBefore(LocalDateTime.now().plusSeconds(1)) shouldBe true
      case _ => fail("Expected Return")
    }
  }

  it should "create reservations with specified dates" in {
    val startDate = now.plusDays(2)
    val endDate = now.plusDays(9)
    val reservation = Transaction.createReservation(sampleBook, sampleUser, startDate, endDate)
    
    reservation shouldBe a[Transaction.Reservation]
    reservation match {
      case Transaction.Reservation(book, user, _, start, end) =>
        book shouldBe sampleBook
        user shouldBe sampleUser
        start shouldBe startDate
        end shouldBe endDate
      case _ => fail("Expected Reservation")
    }
  }

  "Transaction pattern matching" should "work correctly with different types" in {
    val transactions = List(sampleLoan, sampleReturn, sampleReservation)
    
    val loanCount = transactions.count {
      case _: Transaction.Loan => true
      case _ => false
    }
    
    val returnCount = transactions.count {
      case _: Transaction.Return => true
      case _ => false
    }
    
    val reservationCount = transactions.count {
      case _: Transaction.Reservation => true
      case _ => false
    }
    
    loanCount shouldBe 1
    returnCount shouldBe 1
    reservationCount shouldBe 1
  }

  it should "extract specific fields through pattern matching" in {
    sampleLoan match {
      case Transaction.Loan(book, user, timestamp, dueDate) =>
        book shouldBe sampleBook
        user shouldBe sampleUser
        timestamp shouldBe now
        dueDate shouldBe Some(now.plusDays(30))
      case _ => fail("Expected Loan")
    }
    
    sampleReservation match {
      case Transaction.Reservation(book, user, timestamp, start, end) =>
        book shouldBe sampleBook
        user shouldBe sampleUser
        timestamp shouldBe now
        start shouldBe now.plusDays(1)
        end shouldBe now.plusDays(7)
      case _ => fail("Expected Reservation")
    }
  }

  // Property-based tests
  "Transaction property-based tests" should "maintain type consistency" in {
    forAll { (transaction: Transaction) =>
      transaction match {
        case loan: Transaction.Loan =>
          loan.isLoan shouldBe true
          loan.isReturn shouldBe false
          loan.isReservation shouldBe false
        case returnTx: Transaction.Return =>
          returnTx.isReturn shouldBe true
          returnTx.isLoan shouldBe false
          returnTx.isReservation shouldBe false
        case reservation: Transaction.Reservation =>
          reservation.isReservation shouldBe true
          reservation.isLoan shouldBe false
          reservation.isReturn shouldBe false
      }
    }
  }

  it should "always have valid book and user references" in {
    forAll { (transaction: Transaction) =>
      transaction.book should not be null
      transaction.user should not be null
      transaction.timestamp should not be null
    }
  }

  "Transaction collections" should "support filtering by type" in {
    val transactions = List(
      Transaction.createLoan(sampleBook, sampleUser),
      Transaction.createReturn(sampleBook, sampleUser),
      Transaction.createReservation(sampleBook, sampleUser, now.plusDays(1), now.plusDays(7)),
      Transaction.createLoan(sampleBook, sampleUser),
      Transaction.createReturn(sampleBook, sampleUser)
    )
    
    val loans = transactions.filter(_.isLoan)
    val returns = transactions.filter(_.isReturn) 
    val reservations = transactions.filter(_.isReservation)
    
    loans should have size 2
    returns should have size 2
    reservations should have size 1
  }

  it should "support grouping by transaction type" in {
    val transactions = List(sampleLoan, sampleReturn, sampleReservation, sampleLoan, sampleReturn)
    
    val grouped = transactions.groupBy {
      case _: Transaction.Loan => "loans"
      case _: Transaction.Return => "returns"
      case _: Transaction.Reservation => "reservations"
    }
    
    grouped("loans") should have size 2
    grouped("returns") should have size 2
    grouped("reservations") should have size 1
  }

  it should "support sorting by timestamp" in {
    val time1 = now.minusDays(2)
    val time2 = now.minusDays(1)
    val time3 = now
    
    val transactions = List(
      Transaction.Loan(sampleBook, sampleUser, time3, None),
      Transaction.Return(sampleBook, sampleUser, time1),
      Transaction.Reservation(sampleBook, sampleUser, time2, time2.plusDays(1), time2.plusDays(7))
    )
    
    val sorted = transactions.sortBy(_.timestamp)
    sorted.map(_.timestamp) shouldBe List(time1, time2, time3)
  }

  "Transaction business logic" should "handle due date calculations correctly" in {
    val student = User.Student(UserID.random(), "Student", "CS", "pass")
    val faculty = User.Faculty(UserID.random(), "Faculty", "CS", "pass")
    val librarian = User.Librarian(UserID.random(), "Librarian", "EMP001", "pass")
    
    val studentLoan = Transaction.createLoan(sampleBook, student)
    val facultyLoan = Transaction.createLoan(sampleBook, faculty)
    val librarianLoan = Transaction.createLoan(sampleBook, librarian)
    
    studentLoan match {
      case Transaction.Loan(_, _, _, Some(_)) => succeed
      case _ => fail("Student loans should have due dates")
    }
    
    facultyLoan match {
      case Transaction.Loan(_, _, _, None) => succeed
      case _ => fail("Faculty loans should not have due dates")
    }
    
    librarianLoan match {
      case Transaction.Loan(_, _, _, None) => succeed
      case _ => fail("Librarian loans should not have due dates")
    }
  }

  it should "handle reservation periods correctly" in {
    val startDate = now.plusDays(1)
    val endDate = now.plusDays(7)
    val reservation = Transaction.createReservation(sampleBook, sampleUser, startDate, endDate)
    
    reservation match {
      case Transaction.Reservation(_, _, _, start, end) =>
        start.isBefore(end) shouldBe true
        java.time.Duration.between(start, end).toDays shouldBe 6
      case _ => fail("Expected Reservation")
    }
  }
}
