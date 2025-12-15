package com.example.fileuploaddownload.pojo;

import java.util.List;

public record ExportRequest(
        List<String> selectColumns,
        String format,
        boolean async,
        List<Filter> filters,
        SortRequest sort,
        Integer page,
        Integer size
) {}

