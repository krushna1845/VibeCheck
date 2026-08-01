package com.krushna.moviebooking.auth.mapper;

import com.krushna.moviebooking.auth.dto.response.UserResponse;
import com.krushna.moviebooking.auth.entity.Role;
import com.krushna.moviebooking.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToStrings")
    UserResponse toResponse(User user);

    @Named("mapRolesToStrings")
    default Set<String> mapRolesToStrings(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }
}
