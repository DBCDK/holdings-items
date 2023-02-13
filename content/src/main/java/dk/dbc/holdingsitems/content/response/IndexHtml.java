package dk.dbc.holdingsitems.content.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@ApplicationScoped
@Startup
@LocalBean
public class IndexHtml {
    private static final Logger log = LoggerFactory.getLogger(IndexHtml.class);

    private byte[] indexHtml;

    @PostConstruct
    public void init() {
        ClassLoader loader = getClass().getClassLoader();
        try (InputStream is = loader.getResourceAsStream("content-service.html")) {
            indexHtml = new byte[0];
            while (true) {
                int avail = is.available();
                if (avail == 0)
                    break;
                int pos = indexHtml.length;
                indexHtml = Arrays.copyOf(indexHtml, pos + avail);
                is.read(indexHtml, pos, avail);
            }
        } catch (IOException ex) {
            log.error("Error loading index.html: {}", ex.getMessage());
            log.debug("Error loading index.html: ", ex);
            throw new RuntimeException(ex);
        }
    }

    public ByteArrayInputStream getInputStream() {
        return new ByteArrayInputStream(indexHtml);
    }

}
