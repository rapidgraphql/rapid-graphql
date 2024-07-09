package org.rapidgraphql.schemabuilder;

import graphql.kickstart.execution.context.DefaultGraphQLContextBuilder;
import graphql.kickstart.execution.context.GraphQLKickstartContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import org.dataloader.DataLoaderRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import org.rapidgraphql.dataloaders.DataLoaderRegistryFactory;

import java.util.HashMap;
import java.util.Map;

public class RapidGraphQLContextBuilder extends DefaultGraphQLContextBuilder
        implements GraphQLServletContextBuilder {
    private final DataLoaderRegistryFactory dataLoaderRegistryFactory;

    public RapidGraphQLContextBuilder(DataLoaderRegistryFactory dataLoaderRegistryFactory) {
        this.dataLoaderRegistryFactory = dataLoaderRegistryFactory;
    }


    @Override
    public GraphQLKickstartContext build(HttpServletRequest request, HttpServletResponse response) {
        Map<Object, Object> map = new HashMap<>();
        map.put(HttpServletRequest.class, request);
        map.put(HttpServletResponse.class, response);
        return GraphQLKickstartContext.of(buildDataLoaderRegistry(), map);
    }

    @Override
    public GraphQLKickstartContext build(Session session, HandshakeRequest handshakeRequest) {
        Map<Object, Object> map = new HashMap<>();
        map.put(Session.class, session);
        map.put(HandshakeRequest.class, handshakeRequest);
        return GraphQLKickstartContext.of(buildDataLoaderRegistry(), map);
    }

    private DataLoaderRegistry buildDataLoaderRegistry() {
        return dataLoaderRegistryFactory.build();
    }
}