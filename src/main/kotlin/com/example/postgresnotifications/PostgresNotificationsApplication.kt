@file:Suppress("SpringJavaInjectionPointsAutowiringInspection")

package com.example.postgresnotifications

import io.r2dbc.postgresql.api.Notification
import io.r2dbc.postgresql.api.PostgresqlConnection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Wrapped
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.connectionfactory.init.ConnectionFactoryInitializer
import org.springframework.data.r2dbc.connectionfactory.init.ResourceDatabasePopulator
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@SpringBootApplication
@EnableR2dbcRepositories(considerNestedRepositories = true)
class PostgresNotificationsApplication {
	@Bean
	fun initializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer? {
		val initializer = ConnectionFactoryInitializer()
		initializer.setConnectionFactory(connectionFactory)
		val populator = ResourceDatabasePopulator(ClassPathResource("schema.sql"))
		populator.setSeparator(";;")
		initializer.setDatabasePopulator(populator)
		return initializer
	}
}

fun main(args: Array<String>) {
	runApplication<PostgresNotificationsApplication>(*args)
}

@RestController
class LoginController(val repository: LoginEventRepository, connectionFactory: ConnectionFactory) {
	val connection = Mono.from(connectionFactory.create())
			.map<PostgresqlConnection> {
				(it as Wrapped<*>).unwrap() as PostgresqlConnection
			}.block()!!

	@PostConstruct
	private fun postConstruct() {
		connection.createStatement("LISTEN login_event_notification")
				.execute()
				.flatMap { it.rowsUpdated }
				.subscribe()
	}

	@PreDestroy
	private fun preDestroy() {
		connection.close().subscribe()
	}

	@PostMapping("/login/{username}")
	fun login(@PathVariable username: String): Mono<Void> {
		return repository
				.save(LoginEvent(username, LocalDateTime.now()))
				.then()
	}

	@GetMapping(value = ["/login-stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
	fun getStream(): Flux<CharSequence> = connection
				.notifications
				.map(Notification::getParameter)
}

interface LoginEventRepository : ReactiveCrudRepository<LoginEvent, Int>

@Table
class LoginEvent(@field:Column("user_name") val username: String, val loginTime: LocalDateTime) {
	@Id
	var id: Int? = null
}
