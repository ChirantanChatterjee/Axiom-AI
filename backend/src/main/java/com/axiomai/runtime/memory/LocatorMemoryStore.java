package com.axiomai.runtime.memory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocatorMemoryStore {

    private static final Map<String, String>
            MEMORY = new ConcurrentHashMap<>();

    private String buildKey(
            String domain,
            String semanticTarget
    ) {

        return domain + "::" + semanticTarget;
    }

    public void store(
            String domain,
            String semanticTarget,
            String selector
    ) {

        String key =
                buildKey(domain, semanticTarget);

        MEMORY.put(key, selector);

        System.out.println(
                "[MEMORY STORE] SAVED -> "
                        + key
                        + " => "
                        + selector);
    }

    public String retrieve(
            String domain,
            String semanticTarget
    ) {

        String key =
                buildKey(domain, semanticTarget);

        String selector = MEMORY.get(key);

        if(selector != null) {

            System.out.println(
                    "[MEMORY STORE] HIT -> "
                            + key
                            + " => "
                            + selector);
        }

        return selector;
    }

    public boolean contains(
            String domain,
            String semanticTarget
    ) {

        return retrieve(
                domain,
                semanticTarget
        ) != null;
    }

    public void clear() {
        MEMORY.clear();
    }
}