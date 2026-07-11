package site.vnstyz.myblog.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import site.vnstyz.myblog.entity.User;

@Mapper
public interface UserMapper {

    User findByUsername(@Param("username") String username);

    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
