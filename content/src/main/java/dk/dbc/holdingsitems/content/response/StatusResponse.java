package dk.dbc.holdingsitems.content.response;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.ws.rs.core.Response;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class StatusResponse {

    public boolean ok;
    public String message;
    public String trackingId;

    public StatusResponse() {
    }

    public static Response ok(String trackingId) {
        return Response.ok(new StatusResponse(true, trackingId, null))
                .build();
    }

    public static Response ok(String trackingId, String message) {
        return Response.ok(new StatusResponse(true, trackingId, message))
                .build();
    }

    public static Response notFound(String trackingId, String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .header("X-DBC-Status", "200 OK")
                .entity(new StatusResponse(false, trackingId, message))
                .build();
    }

    public static Response error(String trackingId, Response.Status status, String message) {
        return Response.status(status)
                .entity(new StatusResponse(false, trackingId, message))
                .build();
    }

    private StatusResponse(boolean ok, String trackingId, String message) {
        this.ok = ok;
        this.message = message;
        this.trackingId = trackingId;
    }

    @Override
    public String toString() {
        return "StatusResponse{" + "ok=" + ok + ", message=" + message + '}';
    }
}
