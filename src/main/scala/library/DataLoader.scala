package library

import java.util.UUID
import Types.{ISBN, UserID}
import User.*

object DataLoader {

  def loadSampleCatalog(): LibraryCatalog = {
    val books = List(
      Book(ISBN("9780134685991"), "Effective Java", List("Joshua Bloch"), 2018, "Programming", isAvailable = true),
      Book(ISBN("9781491950357"), "Designing Data-Intensive Applications", List("Martin Kleppmann"), 2017, "Data", isAvailable = true),
      Book(ISBN("9780132350884"), "Clean Code", List("Robert C. Martin"), 2008, "Programming", isAvailable = true),
      Book(ISBN("9780321751041"), "Introduction to Algorithms", List("Cormen", "Leiserson", "Rivest", "Stein"), 2009, "Algorithms", isAvailable = true)
    )

    val student1   = Student(UserID(UUID.randomUUID()), "Alice", "Computer Science", "student123")
    val faculty1   = Faculty(UserID(UUID.randomUUID()), "Dr. Smith", "Engineering", "faculty123")
    val librarian1 = Librarian(UserID(UUID.randomUUID()), "Bob", "EMP001", "admin123")

    LibraryCatalog(
      books = books.map(b => b.isbn -> b).toMap,
      users = List(student1, faculty1, librarian1).map(u => u.id -> u).toMap,
      transactions = List()
    )
  }
}
