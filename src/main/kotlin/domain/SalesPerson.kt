package domain

import javafx.beans.binding.Binding
import javafx.collections.FXCollections
import rx.javafx.kt.onChangedObservable
import rx.javafx.kt.toBinding
import rx.lang.kotlin.subscribeWith
import rx.lang.kotlin.toObservable
import java.util.*

class SalesPerson(val id: Int, val firstName: String, val lastName: String) {

    /**
     * The assigned CompanyClient ID's for this SalesPerson
     */
    val customerAssignments by lazy {
        FXCollections.observableSet(HashSet<Int>()).apply {
            assignmentsFor(id)
                    .subscribeWith {
                        onNext { add(it) }
                        onError { throw RuntimeException(it) }
                    }
            onChangedObservable()
        }
    }

    /**
     * A Binding holding a concatenation of the CompanyClient ID's for this SalesPerson
     */
    val customerAssignmentsConcat: Binding<String> by lazy {
        customerAssignments.onChangedObservable().flatMap {
            it.toObservable().map { it.toString() }.reduce("") { x, y -> if (x == "") y else "$x|$y" }
        }.toBinding()
    }

    companion object {
        /**
         * Retrieves all SalesPerson instances from database
         */
        val all = db.select("SELECT * FROM SALES_PERSON")
            .get { SalesPerson(it.getInt("ID"),it.getString("FIRST_NAME"),it.getString("LAST_NAME")) }
            .toList().flatMap { it.toObservable() } //workaround for SQLite locking error

        /**
         * Retrieves all assigned CompanyClient ID's for a given SalesPerson
         */
        fun assignmentsFor(salesPersonId: Int) =
            db.select("SELECT CLIENT_COMPANY_ID FROM ASSIGNMENT WHERE SALES_PERSON_ID = ?")
                .parameter(salesPersonId)
                .getAs(Int::class.java)
                .toList().flatMap { it.toObservable() } //workaround for SQLite locking error
    }
}