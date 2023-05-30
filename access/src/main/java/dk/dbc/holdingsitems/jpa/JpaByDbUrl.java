/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-access
 *
 * holdings-items-access is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-access is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.jpa;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.persistence.config.PersistenceUnitProperties.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public final class JpaByDbUrl {

    private static final Logger log = LoggerFactory.getLogger(JpaByDbUrl.class);

    private static final Pattern DB_URL = Pattern.compile("^(?:postgres(?:ql)?)?(?:([^/@]+)(?::([^@]+))@)?([^:/]+(?::[1-9][0-9]*)?/(\\w+))(?:\\?(\\w+=[^&]*(?:&\\w+=[^&]*)*))?$");
    private static final String DRIVER = "org.postgresql.Driver";
    private static final String PU = "holdingsItemsManual_PU";

    @FunctionalInterface
    public interface EntityManagerBlock<T> {

        T run(EntityManager em) throws Exception;
    }

    @FunctionalInterface
    public interface EntityManagerVoidBlock {

        void run(EntityManager em) throws Exception;
    }

    private final HashMap<String, String> properties;
    private final EntityManagerFactory factory;
    private final EntityManager manager;

    public JpaByDbUrl(String db) {
        this(db, "holdings-items-cli");
    }

    public JpaByDbUrl(String db, String appName) {
        try {
            getClass().getClassLoader().loadClass(DRIVER);

            Matcher matcher = DB_URL.matcher(db);
            if (!matcher.matches())
                throw new IllegalArgumentException("Url is not of database type");
            String username = matcher.group(1);
            String password = matcher.group(2);
            String url = matcher.group(3);
            String opts = matcher.group(4);

            if (opts == null) {
                opts = "ApplicationName=" + appName;
            } else if (!( Arrays.stream(opts.split("&"))
                         .anyMatch(s -> s.startsWith("ApplicationName=")) )) {
                opts += "&ApplicationName=" + appName;
            }

            properties = new HashMap<>();
            properties.put(JDBC_USER, username == null ? "" : URLDecoder.decode(username, "UTF-8"));
            properties.put(JDBC_PASSWORD, password == null ? "" : URLDecoder.decode(password, "UTF-8"));
            properties.put(JDBC_URL, "jdbc:postgresql://" + url + "?" + opts);
            properties.put(JDBC_DRIVER, "org.postgresql.Driver");
            properties.put("eclipselink.logging.level", getEclipseLinkLogLevel());

            factory = Persistence.createEntityManagerFactory(PU, properties);
            manager = factory.createEntityManager(properties);
        } catch (UnsupportedEncodingException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public <T> T run(EntityManagerBlock<T> func) {
        EntityTransaction transaction = manager.getTransaction();
        transaction.begin();
        try {
            T ret = func.run(manager);
            transaction.commit();
            return ret;
        } catch (RuntimeException ex) {
            transaction.rollback();
            throw ex;
        } catch (Exception ex) {
            transaction.rollback();
            throw new RuntimeException(ex);
        }
    }

    public void run(EntityManagerVoidBlock func) {
        EntityTransaction transaction = manager.getTransaction();
        transaction.begin();
        try {
            func.run(manager);
            transaction.commit();
        } catch (RuntimeException ex) {
            transaction.rollback();
            throw ex;
        } catch (Exception ex) {
            transaction.rollback();
            throw new RuntimeException(ex);
        }
    }

    private String getEclipseLinkLogLevel() {
        if (log.isTraceEnabled()) {
            return "FINER";
        } else if (log.isDebugEnabled()) {
            return "FINE";
        } else if (log.isInfoEnabled()) {
            return "INFO";
        } else if (log.isWarnEnabled()) {
            return "WARNING";
        } else {
            return "SEVERE";
        }
    }
}
