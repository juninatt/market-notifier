package se.pbt.ddplus.core.schedule;

/**
 * Represents predefined schedule presets that are translated into cron expressions.
 */
public enum SchedulePreset {
    MORNING("0 0 8 * * *"),
    EVENING("0 0 20 * * *"),
    MORNING_EVENING("0 0 8,20 * * *"),
    MORNING_LUNCH_EVENING("0 0 8,12,20 * * *");

    private final String cron;

    SchedulePreset(String cron) {
        this.cron = cron;
    }

    public String getCron() {
        return cron;
    }
}

