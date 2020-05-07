package ch.frankel.blog.querycache

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_ONLY
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.jpa.QueryHints.HINT_CACHEABLE
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.QueryHints
import javax.persistence.*

@SpringBootApplication
class QueryCacheDemo

fun main(args: Array<String>) {
    runApplication<QueryCacheDemo>(*args)
}

@Entity
@Cache(region = "entities", usage = READ_ONLY)
class Thing(@Id val id: Long, val text: String)

interface ThingRepository : JpaRepository<Thing, Long> {

    @QueryHints(QueryHint(name = HINT_CACHEABLE, value = "true"))
    override fun findAll(): List<Thing>
}