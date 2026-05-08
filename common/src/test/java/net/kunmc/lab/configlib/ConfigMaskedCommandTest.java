package net.kunmc.lab.configlib;

import com.google.gson.Gson;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import net.kunmc.lab.configlib.annotation.Masked;
import net.kunmc.lab.configlib.store.ConfigStore;
import net.kunmc.lab.configlib.store.InMemoryConfigStore;
import net.kunmc.lab.configlib.value.StringValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static net.kunmc.lab.configlib.ConfigCommandTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMaskedCommandTest {
    private MaskedConfig config;

    @AfterEach
    void tearDown() {
        if (config != null) {
            config.close();
        }
    }

    @Test
    void listAndGetMaskSecretValuesWithoutRevealPermission() {
        config = initConfig(new MaskedConfig());
        FakeSender sender = senderWithoutRevealPermission();

        try (CommandTester tester = new CommandTester(commandFor(config), "configlib.test")) {
            tester.execute("config list", sender);
            tester.execute("config secret", sender);
        }

        assertTrue(messages(sender).stream()
                                   .anyMatch(x -> x.contains("secret: <masked>")), messages(sender).toString());
        assertNoRawSecret(sender);
    }

    @Test
    void historyAndDiffMaskSecretValuesWithoutRevealPermission() {
        config = initConfig(new MaskedConfig());
        FakeSender sender = senderWithoutRevealPermission();

        try (CommandTester tester = new CommandTester(commandFor(config), "configlib.test")) {
            tester.execute("config secret updated", sender);
            tester.execute("config history 0", sender);
            tester.execute("config history 1", sender);
            tester.execute("config history diff 1", sender);
            tester.execute("config diff 1", sender);
            tester.execute("config diff default", sender);
        }

        assertTrue(messages(sender).stream()
                                   .anyMatch(x -> x.contains("secret: <masked>")), messages(sender).toString());
        assertTrue(messages(sender).stream()
                                   .anyMatch(x -> x.contains("secret: <masked> -> <masked>")),
                   messages(sender).toString());
        assertNoRawSecret(sender);
    }

    private static FakeSender senderWithoutRevealPermission() {
        return FakeSender.player("Steve")
                         .denyPermissions(MaskedRevealPolicy.DEFAULT_REVEAL_PERMISSION);
    }

    private static void assertNoRawSecret(FakeSender sender) {
        assertTrue(messages(sender).stream()
                                   .noneMatch(x -> x.contains("initial") || x.contains("updated")),
                   messages(sender).toString());
    }

    static class MaskedConfig extends CommonBaseConfig {
        @Masked
        final StringValue secret = new StringValue("initial");
        final transient InMemoryConfigStore store = new InMemoryConfigStore(new Gson());

        @Override
        protected ConfigStore createConfigStore() {
            return store;
        }
    }
}
