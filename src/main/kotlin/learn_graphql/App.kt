package learn_graphql

import graphql.GraphQL
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.StaticDataFetcher
import graphql.schema.idl.RuntimeWiring.newRuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeRuntimeWiring.newTypeWiring


class App {
    private val graphQLDataFetchers = GraphQLDataFetchers()

    val graphQL: GraphQL = GraphQL.newGraphQL(SchemaGenerator()
            .makeExecutableSchema(SchemaParser().parse(
                    this::class.java.getResource("/schema.gql").readText()
            ), newRuntimeWiring()
                    .type("Query") { builder ->
                        builder.dataFetcher("hello", StaticDataFetcher("world"))
                        builder.dataFetcher("banana", StaticDataFetcher("eat me"))
                    }
                    .type(newTypeWiring("Query")
                            .dataFetcher("bookById", graphQLDataFetchers.bookByIdDataFetcher))
                    .type(newTypeWiring("Book")
//Default DataFetchers
//We only implement two DataFetchers. As mentioned above, if you don’t specify one, the default PropertyDataFetcher
// is used. In our case it means Book.id, Book.name, Book.pageCount, Author.id, Author.firstName
// and Author.lastName all have a default PropertyDataFetcher associated with it.
                            .dataFetcher("author", graphQLDataFetchers.authorDataFetcher)
                            .dataFetcher("pageCount", graphQLDataFetchers.getPageCountDataFetcher))
                    .build()))
            .build()
}


class GraphQLDataFetchers() {
    val bookByIdDataFetcher: DataFetcher<Map<String,String>>
        get() = DataFetcher { dataFetchingEnvironment: DataFetchingEnvironment ->
            val bookId: String = dataFetchingEnvironment.getArgument("id")
            books
                    .stream()
                    .filter { book: Map<String, String> -> (book["id"] == bookId) }
                    .findFirst()
                    .orElse(null)
        }

    val getPageCountDataFetcher: DataFetcher<String?>
        get(){
            return DataFetcher {
                val source : Map<String,String> = it.getSource()
                source["pageCount"]
            }
        }

    val authorDataFetcher: DataFetcher<Map<String,String>>
//. Compared to the previously described book DataFetcher, we don’t have an argument, but we have a book instance.
// The result of the DataFetcher from the parent field is made available via getSource.
// This is an important concept to understand: the DataFetcher for each field in GraphQL
// are called in a top-down fashion and the parent’s result is the source property of the child DataFetcherEnvironment.
        get() {
            return DataFetcher { dataFetchingEnvironment: DataFetchingEnvironment ->
                val book: Map<String, String> = dataFetchingEnvironment.getSource()
                val authorId: String? = book["authorId"]
                authors
                        .stream()
                        .filter { author: Map<String, String> -> (author["id"] == authorId) }
                        .findFirst()
                        .orElse(null)
            }
        }

    companion object {
        private val books: List<Map<String, String>> = listOf(
                mapOf("id" to  "book-1",
                        "name" to  "Harry Potter and the Philosopher's Stone",
                        "pageCount" to  "223",
                        "authorId" to  "author-1"),
                mapOf("id" to  "book-2",
                        "name" to  "Moby Dick",
                        "pageCount" to  "635",
                        "authorId" to  "author-2"),
                mapOf("id" to  "book-3",
                        "name" to  "Interview with the vampire",
                        "pageCount" to  "371",
                        "authorId" to  "author-3")
        )
        private val authors: List<Map<String, String>> = listOf(
                mapOf("id" to "author-1",
                        "firstName" to "Joanne",
                        "lastName" to "Rowling"),
                mapOf("id" to "author-2",
                        "firstName" to "Herman",
                        "lastName" to "Melville"),
                mapOf("id" to "author-3",
                        "firstName" to "Anne",
                        "lastName" to "Rice")
        )
    }
}
fun main() {

    val app = App()

    println(app.graphQL.execute("{banana}").getData<Any>().toString())
    println(app.graphQL.execute("""{bookById(id:"book-1"){name}}""").getData<Any>().toString())
}
