package dev.ricodevvv.riven.data.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletTest {

    @Test
    void neverGoesBelowZero() {
        final Wallet wallet = new Wallet();

        wallet.add("coins", 100);
        assertFalse(wallet.withdraw("coins", 150));
        assertTrue(wallet.withdraw("Coins", 60));
        assertEquals(40, wallet.get("coins"));
        assertEquals(0, wallet.add("coins", -500));
    }

}
