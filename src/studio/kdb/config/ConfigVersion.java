package studio.kdb.config;

import java.time.LocalDate;

public enum ConfigVersion {
    V1_1(1.1, "2009-03-10"),
    V1_2(1.2, "2020-04-27"),
    V1_3(1.3, "2021-01-09"),
    V1_4(1.4, "2024-02-22"),
    V2_0(2.0, "2025-03-20");

    private final double version;
    private final LocalDate date;
    ConfigVersion(double version, String date) {
        this.version = version;
        this.date = LocalDate.parse(date);
    }

    @Override
    public String toString() {
        return version + " - " + date;
    }

    public String getVersion() {
        return "" + version;
    }
}
