package com.example.postgresnotifications

import io.r2dbc.spi.ConnectionFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * docker run --name some-postgres -e POSTGRES_PASSWORD=mysecretpassword -d -p 5432:5432 postgres
 */
@SpringBootTest
class PostgresNotificationsApplicationTests(@Autowired private val loginEventRepository: LoginEventRepository,
	@Autowired private val connectionFactory: ConnectionFactory) {

	@Test
	fun contextLoads() {
	}

	@Test
	fun login() {
		val client = WebTestClient.bindToController(LoginController(loginEventRepository,
				connectionFactory)).build()
		client.post()
				.uri("/login/victor")
				.exchange()
				.expectStatus()
				.isOk
	}

}
