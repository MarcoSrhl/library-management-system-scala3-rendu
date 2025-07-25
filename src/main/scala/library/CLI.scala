package library

import java.util.UUID
import scala.util.Try
import Types.{ISBN, UserID}
import User.*
import Transaction.*
import library.SearchOperations.and
import TypeClasses.{given, *}
import Results.{LibraryResult, LibraryError}

/**
 * Enhanced CLI with Scala 3 advanced features integration
 * Uses type classes for polymorphic display and validation
 */
object CLI {
  
  private def promptWithFlush(prompt: String): String = {
    System.out.print(prompt)
    System.out.flush()
    
    // Simple approach without buffer clearing
    val input = scala.io.StdIn.readLine()
    if input == null then "" else input.trim
  }

  def login(catalog: LibraryCatalog): Option[AuthSession] = {
    println("=== Library System Login ===")
    
    val console = System.console()
    val identifier = if console != null then {
      console.readLine("Enter your user ID (UUID) or name: ")
    } else {
      promptWithFlush("Enter your user ID (UUID) or name: ")
    }
    
    val password = if console != null then {
      new String(console.readPassword("Enter password: "))
    } else {
      promptWithFlush("Enter password: ")
    }
    
    AuthSession.login(identifier.trim, password.trim, catalog) match {
      case Some(session) =>
        println(s"Welcome, ${session.userName}! (${session.userType})")
        Some(session)
      case None =>
        println("Login failed: Invalid credentials.")
        None
    }
  }

  def addBook(catalog: LibraryCatalog, session: AuthSession): LibraryCatalog = {
    if !session.hasPermission("add_book") then
      println("Access denied: Only librarians can add books.")
      return catalog

    val isbnStr = promptWithFlush("ISBN: ")
    val title = promptWithFlush("Title: ")
    val authorsStr = promptWithFlush("Authors (comma separated): ")
    val authors = authorsStr.split(",").map(_.trim).toList
    val yearStr = promptWithFlush("Year: ")
    val year = yearStr.toIntOption.getOrElse(0)
    val genre = promptWithFlush("Genre: ")

    val book = Book(ISBN(isbnStr), title, authors, year, genre, isAvailable = true)
    println(s"Book '${book.title}' added by ${session.userName}")
    catalog.addBook(book)
  }

  def addUser(catalog: LibraryCatalog, session: AuthSession): LibraryCatalog = {
    if !session.hasPermission("add_user") then
      println("Access denied: Only librarians can add users.")
      return catalog

    val name = promptWithFlush("Name: ")
    val password = promptWithFlush("Password: ")

    val userType = promptWithFlush("User Type (1=Student, 2=Faculty, 3=Librarian): ")
    userType match {
      case "1" =>
        val major = promptWithFlush("Major: ")
        val student = User.Student(UserID(UUID.randomUUID()), name, major, password)
        println(s"Student ${name} added by ${session.userName}")
        catalog.addUser(student)

      case "2" =>
        val dept = promptWithFlush("Department: ")
        val faculty = User.Faculty(UserID(UUID.randomUUID()), name, dept, password)
        println(s"Faculty ${name} added by ${session.userName}")
        catalog.addUser(faculty)

      case "3" =>
        val empId = promptWithFlush("Employee ID: ")
        val librarian = User.Librarian(UserID(UUID.randomUUID()), name, empId, password)
        println(s"Librarian ${name} added by ${session.userName}")
        catalog.addUser(librarian)

      case _ =>
        println("Invalid type.")
        catalog
    }
  }

  def loanBook(catalog: LibraryCatalog, session: AuthSession): LibraryCatalog = {
    if !session.hasPermission("loan") then
      println("Access denied: You don't have permission to loan books.")
      return catalog

    selectBookInteractively(catalog, "loan") match {
      case Some(isbn) =>
        // If not a librarian, they can only loan to themselves
        val targetUserId = if session.user.isLibrarian then {
          println("User ID (leave empty to loan to yourself): ")
          val input = scala.io.StdIn.readLine().trim
          if input.isEmpty then
            session.userId
          else
            Try(UUID.fromString(input)).toOption match {
              case Some(uuid) => UserID(uuid)
              case None =>
                println("Invalid UUID, loaning to yourself.")
                session.userId
            }
        } else {
          session.userId
        }

        catalog.loanBook(isbn, targetUserId)
      case None =>
        println("Loan cancelled.")
        catalog
    }
  }

  def returnBook(catalog: LibraryCatalog, session: AuthSession): LibraryCatalog = {
    if !session.hasPermission("return") then
      println("Access denied: You don't have permission to return books.")
      return catalog

    selectBorrowedBookForReturn(catalog, session) match {
      case Some(isbn) =>
        // If not a librarian, they can only return their own books
        val targetUserId = if session.user.isLibrarian then {
          println("User ID (leave empty for yourself): ")
          val input = scala.io.StdIn.readLine().trim
          if input.isEmpty then
            session.userId
          else
            Try(UUID.fromString(input)).toOption match {
              case Some(uuid) => UserID(uuid)
              case None =>
                println("Invalid UUID, returning for yourself.")
                session.userId
            }
        } else {
          session.userId
        }

        catalog.returnBook(isbn, targetUserId)
      case None =>
        println("Return cancelled.")
        catalog
    }
  }

  def reserveBook(catalog: LibraryCatalog, session: AuthSession): LibraryCatalog = {
    if !session.hasPermission("reserve") then
      println("Access denied: You don't have permission to make reservations.")
      return catalog

    selectBookInteractively(catalog, "reserve") match {
      case Some(isbn) =>
        catalog.reserveBook(isbn, session.userId)
      case None =>
        println("Reservation cancelled.")
        catalog
    }
  }

