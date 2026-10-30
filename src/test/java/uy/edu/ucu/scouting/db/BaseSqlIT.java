package uy.edu.ucu.scouting.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MSSQLServerContainer;

/** SQL Server real en Docker con las migraciones aplicadas. */
abstract class BaseSqlIT {

    static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    static String url;

    @BeforeAll
    static void iniciar() throws Exception {
        if (url != null) {
            return;
        }
        SQL.start();
        try (Connection c = DriverManager.getConnection(SQL.getJdbcUrl(), SQL.getUsername(), SQL.getPassword());
             Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE ScoutingTest");
        }
        url = SQL.getJdbcUrl() + ";databaseName=ScoutingTest";
        Flyway.configure()
                .dataSource(url, SQL.getUsername(), SQL.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    static Connection conexion() throws Exception {
        return DriverManager.getConnection(url, SQL.getUsername(), SQL.getPassword());
    }
}
