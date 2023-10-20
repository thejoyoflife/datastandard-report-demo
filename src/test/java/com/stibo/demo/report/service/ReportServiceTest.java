package com.stibo.demo.report.service;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stibo.demo.report.model.Datastandard;

@SpringBootTest
@ContextConfiguration(classes = {ReportService.class, ObjectMapper.class})
public class ReportServiceTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ReportService reportService;

    private Datastandard datastandard;

    @BeforeEach
    public void before() throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("datastandard.json");
        this.datastandard = objectMapper.readValue(stream, Datastandard.class);
    }

    @Test
    public void testReport() {
        List<List<String>> report = reportService.report(datastandard, "leaf").map(row -> row.collect(toList())).collect(toList());
    }
}
