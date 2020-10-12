package com.example.actuator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@SpringBootApplication(proxyBeanMethods = false)
public class ActuatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ActuatorApplication.class, args);
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> ready(
		DatabaseClient dbc,
		ReservationRepository rr) {
		return event -> {
			dbc
				.sql("create table reservation(id serial primary key, name varchar(255) not null) ").fetch().rowsUpdated()
				.thenMany(rr.saveAll(Flux.just(new Reservation(null, "Andy"), new Reservation(null, "Josh"))))
				.thenMany(rr.findAll())
				.subscribe(System.out::println);
		};
	}
}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, Integer> {
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Reservation {

	@Id
	private Integer id;
	private String name;

}

@RestController
@RequiredArgsConstructor
class ReservationController {

	private final ReservationRepository reservationRepository;

	@GetMapping("/reservations")
	Flux<Reservation> reservations() {
		return this.reservationRepository.findAll();
	}
}

@RestController
class SlowController	{

	@GetMapping("/slow")
	Mono<String> greet() {
		return Mono.just("Hello, world!").delayElement(Duration.ofSeconds(20));
	}
}

@RestController
class GreetingsController {

	@GetMapping("/")
	public Map<String, String> greet() {
		return Collections.singletonMap("greetings", "Olá!");
	}
}
