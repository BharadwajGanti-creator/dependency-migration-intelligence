package com.dmi.api;

import com.dmi.domain.AnalysisRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class RequestValidator {

    private static final Pattern FIXTURE_ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,62}[a-z0-9]$|^[a-z0-9]$");
    private static final Pattern MAVEN_TOKEN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._\\-]{0,127}$");
    private static final Pattern VERSION = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._\\-]{0,63}$");

    public void validate(AnalysisRequest request) {
        rejectTraversal(request.fixtureId(), "fixtureId");
        rejectTraversal(request.dependencyGroup(), "dependencyGroup");
        rejectTraversal(request.dependencyArtifact(), "dependencyArtifact");
        rejectTraversal(request.fromVersion(), "fromVersion");
        rejectTraversal(request.toVersion(), "toVersion");

        if (!FIXTURE_ID.matcher(request.fixtureId()).matches()) {
            throw badRequest("INVALID_FIXTURE_ID", "fixtureId format is invalid");
        }
        if (!MAVEN_TOKEN.matcher(request.dependencyGroup()).matches()
                || !MAVEN_TOKEN.matcher(request.dependencyArtifact()).matches()) {
            throw badRequest("INVALID_COORDINATES", "dependency coordinates contain invalid characters");
        }
        if (!VERSION.matcher(request.fromVersion()).matches() || !VERSION.matcher(request.toVersion()).matches()) {
            throw badRequest("INVALID_VERSION", "version tokens contain invalid characters");
        }
        if (request.fromVersion().equals(request.toVersion())) {
            throw new AnalysisException(HttpStatus.UNPROCESSABLE_ENTITY, "SAME_VERSION", "fromVersion and toVersion must differ");
        }
    }

    private void rejectTraversal(String value, String field) {
        if (value.contains("..") || value.contains("/") || value.contains("\\") || value.indexOf('\0') >= 0) {
            throw badRequest("UNSAFE_INPUT", field + " contains disallowed characters");
        }
    }

    private AnalysisException badRequest(String code, String message) {
        return new AnalysisException(HttpStatus.BAD_REQUEST, code, message);
    }
}
