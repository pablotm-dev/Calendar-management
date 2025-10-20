package com.calendar_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventDTO {
    private String id;
    private String summary;
    private String organizerEmail;
    private String htmlLink;
    private String start; // ISO-8601
    private String end;   // ISO-8601
    private String location;
}
