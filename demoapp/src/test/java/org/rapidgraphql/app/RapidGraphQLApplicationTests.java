package org.rapidgraphql.app;

import org.junit.jupiter.api.Test;
import org.rapidgraphql.client.RapidGraphQLClient;
import org.rapidgraphql.client.annotations.GraphQL;
import org.rapidgraphql.client.annotations.GraphQLMutation;
import org.rapidgraphql.client.annotations.GraphQLQuery;
import org.rapidgraphql.client.exceptions.GraphQLErrorException;
import org.rapidgraphql.helloworld.Chat;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RapidGraphQLApplicationTests {
	@LocalServerPort
	private int randomServerPort;

	@Test
	void contextLoads() {
	}

	interface GraphQLApi {
		@GraphQL(query="query { helloWorld }")
		String helloWorld();
		@GraphQL(query="mutation message($message: String) {\n" +
				"  message(message: $message) {\n" +
				"    youSaid\n" +
				"    iSay\n" +
				"  }\n" +
				"}")
		Chat message(String message);
	}

	@Test
	void clientTestWithGraphQLAnnotation() {
		GraphQLApi graphQLApi = RapidGraphQLClient.builder()
				.target(GraphQLApi.class, "http://localhost:" + randomServerPort + "/graphql");
		assertThat(graphQLApi.helloWorld()).isEqualTo("Hello World!!");
		Chat chat = graphQLApi.message("hi");
		assertThat(chat).isNotNull()
				.extracting(Chat::getYouSaid, Chat::getISay)
				.containsExactly("hi", "ih");
	}

	interface TestApi {
		@GraphQLQuery
		Integer intValue(Integer val);
		@GraphQLQuery
		Long longValue(Long val);
		@GraphQLQuery
		String longValue(String val);
		@GraphQLQuery
		List<String> stringList(List<String> val);
		@GraphQLQuery
		String throwException(String message);
		@GraphQLMutation("{{youSaid, iSay}}")
		Chat message(String message);
	}
	@Test
	public void clientTestWithGraphQLQueryAnnotation() {
		TestApi testApi = RapidGraphQLClient.builder()
				.target(TestApi.class, "http://localhost:" + randomServerPort + "/graphql");
		assertThat(testApi.intValue(123)).isEqualTo(123);
		assertThat(testApi.longValue(123456789L)).isEqualTo(123456789L);
		assertThat(testApi.stringList(List.of("hello", "world"))).containsExactly("hello", "world");
		GraphQLErrorException error = assertThrows(GraphQLErrorException.class, () -> testApi.throwException("error"));
		assertThat(error.getMessage()).isEqualTo("error");
	}

	@Test
	public void clientTestWithGraphQLMutationAnnotation() {
		TestApi testApi = RapidGraphQLClient.builder()
				.target(TestApi.class, "http://localhost:" + randomServerPort + "/graphql");
		assertThat(testApi.message("hi")).isEqualTo(Chat.builder().iSay("ih").youSaid("hi").build());
	}
}
