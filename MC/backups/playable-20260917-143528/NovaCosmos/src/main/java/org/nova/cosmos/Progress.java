package org.nova.cosmos;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

/** Pure progression rules; no Bukkit dependency. */
public final class Progress {
    public long dust;
    public boolean started;
    public String pendingGear = "", deliveryId = "";
    public String day = "";
    public final int[] counts = new int[3];
    public final boolean[] claimed = new boolean[3];
    public String eventId = "";
    public int eventCount;
    public boolean eventClaimed;
    public static String today() { return LocalDate.now(ZoneId.of("Europe/Istanbul")).toString(); }
    public void rollover(String date) {
        if (day.equals(date)) return;
        day = date;
        Arrays.fill(counts, 0); Arrays.fill(claimed, false);
    }
    public void event(String id) {
        if (eventId.equals(id)) return;
        eventId = id; eventCount = 0; eventClaimed = false;
    }
    public boolean debit(int cost) {
        if (cost <= 0 || dust < cost) return false;
        dust -= cost; return true;
    }
    public boolean claim(int type, int target, int reward) {
        if (type < 0 || type >= 3 || target <= 0 || reward <= 0 || claimed[type] || counts[type] < target) return false;
        claimed[type] = true; dust += reward; return true;
    }
    public Progress copy() {
        Progress p = new Progress();
        p.dust = dust; p.started = started; p.day = day; p.eventId = eventId;
        p.pendingGear = pendingGear; p.deliveryId = deliveryId;
        p.eventCount = eventCount; p.eventClaimed = eventClaimed;
        System.arraycopy(counts, 0, p.counts, 0, 3);
        System.arraycopy(claimed, 0, p.claimed, 0, 3);
        return p;
    }
}
