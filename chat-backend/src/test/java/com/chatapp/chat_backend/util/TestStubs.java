package com.chatapp.chat_backend.util;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class TestStubs {

    private TestStubs() {}

    public static class StubBuilder<T> {
        private final Class<T> clazz;
        private final Map<String, Function<Object[], Object>> handlers = new HashMap<>();

        public StubBuilder(Class<T> clazz) {
            this.clazz = clazz;
        }

        public StubBuilder<T> on(String methodName, Function<Object[], Object> handler) {
            handlers.put(methodName, handler);
            return this;
        }

        @SuppressWarnings("unchecked")
        public T build() {
            return (T) Proxy.newProxyInstance(
                    clazz.getClassLoader(),
                    new Class<?>[]{clazz},
                    (proxy, method, args) -> {
                        if (handlers.containsKey(method.getName())) {
                            return handlers.get(method.getName()).apply(args);
                        }
                        if ("toString".equals(method.getName())) {
                            return "Stub[" + clazz.getSimpleName() + "]";
                        }
                        if ("hashCode".equals(method.getName())) {
                            return clazz.hashCode();
                        }
                        if ("equals".equals(method.getName())) {
                            return proxy == (args != null && args.length > 0 ? args[0] : null);
                        }
                        if (method.getReturnType().equals(Mono.class)) {
                            return Mono.empty();
                        }
                        if (method.getReturnType().equals(Flux.class)) {
                            return Flux.empty();
                        }
                        if (method.getReturnType().equals(boolean.class)) {
                            return false;
                        }
                        if (method.getReturnType().equals(int.class)) {
                            return 0;
                        }
                        if (method.getReturnType().equals(long.class)) {
                            return 0L;
                        }
                        return null;
                    }
            );
        }
    }

    public static <T> StubBuilder<T> of(Class<T> clazz) {
        return new StubBuilder<>(clazz);
    }
}
