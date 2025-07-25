package library

import java.util.UUID

/**
 * Entry point of the Librar                |10. Show Transactions ${if session.hasPermission("view_transactions") then "" else "(Librarians only)"}
                |11. Show User Statistics
                |12. My Active Loans
                |13. Show Recommendations
                |14. Show Trending Books
                |15. Show Library Statistics ${if session.hasPermission("view_statistics") then "" else "(Librarians only)"}
                |16. Remove User ${if session.hasPermission("remove_user") then "" else "(Librarians only)"}
                |17. Remove Book ${if session.hasPermission("remove_book") then "" else "(Librarians only)"}
                |18. Logout
                |0. Exit""".stripMargin)ent System.
 * Demonstrates basic usage by adding books and a student, and performing transactions.
 */
// @main def runLibraryApp(): Unit = {

//   val book1 = Book("12345", "Scala 3 Guide", List("Martin Odersky"), 2021, "Programming", true)
//   val book2 = Book("67890", "Advanced Scala", List("Adam Mounir", "Marco Serhal", "Paul Zamanian"), 2020, "Programming", true)
    
//   val student = Student(UUID.randomUUID(), "Alice", "Computer Science")
  
//   val catalog = new LibraryCatalog(Map(), Map(), List())
//     .addBook(book1)
//     .addBook(book2)
//     .addUser(student)
//     .loanBook("12345", student.id)
//     .loanBook("12345", student.id) // should fail silently

//   println(catalog)
// }


/**
 * entry point for the library management system
 * with authentication and role-based access control
 */
@main def runLibraryApp(): Unit = {
  
  def promptWithFlush(prompt: String): String = {
    System.out.print(prompt)
    System.out.flush()
    
    // Simple approach without buffer clearing
    val input = scala.io.StdIn.readLine()
    if input == null then "" else input.trim
  }
  
  val dataPath = "data"
  var catalog = JsonIO.loadCatalog(dataPath)
  
  // if no users exist, create some default ones
  if catalog.users.isEmpty then
    println("No users found. Creating default users...")
    catalog = createDefaultUsers(catalog)
    JsonIO.saveCatalog(catalog, dataPath)
    println("Default users created:")
    println("- Student: name='Alice', password='student123'")
    println("- Faculty: name='Dr. Smith', password='faculty123'") 
    println("- Librarian: name='Admin', password='admin123'")
    println("")

  var running = true

  // main authentication loop
  while (running) {
    CLI.login(catalog) match {
      case Some(session) =>
        // user successfully logged in, show main menu
        var loggedIn = true
        var showMenu = true // control when to show menu
        while (loggedIn && running) {
          if showMenu then {
            println(
              s"""|
                  |=== Library System - Welcome ${session.userName} (${session.userType}) ===
                  |1. Add Book ${if session.hasPermission("add_book") then "" else "(Librarians only)"}
                  |2. Add User ${if session.hasPermission("add_user") then "" else "(Librarians only)"}
                  |3. Loan Book
                  |4. Return Book
                  |5. Reserve Book ${if session.hasPermission("reserve") then "" else "(Not available for your role)"}
                  |6. Search Books
                  |7. List All Books
                  |8. List All Users ${if session.hasPermission("list_users") then "" else "(Librarians only)"}
                  |9. Show Transactions ${if session.hasPermission("view_transactions") then "" else "(Librarians only)"}
                  |10. Show User Statistics
                  |11. My Active Loans
                  |12. Show Recommendations
                  |13. Show Trending Books
                  |14. Show Library Statistics ${if session.hasPermission("view_statistics") then "" else "(Librarians only)"}
                  |15. Remove User ${if session.hasPermission("remove_user") then "" else "(Librarians only)"}
                  |16. Remove Book ${if session.hasPermission("remove_book") then "" else "(Librarians only)"}
                  |17. Logout
                  |0. Exit""".stripMargin)
          }
          showMenu = true // reset for next iteration

          val choice = promptWithFlush("Choose an option: ")
          
          if choice.nonEmpty then {
            choice match {
              case "1" => catalog = CLI.addBook(catalog, session)
            case "2" => catalog = CLI.addUser(catalog, session)
            case "3" => catalog = CLI.loanBook(catalog, session)
            case "4" => catalog = CLI.returnBook(catalog, session)
            case "5" => catalog = CLI.reserveBook(catalog, session)
            case "6" => CLI.showEnhancedSearch(catalog)
            case "7" => CLI.listBooks(catalog)
            case "8" => CLI.listUsers(catalog, session)
            case "9" => CLI.showTransactions(catalog, session)
            case "10" => catalog = CLI.showUserStats(catalog, session)
            case "11" => CLI.showMyLoans(catalog, session)
            case "12" => CLI.showRecommendations(catalog, session)
            case "13" => CLI.showTrendingBooks(catalog)
            case "14" => CLI.showLibraryStatistics(catalog, session)
            case "15" => catalog = CLI.removeUser(catalog, session)
            case "16" => catalog = CLI.removeBook(catalog, session)
            case "17" =>
              CLI.logout(session)
              loggedIn = false
            case "0" =>
              println("Saving data...")
              JsonIO.saveCatalog(catalog, dataPath)
              println("Goodbye!")
              running = false
            case _ =>
              println(s"Invalid option: '$choice'. Please try again.")
              showMenu = false // pas besoin de réafficher le menu, juste redemander
            }
          } else {
            // empty input, just reprompt without showing menu or error
            showMenu = false
          }
        }

      case None =>
        // login failed, ask if they want to try again or exit
        Thread.sleep(500) // Give more time for I/O to settle
        
        // Use a more robust approach for Windows terminal
        var validResponse = false
        var answer = ""
        var attempts = 0
        
        while !validResponse && attempts < 3 do
          attempts += 1
          println() // Add some spacing
          print("Would you like to try again? (y/n): ")
          System.out.flush()
          Thread.sleep(100) // Small delay to ensure prompt is visible
          
          try {
            val response = scala.io.StdIn.readLine()
            if response != null && response.trim.nonEmpty then
              answer = response.trim.toLowerCase
              validResponse = true
            else
              if attempts < 3 then
                println("Please enter 'y' for yes or 'n' for no.")
              else
                println("No valid response received after 3 attempts, assuming 'no'")
                answer = "n"
                validResponse = true
          } catch {
            case _: Exception =>
              if attempts < 3 then
                println("Input error, please try again.")
              else
                println("Input error after 3 attempts, assuming 'no'")
                answer = "n"
                validResponse = true
          }
        
        if answer == "y" || answer == "yes" then
          println("Retrying login...")
          Thread.sleep(200) // Brief pause before retry
        else
          println("Goodbye!")
          running = false
    }
  }
}

def createDefaultUsers(catalog: LibraryCatalog): LibraryCatalog = {
  import java.util.UUID
  import Types.UserID
  import User.*
  
  val student = Student(UserID(UUID.randomUUID()), "Alice", "Computer Science", "student123")
  val faculty = Faculty(UserID(UUID.randomUUID()), "Dr. Smith", "Engineering", "faculty123")
  val librarian = Librarian(UserID(UUID.randomUUID()), "Admin", "LIB001", "admin123")
  
  catalog
    .addUser(student)
    .addUser(faculty)
    .addUser(librarian)
}
