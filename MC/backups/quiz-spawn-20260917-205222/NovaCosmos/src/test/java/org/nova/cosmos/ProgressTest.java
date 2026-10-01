package org.nova.cosmos;

import java.nio.file.Files;
import java.util.UUID;

public final class ProgressTest {
    public static void main(String[] args) throws Exception {
        Progress p = new Progress(); p.rollover("2026-09-13");
        p.dust = 25;
        assert !p.debit(26) && p.dust == 25;
        assert !p.debit(-1) && !p.debit(0);
        assert p.debit(25) && p.dust == 0;
        assert !p.claim(0, 128, 35);
        p.counts[0] = 128;
        assert p.claim(0, 128, 35) && p.dust == 35;
        assert !p.claim(0, 128, 35) && p.dust == 35;
        p.rollover("2026-09-13"); assert p.claimed[0];
        p.rollover("2026-09-14"); assert !p.claimed[0] && p.counts[0] == 0 && p.dust == 35;
        p.event("first"); p.eventCount = 10; p.eventClaimed = true;
        p.event("first"); assert p.eventClaimed && p.eventCount == 10;
        p.event("second"); assert !p.eventClaimed && p.eventCount == 0;
        Progress copy = p.copy(); copy.dust = 999; copy.counts[0] = 999;
        assert p.dust == 35 && p.counts[0] == 0;
        var dir = Files.createTempDirectory("cosmos-tests-");
        ProgressStore store = new ProgressStore(dir); UUID id = UUID.randomUUID();
        p.started = true; p.counts[2] = 96; p.claim(2, 96, 35);
        store.save(id, p);
        Progress restored = new ProgressStore(dir).get(id);
        assert restored.started && restored.dust == 70 && restored.claimed[2];
        assert !restored.claim(2, 96, 35);
        UUID corrupt = UUID.randomUUID(); Files.writeString(dir.resolve(corrupt + ".yml"), "dust: [broken");
        boolean failed = false;
        try { new ProgressStore(dir).get(corrupt); } catch (Exception expected) { failed = true; }
        assert failed : "Corrupt wallet must not become a fresh starter wallet";
        System.out.println("PASS: debit, duplicate claims, daily rollover, event rollover, copy isolation, persistence, corrupt data fail-closed");
    }
}
