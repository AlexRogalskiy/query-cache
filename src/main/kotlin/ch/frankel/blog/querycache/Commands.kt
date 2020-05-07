package ch.frankel.blog.querycache

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.hibernate.serialization.Value
import org.hibernate.SessionFactory
import org.hibernate.cache.spi.CacheImplementor
import org.hibernate.cache.spi.QueryKey
import org.hibernate.cache.spi.entry.CacheEntry
import org.hibernate.engine.spi.QueryParameters
import org.hibernate.internal.SessionImpl
import org.hibernate.transform.CacheableResultTransformer
import org.hibernate.type.LongType
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.table.*
import java.io.Serializable
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@ShellComponent
class Commands(private val repo: ThingRepository, private val hz: HazelcastInstance) {

    @PersistenceContext
    private lateinit var em: EntityManager

    @ShellMethod("Select all Things from database")
    fun entities() = embedInTable("id", "text") { builder ->
        repo.findAll().forEach {
            builder.addRow()
            builder.addValue(it.id)
            builder.addValue(it.text)
        }
    }

    @ShellMethod("List Hazelcast distributed objects")
    fun maps() = embedInTable("name") { builder ->
        hz.distributedObjects.forEach {
            builder.addRow()
            builder.addValue(it.name)
        }
    }

    @ShellMethod("List query cache region's content")
    fun queryCache() = embedInTable("keys") { builder ->
        fun createQueryKey(session: SessionImpl): QueryKey {
            val parameters = QueryParameters().apply {
                namedParameters = hashMapOf()
            }
            val transformer = CacheableResultTransformer.create(
                parameters.resultTransformer,
                arrayOfNulls(0),
                booleanArrayOf(true)
            )
            return QueryKey.generateQueryKey(
                "select thing0_.id as id1_0_, thing0_.text as text2_0_ from thing thing0_",
                parameters,
                null,
                session,
                transformer
            )
        }
        val factory = em.entityManagerFactory.unwrap(SessionFactory::class.java)
        val session = factory.openSession() as SessionImpl
        val queryKey = createQueryKey(session)
        val cacheImplementor = factory.cache as CacheImplementor
        cacheImplementor.defaultQueryResultsCache.get(queryKey, arrayOf<String>(), arrayOf(LongType()), session)?.let {
            builder.addRow()
            builder.addValue(it)
        }
    }

    @ShellMethod("List all Things stored in the cache")
    fun cache() = embedInTable("id", "text", "timestamp") { builder ->
        hz.getMap<Any, Value>("entities").forEach { (key, value) ->
            builder.addRow()
            builder.addValue(key)
            builder.addValue(value.timestamp)
            val entry = value.value as? CacheEntry
            val text = entry?.disassembledState as Array<Serializable>
            builder.addValue(text[0])
        }
    }

    private fun embedInTable(vararg names: String,
                             fillData: (builder: TableModelBuilder<Any>) -> Unit): Table {
        val builder = TableModelBuilder<Any>()
            .apply {
                addRow()
                names.forEach {
                    addValue(it)
                }
            }
        fillData(builder)
        return TableBuilder(builder.build())
            .apply {
                addHeaderAndVerticalsBorders(BorderStyle.oldschool)
            }.build()
    }
}