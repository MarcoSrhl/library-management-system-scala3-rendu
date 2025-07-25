# ScalaDoc Guide - How to navigate the documentation

## Introduction

This guide explains how to effectively use the ScalaDoc documentation generated for this project. 


## Quick Generation

```bash
# In the project directory as always
sbt doc

# Open the documentation
start target/scala-3.3.1/api/index.html    # Windows
open target/scala-3.3.1/api/index.html     # macOS  
xdg-open target/scala-3.3.1/api/index.html # Linux
```

## Serving Documentation with HTTP Server

**Important**: If the documentation doesn't work properly (links not clickable, JavaScript errors, navigation issues), use the below local HTTP server instead of opening files directly:

```bash
# Method 1: Direct command from project root (Recommended)
python -m http.server 8080 --directory "target/scala-3.3.1/api"

# Method 2: Navigate to directory first
cd target/scala-3.3.1/api
python -m http.server 8080

# Then open in browser
# Go to: http://localhost:8080
```

### Why use HTTP Server?

- **JavaScript Support**: Modern browsers block local file JavaScript for security
- **Navigation Works**: All links, search, and interactive features function properly
- **Cross-references**: Links between classes and packages work correctly
- **Search Functionality**: Built-in ScalaDoc search operates normally

### Alternative Ports
If port 8080 is busy, use any other port:
```bash
python -m http.server 3000 --directory "target/scala-3.3.1/api"
# Access at: http://localhost:3000
```

## Navigation in Documentation

### 1. Home Page (index.html)

When you open the documentation, you arrive at the home page containing:
- **Package List**: `library`, `utils`, `services`
- **Search Bar**: Top right corner
- **Side Navigation**: Package and class tree

### 2. Exploration by Package

#### Package `library` (Main Components)
- **`LibraryCatalog`**: Main class - start here!
- **`Book`**: Book model with ISBN validation
- **`User`**: User hierarchy (Student, Faculty, Librarian)
- **`Transaction`**: Transaction types (Loan, Return, Reservation)

#### Package `utils` (Utilities)
- **`Types`**: Opaque types (`ISBN`, `UserID`, etc.)
- **`Extensions`**: Extension methods for collections
- **`TypeClasses`**: Type class instances (Displayable, Validatable)

### 3. Navigation in a Class

Taking `LibraryCatalog` as example:
1. **General Description**: Top of the page
2. **Constructors**: How to create an instance
3. **Methods**: Organized by category
4. **Examples**: Executable code blocks
5. **Links**: To related classes

## Using Search

### Global Search
- Type in search bar: `addBook`
- Select desired result
- Direct navigation to method

### Search by Type
- Search `ISBN` to see all usages
- Search `User` to see hierarchy
- Search `LibraryError` to see errors

## Practical Tips

### For Beginners
1. **Start with `LibraryCatalog.empty`** - main entry point
2. **Read examples** - each method has concrete examples
3. **Follow links** - explore related classes
4. **Check companion objects** - creation methods

### For Experienced Developers
1. **Explore opaque types** - `ISBN`, `UserID` for type safety
2. **Study extension methods** - enriched APIs for collections
3. **Analyze union types** - functional error handling
4. **Examine given instances** - polymorphism with type classes

## Documentation Usage Examples

### Example 1: Create a Catalog
```scala
// Found in LibraryCatalog.empty
val catalog = LibraryCatalog.empty
```

### Example 2: Add a Book
```scala
// Found in LibraryCatalog.addBook
val book = Book(
  ISBN.safe("978-0134685991"),
  "Effective Java",
  List("Joshua Bloch"),
  2017,
  "Programming",
  true
)
val updatedCatalog = catalog.addBook(book)
```

## IDE Integration

### IntelliJ IDEA
- **Ctrl+Q**: Quick documentation
- **Ctrl+Shift+I**: Quick definition
- **F1**: External documentation

### VS Code + Metals
- **Hover**: Automatic documentation
- **Ctrl+K Ctrl+I**: Force display
- **F12**: Go to definition

## ScalaDoc Comment Structure

### Main Tags
```scala
/**
 * Method description
 *
 * @param name Parameter description
 * @return Return description
 * @throws ExceptionType Exception condition
 * @example
 * {{{
 * val result = myMethod("test")
 * }}}
 * @since 1.0.0
 * @author Author name
 */
```

### Special Formatting
- **Lists**: Use `-` or `*`
- **Inline code**: Surround with backticks `code`
- **Code blocks**: Use `{{{` and `}}}`
- **Links**: `[[package.Class]]` or `[[Class.method]]`

## Types of Available Documentation

### 1. API Documentation
- Complete method signatures
- Parameter and return types
- Constraints and validations

### 2. Practical Examples
- Executable code
- Typical use cases
- Recommended patterns

### 3. Contextual Information
- Algorithmic complexity
- Thread safety
- Immutability guarantees

### 4. Cross References
- Related classes
- Alternative methods
- Design patterns

## Useful Links
- **Scala 3 Documentation**: https://docs.scala-lang.org/scala3/
- **ScalaDoc Guide**: https://docs.scala-lang.org/style/scaladoc.html
- **Scala 3 Reference**: https://dotty.epfl.ch/docs/

## Conclusion

The ScalaDoc documentation for this project combines:
- **Complete API** with all signatures
- **Practical examples** for each component
- **Intuitive navigation** between elements
- **Powerful search** to quickly find information

