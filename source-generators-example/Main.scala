////> using plugin "com.thesamet.scalapb::compilerplugin:0.11.3"
//> using dep "com.thesamet.scalapb::scalapb-runtime:0.11.3"

//> using source.generator scalapbc

import tutorial.addressbook.{AddressBook, Person}

object Main extends App {
    println("Main")
    val person = Person(name = "John Doe", id = 123, email = Some("XD"))
    println(person)
}