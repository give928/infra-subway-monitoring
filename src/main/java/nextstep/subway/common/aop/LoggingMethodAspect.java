package nextstep.subway.common.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import nextstep.subway.common.annotation.LoggingMethod;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static net.logstash.logback.marker.Markers.*;

@Component
@Aspect
public class LoggingMethodAspect {
    private static final String TRACE_ID = "traceId";
    private static final String TRACE_SIGNATURE = "signature";
    private static final Logger out = LoggerFactory.getLogger("out");
    private static final Logger error = LoggerFactory.getLogger("error");
    private static final Logger json = LoggerFactory.getLogger("json");

    @Before("@annotation(nextstep.subway.common.annotation.LoggingMethod) && @annotation(loggingMethod)")
    public void doBefore(JoinPoint joinPoint, LoggingMethod loggingMethod) {
        MDC.put(TRACE_ID, UUID.randomUUID().toString());
        MDC.put(TRACE_SIGNATURE, joinPoint.getSignature().toString());

        out.info("[before][{}] {}", MDC.get(TRACE_ID), MDC.get(TRACE_SIGNATURE));

        if (loggingMethod.json()) {
            logJsonArguments(joinPoint);
        }
    }

    @AfterReturning(value = "@annotation(nextstep.subway.common.annotation.LoggingMethod) && @annotation(loggingMethod)", returning = "result")
    public void doReturn(JoinPoint joinPoint, LoggingMethod loggingMethod, Object result) {
        out.info("[return][{}] {}", MDC.get(TRACE_ID), joinPoint.getSignature());

        if (loggingMethod.json()) {
            logJsonResult(result);
        }
    }

    @AfterThrowing(value = "@annotation(nextstep.subway.common.annotation.LoggingMethod)", throwing = "ex")
    public void doThrowing(JoinPoint joinPoint, Exception ex) {
        error.error("[exception][{}] {}", MDC.get(TRACE_ID), joinPoint.getSignature(), ex);
    }

    @After("@annotation(nextstep.subway.common.annotation.LoggingMethod)")
    public void doAfter(JoinPoint joinPoint) {
        out.info("[after][{}] {}", MDC.get(TRACE_ID), joinPoint.getSignature());
        MDC.clear();
    }

    private void logJsonArguments(JoinPoint joinPoint) {
        json.info(makeArgumentsMarker(joinPoint), "arguments");
    }

    private void logJsonResult(Object result) {
        if (result != null) {
            json.info(append(result.getClass().getSimpleName(), result), "return");
        }
    }

    private LogstashMarker makeArgumentsMarker(JoinPoint joinPoint) {
        CodeSignature methodSignature = (CodeSignature) joinPoint.getSignature();
        String[] parameterNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        LogstashMarker logstashMarker = Markers.empty();
        IntStream.range(0, parameterNames.length)
                .forEach(i -> logstashMarker.and(makeArgumentMarker(parameterNames[i], args[i])));
        return logstashMarker;
    }

    private LogstashMarker makeArgumentMarker(String parameterName, Object arg) {
        if (arg.getClass().getSimpleName().endsWith("List")) {
            return appendRaw(parameterName, convertJsonString((List<?>) arg));
        }
        if (arg.getClass().getSimpleName().endsWith("Map")) {
            return append(parameterName, appendEntries((Map<?, ?>) arg));
        }
        return append(parameterName, arg);
    }

    private String convertJsonString(List<?> values) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonContent = null;
        try {
            jsonContent = mapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            error.error("[exception][{}]", MDC.get(TRACE_ID), e);
        }
        return jsonContent;
    }
}