  def searchBooks(catalog: LibraryCatalog): Unit = {
    println("Search by title: ")
    val query = scala.io.StdIn.readLine().trim.toLowerCase
    val results = catalog.books.values.filter(_.title.toLowerCase.contains(query))
    results.foreach(println)
  }

  def showEnhancedSearch(catalog: LibraryCatalog): Unit = {
    println("\n=== Enhanced Book Search ===")
    println("1. Quick search (title, author, genre - with typo tolerance)")
    println("2. Search by genre")
    println("3. Search by author")
    println("4. Search by publication year range")
    println("5. Search available books only")
    println("6. Combined advanced search")
    println("0. Cancel")
    
    print("Choose search type (0-6): ")
    Console.flush()
    val choice = scala.io.StdIn.readLine().trim
    
    choice match {
      case "1" => enhancedQuickSearch(catalog)
      case "2" => filterByGenreDisplay(catalog)
      case "3" => filterByAuthorDisplay(catalog)
      case "4" => searchByYearRange(catalog)
      case "5" => displaySearchResults(catalog.books.values.filter(_.isAvailable).toList, "Available Books")
      case "6" => combinedAdvancedSearch(catalog)
      case "0" => 
        println("Search cancelled.")
      case _ =>
        println("Invalid choice.")
    }
  }

  private def enhancedQuickSearch(catalog: LibraryCatalog): Unit = {
    print("Enter search term (title, author, or genre): ")
    Console.flush()
    val searchTerm = scala.io.StdIn.readLine().trim.toLowerCase
    
    if searchTerm.isEmpty then
      println("Search term cannot be empty.")
      return
    
    // Combine exact matches and fuzzy matches
    val exactMatches = catalog.books.values.filter { book =>
      book.title.toLowerCase.contains(searchTerm) ||
      book.authors.exists(_.toLowerCase.contains(searchTerm)) ||
      book.genre.toLowerCase.contains(searchTerm)
    }.toList
    
    // Add fuzzy matches (but avoid duplicates)
    val fuzzyMatches = SearchOperations.fuzzySearchWithThreshold(catalog.books.values.toList, searchTerm, 2)
      .filterNot(book => exactMatches.contains(book))
    
    val allResults = exactMatches ++ fuzzyMatches
    
    if allResults.isEmpty then
      println(s"No books found for '$searchTerm'.")
      println("Try a different search term or check your spelling.")
    else
      if exactMatches.nonEmpty && fuzzyMatches.nonEmpty then
        println(s"\n=== Exact matches for '$searchTerm' ===")
        displaySearchResultsSimple(exactMatches)
        println(s"\n=== Similar matches (fuzzy search) ===")
        displaySearchResultsSimple(fuzzyMatches)
      else if exactMatches.nonEmpty then
        displaySearchResults(exactMatches, s"Search results for '$searchTerm'")
      else
        displaySearchResults(fuzzyMatches, s"Similar matches for '$searchTerm' (fuzzy search)")
  }

  private def searchByYearRange(catalog: LibraryCatalog): Unit = {
    print("Enter start year: ")
    val startYear = scala.io.StdIn.readLine().trim.toIntOption.getOrElse(0)
    print("Enter end year: ")
    val endYear = scala.io.StdIn.readLine().trim.toIntOption.getOrElse(9999)
    val results = SearchOperations.searchByYearRange(catalog.books.values.toList, startYear, endYear)
    displaySearchResults(results, s"Books published between $startYear and $endYear")
  }

  private def combinedAdvancedSearch(catalog: LibraryCatalog): Unit = {
    print("Enter genre (or press Enter to skip): ")
    val genre = scala.io.StdIn.readLine().trim
    print("Enter author (or press Enter to skip): ")
    val author = scala.io.StdIn.readLine().trim
    print("Enter minimum year (or press Enter to skip): ")
    val minYear = scala.io.StdIn.readLine().trim.toIntOption
    print("Only available books? (y/n): ")
    val availableOnly = scala.io.StdIn.readLine().trim.toLowerCase == "y"
    
    var predicate = SearchOperations.Predicates.all
    if genre.nonEmpty then predicate = predicate.and(SearchOperations.Predicates.byGenre(genre))
    if author.nonEmpty then predicate = predicate.and(SearchOperations.Predicates.byAuthor(author))
    if minYear.isDefined then predicate = predicate.and(SearchOperations.Predicates.byYear(minYear.get))
    if availableOnly then predicate = predicate.and(SearchOperations.Predicates.isAvailable)
    
    val results = SearchOperations.searchBy(catalog.books.values.toList, predicate)
    displaySearchResults(results, "Combined search results")
  }

  private def displaySearchResultsSimple(books: List[Book]): Unit = {
    books.zipWithIndex.foreach { case (book, index) =>
      println(s"${index + 1}. ${book.title}")
      println(s"   Authors: ${book.authors.mkString(", ")}")
      println(s"   Genre: ${book.genre} (${book.publicationYear})")
      println(s"   Available: ${if book.isAvailable then "Yes" else "No"}")
      println()
    }
  }

