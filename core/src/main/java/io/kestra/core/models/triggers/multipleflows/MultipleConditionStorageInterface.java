package io.kestra.core.models.triggers.multipleflows;

import io.kestra.core.models.flows.Flow;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public interface MultipleConditionStorageInterface {
    Optional<MultipleConditionWindow> get(Flow flow, String conditionId);

    List<MultipleConditionWindow> expired(String tenantId);

    default MultipleConditionWindow getOrCreate(Flow flow, MultipleCondition multipleCondition) {

        ZonedDateTime start;
        ZonedDateTime end;

        ZonedDateTime now = ZonedDateTime.now()
            .withNano(0);

        if (multipleCondition.getLatencySLA() != null) {
            // with latency, the start is always the start of the day
            start = now.truncatedTo(ChronoUnit.DAYS);
            end = start.plusSeconds(multipleCondition.getLatencySLA().getDeadline().toSecondOfDay());
        } else {
            if (multipleCondition.getWindow().toDays() > 0) {
                now = now.withHour(0);
            }

            if (multipleCondition.getWindow().toHours() > 0) {
                now = now.withMinute(0);
            }

            if (multipleCondition.getWindow().toMinutes() > 0) {
                now = now.withSecond(0)
                    .withMinute(0)
                    .plusMinutes(multipleCondition.getWindow().toMinutes() * (now.getMinute() / multipleCondition.getWindow().toMinutes()));
            }

            start = multipleCondition.getWindowAdvance() == null ? now : now.plus(multipleCondition.getWindowAdvance()).truncatedTo(ChronoUnit.MILLIS);
            end = start.plus(multipleCondition.getWindow()).minus(Duration.ofMillis(1)).truncatedTo(ChronoUnit.MILLIS);
        }

        return this.get(flow, multipleCondition.getId())
            .filter(m -> m.isValid(ZonedDateTime.now()))
            .orElseGet(() -> MultipleConditionWindow.builder()
                .namespace(flow.getNamespace())
                .flowId(flow.getId())
                .tenantId(flow.getTenantId())
                .conditionId(multipleCondition.getId())
                .start(start)
                .end(end)
                .results(new HashMap<>())
                .build()
            );
    }

    void save(List<MultipleConditionWindow> multipleConditionWindows);

    void delete(MultipleConditionWindow multipleConditionWindow);
}
