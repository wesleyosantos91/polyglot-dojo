package io.github.wesleyosantos91.core.mapper;

import io.github.wesleyosantos91.api.request.PersonPatchRequest;
import io.github.wesleyosantos91.api.request.PersonRequest;
import io.github.wesleyosantos91.api.response.PersonResponse;
import io.github.wesleyosantos91.domain.entity.PersonEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface PersonMapper {

    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    PersonEntity toEntity(PersonRequest request);

    PersonResponse toResponse(PersonEntity entity);

    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void updateEntity(PersonRequest request, @MappingTarget PersonEntity entity);

    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void patchEntity(PersonPatchRequest request, @MappingTarget PersonEntity entity);
}