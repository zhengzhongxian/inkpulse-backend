package com.inkpulse.config;

import an.awesome.pipelinr.Command;
import an.awesome.pipelinr.CommandHandlers;
import an.awesome.pipelinr.Pipeline;
import an.awesome.pipelinr.Pipelinr;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PipelinRConfig {

    @Bean
    @SuppressWarnings("rawtypes")
    public Pipeline pipeline(
            ObjectProvider<Command.Handler> handlers,
            ObjectProvider<Command.Middleware> middlewares) {
        
        CommandHandlers h =  handlers::orderedStream;
        Command.Middlewares m = middlewares::orderedStream;

        return new Pipelinr()
                .with(h)
                .with(m);
    }
}

