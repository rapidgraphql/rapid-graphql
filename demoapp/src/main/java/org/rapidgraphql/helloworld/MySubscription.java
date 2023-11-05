package org.rapidgraphql.helloworld;

import graphql.kickstart.tools.GraphQLSubscriptionResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
@Log4j2
class MySubscription implements GraphQLSubscriptionResolver {

    public Publisher<Integer> hello(DataFetchingEnvironment env) {
        return Flux.range(0, 100)
                .delayElements(Duration.ofSeconds(1))
                .map(this::fun);
    }
    private Integer fun(Integer i) {
        Integer result = i*10;
        log.info("result={}", result);
        return result;
    }
}