  // New versions for direct display (not book selection)
  private def filterByGenreDisplay(catalog: LibraryCatalog): Unit = {
    val genres = catalog.books.values.map(_.genre).toSet.toList.sorted
    
    println("\nAvailable genres:")
    genres.zipWithIndex.foreach { case (genre, index) =>
      val count = catalog.books.values.count(_.genre == genre)
      println(f"${index + 1}%3d. $genre%-20s ($count books)")
    }
    
    print(s"Select genre (1-${genres.length}) or 0 to cancel: ")
    Console.flush()
    Try(scala.io.StdIn.readInt()) match {
      case scala.util.Success(0) => println("Search cancelled.")
      case scala.util.Success(num) if num > 0 && num <= genres.length =>
        val selectedGenre = genres(num - 1)
        val booksInGenre = catalog.books.values.filter(_.genre == selectedGenre).toList
        displaySearchResults(booksInGenre, s"Books in genre: $selectedGenre")
      case _ =>
        println("Invalid selection.")
    }
  }

  private def filterByAuthorDisplay(catalog: LibraryCatalog): Unit = {
    print("Enter author name to search (partial match allowed): ")
    Console.flush()
    val searchTerm = scala.io.StdIn.readLine().trim.toLowerCase
    
    val matchingBooks = catalog.books.values.filter(
      _.authors.exists(_.toLowerCase.contains(searchTerm))
    ).toList
    
    displaySearchResults(matchingBooks, s"Books by authors matching '$searchTerm'")
  }

