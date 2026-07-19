package vn.thanhtuanle.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.thanhtuanle.common.exception.AppException;
import vn.thanhtuanle.common.exception.ErrorCode;
import vn.thanhtuanle.common.payload.PageResponse;
import vn.thanhtuanle.entity.User;
import vn.thanhtuanle.user.dto.UserResponse;
import vn.thanhtuanle.user.mapper.UserMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceListTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserMapper userMapper;

    @InjectMocks UserService userService;

    @Test
    void list_lowercasesSearch_convertsDayBounds_mapsResults() {
        User user = User.builder().username("alice").build();
        user.setId(UUID.randomUUID());
        Page<User> page = new PageImpl<>(List.of(user));
        UserResponse dto = new UserResponse();
        when(userRepository.search(eq("ab"), isNull(), isNull(),
                eq(LocalDate.of(2026, 7, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 7, 11).atStartOfDay()), any(Pageable.class))).thenReturn(page);
        when(userMapper.toResponse(user)).thenReturn(dto);

        PageResponse<UserResponse> result = userService.list(0, 10, "Ab", null, null,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10));

        assertThat(result.getData()).containsExactly(dto);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void list_blankSearch_passesNull() {
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        userService.list(0, 10, "   ", null, null, null, null);

        verify(userRepository).search(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getById_unknown_throwsUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void getById_found_returnsMappedDto() {
        UUID id = UUID.randomUUID();
        User user = User.builder().username("bob").build();
        user.setId(id);
        UserResponse dto = new UserResponse();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(dto);

        assertThat(userService.getById(id)).isSameAs(dto);
    }
}
