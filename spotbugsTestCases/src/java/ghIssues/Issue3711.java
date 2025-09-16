package ghIssues;

import jakarta.servlet.http.HttpServletRequest;

public class Issue3711 {
    public String provideTransactionName(HttpServletRequest request) {
        return String.format("%s %s", request.getMethod(), request.getRequestURI());
    }
}
