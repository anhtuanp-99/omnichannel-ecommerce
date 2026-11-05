package com.omni.identity.audit;

import com.omni.common.constant.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

/** Ghi audit log — dùng REQUIRES_NEW để audit không bị rollback theo transaction chính. */
@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID actorId, String action, String targetType, String targetId,
                    Map<String, Object> delta, String result) {
        AuditLog a = new AuditLog();
        a.setActorId(actorId);
        a.setAction(action);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setDelta(delta);
        a.setResult(result);
        a.setCorrelationId((String) MDC.get("correlationId"));

        HttpServletRequest req = currentRequest();
        if (req != null) {
            a.setIpAddress(clientIp(req));
            a.setUserAgent(req.getHeader("User-Agent"));
        }
        repo.save(a);
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes s ? s.getRequest() : null;
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return (xff != null) ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}