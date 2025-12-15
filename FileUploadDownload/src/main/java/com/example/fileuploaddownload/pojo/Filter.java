package com.example.fileuploaddownload.pojo;

public record Filter(
        String fieldName,
        String fieldDataType,     // text, date, dropdown, number
        Operator operator,
        Object fieldValue
) {}
