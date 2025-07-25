package library

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import java.util.UUID
import java.time.LocalDateTime
import Types.{ISBN, UserID}
import User.*
import Transaction.*

/**
 * Property-based tests for LibraryCatalog using ScalaCheck.
 * 
 * These tests verify that the core operations of LibraryCatalog maintain
 * important invariants and properties across many randomly generated inputs.
 */
class LibraryCatalogPropertySpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  // Generators for test data
  implicit val arbISBN: Arbitrary[ISBN] = Arbitrary {
    Gen.alphaNumStr.suchThat(_.nonEmpty).map(ISBN(_))
  }

  implicit val arbUserID: Arbitrary[UserID] = Arbitrary {
    Gen.const(UserID(UUID.randomUUID()))
  }

  implicit val arbBook: Arbitrary[Book] = Arbitrary {
    for {
      isbn <- Arbitrary.arbitrary[ISBN]
      title <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      author <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      year <- Gen.choose(1000, 2024)
      genre <- Gen.oneOf("Fiction", "Non-fiction", "Science", "Biography", "Programming")
      available <- Gen.oneOf(true, false)
    } yield Book(isbn, title, List(author), year, genre, available)
  }

  implicit val arbUser: Arbitrary[User] = Arbitrary {
    Gen.oneOf(
      for {
        id <- Arbitrary.arbitrary[UserID]
        name <- Gen.alphaNumStr.suchThat(_.nonEmpty)
        major <- Gen.alphaNumStr.suchThat(_.nonEmpty)
        password <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      } yield Student(id, name, major, password),
      
      for {
        id <- Arbitrary.arbitrary[UserID]
        name <- Gen.alphaNumStr.suchThat(_.nonEmpty)
        department <- Gen.alphaNumStr.suchThat(_.nonEmpty)
        password <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      } yield Faculty(id, name, department, password),
      
      for {
        id <- Arbitrary.arbitrary[UserID]
        name <- Gen.alphaNumStr.suchThat(_.nonEmpty)
        employeeId <- Gen.alphaNumStr.suchThat(_.nonEmpty)
        password <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      } yield Librarian(id, name, employeeId, password)
    )
  }

  implicit val arbLibraryCatalog: Arbitrary[LibraryCatalog] = Arbitrary {
    for {
      books <- Gen.listOf(Arbitrary.arbitrary[Book]).map(_.map(b => b.isbn -> b).toMap)
      users <- Gen.listOf(Arbitrary.arbitrary[User]).map(_.map(u => u.id -> u).toMap)
    } yield LibraryCatalog(books, users, List.empty)
  }

  "LibraryCatalog.addBook" should "always contain the added book" in {
    forAll { (catalog: LibraryCatalog, book: Book) =>
      val updatedCatalog = catalog.addBook(book)
      updatedCatalog.books should contain key book.isbn
      updatedCatalog.books(book.isbn) shouldEqual book
    }
  }

  it should "preserve all existing books when adding a new book" in {
    forAll { (catalog: LibraryCatalog, book: Book) =>
      whenever(!catalog.books.contains(book.isbn)) {
        val updatedCatalog = catalog.addBook(book)
        // All original books should still be present
        catalog.books.keys.foreach { isbn =>
          updatedCatalog.books should contain key isbn
          updatedCatalog.books(isbn) shouldEqual catalog.books(isbn)
        }
        // Plus the new book
        updatedCatalog.books should have size (catalog.books.size + 1)
      }
    }
  }

  it should "replace existing book when ISBN already exists" in {
    forAll { (catalog: LibraryCatalog, book1: Book, book2: Book) =>
      val book2WithSameISBN = book2.copy(isbn = book1.isbn)
      val catalogWithFirstBook = catalog.addBook(book1)
      val catalogWithReplacedBook = catalogWithFirstBook.addBook(book2WithSameISBN)
      
      catalogWithReplacedBook.books(book1.isbn) shouldEqual book2WithSameISBN
      catalogWithReplacedBook.books should have size catalogWithFirstBook.books.size
    }
  }

  "LibraryCatalog.addUser" should "always contain the added user" in {
    forAll { (catalog: LibraryCatalog, user: User) =>
      val updatedCatalog = catalog.addUser(user)
      updatedCatalog.users should contain key user.id
      updatedCatalog.users(user.id) shouldEqual user
    }
  }

  it should "preserve all existing users when adding a new user" in {
    forAll { (catalog: LibraryCatalog, user: User) =>
      val uniqueUserId = UserID(java.util.UUID.randomUUID()) // Generate a guaranteed unique ID
      val uniqueUser = user match {
        case Student(_, name, major, password) => Student(uniqueUserId, name, major, password)
        case Faculty(_, name, department, password) => Faculty(uniqueUserId, name, department, password)
        case Librarian(_, name, employeeId, password) => Librarian(uniqueUserId, name, employeeId, password)
      }
      
      val updatedCatalog = catalog.addUser(uniqueUser)
      // All original users should still be present
      catalog.users.keys.foreach { userId =>
        updatedCatalog.users should contain key userId
        updatedCatalog.users(userId) shouldEqual catalog.users(userId)
      }
      // Plus the new user
      updatedCatalog.users should have size (catalog.users.size + 1)
    }
  }

  it should "maintain catalog immutability" in {
    forAll { (catalog: LibraryCatalog, user: User) =>
      val originalUserCount = catalog.users.size
      val updatedCatalog = catalog.addUser(user)
      
      // Original catalog should be unchanged
      catalog.users should have size originalUserCount
      // New catalog should have the user
      updatedCatalog.users should contain key user.id
    }
  }

  "LibraryCatalog.activeLoansFor" should "never be negative" in {
    forAll { (catalog: LibraryCatalog, userId: UserID) =>
      catalog.activeLoansFor(userId) should be >= 0
    }
  }

  it should "be consistent with transaction history" in {
    forAll { (catalog: LibraryCatalog, user: User) =>
      val catalogWithUser = catalog.addUser(user)
      val activeLoans = catalogWithUser.activeLoansFor(user.id)
      
      // Count actual active loans in transactions
      val loanTransactions = catalogWithUser.transactions.collect {
        case loan @ Loan(_, u, _, _) if u.id == user.id => loan
      }.count { loan =>
        !catalogWithUser.transactions.exists {
          case Return(book, u, timestamp) => 
            book.isbn == loan.book.isbn && u.id == user.id && timestamp.isAfter(loan.timestamp)
          case _ => false
        }
      }
      
      activeLoans shouldEqual loanTransactions
    }
  }

  "LibraryCatalog operations" should "preserve data integrity" in {
    forAll { (books: List[Book], users: List[User]) =>
      whenever(books.nonEmpty && users.nonEmpty) {
        val catalog = LibraryCatalog.empty
        
        // Add all books and users
        val catalogWithBooks = books.foldLeft(catalog)((c, book) => c.addBook(book))
        val fullCatalog = users.foldLeft(catalogWithBooks)((c, user) => c.addUser(user))
        
        // Verify all books are present
        books.foreach { book =>
          fullCatalog.books should contain key book.isbn
        }
        
        // Verify all users are present  
        users.foreach { user =>
          fullCatalog.users should contain key user.id
        }
        
        // Verify no data corruption
        fullCatalog.books.size should be <= (books.size + catalog.books.size)
        fullCatalog.users.size should be <= (users.size + catalog.users.size)
      }
    }
  }

  "LibraryCatalog.loanBook" should "maintain book availability invariants" in {
    forAll { (catalog: LibraryCatalog, book: Book, user: User) =>
      val availableBook = book.copy(isAvailable = true)
      val catalogWithData = catalog.addBook(availableBook).addUser(user)
      
      val result = catalogWithData.loanBook(availableBook.isbn, user.id)
      
      // If loan was successful, book should be unavailable
      if (result != catalogWithData) {
        result.books.get(availableBook.isbn) match {
          case Some(updatedBook) => updatedBook.isAvailable shouldBe false
          case None => fail("Book should still exist in catalog")
        }
      }
    }
  }

  it should "never exceed user loan limits" in {
    forAll { (user: User) =>
      val catalog = LibraryCatalog.empty.addUser(user)
      val books = (1 to user.maxLoans + 2).map { i =>
        Book(ISBN(s"test-isbn-$i"), s"Book $i", List("Author"), 2023, "Test", true)
      }.toList
      
      val catalogWithBooks = books.foldLeft(catalog)((c, book) => c.addBook(book))
      
      // Try to loan more books than the limit
      val finalCatalog = books.foldLeft(catalogWithBooks) { (c, book) =>
        c.loanBook(book.isbn, user.id)
      }
      
      // Should never exceed the user's loan limit
      finalCatalog.activeLoansFor(user.id) should be <= user.maxLoans
    }
  }
}
