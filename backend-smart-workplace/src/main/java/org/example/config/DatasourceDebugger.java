package org.example.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
public class DatasourceDebugger {

    private final DataSource dataSource;

    @PostConstruct
    public void printDatasource() throws Exception {
        System.out.println(">>> DATASOURCE = " + dataSource.getConnection().getMetaData().getURL());
    }
}
