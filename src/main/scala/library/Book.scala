package library

import Types.ISBN

/**
 * just the class to represent a book
 * 
 * made it a case class to get immutability and all the nice methods
 * (equals, hashCode, copy, etc) for free
 * 
 * contains everything you need to know about a book:
 * - isbn: unique identifier (with opaque type to be safe)
 * - title: book title
 * - authors: list of authors
 * - publicationYear: year it was published 
 * - genre: genre (fiction, science, programming, etc)
 * - isAvailable: whether you can borrow it or not
 * 
 * designed to integrate with the rest of the system, especially
 * JSON serialization and type classes for display
 */
case class Book(
  isbn: ISBN,
  title: String,
  authors: List[String],
  publicationYear: Int,
  genre: String,
  isAvailable: Boolean
)
