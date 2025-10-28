package se.pbt.tvm.core.subscription;

import lombok.Getter;

/**
 * Represents predefined schedule presets that are translated into cron expressions.
 */
@Getter
public enum SchedulePreset {
    MORNING("0 0 8 * * *"),
    EVENING("0 0 20 * * *"),
    MORNING_EVENING("0 0 8,20 * * *"),
    MORNING_LUNCH_EVENING("0 0 8,12,20 * * *");

    private final String cron;

    SchedulePreset(String cron) {
        this.cron = cron;
    }
}

