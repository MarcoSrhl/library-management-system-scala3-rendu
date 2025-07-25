# Library Management System - User Manual

**Author:** Marco Serhal, Paul Zamanian, Adam Mounir, Alice Guillou, Rayane Fofana

## Table of Contents

1. [Getting Started](#getting-started)
2. [User Types](#user-types)
3. [Logging In](#logging-in)
4. [Using the System](#using-the-system)
5. [Menu Options by User Type](#menu-options-by-user-type)
6. [Common Operations](#common-operations)

---

## Getting Started

### Running the Application

To start the Library Management System:

1. Open a terminal in the project directory
2. Run the following command:
   ```bash
   sbt run
   ```
3. The system will start and display the login prompt

### System Overview

The Library Management System is a command-line application that allows different types of users to manage books, loans, and library operations. The system has three main user types with different permissions and capabilities.

---

## User Types

The system supports three types of users, each with specific permissions and limits:

### Student
- **Purpose**: Academic students who can borrow books for their studies
- **Loan Limit**: 5 books maximum at one time
- **Loan Period**: 30 days (1 month)
- **Overdue Fee**: $0.50 per day
- **Permissions**: Can borrow, return, search books, and make reservations
- **Special Info**: Must provide their academic major

### Faculty
- **Purpose**: Academic faculty members with extended privileges
- **Loan Limit**: 10 books maximum at one time
- **Loan Period**: 90 days (3 months)
- **Overdue Fee**: $0.25 per day (discounted rate)
- **Permissions**: Can borrow, return, search books, and make reservations
- **Special Info**: Must provide their department affiliation

### Librarian
- **Purpose**: Library staff with administrative access
- **Loan Limit**: Unlimited books
- **Loan Period**: Unlimited (effectively no due date)
- **Overdue Fee**: No fees
- **Permissions**: Can perform all operations including adding books, managing users, and viewing statistics
- **Special Info**: Have an employee ID for identification

---

## Logging In

When you start the application, you'll see:

```
=== Library System Login ===
Enter your user ID (UUID) or name: 
```

For testing purposes, you can use the following default users:

### Default Users

The system comes with three default users for testing:

1. **Student**: 
   - Name: `Alice`
   - Password: `student123`
   - Major: Computer Science

2. **Faculty**: 
   - Name: `Dr. Smith`
   - Password: `faculty123`
   - Department: Engineering

3. **Librarian**: 
   - Name: `Admin`
   - Password: `admin123`
   - Employee ID: LIB001

### Login Process

1. Enter either the user's name (e.g., "Alice") or their UUID
2. Enter the password when prompted
3. If credentials are correct, you'll see: `Welcome, [Name]! ([UserType])`
4. If incorrect, you'll get the option to try again

---

## Using the System

After successful login, you'll see a numbered menu with available options. The options shown depend on your user type.

### Menu Navigation

- Type the number corresponding to your desired action
- Press Enter to execute
- Type `0` to exit the system
- Some options are marked as "Librarians only" and are restricted

### Example Menu Display

```
Choose an option:
1. Add a book (Librarians only)
2. Remove a book (Librarians only)
3. Search for books
4. Show all books
5. Borrow a book
6. Return a book
...
0. Exit
Enter your choice: 
```

---

## Menu Options by User Type

### Student Options
Students can access the following menu options:
- **3**: Search for books
- **4**: Show all books  
- **5**: Borrow a book
- **6**: Return a book
- **7**: Show books on loan
- **8**: Show overdue books
- **10**: Reserve a book
- **11**: Show reservations
- **12**: Cancel a reservation
- **16**: Show loan history
- **17**: Show current loans
- **0**: Exit

### Faculty Options
Faculty members have the same options as students:
- **3**: Search for books
- **4**: Show all books
- **5**: Borrow a book
- **6**: Return a book
- **7**: Show books on loan
- **8**: Show overdue books
- **10**: Reserve a book
- **11**: Show reservations
- **12**: Cancel a reservation
- **16**: Show loan history
- **17**: Show current loans
- **0**: Exit

### Librarian Options
Librarians have access to all options:
- **1**: Add a book *(Librarians only)*
- **2**: Remove a book *(Librarians only)*
- **3**: Search for books
- **4**: Show all books
- **5**: Borrow a book
- **6**: Return a book
- **7**: Show books on loan
- **8**: Show overdue books
- **9**: Show library statistics *(Librarians only)*
- **10**: Reserve a book
- **11**: Show reservations
- **12**: Cancel a reservation
- **13**: Add a user *(Librarians only)*
- **14**: Show all users *(Librarians only)*
- **15**: Remove a user *(Librarians only)*
- **16**: Show loan history
- **17**: Show current loans
- **18**: Process reservations *(Librarians only)*
- **0**: Exit

---

## Common Operations

### Searching for Books
When you select option 3 (Search for books):
1. Choose search criteria (title, author, ISBN, genre, etc.)
2. Enter your search term
3. View the search results
4. The system will show matching books with their availability status

### Borrowing a Book
When you select option 5 (Borrow a book):
1. Enter the ISBN of the book you want to borrow
2. The system checks if:
   - The book exists and is available
   - You haven't exceeded your loan limit
   - You don't already have this book on loan
3. If successful, the book is added to your loans with the appropriate due date

### Returning a Book
When you select option 6 (Return a book):
1. Enter the ISBN of the book you want to return
2. The system checks if you have this book on loan
3. If the book is overdue, any applicable fees are calculated
4. The book is returned and becomes available for others

### Viewing Your Loans
- Option 17 shows your current active loans
- Option 16 shows your complete loan history
- Option 7 shows all books currently on loan (useful for librarians)

### Making Reservations
When you select option 10 (Reserve a book):
1. Enter the ISBN of the book you want to reserve
2. The system checks if the book is currently unavailable
3. If successful, you're added to the reservation queue
4. You'll be notified when the book becomes available

### Administrative Functions (Librarians Only)

#### Adding Books (Option 1)
1. Enter book details: title, author, ISBN, genre, publication year
2. Specify number of copies to add
3. The system validates the information and adds the book to the catalog

#### Adding Users (Option 13)
1. Choose user type (Student, Faculty, or Librarian)
2. Enter user details (name, specific info for user type, password)
3. The system creates a new user account

#### Viewing Statistics (Option 9)
- Shows total books in catalog
- Shows total active loans
- Shows overdue books count
- Shows total users by type
- Shows most popular books

---

## Error Handling and Tips

### Common Issues

1. **Login Failed**: Double-check username and password. Remember default users are case-sensitive.

2. **Can't Borrow Book**: Check if:
   - You've reached your loan limit
   - The book is available
   - You don't already have this book

3. **Invalid Input**: The system will prompt you to re-enter information if invalid.

### Best Practices

- **Students/Faculty**: Check your loan limit before trying to borrow
- **All Users**: Use the search function to find books before borrowing
- **Librarians**: Regularly check statistics and overdue books
- **All Users**: Keep track of due dates to avoid overdue fees

### Exiting the System

- Always use option `0` to exit properly
- This ensures all data is saved correctly
- The system will display "Goodbye!" when closing

---

*This manual covers the core functionality of the Library Management System CLI interface. For technical documentation and API details, refer to the ScalaDoc documentation.*
