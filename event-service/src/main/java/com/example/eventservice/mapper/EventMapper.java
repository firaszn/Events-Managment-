package com.example.eventservice.mapper;

import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.model.EventRequest;
import com.example.eventservice.model.EventResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {
    EventEntity toEntity(EventRequest request);

    EventResponse toResponse(EventEntity entity);

    List<EventResponse> toResponseList(List<EventEntity> entities);
} 