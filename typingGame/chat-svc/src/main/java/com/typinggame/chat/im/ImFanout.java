package com.typinggame.chat.im;

import java.util.function.Consumer;

public final class ImFanout {
    private ImFanout() {
    }

    public static void deliver(Iterable<Consumer<String>> nodes, String payload) {
        for (Consumer<String> node : nodes) {
            node.accept(payload);
        }
    }
}
