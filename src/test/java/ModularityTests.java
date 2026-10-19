package com.eylul.ticketplatform;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(TicketPlatformApplication.class);

    @Test
    void printsModules() {
        modules.forEach(System.out::println);
    }

    @Test
    void verifiesModuleStructure() {
        modules.verify();
    }
}