  def listBooks(catalog: LibraryCatalog): Unit = {
    println("=== Books in Library ===")
    
    val books = catalog.books.values.toList.sortBy(_.title)
    
    if books.isEmpty then
      println("No books in the library.")
    else
      println(f"${"#"}%3s ${"Title"}%-40s ${"Authors"}%-30s ${"Genre"}%-15s ${"Year"}%6s ${"Status"}%-15s")
      println("=" * 120)
      
      books.zipWithIndex.foreach { case (book, index) =>
        val status = getBookStatus(book, catalog)
        val authorsStr = book.authors.mkString(", ")
        val truncatedAuthors = if authorsStr.length > 28 then authorsStr.take(25) + "..." else authorsStr
        
        println(f"${index + 1}%3d. ${book.title}%-40s ${truncatedAuthors}%-30s ${book.genre}%-15s ${book.publicationYear}%6d $status%-15s")
      }
      
      println(s"\nTotal: ${books.length} books")
      val available = books.count(_.isAvailable)
      println(s"Available: $available, On loan: ${books.length - available}")
  }

  def showTransactions(catalog: LibraryCatalog, session: AuthSession): Unit = {
    if !session.hasPermission("view_transactions") then
      println("Access denied: Only librarians can view transaction history.")
      return

    println("=== Transaction History ===")
    catalog.transactions.foreach {
      case Loan(book, user, timestamp, dueDate) =>
        println(s"[LOAN] ${book.title} -> ${user.name} on $timestamp" +
          dueDate.map(date => s", due: $date").getOrElse(""))

      case Return(book, user, timestamp) =>
        println(s"[RETURN] ${book.title} <- ${user.name} on $timestamp")

      case Reservation(book, user, timestamp, startDate, endDate) =>
        println(s"[RESERVED] ${book.title} by ${user.name} on $timestamp (${startDate.toLocalDate} to ${endDate.toLocalDate})")
    }
  }

  def showUserStats(catalog: LibraryCatalog, session: AuthSession): LibraryCatalog = {
    val targetUserId = if session.user.isLibrarian then {
      println("Enter User ID (leave empty for yourself): ")
      val input = scala.io.StdIn.readLine().trim
      if input.isEmpty then
        session.userId
      else
        Try(UUID.fromString(input)).toOption match {
          case Some(uuid) => UserID(uuid)
          case None =>
            println("Invalid UUID, showing your stats.")
            session.userId
        }
    } else {
      session.userId
    }

    catalog.users.get(targetUserId) match {
      case Some(user) =>
        val active = catalog.activeLoansFor(targetUserId)
        val late = catalog.overdueLoansFor(targetUserId)
        val fees = catalog.calculateOverdueFees(targetUserId)

        println(s"=== Stats for ${user.name} ===")
        println(s"User Type: ${if user.isStudent then "Student" else if user.isFaculty then "Faculty" else "Librarian"}")
        println(s"Max Loans: ${user.maxLoans}")
        println(s"Loan Period: ${user.loanPeriod.toDays} days")
        println(s"Can Reserve: ${user.canReserve}")
        println(s"Active loans: $active")
        println(s"Overdue loans: $late")
        println(f"Outstanding fees: $$${fees}%.2f")

        if session.user.isLibrarian && fees > 0 then {
          println("Waive fees? (y/n): ")
          if scala.io.StdIn.readLine().trim.toLowerCase == "y" then
            return catalog.waiveFees(targetUserId, session.user)
        }

      case None =>
        println("User not found.")
    }
    catalog
  }

  def logout(session: AuthSession): Unit = {
    println(s"Goodbye, ${session.userName}!")
  }

  def listUsers(catalog: LibraryCatalog, session: AuthSession): Unit = {
    if !session.hasPermission("list_users") then
      println("Access denied: Only librarians can view user lists.")
      return

    println("=== All Users ===")
    catalog.users.values.foreach { user =>
      val userType = if user.isStudent then "Student" else if user.isFaculty then "Faculty" else "Librarian"
      val activeLoans = catalog.activeLoansFor(user.id)
      println(s"${user.name} ($userType) - ID: ${user.id.value} - Active loans: $activeLoans")
      
      if user.isStudent then
        user match
          case User.Student(_, _, major, _) => println(s"  Major: $major")
          case _ =>
      else if user.isFaculty then
        user match
          case User.Faculty(_, _, department, _) => println(s"  Department: $department")
          case _ =>
      else if user.isLibrarian then
        user match
          case User.Librarian(_, _, employeeId, _) => println(s"  Employee ID: $employeeId")
          case _ =>
    }
  }

  def removeUser(catalog: LibraryCatalog, session: AuthSession): LibraryCatalog = {
    if !session.hasPermission("remove_user") then
      println("Access denied: Only librarians can remove users.")
      return catalog

    val users = catalog.users.values.toList
    
    if users.isEmpty then
      println("No users found in the system.")
      return catalog
    
    println("\n=== Select User to Remove ===")
    users.zipWithIndex.foreach { case (user, index) =>
      val userType = user match {
        case User.Student(_, _, major, _) => s"Student (Major: $major)"
        case User.Faculty(_, _, department, _) => s"Faculty (Dept: $department)"
        case User.Librarian(_, _, employeeId, _) => s"Librarian (ID: $employeeId)"
      }
      val isSelf = user.id == session.user.id
      val selfNote = if isSelf then " (Cannot remove yourself)" else ""
      println(s"${index + 1}. ${user.name} - $userType$selfNote")
    }
    println("0. Cancel")
    
    val choice = promptWithFlush("Choose a user to remove: ")
    
    choice.toIntOption match {
      case Some(0) =>
        println("Operation cancelled.")
        catalog
      case Some(index) if index > 0 && index <= users.length =>
        val selectedUser = users(index - 1)
        
        // Check if user is trying to remove themselves
        if selectedUser.id == session.user.id then
          println("Error: You cannot remove yourself from the system.")
          return catalog
        
        // Ask for confirmation
        println(s"\nAre you sure you want to remove user '${selectedUser.name}'? (y/n)")
        val confirmation = promptWithFlush("Confirm: ").toLowerCase
        
        if confirmation == "y" || confirmation == "yes" then
          catalog.removeUser(selectedUser.id, session.user)
        else
          println("User removal cancelled.")
          catalog
      case _ =>
        println("Invalid selection. Please choose a valid number.")
        catalog
    }
  }

  def removeBook(catalog: LibraryCatalog, session: AuthSession): LibraryCatalog = {
    if !session.hasPermission("remove_book") then
      println("Access denied: Only librarians can remove books.")
      return catalog

    val books = catalog.books.values.toList
    
    if books.isEmpty then
      println("No books found in the catalog.")
      return catalog
    
    println("\n=== Select Book to Remove ===")
    books.zipWithIndex.foreach { case (book, index) =>
      val authors = book.authors.mkString(", ")
      val status = if book.isAvailable then "Available" else "Loaned"
      println(s"${index + 1}. ${book.title} by $authors (${book.publicationYear}) - $status")
    }
    println("0. Cancel")
    
    val choice = promptWithFlush("Choose a book to remove: ")
    
    choice.toIntOption match {
      case Some(0) =>
        println("Operation cancelled.")
        catalog
      case Some(index) if index > 0 && index <= books.length =>
        val selectedBook = books(index - 1)
        
        // Ask for confirmation
        println(s"\nAre you sure you want to remove book '${selectedBook.title}'? (y/n)")
        val confirmation = promptWithFlush("Confirm: ").toLowerCase
        
        if confirmation == "y" || confirmation == "yes" then
          catalog.removeBook(selectedBook.isbn, session.user)
        else
          println("Book removal cancelled.")
          catalog
      case _ =>
        println("Invalid selection. Please choose a valid number.")
        catalog
    }
  }

  def showRecommendations(catalog: LibraryCatalog, session: AuthSession): Unit = {
    println("\n=== Book Recommendations ===")
    
    val config = RecommendationEngine.RecommendationConfig(
      maxRecommendations = 10,
      includeRead = false,
      diversityWeight = 0.3,
      popularityWeight = 0.2,
      similarityWeight = 0.5
    )
    
    val recommendations = RecommendationEngine.recommend(session.user, catalog, config)
    
    if recommendations.isEmpty then
      println("No recommendations available. Try reading some books first!")
    else
      println(s"Here are ${recommendations.length} book recommendations for you:\n")
      
      recommendations.zipWithIndex.foreach { case (book, index) =>
        println(s"${index + 1}. ${book.title}")
        println(s"   Authors: ${book.authors.mkString(", ")}")
        println(s"   Genre: ${book.genre}")
        println(s"   Year: ${book.publicationYear}")
        println(s"   Available: ${if book.isAvailable then "Yes" else "No"}")
        
        val explanation = RecommendationEngine.explainRecommendation(session.user, book, catalog)
        println(s"   Why: $explanation")
        println()
      }
  }

  def showTrendingBooks(catalog: LibraryCatalog): Unit = {
    println("\n=== Trending Books (Last 30 Days) ===")
    
    val trending = RecommendationEngine.getTrendingBooks(catalog, 30)
    
    if trending.isEmpty then
      println("No trending books found.")
    else
      println(s"Here are the ${trending.length} most popular books recently:\n")
      
      trending.zipWithIndex.foreach { case (book, index) =>
        println(s"${index + 1}. ${book.title}")
        println(s"   Authors: ${book.authors.mkString(", ")}")
        println(s"   Genre: ${book.genre}")
        println(s"   Available: ${if book.isAvailable then "Yes" else "No"}")
        println()
      }
  }

  def showAdvancedSearch(catalog: LibraryCatalog): Unit = {
    println("\n=== Advanced Book Search ===")
    println("1. Search by genre")
    println("2. Search by author")
    println("3. Search by publication year range")
    println("4. Search by availability")
    println("5. Combined search")
    println("6. Fuzzy search")
    
    print("Choose search type (1-6): ")
    val choice = scala.io.StdIn.readLine().trim
    
    choice match {
      case "1" =>
        print("Enter genre: ")
        val genre = scala.io.StdIn.readLine().trim
        val results = SearchOperations.searchByGenre(catalog.books.values.toList, genre)
        displaySearchResults(results, s"Books in genre '$genre'")
        
      case "2" =>
        print("Enter author name: ")
        val author = scala.io.StdIn.readLine().trim
        val results = SearchOperations.searchByAuthor(catalog.books.values.toList, author)
        displaySearchResults(results, s"Books by author '$author'")
        
      case "3" =>
        print("Enter start year: ")
        val startYear = scala.io.StdIn.readLine().trim.toIntOption.getOrElse(0)
        print("Enter end year: ")
        val endYear = scala.io.StdIn.readLine().trim.toIntOption.getOrElse(9999)
        val results = SearchOperations.searchByYearRange(catalog.books.values.toList, startYear, endYear)
        displaySearchResults(results, s"Books published between $startYear and $endYear")
        
      case "4" =>
        val availableBooks = SearchOperations.searchAvailable(catalog.books.values.toList)
        displaySearchResults(availableBooks, "Available books")
        
      case "5" =>
        print("Enter genre (or press Enter to skip): ")
        val genre = scala.io.StdIn.readLine().trim
        print("Enter author (or press Enter to skip): ")
        val author = scala.io.StdIn.readLine().trim
        print("Enter minimum year (or press Enter to skip): ")
        val minYear = scala.io.StdIn.readLine().trim.toIntOption
        print("Only available books? (y/n): ")
        val availableOnly = scala.io.StdIn.readLine().trim.toLowerCase == "y"
        
        var predicate = SearchOperations.Predicates.all
        if genre.nonEmpty then predicate = predicate.and(SearchOperations.Predicates.byGenre(genre))
        if author.nonEmpty then predicate = predicate.and(SearchOperations.Predicates.byAuthor(author))
        if minYear.isDefined then predicate = predicate.and(SearchOperations.Predicates.byYear(minYear.get))
        if availableOnly then predicate = predicate.and(SearchOperations.Predicates.isAvailable)
        
        val results = SearchOperations.searchBy(catalog.books.values.toList, predicate)
        displaySearchResults(results, "Combined search results")
        
      case "6" =>
        print("Enter search term: ")
        val term = scala.io.StdIn.readLine().trim
        print("Enter max distance (1-3, default 2): ")
        val maxDistance = scala.io.StdIn.readLine().trim.toIntOption.getOrElse(2)
        val results = SearchOperations.fuzzySearchWithThreshold(catalog.books.values.toList, term, maxDistance)
        displaySearchResults(results, s"Fuzzy search results for '$term'")
        
      case _ =>
        println("Invalid choice.")
    }
  }

  private def displaySearchResults(books: List[Book], title: String): Unit = {
    println(s"\n=== $title ===")
    if books.isEmpty then
      println("No books found.")
    else
      println(s"Found ${books.length} book(s):\n")
      books.zipWithIndex.foreach { case (book, index) =>
        println(s"${index + 1}. ${book.title}")
        println(s"   Authors: ${book.authors.mkString(", ")}")
        println(s"   Genre: ${book.genre}")
        println(s"   Year: ${book.publicationYear}")
        println(s"   Available: ${if book.isAvailable then "Yes" else "No"}")
        println()
      }
  }

  def showLibraryStatistics(catalog: LibraryCatalog, session: AuthSession): Unit = {
    if !session.hasPermission("view_statistics") then
      println("Access denied: Only librarians can view detailed statistics.")
      return

    println("\n=== Library Statistics ===")
    
    val report = library.DataTransformation.generateLibraryReport(catalog)
    
    println(s"Total Books: ${report.totalBooks}")
    println(s"Available Books: ${report.availableBooks}")
    println(s"Loaned Books: ${report.loanedBooks}")
    println(s"Total Users: ${report.totalUsers}")
    println(s"Active Loans: ${report.activeLoans}")
    println(s"Total Reservations: ${report.totalReservations}")
    println()
    
    println("Books by Genre:")
    report.booksByGenre.foreach { case (genre, count) =>
      println(s"  $genre: $count")
    }
    println()
    
    println("Users by Type:")
    report.usersByType.foreach { case (userType, count) =>
      println(s"  $userType: $count")
    }
    println()
    
    println("Most Popular Books:")
    val popularBooks = library.DataTransformation.getMostPopularBooks(catalog, 5)
    popularBooks.zipWithIndex.foreach { case ((bookTitle, loanCount), index) =>
      println(s"${index + 1}. ${bookTitle} (${loanCount} loans)")
    }
    println()
    
    println("Most Active Users:")
    val activeUsers = library.DataTransformation.getMostActiveUsers(catalog, 5)
    activeUsers.zipWithIndex.foreach { case ((userName, loanCount), index) =>
      println(s"${index + 1}. ${userName} (${loanCount} loans)")
    }
  }

  /**
   * Interactive book selection with improved UX
   */
  def selectBookInteractively(catalog: LibraryCatalog, purpose: String = "select"): Option[ISBN] = {
    println(s"=== Enhanced Book Search for ${purpose} ===")
    println("1. Quick search (title, author, genre - with typo tolerance)")
    println("2. Search by genre")
    println("3. Search by author")
    println("4. Search by publication year range")
    println("5. Show available books only")
    println("6. Combined advanced search")
    println("7. Browse all books")
    println("0. Cancel")
    
    print("Choose search type (0-7): ")
    Console.flush()
    val choice = scala.io.StdIn.readLine().trim
    
    choice match {
      case "1" => enhancedQuickSearchWithSelection(catalog, purpose)
      case "2" => filterByGenreWithSelection(catalog, purpose)
      case "3" => filterByAuthorWithSelection(catalog, purpose)
      case "4" => searchByYearRangeWithSelection(catalog, purpose)
      case "5" => selectFromBookList(catalog.books.values.filter(_.isAvailable).toList, s"Available Books for ${purpose}")
      case "6" => combinedAdvancedSearchWithSelection(catalog, purpose)
      case "7" => selectFromBookList(catalog.books.values.toList, s"All Books for ${purpose}")
      case "0" => 
        println("Selection cancelled.")
        None
      case _ =>
        println("Invalid choice.")
        None
    }
  }

  private def selectFromBookList(books: List[Book], title: String): Option[ISBN] = {
    if books.isEmpty then
      println(s"No books found in category: $title")
      return None
    
    println(s"\n=== $title ===")
    books.zipWithIndex.foreach { case (book, index) =>
      val status = if book.isAvailable then "[Available]" else "[On Loan]"
      println(f"${index + 1}%3d. ${book.title}%-40s by ${book.authors.mkString(", ")}%-30s [$status]")
      println(f"     ISBN: ${book.isbn.value}%-20s Genre: ${book.genre}%-15s Year: ${book.publicationYear}")
      println()
    }
    
    print(s"Enter book number (1-${books.length}) or 0 to cancel: ")
    Console.flush()
    Try(scala.io.StdIn.readInt()) match {
      case scala.util.Success(0) => None
      case scala.util.Success(num) if num > 0 && num <= books.length =>
        Some(books(num - 1).isbn)
      case _ =>
        println("Invalid selection.")
        None
    }
  }

  private def searchByTitle(catalog: LibraryCatalog): Option[ISBN] = {
    print("Enter title to search (partial match allowed): ")
    Console.flush()
    val searchTerm = scala.io.StdIn.readLine().trim.toLowerCase
    
    val matchingBooks = catalog.books.values.filter(_.title.toLowerCase.contains(searchTerm)).toList
    selectFromBookList(matchingBooks, s"Books matching '$searchTerm'")
  }

  private def filterByGenre(catalog: LibraryCatalog): Option[ISBN] = {
    val genres = catalog.books.values.map(_.genre).toSet.toList.sorted
    
    println("\nAvailable genres:")
    genres.zipWithIndex.foreach { case (genre, index) =>
      val count = catalog.books.values.count(_.genre == genre)
      println(f"${index + 1}%3d. $genre%-20s ($count books)")
    }
    
    print(s"Select genre (1-${genres.length}) or 0 to cancel: ")
    Console.flush()
    Try(scala.io.StdIn.readInt()) match {
      case scala.util.Success(0) => None
      case scala.util.Success(num) if num > 0 && num <= genres.length =>
        val selectedGenre = genres(num - 1)
        val booksInGenre = catalog.books.values.filter(_.genre == selectedGenre).toList
        selectFromBookList(booksInGenre, s"Books in genre: $selectedGenre")
      case _ =>
        println("Invalid selection.")
        None
    }
  }

  private def filterByAuthor(catalog: LibraryCatalog): Option[ISBN] = {
    print("Enter author name to search (partial match allowed): ")
    Console.flush()
    val searchTerm = scala.io.StdIn.readLine().trim.toLowerCase
    
    val matchingBooks = catalog.books.values.filter(
      _.authors.exists(_.toLowerCase.contains(searchTerm))
    ).toList
    
    selectFromBookList(matchingBooks, s"Books by authors matching '$searchTerm'")
  }

  /**
   * Enhanced book status display
   */
  def getBookStatus(book: Book, catalog: LibraryCatalog): String = {
    if book.isAvailable then
      "[Available]"
    else
      // Check if it's on loan or reserved
      val currentLoan = catalog.transactions.collectFirst {
        case Transaction.Loan(loanBook, user, _, _) if loanBook.isbn == book.isbn &&
          !catalog.transactions.exists {
            case Transaction.Return(returnBook, returnUser, _) => 
              returnBook.isbn == book.isbn && returnUser.id == user.id
            case _ => false
          } => user.name
      }
      
      currentLoan match {
        case Some(borrower) => s"[On loan to $borrower]"
        case None => "[Reserved]"
      }
  }

  /**
   * Select a book from the user's currently borrowed books for return
   */
  def selectBorrowedBookForReturn(catalog: LibraryCatalog, session: AuthSession): Option[ISBN] = {
    println(s"=== Select Book to Return - ${session.userName} ===")
    
    // Get all active loans for the current user (or target user if librarian)
    val targetUserId = if session.user.isLibrarian then {
      println("Return book for which user? (leave empty for yourself): ")
      val input = scala.io.StdIn.readLine().trim
      if input.isEmpty then
        session.userId
      else
        Try(UUID.fromString(input)).toOption match {
          case Some(uuid) => UserID(uuid)
          case None =>
            println("Invalid UUID, showing your books.")
            session.userId
        }
    } else {
      session.userId
    }
    
    val activeLoans = catalog.transactions.collect {
      case loan @ Transaction.Loan(book, user, timestamp, dueDate) 
        if user.id == targetUserId && !catalog.transactions.exists {
          case Transaction.Return(returnBook, returnUser, returnTimestamp) => 
            returnBook.isbn == book.isbn && returnUser.id == user.id && returnTimestamp.isAfter(timestamp)
          case _ => false
        } => (book, dueDate)
    }
    
    if activeLoans.isEmpty then
      val userName = catalog.users.get(targetUserId).map(_.name).getOrElse("User")
      println(s"$userName has no books to return.")
      return None
    
    println(f"${"#"}%3s ${"Book Title"}%-45s ${"Authors"}%-30s ${"Due Date"}%-15s ${"Status"}%-12s")
    println("=" * 110)
    
    val loansWithIndex = activeLoans.zipWithIndex
    loansWithIndex.foreach { case ((book, dueDate), index) =>
      val authorsStr = book.authors.mkString(", ")
      val truncatedAuthors = if authorsStr.length > 28 then authorsStr.take(25) + "..." else authorsStr
      
      val dueDateStr = dueDate.map(_.toLocalDate.toString).getOrElse("No limit")
      val status = dueDate match {
        case Some(date) if date.isBefore(java.time.LocalDateTime.now()) => "[OVERDUE]"
        case Some(date) if date.minusDays(3).isBefore(java.time.LocalDateTime.now()) => "[Due soon]"
        case _ => "[Current]"
      }
      
      println(f"${index + 1}%3d. ${book.title}%-45s ${truncatedAuthors}%-30s ${dueDateStr}%-15s $status%-12s")
    }
    
    println()
    print(s"Enter book number (1-${activeLoans.length}) or 0 to cancel: ")
    Console.flush()
    Try(scala.io.StdIn.readInt()) match {
      case scala.util.Success(0) => None
      case scala.util.Success(num) if num > 0 && num <= activeLoans.length =>
        Some(activeLoans(num - 1)._1.isbn)
      case _ =>
        println("Invalid selection.")
        None
    }
  }

  def showMyLoans(catalog: LibraryCatalog, session: AuthSession): Unit = {
    println(s"=== My Active Loans - ${session.userName} ===")
    
    // Get all active loans for the current user
    val activeLoans = catalog.transactions.collect {
      case loan @ Transaction.Loan(book, user, timestamp, dueDate) 
        if user.id == session.userId && !catalog.transactions.exists {
          case Transaction.Return(returnBook, returnUser, returnTimestamp) => 
            returnBook.isbn == book.isbn && returnUser.id == user.id && returnTimestamp.isAfter(timestamp)
          case _ => false
        } => (loan, book, dueDate)
    }
    
    if activeLoans.isEmpty then
      println("You have no active loans.")
      println("Visit option 3 to loan some books!")
    else
      println(f"${"Book Title"}%-50s ${"Authors"}%-30s ${"Due Date"}%-15s ${"Status"}%-10s")
      println("=" * 110)
      
      activeLoans.zipWithIndex.foreach { case ((loan, book, dueDate), index) =>
        val authorsStr = book.authors.mkString(", ")
        val truncatedAuthors = if authorsStr.length > 28 then authorsStr.take(25) + "..." else authorsStr
        
        val dueDateStr = dueDate.map(_.toLocalDate.toString).getOrElse("No limit")
        val status = dueDate match {
          case Some(date) if date.isBefore(java.time.LocalDateTime.now()) => "[OVERDUE]"
          case Some(date) if date.minusDays(3).isBefore(java.time.LocalDateTime.now()) => "[Due soon]"
          case _ => "[Current]"
        }
        
        println(f"${book.title}%-50s ${truncatedAuthors}%-30s ${dueDateStr}%-15s $status%-10s")
      }
      
      val overdueCount = activeLoans.count { case (_, _, dueDate) =>
        dueDate.exists(_.isBefore(java.time.LocalDateTime.now()))
      }
      
      println()
      println(s"Total active loans: ${activeLoans.length}")
      if overdueCount > 0 then
        println(s"WARNING: Overdue loans: $overdueCount")
        val fees = catalog.calculateOverdueFees(session.userId)
        if fees > 0 then
          println(f"Outstanding fees: $$${fees}%.2f")
    }

  // Enhanced search functions with selection
  private def enhancedQuickSearchWithSelection(catalog: LibraryCatalog, purpose: String): Option[ISBN] = {
    print("Enter search term (title, author, or genre): ")
    Console.flush()
    val searchTerm = scala.io.StdIn.readLine().trim.toLowerCase
    
    if searchTerm.isEmpty then
      println("Search term cannot be empty.")
      return None
    
    // Combine exact matches and fuzzy matches
    val exactMatches = catalog.books.values.filter { book =>
      book.title.toLowerCase.contains(searchTerm) ||
      book.authors.exists(_.toLowerCase.contains(searchTerm)) ||
      book.genre.toLowerCase.contains(searchTerm)
    }.toList
    
    // Add fuzzy matches (but avoid duplicates)
    val fuzzyMatches = SearchOperations.fuzzySearchWithThreshold(catalog.books.values.toList, searchTerm, 2)
      .filterNot(book => exactMatches.contains(book))
    
    val allResults = exactMatches ++ fuzzyMatches
    
    if allResults.isEmpty then
      println(s"No books found matching '$searchTerm'")
      None
    else
      selectFromBookList(allResults, s"Search results for '$searchTerm'")
  }

  private def filterByGenreWithSelection(catalog: LibraryCatalog, purpose: String): Option[ISBN] = {
    val genres = catalog.books.values.map(_.genre).toSet.toList.sorted
    
    println("\nAvailable genres:")
    genres.zipWithIndex.foreach { case (genre, index) =>
      val count = catalog.books.values.count(_.genre == genre)
      println(f"${index + 1}%3d. $genre%-20s ($count books)")
    }
    
    print(s"Select genre (1-${genres.length}) or 0 to cancel: ")
    Console.flush()
    Try(scala.io.StdIn.readInt()) match {
      case scala.util.Success(0) => None
      case scala.util.Success(num) if num > 0 && num <= genres.length =>
        val selectedGenre = genres(num - 1)
        val booksInGenre = catalog.books.values.filter(_.genre == selectedGenre).toList
        selectFromBookList(booksInGenre, s"Books in genre: $selectedGenre")
      case _ =>
        println("Invalid selection.")
        None
    }
  }

  private def filterByAuthorWithSelection(catalog: LibraryCatalog, purpose: String): Option[ISBN] = {
    print("Enter author name to search (partial match allowed): ")
    Console.flush()
    val searchTerm = scala.io.StdIn.readLine().trim.toLowerCase
    
    if searchTerm.isEmpty then
      println("Author name cannot be empty.")
      return None
    
    val matchingBooks = catalog.books.values.filter(
      _.authors.exists(_.toLowerCase.contains(searchTerm))
    ).toList
    
    if matchingBooks.isEmpty then
      println(s"No books found by authors matching '$searchTerm'")
      None
    else
      selectFromBookList(matchingBooks, s"Books by authors matching '$searchTerm'")
  }

  private def searchByYearRangeWithSelection(catalog: LibraryCatalog, purpose: String): Option[ISBN] = {
    print("Enter start year (or press Enter for any): ")
    Console.flush()
    val startInput = scala.io.StdIn.readLine().trim
    val startYear = if startInput.isEmpty then None else startInput.toIntOption
    
    print("Enter end year (or press Enter for any): ")
    Console.flush()
    val endInput = scala.io.StdIn.readLine().trim
    val endYear = if endInput.isEmpty then None else endInput.toIntOption
    
    val filteredBooks = catalog.books.values.filter { book =>
      val yearMatches = (startYear, endYear) match {
        case (Some(start), Some(end)) => book.publicationYear >= start && book.publicationYear <= end
        case (Some(start), None) => book.publicationYear >= start
        case (None, Some(end)) => book.publicationYear <= end
        case (None, None) => true
      }
      yearMatches
    }.toList
    
    val yearRangeStr = (startYear, endYear) match {
      case (Some(start), Some(end)) => s"$start-$end"
      case (Some(start), None) => s"$start onwards"
      case (None, Some(end)) => s"up to $end"
      case (None, None) => "all years"
    }
    
    if filteredBooks.isEmpty then
      println(s"No books found for year range: $yearRangeStr")
      None
    else
      selectFromBookList(filteredBooks, s"Books published in $yearRangeStr")
  }

  private def combinedAdvancedSearchWithSelection(catalog: LibraryCatalog, purpose: String): Option[ISBN] = {
    println("\n=== Advanced Combined Search ===")
    
    print("Title contains (or Enter to skip): ")
    Console.flush()
    val titleFilter = scala.io.StdIn.readLine().trim.toLowerCase
    
    print("Author contains (or Enter to skip): ")
    Console.flush()
    val authorFilter = scala.io.StdIn.readLine().trim.toLowerCase
    
    print("Genre contains (or Enter to skip): ")
    Console.flush()
    val genreFilter = scala.io.StdIn.readLine().trim.toLowerCase
    
    print("Minimum year (or Enter to skip): ")
    Console.flush()
    val minYear = scala.io.StdIn.readLine().trim match {
      case "" => None
      case year => year.toIntOption
    }
    
    print("Maximum year (or Enter to skip): ")
    Console.flush()
    val maxYear = scala.io.StdIn.readLine().trim match {
      case "" => None
      case year => year.toIntOption
    }
    
    print("Show only available books? (y/n): ")
    Console.flush()
    val availableOnly = scala.io.StdIn.readLine().trim.toLowerCase == "y"
    
    val filteredBooks = catalog.books.values.filter { book =>
      val titleMatch = titleFilter.isEmpty || book.title.toLowerCase.contains(titleFilter)
      val authorMatch = authorFilter.isEmpty || book.authors.exists(_.toLowerCase.contains(authorFilter))
      val genreMatch = genreFilter.isEmpty || book.genre.toLowerCase.contains(genreFilter)
      val yearMatch = (minYear, maxYear) match {
        case (Some(min), Some(max)) => book.publicationYear >= min && book.publicationYear <= max
        case (Some(min), None) => book.publicationYear >= min
        case (None, Some(max)) => book.publicationYear <= max
        case (None, None) => true
      }
      val availabilityMatch = !availableOnly || book.isAvailable
      
      titleMatch && authorMatch && genreMatch && yearMatch && availabilityMatch
    }.toList
    
    val filters = List(
      if titleFilter.nonEmpty then Some(s"title contains '$titleFilter'") else None,
      if authorFilter.nonEmpty then Some(s"author contains '$authorFilter'") else None,
      if genreFilter.nonEmpty then Some(s"genre contains '$genreFilter'") else None,
      if minYear.isDefined then Some(s"year >= ${minYear.get}") else None,
      if maxYear.isDefined then Some(s"year <= ${maxYear.get}") else None,
      if availableOnly then Some("available only") else None
    ).flatten
    
    val filterDescription = if filters.nonEmpty then filters.mkString(", ") else "no filters"
    
    if filteredBooks.isEmpty then
      println(s"No books found matching criteria: $filterDescription")
      None
    else
      selectFromBookList(filteredBooks, s"Advanced search results ($filterDescription)")
  }
}
