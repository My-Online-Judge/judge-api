package vn.thanhtuanle.user.mapper;

import org.mapstruct.Mapper;
import vn.thanhtuanle.entity.User;
import vn.thanhtuanle.user.dto.UserResponse;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
