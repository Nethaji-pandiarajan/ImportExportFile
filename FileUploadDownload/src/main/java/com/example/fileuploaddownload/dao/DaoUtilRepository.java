package com.example.fileuploaddownload.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DaoUtilRepository {

    private final JdbcTemplate jdbc;

    public DaoUtilRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;


    }


}
