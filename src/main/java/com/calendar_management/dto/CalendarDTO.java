package com.calendar_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CalendarDTO {
    private String id;
    private String summary;
    private String description;
    private String timeZone;
    private String primary; // "true"/"false" para simplificar
}
