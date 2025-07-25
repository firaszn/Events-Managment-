package com.example.userservice.mapper;

import com.example.userservice.entity.UserEntity;
import com.example.userservice.model.UserRequest;
import com.example.userservice.model.UserUpdateRequest;
import com.example.userservice.model.UserResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class UserMapperTest {
    private UserMapper mapper;
    @BeforeEach
    void setUp() { mapper = new UserMapper(); }

    @Test
    void canInstantiate() {
        assertThat(mapper).isNotNull();
    }

    @Test
    void toUserResponse_returnsExpected() {
        UserEntity entity = new UserEntity();
        entity.setId(1L);
        entity.setEmail("test@example.com");
        entity.setUsernameField("user");
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setPhoneNumber("123");
        entity.setEnabled(true);
        entity.setRole(com.example.userservice.entity.UserEntity.Role.USER);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        UserResponse response = mapper.toUserResponse(entity);
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void toUserResponse_nullEntity() {
        assertThat(mapper.toUserResponse(null)).isNull();
    }

    @Test
    void toUserEntity_returnsExpected() {
        UserRequest req = new UserRequest();
        req.setUsername("user");
        req.setEmail("test@example.com");
        req.setPassword("pass");
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setPhoneNumber("123");
        req.setRole("USER");
        UserEntity entity = mapper.toUserEntity(req);
        assertThat(entity).isNotNull();
        assertThat(entity.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void toUserEntity_nullRequest() {
        assertThat(mapper.toUserEntity(null)).isNull();
    }

    @Test
    void updateUserEntity_updatesFields() {
        UserEntity entity = new UserEntity();
        UserUpdateRequest req = new UserUpdateRequest();
        req.setEmail("new@example.com");
        req.setUsername("newuser");
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setPhoneNumber("456");
        req.setRole("ADMIN");
        req.setEnabled(false);
        req.setPassword("pass");
        mapper.updateUserEntity(entity, req);
        assertThat(entity.getEmail()).isEqualTo("new@example.com");
        assertThat(entity.getUsernameField()).isEqualTo("newuser");
        assertThat(entity.getFirstName()).isEqualTo("Jane");
        assertThat(entity.getLastName()).isEqualTo("Smith");
        assertThat(entity.getPhoneNumber()).isEqualTo("456");
        assertThat(entity.getRole().name()).isEqualTo("ADMIN");
        assertThat(entity.isEnabled()).isFalse();
        assertThat(entity.getPassword()).isEqualTo("pass");
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateUserEntity_nulls() {
        UserEntity entity = new UserEntity();
        mapper.updateUserEntity(entity, null); // should not throw
        assertThat(entity.getEmail()).isNull();
        mapper.updateUserEntity(null, new UserUpdateRequest()); // should not throw
    }

    @Test
    void toUserResponseList_handlesNullAndList() {
        assertThat(mapper.toUserResponseList(null)).isEmpty();
        UserEntity entity = new UserEntity();
        entity.setEmail("a@b.com");
        List<UserResponse> list = mapper.toUserResponseList(List.of(entity));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getEmail()).isEqualTo("a@b.com");
    }
} 